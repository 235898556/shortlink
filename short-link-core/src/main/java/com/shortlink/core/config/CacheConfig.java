package com.shortlink.core.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean("caffeineCache")
    public Cache<String, String> caffeineCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1_000_000)  // 初始容量100万
                .maximumSize(10_000_000)      // 最大1000万（支持更大规模）
                .expireAfterWrite(2, TimeUnit.HOURS)  // 写入过期（主）
                .expireAfterAccess(30, TimeUnit.MINUTES) // 访问过期（辅）
                .softValues()  // 内存不足时自动回收，防OOM
                .recordStats() // 监控缓存命中率
                .build();
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(6))           // Redis 存更久
                .disableCachingNullValues()
                .prefixCacheNameWith("shortlink:info:");

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}