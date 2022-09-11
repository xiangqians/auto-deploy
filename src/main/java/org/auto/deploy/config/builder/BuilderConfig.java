package org.auto.deploy.config.builder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 构建器配置
 *
 * @author xiangqian
 * @date 12:24 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuilderConfig {

    /**
     * 构建命令集
     */
    private List<List<String>> cmds;

    public void validate() {
    }

}
