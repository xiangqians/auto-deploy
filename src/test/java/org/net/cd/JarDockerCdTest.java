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
                    .filePaths("E:\\workspace\\idea-my\\jar-tmp\\out\\artifacts\\jar_tmp_jar\\jar-tmp.jar",
                            "C:\\Users\\xiangqian\\Desktop\\repository\\net")
                    .name("org_test")
                    .t("org/test:v2022.8")
                    .p("8080:8080")
                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
