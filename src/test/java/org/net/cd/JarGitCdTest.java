package org.net.cd;

import org.apache.commons.io.IOUtils;
import org.net.cd.jar.JarGitCd;
import org.net.cd.source.GitSource;
import org.net.ssh.SshTest;

import java.time.Duration;

/**
 * @author xiangqian
 * @date 10:17 2022/08/21
 */
public class JarGitCdTest {

    public static void main(String[] args) throws Exception {
        Cd cd = null;
        try {
            cd = JarGitCd.builder()
                    .connectionProperties(SshTest.getConnectionProperties())
                    .sessionConnectTimeout(Duration.ofSeconds(60))
                    .channelConnectTimeout(Duration.ofSeconds(60))

                    // 设置远程服务器的工作路径
                    .workDir("test")

                    // 是否以sudo执行命令
                    .sudo(true)

                    // GitSource配置资源
                    .source(GitSource.builder()
                            .username("Git用户名")
                            .password("Git密码")
                            .repoUrl("仓库地址")
                            .branch("分支名称")
                            .build())

                    // maven
//                    .mavenHome("")
//                    .filePaths()
                    // mvn
                    .mvn()
                    .P("test") // dev,test,prod
                    .and()

                    // docker build
                    .dockerBuild()
                    .tag("org/test:2022.8")
                    .and()

                    // docker run
                    .dockerRun()
                    .name("test")
                    .p("8081:8081")
                    .add_host("hostname:192.168.2.43")
                    .and()

                    .build();
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
        }
    }


}
