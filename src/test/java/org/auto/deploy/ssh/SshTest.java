package org.auto.deploy.ssh;

import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.ssh.impl.JSchShellChannelSshImpl;

import java.io.Closeable;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author xiangqian
 * @date 14:00 2022/07/23
 */
@Slf4j
public class SshTest implements Closeable {

    private Ssh ssh;

    public static void main(String[] args) throws Exception {
        new SshTest().main();
    }

    public void main() throws Exception {
        long beginTime = System.currentTimeMillis();
        try {
            init();

            test();

            // thread
//            Thread thread1 = new Thread(this::test, "Thread-1");
//            thread1.start();
//            Thread thread2 = new Thread(this::test, "Thread-2");
//            thread2.start();
//            // join
//            thread1.join();
//            thread2.join();
        } finally {
            IOUtils.closeQuietly(this);
            long endTime = System.currentTimeMillis();
            DecimalFormat format = new DecimalFormat("###,###,###");
            log.debug("time: {} ms", format.format(endTime - beginTime));
        }
    }

    // 15,910 ms
    // 15,806 ms
    // 133,961 ms

    private void test() {
//        execute("ls -l");
//        execute("cd test1");
//        execute("pwd");
//        execute("touch tmp.txt");
//        execute("ls -l");
//        execute("cd test;pwd;ls -l");

        execute("cd test");
        execute("pwd");
        execute("sudo docker build -t test .", Duration.ofMinutes(10));
    }

    private void execute(String cmd) {
        try {
            ssh.execute(cmd, resultConsumer(cmd));
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void execute(String cmd, Duration timeout) {
        try {
            ssh.execute(cmd, timeout, resultConsumer(cmd));
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private void init() throws JSchException {
        ssh = JSchShellChannelSshImpl.builder()
                .connectionProperties(getConnectionProperties())
                .sessionConnectTimeout(Duration.ofSeconds(60))
                .channelConnectTimeout(Duration.ofSeconds(60))
                .defaultQuickEndSignPatterns()
                .build();
//            log.debug("ssh {}", ssh);
    }

    public static ConnectionProperties getConnectionProperties() {
        ConnectionProperties connectionProperties = new ConnectionProperties();
        connectionProperties.setHost("debian");
        connectionProperties.setPort(22);
        connectionProperties.setUsername("xiangqian");
        connectionProperties.setPassword("xiangqian");
        return connectionProperties;
    }

    public static Consumer<String> resultConsumer(String cmd) {
        return new Consumer<>() {
            private int index = 0;

            @Override
            public void accept(String result) {
                if (index == 0) {
                    System.out.println('\n');
                    log.debug("{}", cmd);
                }
                System.out.format("[%03d] %s", index++, result).println();
            }
        };
    }

    public static void print(String cmd, List<?> results) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0, size = results.size(); i < size; i++) {
            Object result = results.get(i);
            builder.append("[").append(i).append("] ").append(result).append('\n');
        }
        log.debug("{}\n{}", cmd, builder);
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(ssh);
    }

}
