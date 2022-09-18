package org.auto.deploy.item.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.Getter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 资源
 *
 * @author xiangqian
 * @date 01:30 2022/09/10
 */
public interface ItemSource extends Closeable {

    /**
     * 获取资源
     *
     * @return
     * @throws Exception
     */
    File get() throws Exception;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    class Config {

        /**
         * 资源类型, 可选值: [ local, git ]
         */
        @JsonDeserialize(using = TypeJsonDeserializer.class)
        private Type type;

        private ItemLocalSource.Config local;
        private ItemGitSource.Config git;

        public void validate() {
            switch (type) {
                case LOCAL:
                    local.validate();
                    break;
                case GIT:
                    git.validate();
                    break;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("SourceConfig {").append('\n');
            builder.append('\t').append('\t').append(type).append('\n');
            builder.append('\t').append('\t').append(local).append('\n');
            builder.append('\t').append('\t').append(git).append('\n');
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
            LOCAL("local"),
            GIT("git"),
            ;
            private final String value;

            Type(String value) {
                this.value = value;
            }

            public static Type of(String value) {
                if (Objects.isNull(value)) {
                    throw new IllegalArgumentException("必须指定资源类型(source.type), 可选值: [ local, git ]");
                }

                for (Type type : Type.values()) {
                    if (type.value.equals(value)) {
                        return type;
                    }
                }
                throw new IllegalArgumentException(String.format("目前不支持 %s 资源类型(source.type), 可选值: [ local, git ]", value));
            }

        }

    }

}
