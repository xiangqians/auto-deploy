package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.net.cd.jar.JarGeneralCd;
import org.net.cd.source.MavenSource;
import org.net.ssh.SshTest;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 01:42 2022/07/26
 */
@Slf4j
public class JarGeneralCdTest {

    public static void main(String[] args) throws Exception {
        Cd cd = null;
        try {
            cd = JarGeneralCd.builder()
                    .connectionProperties(SshTest.getConnectionProperties())
                    .sessionConnectTimeout(Duration.ofSeconds(60))
                    .channelConnectTimeout(Duration.ofSeconds(60))

                    // 设置远程服务器的工作路径
                    .workDir("test")

                    // 是否以sudo执行命令
                    .sudo(true)

                    // $ which java
                    // /usr/bin/java
                    .javaHome("/usr")

                    // 配置资源
                    // FileSource
//                    .source(FileSource.builder()
//                            .filePaths("E:\\workspace\\idea-my\\jar-tmp\\out\\artifacts\\jar_tmp_jar\\jar-tmp.jar",
//                                    "C:\\Users\\xiangqian\\Desktop\\repository\\net",
//                                    "C:\\Users\\xiangqian\\Desktop\\repository\\repository")
//                            .build())
                    // MavenSource
                    .source(MavenSource.builder()
                            .mavenHome("E:\\build-tools\\apache-maven-3.6.0")
                            .projectDir("C:\\Users\\xiangqian\\Desktop\\tmp\\maven-project")
                            .filePaths("C:\\Users\\xiangqian\\Desktop\\repository\\net",
                                    "C:\\Users\\xiangqian\\Desktop\\repository\\repository")
                            // mvn
                            .mvn()
                            .P("test") // dev,test,prod
                            .and()
                            .build())

                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }

}
