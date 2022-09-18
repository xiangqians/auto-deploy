package org.auto.deploy.item;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目阶段
 *
 * @author xiangqian
 * @date 22:51 2022/09/17
 */
@Data
public class ItemStage {

    /**
     * 项目阶段名称
     */
    private String name;

    /**
     * 项目阶段开始时间
     */
    private LocalDateTime startTime;

    /**
     * 项目阶段结束时间
     */
    private LocalDateTime endTime;

}
