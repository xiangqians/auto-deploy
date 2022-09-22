package org.auto.deploy.item;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.Objects;

/**
 * @author xiangqian
 * @date 00:07 2022/09/19
 */
@Slf4j
@Component
@ServerEndpoint("/item/websocket")
public class ItemWebSocket {

    @Getter
    private String id;
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        log.info("{} 连接成功", session.getId());
        this.session = session;
        this.id = session.getId();
        ItemWebSocketManager.add(this);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("{} 连接断开", session.getId());
        this.session = null;
        ItemWebSocketManager.remove(this);
    }

    @OnMessage
    public String onMessage(Session session, String message) {
        log.info("{}, {}", session.getId(), message);
        return "server: undefined";
    }

    @OnError
    public void onError(Session session, Throwable t) {
        log.error("", t);
        this.session = null;
        ItemWebSocketManager.remove(this);
    }

    public void sendText(String text) {
        if (Objects.nonNull(session)) {
            session.getAsyncRemote().sendText(text);
        }
    }

}
