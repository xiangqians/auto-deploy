package org.auto.deploy.logback;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * see: {@link ch.qos.logback.core.ConsoleAppender}
 *
 * @author xiangqian
 * @date 21:25 2022/09/16
 */
@Data
public class CustomAppender<E> extends UnsynchronizedAppenderBase<E> {

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
        if (Objects.isNull(eventObject) || !isStarted()) {
            return;
        }

        try {
            if (eventObject instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) eventObject).prepareForDeferredProcessing();
            }

            byte[] bytes = encoder.encode(eventObject);
            if (ArrayUtils.isNotEmpty(bytes)) {
                synchronized (this) {
                    System.err.println(eventObject+">>>>>>>>>" + new String(bytes, StandardCharsets.UTF_8));
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
