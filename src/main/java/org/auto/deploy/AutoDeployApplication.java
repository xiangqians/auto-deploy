package org.auto.deploy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.config.Config;
import org.auto.deploy.support.Server;
import org.auto.deploy.support.builder.Builder;
import org.auto.deploy.support.deployment.Deployment;
import org.auto.deploy.support.deployment.JarDeployment;
import org.auto.deploy.support.deployment.JarDockerDeployment;
import org.auto.deploy.support.deployment.StaticDeployment;
import org.auto.deploy.support.source.GitSource;
import org.auto.deploy.support.source.LocalSource;
import org.auto.deploy.support.source.Source;
import org.auto.deploy.util.OS;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.Closeable;
import java.io.IOException;

/**
 * 自动化部署应用
 *
 * @author xiangqian
 * @date 20:50 2022/09/09
 */
@Slf4j
public class AutoDeployApplication implements SignalHandler, Closeable {

    // 主线线程
    private Thread mainThread;

    private Config config;
    private Server server;
    private Source source;
    private Builder builder;
    private Deployment deployment;

    private volatile boolean deploying;

    public AutoDeployApplication() {
        this.mainThread = Thread.currentThread();
    }

    public void run() throws Exception {
	
        // ========== addShutdownHook
//        log.debug("add shutdown hook ...");
//        addShutdownHook();
//        log.debug("added shutdown hook!");

        // ========== registerSignal
        log.debug("register signal ...");
        registerSignal();
        log.debug("registered signal!");

        // ========== init

        // config
        log.debug("初始化config ...");
        config = Config.get();
        config.validate();
        log.debug("已初始化config!\n{}", config);

        // server
        log.debug("初始化server ...");
        server = new Server(config.getServer());
        log.debug("已初始化server!\n\t{}", server);

        // source
        log.debug("初始化source ...");
        switch (config.getSource().getType()) {
            case LOCAL:
                source = new LocalSource(config.getSource().getLocal());
                break;

            case GIT:
                source = new GitSource(config.getSource().getGit());
                break;
        }
        log.debug("已初始化source!\n\t{}", source);

        // builder
        log.debug("初始化builder ...");
        builder = new Builder(config.getBuilder(), source);
        log.debug("已初始化builder!\n\t{}", builder);

        // deployment
        log.debug("初始化deployment ...");
        switch (config.getDeployment().getType()) {
            case STATIC:
                deployment = new StaticDeployment(config.getDeployment().getStc(), server, source);
                break;

            case JAR:
                deployment = new JarDeployment(config.getDeployment().getJar(), server, source);
                break;

            case JAR_DOCKER:
                deployment = new JarDockerDeployment(config.getDeployment().getJarDocker(), server, source);
                break;
        }
        log.debug("已初始化deployment!\n\t{}", deployment);

        // ========== execute

        switch (config.getSource().getType()) {
            case LOCAL:
                buildAndDeploy();
                break;

            case GIT:
                GitSource gitSource = (GitSource) source;
                gitSource.listen(this::buildAndDeploy);
                break;
        }
    }

    private void buildAndDeploy() throws Exception {
        try {
            log.debug("building ...");
            builder.build();
            log.debug("built!");

            // 连接到服务
            server.connect();

            log.debug("部署中 ...");
            deployment.deploy();
            log.debug("已部署!");
        } finally {
            IOUtils.closeQuietly(server);
        }
    }

    /**
     * 注册 Linux(具体信号kill -l命令查看) & Windows 信号
     * $ kill -l
     * 1) SIGHUP       2) SIGINT       3) SIGQUIT      4) SIGILL       5) SIGTRAP
     * 6) SIGABRT      7) SIGBUS       8) SIGFPE       9) SIGKILL     10) SIGUSR1
     * 11) SIGSEGV     12) SIGUSR2     13) SIGPIPE     14) SIGALRM     15) SIGTERM
     * 16) SIGSTKFLT   17) SIGCHLD     18) SIGCONT     19) SIGSTOP     20) SIGTSTP
     * 21) SIGTTIN     22) SIGTTOU     23) SIGURG      24) SIGXCPU     25) SIGXFSZ
     * 26) SIGVTALRM   27) SIGPROF     28) SIGWINCH    29) SIGIO       30) SIGPWR
     * 31) SIGSYS      34) SIGRTMIN    35) SIGRTMIN+1  36) SIGRTMIN+2  37) SIGRTMIN+3
     * 38) SIGRTMIN+4  39) SIGRTMIN+5  40) SIGRTMIN+6  41) SIGRTMIN+7  42) SIGRTMIN+8
     * 43) SIGRTMIN+9  44) SIGRTMIN+10 45) SIGRTMIN+11 46) SIGRTMIN+12 47) SIGRTMIN+13
     * 48) SIGRTMIN+14 49) SIGRTMIN+15 50) SIGRTMAX-14 51) SIGRTMAX-13 52) SIGRTMAX-12
     * 53) SIGRTMAX-11 54) SIGRTMAX-10 55) SIGRTMAX-9  56) SIGRTMAX-8  57) SIGRTMAX-7
     * 58) SIGRTMAX-6  59) SIGRTMAX-5  60) SIGRTMAX-4  61) SIGRTMAX-3  62) SIGRTMAX-2
     * 63) SIGRTMAX-1  64) SIGRTMAX
     */
    private void registerSignal() {
        // SIGTERM(15): 终止进程 - 软件终止信号
        // Linux: kill $pid or kill -15 $pid
        Signal.handle(new Signal("TERM"), this);

        // SIGINT(2): 终止进程 - 中断进程，在用户键入INTR字符（通常是 Ctrl + C）时发出
        // Windows: Ctrl + C
        Signal.handle(new Signal("INT"), this);

        if (OS.get() == OS.LINUX) {
            // SIGUSR1(10): 终止进程 - 用户定义信号1
            Signal.handle(new Signal("USR1"), this);

            // SIGUSR2(12): 终止进程 - 用户定义信号2
            //Signal.handle(new Signal("USR2"), this);
        }
    }

    /**
     * 处理信号
     *
     * @param signal
     */
    @Override
    public void handle(Signal signal) {
        log.debug("{}: {}) {}", signal, signal.getNumber(), signal.getName());
        String name = signal.getName();
        // 优雅地关闭应用
        if (name.equals("TERM") || name.equals("INT")) {
            if (source instanceof GitSource) {
                log.debug("优雅地关闭应用!");
                ((GitSource) source).setListenFlag(false);
            }
        }
        // 触发部署
        else if (name.equals("USR1")) {
            log.debug("触发部署!");
            mainThread.interrupt();
        }


    }

    /**
     * 在JVM中增加一个关闭的钩子。
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                AutoDeployApplication.this.close();
            } catch (Exception e) {
                log.error("", e);
            }
        }));
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(server, source, builder, deployment);
        server = null;
        source = null;
        builder = null;
        deployment = null;
    }

    public static void main(String[] args) throws Exception {
        AutoDeployApplication application = null;
        try {
            application = new AutoDeployApplication();
            application.run();
        } finally {
            IOUtils.closeQuietly(application);
            log.debug("已关闭自动化部署应用资源");
        }
    }

}
