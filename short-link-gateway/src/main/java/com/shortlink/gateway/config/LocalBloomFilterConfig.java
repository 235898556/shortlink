package com.shortlink.gateway.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 网关本地布隆过滤器（纯内存，无网络IO）
 */
@Component
public class LocalBloomFilterConfig {

    // 布隆过滤器容量：1亿个短码，误判率 0.001（极致精准）
    private static final long CAPACITY = 100_000_000L;
    private static final double FPP = 0.001;

    // 本地布隆过滤器（单例）
    private BloomFilter<String> bloomFilter;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    // Redis存储全量短码的key（core服务新增短码时写入这里）
    private static final String REDIS_SHORT_CODE_SET = "shortlink:all:shortcodes";

    @PostConstruct
    public void initBloomFilter() {
        // 1. 创建本地布隆过滤器
        bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                CAPACITY,
                FPP
        );

        // 2. 从Redis加载全量短码到本地布隆
        Set<String> allShortCodes = redisTemplate.opsForSet().members(REDIS_SHORT_CODE_SET);
        if (allShortCodes != null && !allShortCodes.isEmpty()) {
            allShortCodes.forEach(bloomFilter::put);
            System.out.println("✅ 网关布隆过滤器初始化完成，加载短码数量：" + allShortCodes.size());
        }
    }

    /**
     * 判断短码是否存在（对外提供方法）
     */
    public boolean mightContain(String shortCode) {
        return bloomFilter.mightContain(shortCode);
    }

    /**
     * 新增短码到布隆过滤器（core服务新增短码后调用）
     */
    public void put(String shortCode) {
        bloomFilter.put(shortCode);
    }
}