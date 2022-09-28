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
        log.info("id={} 连接成功", session.getId());
        this.session = session;
        this.id = session.getId();
        ItemWebSocketManager.add(this);
    }

    @OnClose
    public void onClose(Session session) {
        log.info("id={} 连接断开", session.getId());
        this.session = null;
        ItemWebSocketManager.remove(this);
    }

    @OnMessage
    public String onMessage(Session session, String message) {
        log.info("id={}, message={}", session.getId(), message);
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
//org.eclipse.jetty.websocket.api.CloseException: java.util.concurrent.TimeoutException: Idle timeout expired: 300003/300000 ms
//	at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.onReadTimeout(AbstractWebSocketConnection.java:564)
//	at org.eclipse.jetty.io.AbstractConnection.onFillInterestedFailed(AbstractConnection.java:172)
//	at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.onFillInterestedFailed(AbstractWebSocketConnection.java:539)
//	at org.eclipse.jetty.io.AbstractConnection$ReadCallback.failed(AbstractConnection.java:317)
//	at org.eclipse.jetty.io.FillInterest.onFail(FillInterest.java:140)
//	at org.eclipse.jetty.io.AbstractEndPoint.onIdleExpired(AbstractEndPoint.java:407)
//	at org.eclipse.jetty.io.IdleTimeout.checkIdleTimeout(IdleTimeout.java:171)
//	at org.eclipse.jetty.io.IdleTimeout.idleCheck(IdleTimeout.java:113)
//	at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:515)
//	at java.base/java.util.concurrent.FutureTask.run$$$capture(FutureTask.java:264)
//	at java.base/java.util.concurrent.FutureTask.run(FutureTask.java)
//	at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304)
//	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
//	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
//	at java.base/java.lang.Thread.run(Thread.java:835)
//Caused by: java.util.concurrent.TimeoutException: Idle timeout expired: 300003/300000 ms
//	... 9 common frames omitted
