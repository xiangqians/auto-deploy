package org.auto.deploy.config.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

import java.util.List;

/**
 * @author xiangqian
 * @date 01:06 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StaticDeploymentConfig {

    /**
     * 部署位置
     */
    private String location;

    /**
     * 打包文件位置，相对路径
     */
    @JsonProperty("pkg-file")
    private String pkgFile;

    /**
     * 附加文件或目录集
     */
    @JsonProperty("addl-files")
    private List<String> addlFiles;

    public void validate() {
        Assert.notNull(location, "deployment.static.location不能为null");
        Assert.notNull(pkgFile, "deployment.static.pkg-file不能为null");
    }

}
