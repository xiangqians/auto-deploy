package org.auto.deploy.item.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.auto.deploy.sftp.FileTransferMode;
import org.auto.deploy.sftp.Sftp;
import org.auto.deploy.sftp.impl.DefaultSftpProgressMonitor;
import org.auto.deploy.sftp.impl.JSchSftpImpl;
import org.auto.deploy.ssh.ConnectionProperties;
import org.auto.deploy.ssh.Ssh;
import org.auto.deploy.ssh.impl.JSchShellChannelSshImpl;
import org.auto.deploy.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 服务器
 *
 * @author xiangqian
 * @date 13:06 2022/09/10
 */
@Slf4j
public class ItemServer implements Closeable {

    private Config config;
    private Ssh ssh;
    private Sftp sftp;

    // 绝对工作路径
    @Getter
    private String absoluteWorkDir;

    // 错误集
    private Set<String> errorSet = Set.of("No such file or directory",
            "Permission denied",
            "-bash:"
    );

    public ItemServer(Config config) {
        this.config = config;
    }

    /**
     * 上传文件
     *
     * @param src
     * @param dst
     * @throws Exception
     */
    public void uploadFile(String src, String dst) throws Exception {
        sftp.put(src, dst, DefaultSftpProgressMonitor.builder().build(), FileTransferMode.OVERWRITE);
    }

    public void executeCmd(String cmd) throws Exception {
        executeCmd(cmd, true);
    }

    public void executeCmd(String cmd, boolean isIgnoreError) throws Exception {
        executeCmd(cmd, Ssh.DEFAULT_TIMEOUT, isIgnoreError);
    }

    public void executeCmd(String cmd, Duration timeout) throws Exception {
        executeCmd(cmd, timeout, true);
    }

    /**
     * 执行命令
     *
     * @param cmd           命令
     * @param timeout       执行命令超时时间
     * @param isIgnoreError 是否忽略命令执行异常
     * @throws Exception
     */
    public void executeCmd(String cmd, Duration timeout, boolean isIgnoreError) throws Exception {
        if (isIgnoreError) {
            List<String> results = new ArrayList<>();
            executeCmd(cmd, timeout, (result) -> {
//                System.out.format("[%03d] %s", index++, result).println();
                System.out.format("%s", result).println();
                results.add(result);
            });
            log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
            return;
        }

        List<String> results = executeCmdForResults(cmd, timeout);
        log.debug("<ssh> {}\n{}", cmd, StringUtils.join(results, "\n"));
        for (String error : errorSet) {
            Assert.isTrue(ListUtils.indexOf(results, result -> result.contains(error)) == -1, String.format("%s: %s", cmd, error));
        }
    }

    public List<String> executeCmdForResults(String cmd) throws Exception {
        return executeCmdForResults(cmd, Ssh.DEFAULT_TIMEOUT);
    }

    public List<String> executeCmdForResults(String cmd, Duration timeout) throws Exception {
        List<String> results = new ArrayList<>();
        executeCmd(cmd, timeout, results::add);
        return results;
    }

    public void executeCmd(String cmd, Duration timeout, Consumer<String> consumer) throws Exception {
        // sudo
        if (BooleanUtils.isTrue(config.getSudo())
                && StringUtils.startsWithAny(cmd, "./jps.sh", "./startup.sh", "./shutdown.sh", "./clean.sh",
                "cp", "mv", "rm", "chmod", "tar",
                "docker")) {
            cmd = "sudo " + cmd;
        }

        // execute
        ssh.execute(cmd, timeout, consumer);
    }

    /**
     * 连接服务器
     */
    public synchronized void connect() throws Exception {
        log.debug("连接到 {} 服务器 ...", config.getHost());
        ConnectionProperties connectionProperties = new ConnectionProperties();
        connectionProperties.setHost(config.getHost());
        connectionProperties.setPort(config.getPort());
        connectionProperties.setUsername(config.getUsername());
        connectionProperties.setPassword(config.getPassword());

        Duration sessionConnTimeout = Duration.ofSeconds(config.getSessionConnTimeout());
        Duration channelConnTimeout = Duration.ofSeconds(config.getChannelConnTimeout());

        ssh = JSchShellChannelSshImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnTimeout)
                .channelConnectTimeout(channelConnTimeout)
                .defaultQuickEndSignPatterns()
                .build();
        sftp = JSchSftpImpl.builder()
                .connectionProperties(connectionProperties)
                .sessionConnectTimeout(sessionConnTimeout)
                .channelConnectTimeout(channelConnTimeout)
                .build();
        log.debug("已成功连接到 {} 服务器!", config.getHost());

        cdWorkDir();
    }

    private void cdWorkDir() throws Exception {
        String workDir = config.getWorkDir();
        log.debug("进入工作目录 ...\n\t{}", workDir);
        String cmd = null;

        // ssh
        cmd = String.format("cd %s", workDir);
        executeCmd(cmd, false);

        // pwd
        cmd = "pwd";
        List<String> results = executeCmdForResults(cmd);
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
        log.debug("absoluteWorkDir: {}", absoluteWorkDir);

        // sftp
        sftp.cd(workDir);
        log.debug("<sftp> cd {}", workDir);

        log.debug("已进入工作目录!\n\t{}", workDir);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(ssh, sftp);
        ssh = null;
        sftp = null;
        absoluteWorkDir = null;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {
        private String host;
        private Integer port;

        private String username;
        private String password;

        /**
         * session连接超时，单位s
         */
        @JsonProperty("session-conn-timeout")
        private Integer sessionConnTimeout;

        /**
         * channel连接超时，单位s
         */
        @JsonProperty("channel-conn-timeout")
        private Integer channelConnTimeout;

        /**
         * 工作目录
         */
        @JsonProperty("work-dir")
        private String workDir;

        private Boolean sudo;

        public void validate() {
            Assert.notNull(host, "host不能为null");
            Assert.notNull(port, "port不能为null");
            Assert.notNull(username, "username不能为null");
            Assert.notNull(password, "password不能为null");
            Assert.isTrue(Objects.nonNull(sessionConnTimeout) && sessionConnTimeout > 0, "session-conn-timeout必须大于0");
            Assert.isTrue(Objects.nonNull(channelConnTimeout) && channelConnTimeout > 0, "channel-conn-timeout必须大于0");
            Assert.notNull(workDir, "work-dir不能为null");
        }

    }

}
