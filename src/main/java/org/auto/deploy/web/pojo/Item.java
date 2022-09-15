package org.auto.deploy.web.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目信息
 *
 * @author xiangqian
 * @date 23:06 2022/09/13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    // 项目名称
    private String name;

    // 最新部署时间
    private LocalDateTime lastDeployTime;

    // 项目状态
    private List<Integer> statuses;

}
