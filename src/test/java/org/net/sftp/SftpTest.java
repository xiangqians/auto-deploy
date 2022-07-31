package org.net.sftp;

import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.net.sftp.impl.DefaultSftpProgressMonitor;
import org.net.sftp.impl.JSchSftpImpl;
import org.net.ssh.SshTest;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author xiangqian
 * @date 01:17 2022/07/24
 */
@Slf4j
public class SftpTest implements Closeable {
    private Sftp sftp;

    public static void main(String[] args) throws Exception {
        new SftpTest().main();
    }

    public void main() throws Exception {
        try {
            init();
//            testLs();
//            testMkdir();
            testPut();
//            testGet();

        } finally {
            IOUtils.closeQuietly(this);
        }
    }

    private void testPut() throws Exception {
        sftp.cd("test");
//        ll();
        sftp.put("C:\\Users\\xiangqian\\Desktop\\tmp\\Screenshot_1.png",
                "Screenshot_1.png",
                DefaultSftpProgressMonitor.builder().build(),
                FileTransferMode.OVERWRITE);
//        sftp.put("C:\\Users\\xiangqian\\Desktop\\tmp\\apache-skywalking-java-agent-8.9.0.tgz",
//                "apache-skywalking-java-agent-8.9.0.tgz",
//                DefaultSftpProgressMonitor.builder().build(),
//                FileTransferMode.OVERWRITE);
        ll();
    }

    private void testGet() throws Exception {
        sftp.cd("test");
        sftp.get("Screenshot_1.png",
                "C:\\Users\\xiangqian\\Desktop\\tmp\\download\\",
                DefaultSftpProgressMonitor.builder().build(),
                FileTransferMode.OVERWRITE);
        ll();
        sftp.rm("Screenshot_1.png");
        sftp.rm("apache-skywalking-java-agent-8.9.0.tgz");
    }

    private void testMkdir() throws Exception {
//        sftp.mkdir("test1");
        ll();
        sftp.rmdir("test1");
        ll();
    }

    private void testLs() throws Exception {
        String command = null;
        List<FileEntry> results = null;

        command = "./";
        results = sftp.ls(command);
        SshTest.print(command, results);

        command = "/";
        results = sftp.ls(command);
        SshTest.print(command, results);
    }

    private void ll() throws Exception {
        String command = "./";
        List<FileEntry> results = sftp.ls(command);
        SshTest.print(command, results);
    }

    private void init() throws JSchException {
        sftp = JSchSftpImpl.builder()
                .connectionProperties(SshTest.getConnectionProperties())
                .sessionConnectTimeout(Duration.ofSeconds(60))
                .channelConnectTimeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(sftp);
    }
}
