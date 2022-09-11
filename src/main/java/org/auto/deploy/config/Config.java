package org.auto.deploy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.auto.deploy.config.builder.BuilderConfig;
import org.auto.deploy.config.deployment.DeploymentConfig;
import org.auto.deploy.config.source.SourceConfig;
import org.auto.deploy.util.JacksonUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * @author xiangqian
 * @date 22:19 2022/09/09
 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略不存在字段
public class Config {

    private ServerConfig server;
    private SourceConfig source;
    private BuilderConfig builder;
    private DeploymentConfig deployment;

    public static Config get() throws IOException {
        String configLocation = Optional.ofNullable(System.getProperty("auto.deploy.config.location")).orElse("config.yml");
        log.info("configLocation: {}", configLocation);
        InputStream input = null;
        try {
            input = new FileInputStream(configLocation);
            Yaml yaml = new Yaml();
            return JacksonUtils.toObject(yaml.loadAs(input, Map.class), Config.class);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public void validate() {
        server.validate();
        source.validate();
        builder.validate();
        deployment.validate();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Config {").append('\n');
        builder.append('\t').append(server).append('\n');
        builder.append('\t').append(source).append('\n');
        builder.append('\t').append(this.builder).append('\n');
        builder.append('\t').append(deployment).append('\n');
        builder.append('}');
        return builder.toString();
    }

}
