package org.auto.deploy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.auto.deploy.util.Assert;

import java.util.Objects;

/**
 * 服务器配置
 *
 * @author xiangqian
 * @date 00:03 2022/09/10
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfig {

    private String host;
    private Integer port;

    private String username;
    private String password;

    /**
     * session连接超时，单位s
     */
    @JsonProperty("session-conn-timeout")
    private Integer sessionConnTimeout;

    /**
     * channel连接超时，单位s
     */
    @JsonProperty("channel-conn-timeout")
    private Integer channelConnTimeout;

    /**
     * 工作目录
     */
    @JsonProperty("work-dir")
    private String workDir;

    private Boolean sudo;

    public void validate() {
        Assert.notNull(host, "host不能为null");
        Assert.notNull(port, "port不能为null");
        Assert.notNull(username, "username不能为null");
        Assert.notNull(password, "password不能为null");
        Assert.isTrue(Objects.nonNull(sessionConnTimeout) && sessionConnTimeout > 0, "session-conn-timeout必须大于0");
        Assert.isTrue(Objects.nonNull(channelConnTimeout) && channelConnTimeout > 0, "channel-conn-timeout必须大于0");
        Assert.notNull(workDir, "work-dir不能为null");
    }

}
