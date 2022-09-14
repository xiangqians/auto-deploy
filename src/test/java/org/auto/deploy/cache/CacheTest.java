package org.auto.deploy.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.util.DigestUtils;

import java.util.Collection;
import java.util.Optional;

/**
 * @author xiangqian
 * @date 01:46 2022/09/14
 */
@Slf4j
public class CacheTest {

    public static void main(String[] args) throws Exception {
        Cache cache = AutoDeployCacheManager.getCache("test");
        for (int i = 0; i < 10; i++) {
            cache.put(i, i);
        }
        log.debug("{}", Optional.ofNullable(cache.get("test")).map(Cache.ValueWrapper::get).orElse(null));

        Collection values = (((CaffeineCache) cache).getNativeCache()).asMap().values();
        log.debug("{}", values);

    }

}
