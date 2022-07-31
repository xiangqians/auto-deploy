package org.net.ssh.impl;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.net.ssh.ConnectionProperties;
import org.net.ssh.JSchSupport;
import org.net.ssh.Ssh;
import org.net.util.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * {@link ChannelShell}，可执行多行命令
 *
 * @author xiangqian
 * @date 22:37 2022/07/25
 */
@Slf4j
public class JSchShellChannelSshImpl extends JSchSupport implements Ssh, Runnable {

    private final String FINISH = new String();

    // shell通道
    private volatile ChannelShell channel;

    // 命令队列
    private BlockingQueue<CmdEntry> cmdEntryQueue;
    private final String CMD_EXIT = "exit";

    // 命令执行结果队列
    private BlockingQueue<String> resultQueue;

    // 输入流
    private volatile InputStream is;

    // 使用PrintWriter为了使用println方法，从而不需要在写入字符时添加\n
    private volatile PrintWriter writer;

    // 执行命令线程循环标识
    private volatile boolean flag;

    // 快速结束标识模式
    private List<Pattern> quickEndSignPatterns;

    // shell是否已准备
    private AtomicBoolean ready;

    private JSchShellChannelSshImpl() {
    }

    @Override
    public void execute(String cmd, Duration timeout, Consumer<String> consumer) throws Exception {
        Assert.notNull(cmd, "命令不能为null");

        // 等待shell初始化完成，最久等待5m
        long max = 10 * 100 * 60 * 5;
        while (max-- > 0 && !ready.get()) {
            TimeUnit.MILLISECONDS.sleep(10);
        }

        // 将命令添加到队列，添加成功返回true，添加失败或者超时返回false
        Assert.isTrue(cmdEntryQueue.offer(new CmdEntry(cmd, timeout), timeout.toMillis(), TimeUnit.MILLISECONDS), String.format("命令执行超时: %s", cmd));

        // 继续从队列中获取元素，直到队列中没有元素或者等待超时后返回null
        String result = null;
        while (Objects.nonNull(result = resultQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)) && result != FINISH) {
            consumer.accept(result);
        }
    }

    @Override
    protected void init(ConnectionProperties connectionProperties, Duration sessionConnectTimeout, Duration channelConnectTimeout) throws JSchException {
        super.init(connectionProperties, sessionConnectTimeout, channelConnectTimeout);

        // 初始化channel
        channel = openShellChannel();
        cmdEntryQueue = new ArrayBlockingQueue<>(1);
        resultQueue = new ArrayBlockingQueue<>(1);
        flag = true;
        ready = new AtomicBoolean(false);
        new Thread(this).start();
    }

    public void run0() throws Exception {
        // 获取输入流
        is = channel.getInputStream();
        // 初始化输出流
        writer = new PrintWriter(channel.getOutputStream());

        // pty
        channel.setPty(true);
        // connect
        connectChannel(channel);

        // 线程阻塞等待，给时间让服务器端将结果输出到客户端
        TimeUnit.MILLISECONDS.sleep(200);

        // 是否继续尝试从输入流中读取数据
        AtomicBoolean tryBool = new AtomicBoolean(true);

        // ###
        int maxLength = 1024;
        byte[] buffer = new byte[maxLength];
        // 执行的命令
        CmdEntry cmdEntry = null;
        // 尝试计数
        long tryCount = 0;
        // 尝试从输入流中获取执行结果最大计数
        long maxTryCount = 20;
        // 每次尝试从输入流中读取完数据（或许未读取到数据）等待时间
        Duration tryWaitTime = Duration.ofMillis(100);
        while (flag) {
            if (Objects.isNull(channel)) {
                break;
            }

            // 如果channel已关闭时
            if (channel.isClosed()) {
                String exitStatus = String.format("exit-status: %s", Optional.ofNullable(channel).map(Channel::getExitStatus).orElse(null));
                System.out.println(exitStatus);
                break;
            }

            // 如果channel还未关闭时
            // try
            while (tryBool.get() && tryCount++ < maxTryCount) {
                // 尝试从输入流获取执行结果
                // 读取输入流可用的数据，循环获取可用数据大小（避免阻塞）
                while (Objects.nonNull(is) && is.available() != 0) {
                    // 读取执行结果到缓存数组
                    int length = Objects.nonNull(is) ? is.read(buffer, 0, maxLength) : -1;
                    if (length == -1) {
                        break;
                    }

                    // 处理命令执行结果
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer, 0, length), StandardCharsets.UTF_8));
                        String result = null;
                        while (tryBool.get() && (result = reader.readLine()) != null) {
                            if (CollectionUtils.isNotEmpty(quickEndSignPatterns)) {
                                for (Pattern quickEndSignPattern : quickEndSignPatterns) {
                                    if (quickEndSignPattern.matcher(result).matches()) {
                                        tryBool.set(false);
                                        result = FINISH;
                                        break;
                                    }
                                }
                            }

                            if (Objects.nonNull(cmdEntry)) {
                                // 将命令执行结果添加到队列，添加成功返回true，添加失败或者超时返回false
                                resultQueue.offer(result, cmdEntry.getTimeout().toMillis(), TimeUnit.MILLISECONDS);

                                // 清空命令队列（只移除一个头部元素），使得其他命令能够入队
                                if (result == FINISH) {
                                    cmdEntryQueue.poll();
                                }
                            } else {
                                System.out.println(result);
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(reader);
                    }
                }

//                log.debug("tryBool={}, tryCount={}, maxTryCount={}, cmdEntry={}", tryBool.get(), tryCount, maxTryCount, cmdEntry);
                if (tryBool.get() && tryCount < maxTryCount) {
                    // 线程阻塞等待，给时间让服务器端将结果输出到客户端
                    TimeUnit.MILLISECONDS.sleep(tryWaitTime.toMillis());
                }
            }

            // 当执行命令不为空时，
            if (Objects.nonNull(cmdEntry) && tryBool.get()) {
                // 向命令执行结果队列添加结束标识元素
                // 将命令添加到队列，添加成功返回true，添加失败或者超时返回false
                resultQueue.offer(FINISH, cmdEntry.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                // 清空命令队列（只移除一个头部元素），使得其他命令能够入队
                cmdEntryQueue.poll();
            }

            // 初始化完毕
            if (Objects.isNull(cmdEntry)) {
                ready.set(true);
            }

            // 从队列中获取命令，如果队列为空，则阻塞，直至有数据
//            CmdEntry preCmdEntry = cmdEntryQueue.take();
            // 继续从队列中获取元素，直到队列中没有元素或者等待超时后返回null
//            Duration cmdDequeueTimeout = Duration.ofSeconds(5);
//            CmdEntry preCmdEntry = cmdEntryQueue.poll(cmdDequeueTimeout.toMillis(), TimeUnit.MILLISECONDS);
            // 返回队列头部的元素（并不移除），如果队列为空，则返回null
            Duration cmdDequeueTimeout = Duration.ofMillis(10);
            CmdEntry preCmdEntry = cmdEntryQueue.peek();
//            log.debug("preCmdEntry {}", preCmdEntry);
            if (Objects.isNull(preCmdEntry)) {
                tryBool.set(false);
                TimeUnit.MILLISECONDS.sleep(cmdDequeueTimeout.toMillis());
                continue;
            }

            if (Objects.isNull(is)) {
                break;
            }
            // 跳过输入流中可用的字节数（即，舍弃输入流中可用的字节数）
            is.skip(is.available());

            // 重置尝试标识
            tryBool.set(true);

            // 发送命令
            sendCmd(preCmdEntry.getCmd());

            // 重置尝试计数
            tryCount = 0;

            // 设置最大尝试次数
            Duration timeout = preCmdEntry.getTimeout();
//            long millis = timeout.toMillis();
            long millis = (long) (timeout.toMillis() * 0.8);
            maxTryCount = millis / tryWaitTime.toMillis();
            if (millis % tryWaitTime.toMillis() != 0) {
                maxTryCount += 1;
            }

            //
            cmdEntry = preCmdEntry;
        }
    }

    private void sendCmd(String cmd) {
        if (Objects.nonNull(cmd) && Objects.nonNull(writer)) {
            // write
            writer.println(cmd);
            // 刷新缓冲区（把缓冲区数据强行输出）
            writer.flush();
        }
    }

    @Override
    public void run() {
        try {
            run0();
        } catch (Exception e) {
            log.error("", e);
        } finally {
            IOUtils.closeQuietly(this);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (Objects.isNull(channel)) {
            return;
        }

        // 结束循环
        flag = false;

        try {
            // 发送exit命令，结束本次交互
            sendCmd(CMD_EXIT);

            // 读取执行结果
            long tryCount = 0;
            long maxTryCount = 10;
            Duration tryWaitTime = Duration.ofMillis(10);
            int maxLength = 1024;
            byte[] buffer = new byte[maxLength];
            try_label:
            while (tryCount++ < maxTryCount) {
                while (is.available() != 0) {
                    int length = is.read(buffer, 0, maxLength);
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer, 0, length), StandardCharsets.UTF_8));
                        String result = null;
                        while ((result = reader.readLine()) != null) {
                            System.out.println(result);
                            if (result.contains("logout")) {
                                break try_label;
                            }
                        }
                    } finally {
                        IOUtils.closeQuietly(reader);
                    }
                }
                if (tryCount < maxTryCount) {
                    // 线程阻塞等待，给时间让服务器端将结果输出到客户端
                    TimeUnit.MILLISECONDS.sleep(tryWaitTime.toMillis());
                }
            }

        } catch (Exception e) {
            log.debug("", e);
        } finally {
            try {
                // 关闭输入流和输出流
                IOUtils.closeQuietly(is, writer);
                is = null;
                writer = null;

                // 关闭channel
                close(channel);
                channel = null;
            } finally {
                super.close();
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class CmdEntry {
        private String cmd;
        private Duration timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JSchSupport.Builder<Builder, JSchShellChannelSshImpl> {
        private List<Pattern> quickEndSignPatterns;

        private Builder() {
        }

        public Builder quickEndSignPatterns(Pattern... quickEndSignPatterns) {
            this.quickEndSignPatterns = Optional.ofNullable(quickEndSignPatterns).map(Arrays::asList).orElse(null);
            return this;
        }

        public Builder defaultQuickEndSignPatterns() {
            // 以 "$ "结尾的命令执行结果
            // (.*)(\$ )$
            return quickEndSignPatterns(Pattern.compile("(.*)(\\$ )$"),
                    // 以 "]# "结尾的命令执行结果
                    // (.*)(\]# )$
                    Pattern.compile("(.*)(\\]# )$"));
        }

        @Override
        protected JSchShellChannelSshImpl get() {
            JSchShellChannelSshImpl jschShellChannelSshImpl = new JSchShellChannelSshImpl();
            jschShellChannelSshImpl.quickEndSignPatterns = quickEndSignPatterns;
            return jschShellChannelSshImpl;
        }
    }

}
