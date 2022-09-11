package org.auto.deploy.config.deployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * @author xiangqian
 * @date 01:11 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JarDockerDeploymentConfig extends JarDeploymentConfig {

    /**
     * 执行docker超时时间s
     */
    private Integer timeout;

    /**
     * docker命令
     */
    @JsonProperty("run-cmd")
    private List<String> runCmd;

    public void validate() {
        Assert.notNull(getPkgFile(), "deployment.jar-docker.pkg-file不能为null");
        Assert.isTrue(Objects.nonNull(timeout) && timeout > 0, "deployment.jar-docker.timeout必须大于0");
        Assert.notNull(runCmd, "deployment.jar-docker.run-cmd不能为null");
        if (!runCmd.stream().filter(str -> str.startsWith("--name ")).findFirst().map(str -> true).orElse(false)) {
            throw new IllegalArgumentException("deployment.jar-docker.run-cmd必须指定--name");
        }
        if (!runCmd.stream().filter(str -> str.startsWith("-t ")).findFirst().map(str -> true).orElse(false)) {
            throw new IllegalArgumentException("deployment.jar-docker.run-cmd必须指定-t");
        }
    }

}
