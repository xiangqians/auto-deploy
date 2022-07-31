package org.net.ssh;

import com.jcraft.jsch.*;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.net.util.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * JSch
 * http://www.jcraft.com/jsch/
 *
 * @author xiangqian
 * @date 13:50 2022/07/23
 */
public abstract class JSchSupport implements Closeable {

    protected ConnectionProperties connectionProperties;
    protected Duration sessionConnectTimeout;
    protected Duration channelConnectTimeout;
    protected Session session;

    protected void init(ConnectionProperties connectionProperties,
                        Duration sessionConnectTimeout,
                        Duration channelConnectTimeout) throws JSchException {
        this.connectionProperties = connectionProperties;
        this.sessionConnectTimeout = sessionConnectTimeout;
        this.channelConnectTimeout = channelConnectTimeout;

        JSch jSch = new JSch();
        // host & port
        // username & password
        session = jSch.getSession(connectionProperties.getUsername(), connectionProperties.getHost(), connectionProperties.getPort());
        session.setPassword(connectionProperties.getPassword());
        // 跳过公钥检测
        session.setConfig("StrictHostKeyChecking", "no");

        // connect
        session.connect((int) sessionConnectTimeout.toMillis());
        Assert.isTrue(session.isConnected(), "session连接失败");
    }

    /**
     * {@link ChannelExec}，用于执行命令（执行单行命令）
     *
     * @return
     * @throws JSchException
     */
    public ChannelExec openExecChannel() throws JSchException {
        Assert.isTrue(session.isConnected(), "session未连接");
        ChannelExec channel = (ChannelExec) session.openChannel(ChannelType.EXEC.getValue());
        channel.setErrStream(System.err);
        return channel;
    }

    /**
     * {@link ChannelShell}，用于执行命令（可以执行多行命令）
     *
     * @return
     * @throws JSchException
     */
    public ChannelShell openShellChannel() throws JSchException {
        Assert.isTrue(session.isConnected(), "session未连接");
        ChannelShell channel = (ChannelShell) session.openChannel(ChannelType.SHELL.getValue());
        return channel;
    }

    /**
     * {@link ChannelSftp}，用于上传、下载文件
     *
     * @return
     * @throws JSchException
     */
    public ChannelSftp openSftpChannel() throws JSchException {
        Assert.isTrue(session.isConnected(), "session未连接");
        ChannelSftp channel = (ChannelSftp) session.openChannel(ChannelType.SFTP.getValue());
        return channel;
    }

    public void connectChannel(Channel channel) throws JSchException {
        channel.connect((int) channelConnectTimeout.toMillis());
        Assert.isTrue(channel.isConnected(), "channel连接失败");
    }

    public static List<String> inputStreamToStrList(InputStream is, Function<String, String> function) throws IOException {
        byte[] buffer = is.readAllBytes();
        return byteArrayToStrList(buffer, 0, buffer.length, function);
    }

    /**
     * @param buffer
     * @param offset
     * @param length
     * @param function 命令行执行结果处理函数，Function<命令行处理结果, 自定义处理结果>
     * @return
     * @throws IOException
     */
    public static List<String> byteArrayToStrList(byte[] buffer, int offset, int length, Function<String, String> function) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buffer, offset, length), StandardCharsets.UTF_8));
            List<String> results = new ArrayList<>();
            String line = null;
            while ((line = reader.readLine()) != null) {
                String result = Objects.nonNull(function) ? function.apply(line) : line;
                if (Objects.isNull(result)) {
                    break;
                }
                results.add(result);
            }
            return results;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static void close(Channel channel) {
        if (Objects.nonNull(channel)) {
            channel.disconnect();
        }
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(session)) {
            session.disconnect();
            session = null;
        }
    }

    protected static abstract class Builder<B extends Builder, J extends JSchSupport> {

        protected ConnectionProperties connectionProperties;
        protected Duration sessionConnectTimeout;
        protected Duration channelConnectTimeout;

        public B connectionProperties(ConnectionProperties connectionProperties) {
            this.connectionProperties = connectionProperties;
            return (B) this;
        }

        public B sessionConnectTimeout(Duration channelConnectTimeout) {
            this.sessionConnectTimeout = channelConnectTimeout;
            return (B) this;
        }

        public B channelConnectTimeout(Duration channelConnectTimeout) {
            this.channelConnectTimeout = channelConnectTimeout;
            return (B) this;
        }

        protected abstract J get();

        public J build() throws JSchException {
            Assert.notNull(connectionProperties, "连接配置信息不能为空");
            Assert.notNull(sessionConnectTimeout, "session连接超时时间不能为空");
            Assert.notNull(channelConnectTimeout, "channel连接超时时间不能为空");
            J j = get();
            j.init(connectionProperties, sessionConnectTimeout, channelConnectTimeout);
            return j;
        }
    }

    /**
     * JSch通道类型
     *
     * @author xiangqian
     * @date 02:33 2022/07/24
     */
    @Getter
    public static enum ChannelType {

        SESSION("session"),
        SHELL("shell"),
        EXEC("exec"),
        SFTP("sftp"),
        ;
        private final String value;

        ChannelType(String value) {
            this.value = value;
        }
    }

}
