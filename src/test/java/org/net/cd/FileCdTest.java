package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.net.cd.file.FileCd;
import org.net.cd.source.FileSource;
import org.net.ssh.SshTest;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 23:01 2022/07/27
 */
@Slf4j
public class FileCdTest {

    public static void main(String[] args) throws Exception {
        Cd cd = null;
        try {
            cd = FileCd.builder()
                    .connectionProperties(SshTest.getConnectionProperties())
                    .sessionConnectTimeout(Duration.ofSeconds(60))
                    .channelConnectTimeout(Duration.ofSeconds(60))

                    // 设置远程服务器的工作路径
                    .workDir("test")

                    // 是否以sudo执行命令（）
                    .sudo(true)

                    // 配置资源
                    .source(FileSource.builder()
                            .filePaths("C:\\Users\\xiangqian\\Desktop\\repository\\net",
                                    "C:\\Users\\xiangqian\\Desktop\\tmp\\apache-skywalking-apm-9.0.0.tar.gz",
                                    "C:\\Users\\xiangqian\\Desktop\\repository\\repository")
                            .build())
                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
