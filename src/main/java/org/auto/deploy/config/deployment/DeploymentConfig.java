package org.auto.deploy.config.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author xiangqian
 * @date 01:06 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentConfig {

    /**
     * 部署类型, 可选值: [ static, jar, jar-docker ]
     */
    @JsonDeserialize(using = TypeJsonDeserializer.class)
    private Type type;

    @JsonProperty("static")
    private StaticDeploymentConfig stc;

    private JarDeploymentConfig jar;

    @JsonProperty("jar-docker")
    private JarDockerDeploymentConfig jarDocker;

    public void validate() {
        switch (type) {
            case STATIC:
                stc.validate();
                break;
            case JAR:
                jar.validate();
                break;
            case JAR_DOCKER:
                jarDocker.validate();
                break;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("DeploymentConfig {").append('\n');
        builder.append('\t').append('\t').append(type).append('\n');
        builder.append('\t').append('\t').append(stc).append('\n');
        builder.append('\t').append('\t').append(jar).append('\n');
        builder.append('\t').append('\t').append(jarDocker).append('\n');
        builder.append('\t').append('}');
        return builder.toString();
    }

    public static class TypeJsonDeserializer extends JsonDeserializer<Type> {

        @Override
        public Type deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
            String text = null;
            if (Objects.isNull(text = jsonParser.getText())) {
                return null;
            }
            return Type.of(text);
        }

    }

    @Getter
    public static enum Type {
        STATIC("static"),
        JAR("jar"),
        JAR_DOCKER("jar-docker"),
        ;
        private final String value;

        Type(String value) {
            this.value = value;
        }

        public static Type of(String value) {
            if (Objects.isNull(value)) {
                throw new IllegalArgumentException("必须指定部署类型(deployment.type), 可选值: [ static, jar, jar-docker ]");
            }

            for (Type type : Type.values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(String.format("目前不支持 %s 部署类型(deployment.type), 可选值: [ static, jar, jar-docker ]", value));
        }

    }

}
