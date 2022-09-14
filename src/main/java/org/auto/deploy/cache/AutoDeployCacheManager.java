package org.auto.deploy.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xiangqian
 * @date 01:39 2022/09/14
 */
public class AutoDeployCacheManager {

    private static final CaffeineCacheManager CACHE_MANAGER;
    private static final Set<String> CACHE_NAME;

    static {
        CACHE_NAME = new HashSet<>();

        CACHE_MANAGER = new CaffeineCacheManager();
        CACHE_MANAGER.setCaffeine(Caffeine.newBuilder()
                // 初始缓存容量
                .initialCapacity(1024)
                // 设置写后过期时间
                .expireAfterWrite(Duration.ofMinutes(30))
                // 设置访问后过期时间
//                .expireAfterAccess(Duration.ofMinutes(1))
                // 设置cache的最大缓存数量
                .maximumSize(1024 * 1024));

        // 设置缓存加载器
        CACHE_MANAGER.setCacheLoader(key -> null);

        // 不允许空值
        CACHE_MANAGER.setAllowNullValues(false);
    }

    public static Cache getCache(String name) {
        if (!CACHE_NAME.contains(name)) {
            return null;
        }
        return CACHE_MANAGER.getCache(name);
    }

    public static boolean addCacheName(String cacheName) {
        return CACHE_NAME.add(cacheName);
    }

}
