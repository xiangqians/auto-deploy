package org.auto.deploy.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.core.build.Build;
import org.auto.deploy.core.deployment.Deployment;
import org.auto.deploy.core.server.Server;
import org.auto.deploy.core.source.Source;

/**
 * @author xiangqian
 * @date 22:19 2022/09/09
 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略不存在字段
public class Config {

    private String md5;

    private Server.Config server;
    private Source.Config source;
    private Build.Config build;
    private Deployment.Config deployment;

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
