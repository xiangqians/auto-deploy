package org.auto.deploy.item;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.core.Config;
import org.auto.deploy.core.build.Build;
import org.auto.deploy.core.deployment.Deployment;
import org.auto.deploy.core.deployment.jar.JarDeployment;
import org.auto.deploy.core.deployment.jar.docker.JarDockerDeployment;
import org.auto.deploy.core.deployment.stc.StaticDeployment;
import org.auto.deploy.core.server.Server;
import org.auto.deploy.core.source.GitSource;
import org.auto.deploy.core.source.LocalSource;
import org.auto.deploy.core.source.Source;
import org.auto.deploy.util.DateUtils;
import org.auto.deploy.util.JacksonUtils;

import java.io.Flushable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 项目部署器
 *
 * @author xiangqian
 * @date 00:03 2022/09/19
 */
@Slf4j
@Getter
public class ItemDeployer extends Thread implements Flushable {

    // 项目名
    private String itemName;

    // 开启资源监听
//    @Setter
    private volatile boolean enableSourceMonitoring;

    private volatile ItemInfo itemInfo;
    private volatile LocalDateTime lastDeploymentTime;
    private volatile String logRelativePath;
    private volatile ItemStage serverStage;
    private volatile ItemStage sourceStage;
    private volatile ItemStage buildrStage;
    private volatile ItemStage deploymentStage;

    public ItemDeployer(String itemName) {
        super(itemName);
        this.itemName = itemName;
        this.enableSourceMonitoring = false;
    }

    @Override
    public void run() {
        //  source
        Source source = null;
        // md5校验文件变更
        String md5 = null;
        // ScheduledFuture
        ScheduledFuture<?> scheduledFuture = null;
        try {
            do {
                // initIemInfo
                initIemInfo();

                // config
                Config config = getConfig();
                if (Objects.isNull(md5)) {
                    md5 = config.getMd5();

                    // 获取source
                    source = getSource(config);
                }
                // config发生改变
                else if (!md5.equals(config.getMd5())) {
                    // 关闭source并重新获取source
                    IOUtils.closeQuietly(source);
                    source = getSource(config);

                    // 先注释，目前暂不支持
                    // 是否开启资源监听
//                    if (enableSourceMonitoring) {
//                        TaskScheduler.cancel(scheduledFuture, true);
//                        Thread currentThread = Thread.currentThread();
//                        String cron = null;
//                        switch (config.getSource().getType()) {
//                            case LOCAL:
//                                cron = config.getSource().getLocal().getCron();
//                                break;
//
//                            case GIT:
//                                cron = config.getSource().getGit().getCron();
//                                break;
//                        }
//                        scheduledFuture = TaskScheduler.schedule(currentThread::interrupt, new CronTrigger(cron));
//                    }
                }

                // 先注释，目前暂不支持
                // 等待中断
//                try {
//                    Thread.currentThread().wait();
//                } catch (InterruptedException e) {
//                    log.debug("{} 项目资源监听线程被中断!", itemName);
//                }

                // 先注释，目前暂不支持
                // 资源发生改变时
//                if (source.isChanged()) {
//                    // 构建并部署
//                    buildAndDeploy(config, source);
//                }
                buildAndDeploy(config, source);

            } while (enableSourceMonitoring);
        } catch (Exception e) {
            log.error(String.format("%s 项目部署异常!", itemName), e);
        } finally {
            IOUtils.closeQuietly(source);
            TaskScheduler.cancel(scheduledFuture, true);
        }
    }

    private void buildAndDeploy(Config config, Source source) throws Exception {
        Build build = null;
        Server server = null;
        Deployment deployment = null;
        ItemStage curStage = null;
        try {
            // source
            curStage = sourceStage;
            curStage.setStartTime(System.currentTimeMillis());
            flush();
            source.get();
            if (source instanceof GitSource) {
                String lastRevCommitStr = ((GitSource) source).getLastRevCommitStr();
                curStage.setDesc("\n最新一次提交信息: " + lastRevCommitStr);
            }
            curStage.setEndTime(System.currentTimeMillis());
            flush();

            // build
            curStage = buildrStage;
            curStage.setStartTime(System.currentTimeMillis());
            flush();
            build = getBuild(config, source);
            build(build);
            curStage.setEndTime(System.currentTimeMillis());
            flush();

            // server
            curStage = serverStage;
            curStage.setStartTime(System.currentTimeMillis());
            flush();
            server = getServer(config);
            // 连接到服务
            server.connect();
            curStage.setEndTime(System.currentTimeMillis());
            flush();

            // deployment
            curStage = deploymentStage;
            curStage.setStartTime(System.currentTimeMillis());
            flush();
            deployment = getDeployment(config, server, source);
            log.debug("部署中 ...");
            deployment.deploy();
            log.debug("已部署!");
            curStage.setEndTime(System.currentTimeMillis());
            flush();

        } catch (Exception e) {
            curStage.setEndTime(-1L);
            throw e;
        } finally {
            IOUtils.closeQuietly(deployment, server, build);
            flush();
        }
    }

    public Deployment getDeployment(Config config, Server server, Source source) {
        log.debug("初始化deployment ...");
        Deployment deployment = null;
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
        return deployment;
    }

    private Server getServer(Config config) {
        log.debug("初始化server ...");
        Server server = new Server(config.getServer());
        log.debug("已初始化server!\n\t{}", server);
        return server;
    }

    private void build(Build build) throws Exception {
        log.debug("building ...");
        build.build();
        log.debug("built!");
    }

    private Build getBuild(Config config, Source source) {
        log.debug("初始化build ...");
        Build build = new Build(config.getBuild(), source);
        log.debug("已初始化build!\n\t{}", build);
        return build;
    }

    private Source getSource(Config config) {
        log.debug("初始化source ...");
        Source source = null;
        switch (config.getSource().getType()) {
            case LOCAL:
                source = new LocalSource(config.getSource().getLocal());
                break;

            case GIT:
                source = new GitSource(config.getSource().getGit());
                break;
        }
        log.debug("已初始化source!\n\t{}", source);
        return source;
    }

    private Config getConfig() throws IOException {
        log.debug("初始化 config ...");
        Config config = ItemService.getItemConfig(itemName);
        config.validate();
        log.debug("已初始化 config!\n{}", config);
        return config;
    }

    private void initIemInfo() throws IOException {
        // getItemInfo
        itemInfo = ItemService.getItemInfo(itemName);

        // 获取部署时间和日志相对路径
        lastDeploymentTime = LocalDateTime.now();
        logRelativePath = ItemService.getLogRelativePath(itemName, DateUtils.format(lastDeploymentTime, "yyyy-MM-dd_HHmmss") + ".log");

        // 重新设置 itemInfo
        itemInfo.setName(itemName);
        sourceStage = new ItemStage("拉取资源");
        buildrStage = new ItemStage("构建");
        serverStage = new ItemStage("连接服务");
        deploymentStage = new ItemStage("部署");
        itemInfo.setStages(List.of(sourceStage, buildrStage, serverStage, deploymentStage));
        itemInfo.setLastDeploymentTime(lastDeploymentTime);

        // flush
        flush();
    }

    @Override
    public void flush() throws IOException {
        ItemService.writeItemInfo(itemName, itemInfo);
        ItemWebSocketManager.broadcast(JacksonUtils.toJson(itemInfo));
    }

}
