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

    private Config config;
    private Server server;
    private Source source;
    private Builder builder;
    private Deployment deployment;

    public void run() throws Exception {

        // ========== addShutdownHook
//        log.debug("addShutdownHook ...");
//        addShutdownHook();
//        log.debug("addedShutdownHook!");

        // ========== registerSignal
        log.debug("registerSignal ...");
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

        server.connect();
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
        log.debug("building ...");
        builder.build();
        log.debug("built!");

        log.debug("部署中 ...");
        deployment.deploy();
        log.debug("已部署!");
    }

    /**
     * 注册信号
     */
    private void registerSignal() {
        // Linux(具体信号kill -l命令查看) & Windows 信号支持
        Signal.handle(new Signal("TERM"), this);
        Signal.handle(new Signal("INT"), this);

        // Linux: kill $pid or kill -15 $pid
        // Windows: Ctrl + C
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

    /**
     * 处理信号
     *
     * @param signal
     */
    @Override
    public void handle(Signal signal) {
        log.debug("signal: {}", signal);

        if (source instanceof GitSource) {
            ((GitSource) source).setListenFlag(false);
        }

        //System.exit(0);

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
            log.debug("关闭自动化部署应用资源");
        }
    }

}
