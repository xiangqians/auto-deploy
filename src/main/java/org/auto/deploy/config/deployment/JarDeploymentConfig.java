package org.auto.deploy.config.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

import java.util.List;

/**
 * @author xiangqian
 * @date 01:08 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JarDeploymentConfig {

    @JsonProperty("java-home")
    private String javaHome;

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
        Assert.notNull(javaHome, "deployment.jar.java-home不能为null");
        Assert.notNull(pkgFile, "deployment.jar.pkg-file不能为null");
    }

}
