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

    /**
     * 阶段集
     */
    private List<ItemStage> stages;

    private String lastRevCommit;

    /**
     * 最新一次构建时间
     */
    private LocalDateTime lastDeploymentTime;

//    /**
//     * 项目构建历史日志集
//     */
//    private List<DeploymentHistoryLog> deploymentHistoryLogs;

    /**
     * 阶段集
     *
     * @author xiangqian
     * @date 15:10 2022/09/18
     */
    @Data
    public class ItemStages {

        // git版本信息
        private String overview;

        private ItemStage sourceStage;
        private ItemStage buildStage;
        private ItemStage deploymentStage;

    }

    /**
     * 项目阶段
     *
     * @author xiangqian
     * @date 22:51 2022/09/17
     */
    @Data
    public static class ItemStage {

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


}
