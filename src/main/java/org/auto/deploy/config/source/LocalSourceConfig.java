package org.auto.deploy.config.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

/**
 * 本地资源配置
 *
 * @author xiangqian
 * @date 00:40 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalSourceConfig {

    /**
     * 本地资源位置
     */
    private String location;

    /**
     * 是否使用临时工作空间
     * 如果设置为true，则会创建一个临时目录来处理资源；
     * 如果设置为false，则会在当前指定的位置处理资源；
     */
    @JsonProperty("use-temp-workspace")
    private Boolean useTempWorkspace;

    public void validate() {
        Assert.notNull(location, "source.local.location不能为null");
    }

}
