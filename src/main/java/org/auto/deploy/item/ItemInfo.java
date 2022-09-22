package org.auto.deploy.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * item/{itemName}/info.json
 *
 * @author xiangqian
 * @date 22:32 2022/09/17
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemInfo {

    /**
     * 项目名称
     */
    private String name;

    // 阶段集
    private List<ItemStage> stages;

    /**
     * 最新一次构建时间
     */
    private LocalDateTime lastDeploymentTime;

}
