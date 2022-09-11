package org.auto.deploy.config.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

import java.util.Objects;

/**
 * git资源配置
 *
 * @author xiangqian
 * @date 00:41 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitSourceConfig {

    private String username;
    private String password;

    @JsonProperty("repo-url")
    private String repoUrl;
    private String branch;

    /**
     * 资源更新检测轮询计时器，单位s
     */
    @JsonProperty("poll-timer")
    private Integer pollTimer;

    public void validate() {
        Assert.notNull(repoUrl, "source.git.repo-url不能为null");
        Assert.notNull(branch, "source.git.branch不能为null");
        Assert.isTrue(Objects.nonNull(pollTimer) && pollTimer >= 0, "source.git.poll-timer不能小于0");
    }

}
