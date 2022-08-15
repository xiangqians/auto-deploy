package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.net.ssh.SshTest;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 01:42 2022/07/26
 */
@Slf4j
public class JarCdTest {

    public static void main(String[] args) throws Exception {
        Cd cd = null;
        try {
            cd = JarCd.builder()
                    .connectionProperties(SshTest.getConnectionProperties())
                    .sessionConnectTimeout(Duration.ofSeconds(60))
                    .channelConnectTimeout(Duration.ofSeconds(60))
                    .workDir("test")
                    // $ which java
                    // /usr/bin/java
                    .javaHome("/usr")
                    .srcFilePaths("E:\\workspace\\idea-my\\jar-tmp\\out\\artifacts\\jar_tmp_jar\\jar-tmp.jar",
                            "C:\\Users\\xiangqian\\Desktop\\repository\\net",
                            "C:\\Users\\xiangqian\\Desktop\\repository\\repository")
                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
