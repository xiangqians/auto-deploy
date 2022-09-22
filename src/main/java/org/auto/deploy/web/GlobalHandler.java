package org.auto.deploy.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局处理器
 *
 * @author xiangqian
 * @date 23:27 2022/08/16
 */
@Slf4j
@RestControllerAdvice(basePackages = {"org.auto.deploy.item"})
public class GlobalHandler {

    /**
     * internal server error
     *
     * @param exception
     * @return
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleException(Exception exception) {
        log.error(exception.getMessage(), exception);
        return exception.getMessage();
    }

}
