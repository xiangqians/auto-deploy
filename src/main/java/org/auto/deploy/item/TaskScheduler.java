package org.auto.deploy.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务调度器
 *
 * @author xiangqian
 * @date 21:54 2022/09/19
 */
@Slf4j
public class TaskScheduler {

    // 线程池任务调度器
    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private static volatile boolean isShutdown = false;

    /**
     * cancel
     *
     * @param scheduledFuture
     * @param mayInterruptIfRunning 如果运行可能会中断
     */
    public static boolean cancel(ScheduledFuture<?> scheduledFuture, boolean mayInterruptIfRunning) {
        if (Objects.nonNull(scheduledFuture)) {
            return scheduledFuture.cancel(mayInterruptIfRunning);
        }
        return false;
    }

    public static synchronized ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        if (isShutdown) {
            throw new UnsupportedOperationException("任务调度器已关闭!");
        }

        if (Objects.isNull(threadPoolTaskScheduler)) {
            log.debug("初始化线程池任务调度器 ...");
            threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
            // 初始化
            threadPoolTaskScheduler.initialize();
            // 设置线程池大小
            threadPoolTaskScheduler.setPoolSize(1);
            log.debug("已初始化线程池任务调度器!");
        }

        return threadPoolTaskScheduler.schedule(task, trigger);
    }

    public static synchronized void shutdown() {
        if (Objects.nonNull(threadPoolTaskScheduler)) {
            threadPoolTaskScheduler.shutdown();
            threadPoolTaskScheduler = null;
        }
        isShutdown = true;
    }

}
