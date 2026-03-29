package com.shortlink.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shortlink.common.util.ShortLinkUtil;
import com.shortlink.core.entity.ShortLink;
import com.shortlink.core.mapper.ShortLinkMapper;
import com.shortlink.common.metrics.CustomMetrics;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ShortLinkGenerateService {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkGenerateService.class);

    @Autowired
    private ShortLinkMapper shortLinkMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CustomMetrics customMetrics;

    // 注入 RedisTemplate 用于同步短码到 Redis Set
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Redis 存储全量短码的 Key（和网关布隆配置一致）
    private static final String REDIS_SHORT_CODE_SET = "shortlink:all:shortcodes";
    @PostConstruct
    public void initShortCodesToRedis() {
        // 1. 查询数据库所有短码
        List<String> shortCodes = shortLinkMapper.selectList(null).stream()
                .map(ShortLink::getShortCode)
                .toList();

        // 2. 批量写入Redis全量集合
        if (!shortCodes.isEmpty()) {
            redisTemplate.opsForSet().add(REDIS_SHORT_CODE_SET, shortCodes.toArray(new String[0]));
            System.out.println("历史短码写入Redis完成，数量：" + shortCodes.size());
        }
    }

    @Transactional
    public String generateShortLink(String longUrl) {
        if (longUrl == null || longUrl.trim().isEmpty()) {
            log.warn("生成短链时 longUrl 为空");
            return null;
        }

        // 1. 检查长链接是否已存在（防重复生成）
        LambdaQueryWrapper<ShortLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShortLink::getLongUrl, longUrl);
        ShortLink exist = shortLinkMapper.selectOne(wrapper);
        if (exist != null) {
            return exist.getShortCode();
        }

        // 2. 生成短码 + 分布式锁防并发冲突
        String shortCode;
        RLock lock = redissonClient.getLock("shortlink:generate:lock:" + longUrl.hashCode());

        try {
            boolean locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取生成锁失败 longUrl={}", longUrl);
                throw new RuntimeException("系统繁忙，请稍后重试");
            }

            shortCode = ShortLinkUtil.generateShortCode(longUrl);
            int maxRetries = 5;
            while (maxRetries-- > 0) {
                LambdaQueryWrapper<ShortLink> codeWrapper = new LambdaQueryWrapper<>();
                codeWrapper.eq(ShortLink::getShortCode, shortCode);
                if (shortLinkMapper.selectCount(codeWrapper) == 0) {
                    break;
                }
                shortCode = ShortLinkUtil.generateShortCode(longUrl + System.nanoTime());
            }

            // 3. 保存到数据库
            ShortLink link = new ShortLink();
            link.setShortCode(shortCode);
            link.setLongUrl(longUrl);
            link.setStatus(0);
            link.setCreateTime(LocalDateTime.now());
            link.setExpireTime(LocalDateTime.now().plusDays(30));

            shortLinkMapper.insert(link);

            // 4. 同步写入 Redis Set（供网关本地布隆过滤器加载）
            redisTemplate.opsForSet().add(REDIS_SHORT_CODE_SET, shortCode);
          //  redisTemplate.opsForSet().add(REDIS_SHORT_CODE_SET, shortCode);

            // 5. 监控指标
            customMetrics.incrementGenerate();

            log.info("成功生成短链 shortCode={} longUrl={}", shortCode, longUrl);

            return shortCode;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("生成短链中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}