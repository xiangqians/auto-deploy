package org.net.ssh;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ssh
 *
 * @author xiangqian
 * @date 13:10 2022/07/23
 */
public interface Ssh extends Closeable {

    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 执行命令
     *
     * @param cmd      命令
     * @param timeout  执行命令超时时间
     * @param consumer 命令执行结果消费者
     * @throws Exception
     */
    void execute(String cmd, Duration timeout, Consumer<String> consumer) throws Exception;

    default void execute(String cmd, Consumer<String> consumer) throws Exception {
        execute(cmd, DEFAULT_TIMEOUT, consumer);
    }

    default List<String> execute(String cmd, Duration timeout) throws Exception {
        List<String> results = new ArrayList<>();
        execute(cmd, timeout, results::add);
        return results;
    }

    default List<String> execute(String cmd) throws Exception {
        return execute(cmd, DEFAULT_TIMEOUT);
    }

}
