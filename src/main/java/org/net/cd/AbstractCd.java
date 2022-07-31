package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.net.sftp.Sftp;
import org.net.sftp.impl.JSchSftpImpl;
import org.net.ssh.ConnectionProperties;
import org.net.ssh.Ssh;
import org.net.ssh.impl.JSchShellChannelSshImpl;
import org.net.util.Assert;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author xiangqian
 * @date 22:34 2022/07/25
 */
@Slf4j
public abstract class AbstractCd implements Cd {

    protected ConnectionProperties connectionProperties;
    protected Duration sessionConnectTimeout;
    protected Duration channelConnectTimeout;
    protected Ssh ssh;
    protected Sftp sftp;
    // 工作路径
    protected String workDir;
    // 绝对工作路径
    protected String absoluteWorkDir;

    protected void init(ConnectionProperties connectionProperties,
                        Duration sessionConnectTimeout,
                        Duration channelConnectTimeout,
                        String workDir) throws Exception {
        this.connectionProperties = connectionProperties;
        this.sessionConnectTimeout = sessionConnectTimeout;
        this.channelConnectTimeout = channelConnectTimeout;
        this.workDir = workDir;
        this.ssh = JSchShellChannelSshImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .defaultQuickEndSignPatterns()
                .build();
        this.sftp = JSchSftpImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnectTimeout)
                .channelConnectTimeout(channelConnectTimeout)
                .build();
    }

    protected void cdWorkDir() throws Exception {
        String cmd = null;
        List<String> results = null;

        // ssh
        cmd = String.format("cd %s", workDir);
        results = ssh.execute(cmd);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        Assert.isTrue(ListUtils.indexOf(results, result -> result.contains("No such file or directory")) == -1,
                String.format("%s: No such file or directory", workDir));

        // pwd
        cmd = "pwd";
        results = ssh.execute(cmd);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        if (CollectionUtils.isNotEmpty(results)) {
            for (String result : results) {
                if (result.startsWith("/")) {
                    absoluteWorkDir = result;
                    break;
                }
            }
        }
        Assert.notNull(absoluteWorkDir, "无法解析绝对工作路径");

        // sftp
        sftp.cd(workDir);
        log.debug("<sftp> cd {}", workDir);
    }

    public static Consumer<String> resultConsumer(String cmd) {
        return new Consumer<>() {
            private int index = 0;

            @Override
            public void accept(String result) {
                if (index == 0) {
                    log.debug("{}", cmd);
                }
                index++;
//                System.out.format("[%03d] %s", index++, result).println();
                System.out.format("%s", result).println();
            }
        };
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(ssh, sftp);
    }

    protected static abstract class Builder<B extends Builder, A extends AbstractCd> {

        protected ConnectionProperties connectionProperties;
        protected Duration sessionConnectTimeout;
        protected Duration channelConnectTimeout;
        protected String workDir;

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

        public B workDir(String workDir) {
            this.workDir = workDir;
            return (B) this;
        }

        protected abstract A get();

        public A build() throws Exception {
            Assert.notNull(workDir, "workDir不能为空");
            A a = get();
            a.init(connectionProperties, sessionConnectTimeout, channelConnectTimeout, workDir);
            return a;
        }

    }

}
