package org.auto.deploy.item;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.item.build.ItemBuild;
import org.auto.deploy.item.deployment.ItemDeployment;
import org.auto.deploy.item.deployment.jar.ItemJarDeployment;
import org.auto.deploy.item.deployment.jar.docker.ItemJarDockerDeployment;
import org.auto.deploy.item.deployment.stc.ItemStaticDeployment;
import org.auto.deploy.item.server.ItemServer;
import org.auto.deploy.item.source.ItemGitSource;
import org.auto.deploy.item.source.ItemLocalSource;
import org.auto.deploy.item.source.ItemSource;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author xiangqian
 * @date 00:03 2022/09/19
 */
@Slf4j
public class ItemDeployer extends Thread implements Closeable {

    private String itemName;
    private ItemInfo itemInfo;
    private ItemConfig config;
    private ItemServer server;
    private ItemSource source;
    private ItemBuild builder;
    private ItemDeployment deployment;

    public ItemDeployer(String itemName) {
        super(itemName);
        this.itemName = itemName;
    }

    @Override
    public void run() {
        try {
            run0();
        } catch (Exception e) {
            log.error("", e);
        } finally {
            IOUtils.closeQuietly(this);
        }
    }

    public void run0() throws Exception {

        // ========== init
        // config
        log.debug("初始化config ...");
        config = ItemService.getItemConfig(itemName);
        config.validate();
        log.debug("已初始化config!\n{}", config);

        if (config != null) {
            return;
        }

        // server
        log.debug("初始化server ...");
        server = new ItemServer(config.getServer());
        log.debug("已初始化server!\n\t{}", server);

        // source
        log.debug("初始化source ...");
        switch (config.getSource().getType()) {
            case LOCAL:
                source = new ItemLocalSource(config.getSource().getLocal());
                break;

            case GIT:
                source = new ItemGitSource(config.getSource().getGit());
                break;
        }
        log.debug("已初始化source!\n\t{}", source);

        // builder
        log.debug("初始化builder ...");
        builder = new ItemBuild(config.getBuild(), source);
        log.debug("已初始化builder!\n\t{}", builder);

        // deployment
        log.debug("初始化deployment ...");
        switch (config.getDeployment().getType()) {
            case STATIC:
                deployment = new ItemStaticDeployment(config.getDeployment().getStc(), server, source);
                break;

            case JAR:
                deployment = new ItemJarDeployment(config.getDeployment().getJar(), server, source);
                break;

            case JAR_DOCKER:
                deployment = new ItemJarDockerDeployment(config.getDeployment().getJarDocker(), server, source);
                break;
        }
        log.debug("已初始化deployment!\n\t{}", deployment);

        // ========== execute

        switch (config.getSource().getType()) {
            case LOCAL:
                buildAndDeploy();
                break;

            case GIT:
                ItemGitSource gitSource = (ItemGitSource) source;
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

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(server, source, builder, deployment);
        server = null;
        source = null;
        builder = null;
        deployment = null;
    }


}
