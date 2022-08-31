package org.net.cd;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.net.cd.jar.JarDockerCd;
import org.net.cd.source.GitSource;
import org.net.cd.source.MavenSource;
import org.net.ssh.SshTest;

import java.io.*;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author xiangqian
 * @date 10:17 2022/08/21
 */
@Slf4j
public abstract class JarGitCdTest implements Runnable, Closeable {

    private GitSource gitSource;
    private volatile String projectDir;
    private volatile boolean exit;

    // 是否正在部署
    private volatile boolean deploying;

    public JarGitCdTest(GitSource gitSource) {
        this.gitSource = gitSource;
        this.exit = false;
        this.deploying = false;
    }

    @Override
    public void run() {
        // Options
        Options options = new Options();
        // help
        options.addOption(Option.builder("h").hasArg(false).longOpt("help").type(String.class).desc("usage help").build());
        // deploy
        options.addOption(Option.builder("d").hasArg(false).longOpt("deploy").type(String.class).desc("从git拉取最新代码，并部署").build());
        // exit
        options.addOption(Option.builder("e").hasArg(false).longOpt("exit").type(String.class).desc("退出应用").build());

        PrintWriter printWriter = null;
        String help = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            printWriter = new PrintWriter(byteArrayOutputStream);
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp(printWriter,
                    HelpFormatter.DEFAULT_WIDTH,
                    "cli -help",
                    null,
                    options,
                    HelpFormatter.DEFAULT_LEFT_PAD,
                    HelpFormatter.DEFAULT_DESC_PAD,
                    null);
            printWriter.flush();
            help = new String(byteArrayOutputStream.toByteArray());
        } finally {
            IOUtils.closeQuietly(printWriter);
        }

        // in
        Scanner scanner = new Scanner(System.in);
        while (!exit) {
            System.out.print("> ");
            String args = scanner.nextLine();
            CommandLine commandLine = null;
            try {
                CommandLineParser commandLineParser = new DefaultParser();
                commandLine = commandLineParser.parse(options, args.split("\\s+"));
            } catch (ParseException e) {
                System.err.println(e.getMessage() + "\n" + help);
                continue;
            }

            // 查询帮助
            if (commandLine.hasOption("h")) {
                System.out.println(help);
                continue;
            }

            // 退出
            if (commandLine.hasOption("e")) {
                exit = true;
                break;
            }

            if (commandLine.hasOption("d")) {
                try {
                    execute();
                    continue;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            System.err.println(help);

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(gitSource);
    }

    protected abstract Cd getCd(String projectDir) throws Exception;

    private void execute() throws Exception {
        synchronized (this) {
            if (deploying) {
                log.warn("当前正在执行部署操作，请稍后再试！");
                return;
            }
            deploying = true;
        }

        Cd cd = null;
        try {
            cd = getCd(projectDir);
            cd.execute();
        } finally {
            IOUtils.closeQuietly(cd);
            deploying = false;
        }
    }

    public void start() throws Exception {
        // 获取git资源工作目录
        File[] files = gitSource.get();
        projectDir = files[0].getAbsolutePath();

        // 监听控制台输入
        new Thread(this).start();

        // while
        while (!exit) {
            // 监测git资源变动
            if (!deploying && gitSource.pull()) {
                execute();
            }

            // 每隔 30s pull一次
            TimeUnit.SECONDS.sleep(30);
        }
    }

    public static void main(String[] args) throws Exception {
        JarGitCdTest jarGitCdTest = null;
        try {
            // Git资源配置
            GitSource gitSource = GitSource.builder()
                    .username("Git用户名")
                    .password("Git密码")
                    .repoUrl("仓库地址")
                    .branch("分支名称")
                    .build();

            // new JarGitCdTest
            jarGitCdTest = new JarGitCdTest(gitSource) {
                @Override
                protected Cd getCd(String projectDir) throws Exception {
                    return JarDockerCd.builder()
                            .connectionProperties(SshTest.getConnectionProperties())
                            .sessionConnectTimeout(Duration.ofSeconds(60))
                            .channelConnectTimeout(Duration.ofSeconds(60))

                            // 设置远程服务器的工作路径
                            .workDir("test")

                            // 是否以sudo执行命令
                            .sudo(true)

                            // MavenSource
                            .source(MavenSource.builder()
                                    .mavenHome("E:\\build-tools\\apache-maven-3.6.0")
                                    .projectDir(projectDir)
                                    .filePaths("C:\\Users\\xiangqian\\Desktop\\repository\\net",
                                            "C:\\Users\\xiangqian\\Desktop\\repository\\repository")
                                    // mvn
                                    .mvn()
                                    .P("test") // dev,test,prod
                                    .and()

                                    .build())

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
                }
            };

            // start
            jarGitCdTest.start();
        } finally {
            IOUtils.closeQuietly(jarGitCdTest);
        }
    }

}
