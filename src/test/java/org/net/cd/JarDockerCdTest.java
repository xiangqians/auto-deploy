package org.net.cd;

import org.apache.commons.io.IOUtils;
import org.net.ssh.SshTest;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 23:12 2022/08/11
 */
public class JarDockerCdTest {

    public static void main(String[] args) throws Exception {
        Cd cd = null;
        try {
            cd = JarDockerCd.builder()
                    .connectionProperties(SshTest.getConnectionProperties())
                    .sessionConnectTimeout(Duration.ofSeconds(60))
                    .channelConnectTimeout(Duration.ofSeconds(60))
                    .workDir("test")
                    .srcFilePaths("E:\\workspace\\idea-my\\jar-tmp\\out\\artifacts\\jar_tmp_jar\\jar-tmp.jar",
                            "C:\\Users\\xiangqian\\Desktop\\repository\\net",
                            "C:\\Users\\xiangqian\\Desktop\\repository\\repository")

                    // docker build
                    .dockerBuild()
                    .tag("org/test:v2022.08")
                    .and()

                    // docker run
                    .dockerRun()
                    .p("8080:8080")
                    .name("test")
                    .and()

                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
