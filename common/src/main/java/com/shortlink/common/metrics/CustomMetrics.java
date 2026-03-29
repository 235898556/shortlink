package com.shortlink.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CustomMetrics {

    // ==================== Counters ====================
    private final Counter generateCounter;
    private final Counter redirectCounter;
    private final Counter clickCounter;               // 注意：这里还是 Counter，但支持批量累加
    private final Counter bloomRejectCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter rocketmqSendCounter;
    private final Counter sentinelBlockCounter;

    // 耗时统计
    private final Timer redirectTimer;

    // Gauge 示例
    private final AtomicInteger activeShortLinks = new AtomicInteger(0);

    public CustomMetrics(MeterRegistry registry) {
        this.generateCounter = Counter.builder("shortlink.generate.total")
                .description("短链生成总次数")
                .tag("type", "generate")
                .register(registry);

        this.redirectCounter = Counter.builder("shortlink.redirect.total")
                .description("短链重定向请求总次数")
                .tag("type", "redirect")
                .register(registry);

        this.clickCounter = Counter.builder("shortlink.click.total")
                .description("有效点击次数（来自 RocketMQ 异步统计）")
                .tag("type", "click")
                .register(registry);

        this.bloomRejectCounter = Counter.builder("shortlink.bloom.reject.total")
                .description("布隆过滤器拦截无效短码次数（防缓存穿透）")
                .tag("type", "bloom")
                .register(registry);

        this.cacheHitCounter = Counter.builder("shortlink.cache.hit.total")
                .description("多级缓存命中次数（Caffeine + Redis）")
                .tag("type", "cache")
                .register(registry);

        this.cacheMissCounter = Counter.builder("shortlink.cache.miss.total")
                .description("多级缓存未命中次数（走数据库）")
                .tag("type", "cache")
                .register(registry);

        this.rocketmqSendCounter = Counter.builder("shortlink.rocketmq.send.total")
                .description("RocketMQ 发送点击统计消息次数")
                .tag("type", "rocketmq")
                .register(registry);

        this.sentinelBlockCounter = Counter.builder("shortlink.sentinel.block.total")
                .description("Sentinel 限流/熔断触发次数")
                .tag("type", "sentinel")
                .register(registry);

        // 耗时统计
        this.redirectTimer = Timer.builder("shortlink.redirect.duration")
                .description("短链重定向接口耗时分布（毫秒）")
                .tag("type", "redirect")
                .register(registry);

        // Gauge
        Gauge.builder("shortlink.active.shortlinks", activeShortLinks, AtomicInteger::get)
                .description("当前活跃短链数量")
                .register(registry);
    }

    // ====================== 公共方法（支持批量）======================
    public void incrementGenerate() {
        generateCounter.increment();
    }

    public void incrementGenerate(long delta) {
        generateCounter.increment(delta);
    }

    public void incrementRedirect() {
        redirectCounter.increment();
    }

    public void incrementRedirect(long delta) {
        redirectCounter.increment(delta);
    }

    /**
     * 单次点击增加
     */
    public void incrementClick() {
        clickCounter.increment();
    }

    /**
     * 批量增加点击次数（解决 Stats 模块批量消费时一次性记录多条的诉求）
     *
     * @param delta 增加的次数
     */
    public void incrementClick(long delta) {
        clickCounter.increment(delta);
    }

    public void incrementBloomReject() {
        bloomRejectCounter.increment();
    }

    public void incrementBloomReject(long delta) {
        bloomRejectCounter.increment(delta);
    }

    public void incrementCacheHit() {
        cacheHitCounter.increment();
    }

    public void incrementCacheHit(long delta) {
        cacheHitCounter.increment(delta);
    }

    public void incrementCacheMiss() {
        cacheMissCounter.increment();
    }

    public void incrementCacheMiss(long delta) {
        cacheMissCounter.increment(delta);
    }

    public void incrementRocketmqSend() {
        rocketmqSendCounter.increment();
    }

    public void incrementRocketmqSend(long delta) {
        rocketmqSendCounter.increment(delta);
    }

    public void incrementSentinelBlock() {
        sentinelBlockCounter.increment();
    }

    public void incrementSentinelBlock(long delta) {
        sentinelBlockCounter.increment(delta);
    }

    /**
     * 记录重定向耗时
     */
    public void recordRedirectDuration(long milliseconds) {
        redirectTimer.record(milliseconds, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void setActiveShortLinks(int count) {
        activeShortLinks.set(count);
    }

    // 如果需要暴露某些查询方法给 Controller
    public double getRedirectCount() {
        return redirectCounter.count();
    }
}