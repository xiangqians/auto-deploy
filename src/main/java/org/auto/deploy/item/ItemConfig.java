package org.auto.deploy.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.item.build.ItemBuild;
import org.auto.deploy.item.deployment.ItemDeployment;
import org.auto.deploy.item.server.ItemServer;
import org.auto.deploy.item.source.ItemSource;

/**
 * @author xiangqian
 * @date 22:19 2022/09/09
 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略不存在字段
public class ItemConfig {

    private String itemName;

    private ItemServer.Config server;
    private ItemSource.Config source;
    private ItemBuild.Config build;
    private ItemDeployment.Config deployment;

    public void validate() {
        server.validate();
        source.validate();
        build.validate();
        deployment.validate();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Config {").append('\n');
        builder.append('\t').append(server).append('\n');
        builder.append('\t').append(source).append('\n');
        builder.append('\t').append(build).append('\n');
        builder.append('\t').append(deployment).append('\n');
        builder.append('}');
        return builder.toString();
    }

}
