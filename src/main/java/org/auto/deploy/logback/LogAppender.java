package org.auto.deploy.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.auto.deploy.core.ItemDeployer;
import org.auto.deploy.core.ItemService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * see: {@link ch.qos.logback.core.ConsoleAppender}
 *
 * @author xiangqian
 * @date 21:25 2022/09/16
 */
@Data
public class LogAppender<E> extends UnsynchronizedAppenderBase<E> {

    protected Encoder<E> encoder;

    @Override
    public void start() {
        int errors = 0;

        if (encoder == null) {
            addStatus(new ErrorStatus("No encoder set for the appender named \"" + name + "\".", this));
            ++errors;
        }

        if (errors == 0) {
            super.start();
        }
    }

    @Override
    protected void append(E eventObject) {
        if (Objects.isNull(eventObject)
                || !isStarted()
                || !(eventObject instanceof ILoggingEvent)) {
            return;
        }

        try {
            if (eventObject instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) eventObject).prepareForDeferredProcessing();
            }

            ILoggingEvent iLoggingEvent = (ILoggingEvent) eventObject;
            String threadName = iLoggingEvent.getThreadName();
            ItemDeployer itemDeployer = ItemService.getItemDeployer(threadName);
            if (Objects.nonNull(itemDeployer) && Objects.nonNull(itemDeployer.getLogPathName())) {
                byte[] bytes = encoder.encode(eventObject);
                if (ArrayUtils.isNotEmpty(bytes)) {
                    synchronized (LogAppender.class) {
                        FileUtils.write(new File(itemDeployer.getLogPathName()),
                                new String(bytes, StandardCharsets.UTF_8),
                                StandardCharsets.UTF_8, true);
                    }
                }
            }

        } catch (Exception e) {
            started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, e));
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

}
