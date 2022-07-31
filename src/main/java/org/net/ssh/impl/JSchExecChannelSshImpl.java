package org.net.ssh.impl;

import com.jcraft.jsch.ChannelExec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.net.ssh.JSchSupport;
import org.net.ssh.Ssh;
import org.net.util.Assert;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * {@link ChannelExec}，一次只能执行一条命令
 *
 * @author xiangqian
 * @date 13:11 2022/07/23
 */
@Slf4j
public class JSchExecChannelSshImpl extends JSchSupport implements Ssh {

    private JSchExecChannelSshImpl() {
    }

    @Override
    public void execute(String cmd, Duration timeout, Consumer<String> consumer) throws Exception {
        Assert.notNull(cmd, "命令不能为null");
        ChannelExec channel = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            // 打开一个执行通道
            channel = openExecChannel();

            // 获取输入流
            is = channel.getInputStream();

            // 设置命令
            channel.setCommand(cmd);

            // 连接并执行命令
            connectChannel(channel);

            // 线程阻塞等待，给时间让服务器端将结果输出到客户端
            TimeUnit.MILLISECONDS.sleep(timeout.toMillis());

            // 读取命令执行结果
            byte[] buffer = is.readAllBytes();
            reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer, 0, buffer.length), StandardCharsets.UTF_8));
            String line = null;
            while (Objects.nonNull(line = reader.readLine())) {
                consumer.accept(line);
            }
        } finally {
            IOUtils.closeQuietly(reader, is);
            close(channel);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends JSchSupport.Builder<Builder, JSchExecChannelSshImpl> {
        private Builder() {
        }

        @Override
        protected JSchExecChannelSshImpl get() {
            return new JSchExecChannelSshImpl();
        }
    }

}
