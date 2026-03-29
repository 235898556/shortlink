package com.shortlink.gateway.filter;

import com.shortlink.gateway.config.LocalBloomFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import jakarta.annotation.Resource;

@Component
public class ShortLinkBloomFilterGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkBloomFilterGlobalFilter.class);

    // 注入本地布隆过滤器
    @Resource
    private LocalBloomFilterConfig localBloomFilter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath().trim();

        // 只对短码路径生效（/xxx 6-10位字符）
        if (path.matches("^/[a-zA-Z0-9]{6,10}$")) {
            String shortCode = path.substring(1);

            // 🔥 本地内存布隆判断（无任何网络IO，微秒级）
            if (!localBloomFilter.mightContain(shortCode)) {
                log.debug("本地布隆拦截无效短码: {}", shortCode);
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }
        }

        // 有效请求，转发到core服务
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -200; // 最高优先级，在限流前拦截
    }
}