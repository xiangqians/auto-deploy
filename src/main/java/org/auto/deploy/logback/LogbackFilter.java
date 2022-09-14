package org.auto.deploy.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import org.apache.commons.codec.digest.DigestUtils;
import org.auto.deploy.cache.AutoDeployCacheManager;
import org.springframework.cache.Cache;

import java.util.Objects;

/**
 * @author xiangqian
 * @date 01:34 2022/09/14
 */
public class LogbackFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        Cache cache = AutoDeployCacheManager.getCache(event.getLoggerName());
        if (Objects.nonNull(cache)) {
            String message = event.getMessage();
            cache.put(DigestUtils.md5Hex(message), message);
        }
        return FilterReply.NEUTRAL;
    }

}
