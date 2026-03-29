package com.shortlink.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

@Configuration
public class RateLimiterConfig {

    /**
     * 改进版 IP KeyResolver（优先取 X-Forwarded-For，防代理穿透）
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String realIp = headers.getFirst("X-Forwarded-For");
            if (realIp != null && !realIp.isEmpty()) {
                // 取第一个 IP（真实客户端）
                realIp = realIp.split(",")[0].trim();
            } else {
                realIp = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
            }
            return Mono.just(realIp);
        };
    }

    /**
     * 可选：按短码维度限流（防止单个短链被恶意刷）
     */
    @Bean
    public KeyResolver shortCodeKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getURI().getPath();
            String shortCode = path.substring(path.lastIndexOf('/') + 1);
            return Mono.just("shortcode:" + shortCode);
        };
    }
}