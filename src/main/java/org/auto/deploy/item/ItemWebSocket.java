package org.auto.deploy.item;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 * @author xiangqian
 * @date 00:07 2022/09/19
 */
@Slf4j
@Component
@ServerEndpoint("/item")    // 指定websocket 连接的url
public class ItemWebSocket {

    @OnOpen
    public void onOpen(Session session) {
        log.info("{} 连接成功", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        log.info("{} 连接断开", session.getId());
    }

    @OnMessage
    public String onMessage(Session session, String message) {
        log.info("{}, {}", session.getId(), message);
        return "server: undefined";
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.error("", t);
    }

}
