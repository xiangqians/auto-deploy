package org.auto.deploy;

import lombok.extern.slf4j.Slf4j;
import org.auto.deploy.core.TaskScheduler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * 自动化部署应用
 *
 * @author xiangqian
 * @date 20:50 2022/09/09
 */
@Slf4j
@SpringBootApplication
public class AutoDeployApplication implements ApplicationListener<ContextClosedEvent> {

    public static void main(String[] args) {
        SpringApplication.run(AutoDeployApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.debug("应用关闭!");
        TaskScheduler.shutdown();
    }

}
