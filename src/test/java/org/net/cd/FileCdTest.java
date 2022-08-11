package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
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
                    .workDir("test")
//                    .filePath("C:\\Users\\xiangqian\\Desktop\\tmp\\properties-maven-plugin-master.xxxx")
//                    .filePath("C:\\Users\\xiangqian\\Desktop\\tmp\\apache-skywalking-apm-bin.zip")
//                    .filePath("C:\\Users\\xiangqian\\Desktop\\tmp\\apache-skywalking-apm-9.0.0.tar.gz")
                    .filePath("C:\\Users\\xiangqian\\Desktop\\repository\\net")
                    .clearWorkDir(true)
                    .build();

            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
