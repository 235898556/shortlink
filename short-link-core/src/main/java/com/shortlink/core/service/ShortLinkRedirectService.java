package com.shortlink.core.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.shortlink.common.dto.ClickStatsDTO;
import com.shortlink.common.util.WebUtil;
import com.shortlink.core.entity.ShortLink;
import com.shortlink.core.mapper.ShortLinkMapper;
import com.shortlink.common.metrics.CustomMetrics;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkRedirectService {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkRedirectService.class);
    private static final String CACHE_KEY_PREFIX = "short:";

    @Autowired
    private ShortLinkMapper shortLinkMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    @Qualifier("caffeineCache")
    private Cache<String, String> localCache;
    @Autowired
    private CustomMetrics customMetrics;

    @SentinelResource(value = "getLongUrl", fallback = "fallbackGetLongUrl")
    public String getLongUrl(String shortCode, HttpServletRequest request) {
        if (shortCode == null || shortCode.length() < 6) {
            return null;
        }

        long start = System.currentTimeMillis();

        try {
            String cacheKey = CACHE_KEY_PREFIX + shortCode;

            // 1. 本地缓存（最快）
            String longUrl = localCache.getIfPresent(shortCode);
            if (longUrl != null) {
                handleHit(longUrl, shortCode, request);
                return "NULL".equals(longUrl) ? null : longUrl;
            }

            // 2. Redis 缓存
            longUrl = redisTemplate.opsForValue().get(cacheKey);
            if (longUrl != null) {
                localCache.put(shortCode, longUrl);
                handleHit(longUrl, shortCode, request);
                return "NULL".equals(longUrl) ? null : longUrl;
            }

            // 3. 数据库兜底
            ShortLink link = queryFromDBWithHint(shortCode);
            if (link == null) {
                redisTemplate.opsForValue().set(cacheKey, "NULL", 60, TimeUnit.SECONDS);
                localCache.put(shortCode, "NULL");
                customMetrics.incrementCacheMiss();
                return null;
            }

            longUrl = link.getLongUrl();

            // 4. 写回多级缓存
            long expire = 3600 + (long) (Math.random() * 1200);
            redisTemplate.opsForValue().set(cacheKey, longUrl, expire, TimeUnit.SECONDS);
            localCache.put(shortCode, longUrl);

            customMetrics.incrementCacheMiss();
            customMetrics.incrementRedirect();

            sendStatsMessage(shortCode, request);
            return longUrl;

        } finally {
            customMetrics.recordRedirectDuration(System.currentTimeMillis() - start);
        }
    }

    /** 命中后的统一处理 */
    private void handleHit(String longUrl, String shortCode, HttpServletRequest request) {
        customMetrics.incrementCacheHit();
        customMetrics.incrementRedirect();
        sendStatsMessage(shortCode, request);
    }

    private ShortLink queryFromDBWithHint(String shortCode) {
        try (HintManager hint = HintManager.getInstance()) {
            hint.addTableShardingValue("t_short_link", shortCode);
            return shortLinkMapper.selectOne(new LambdaQueryWrapper<ShortLink>()
                    .eq(ShortLink::getShortCode, shortCode));
        } catch (Exception e) {
            log.error("DB query error shortCode={}", shortCode, e);
            return null;
        }
    }

    private void sendStatsMessage(String shortCode, HttpServletRequest request) {
        ClickStatsDTO stats = new ClickStatsDTO();
        stats.setShortCode(shortCode);
        stats.setClickTime(System.currentTimeMillis());

        if (request != null) {
            stats.setIp(WebUtil.getClientIp(request));
            stats.setUserAgent(request.getHeader("User-Agent"));
            stats.setReferer(request.getHeader("Referer"));
        }

        customMetrics.incrementRocketmqSend();
        rocketMQTemplate.asyncSend("click-stats-topic", stats, null);
    }

    public String fallbackGetLongUrl(String shortCode, Throwable ex) {
        customMetrics.incrementSentinelBlock();
        return null;
    }
}