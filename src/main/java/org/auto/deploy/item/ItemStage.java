package org.auto.deploy.item;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目阶段
 *
 * @author xiangqian
 * @date 22:51 2022/09/17
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemStage {

    /**
     * 项目阶段名称
     */
    private String name;

    /**
     * 项目阶段描述
     */
    private String desc;

    /**
     * 项目阶段开始时间戳
     */
    private Long startTime;

    /**
     * 项目阶段结束时间戳
     */
    private Long endTime;

    public ItemStage(String name) {
        this.name = name;
    }

}
