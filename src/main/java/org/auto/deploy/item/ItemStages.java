package org.auto.deploy.item;

import lombok.Data;

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
