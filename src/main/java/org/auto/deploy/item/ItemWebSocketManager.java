package org.auto.deploy.item;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xiangqian
 * @date 22:27 2022/09/22
 */
@Slf4j
public class ItemWebSocketManager {

    private static final Map<String, ItemWebSocket> MAP;

    static {
        MAP = new ConcurrentHashMap<>();
    }

    public static Collection<ItemWebSocket> collection() {
        return MAP.values();
    }

    public static boolean remove(ItemWebSocket itemWebSocket) {
        if (Objects.isNull(itemWebSocket)) {
            return false;
        }
        return Objects.nonNull(MAP.remove(itemWebSocket.getId()));
    }

    public static boolean add(ItemWebSocket itemWebSocket) {
        MAP.put(itemWebSocket.getId(), itemWebSocket);
        return true;
    }

    public static void broadcast(String text) {
        Collection<ItemWebSocket> itemWebSockets = MAP.values();
        if (CollectionUtils.isNotEmpty(itemWebSockets)) {
            log.debug("广播 ... {}", itemWebSockets.size());
            itemWebSockets.forEach(itemWebSocket -> itemWebSocket.sendText(text));
            log.debug("已广播!");
        }

    }

}
