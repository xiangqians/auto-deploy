package org.auto.deploy.web.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // 项目状态码
    // 1-执行正常且线程还在监听中（greed）
    // 2-执行正常但线程已结束（black）
    // 3-执行异常且线程已结束（red）
    // 4-执行异常但线程还在监听中（yellow）
    private Integer status;

}
