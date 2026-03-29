package com.shortlink.monitor.controller;

import com.shortlink.common.metrics.CustomMetrics;    // 改为引用 common
import com.shortlink.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Autowired
    private CustomMetrics customMetrics;

    @PostMapping("/inc")
    public Result<Map<String, Object>> increment(@RequestParam String metric) {
        Map<String, Object> data = new HashMap<>();
        data.put("metric", metric);
        data.put("timestamp", System.currentTimeMillis());

        switch (metric) {
            case "shortlink.generate":
                customMetrics.incrementGenerate();
                break;
            case "shortlink.redirect":
                customMetrics.incrementRedirect();
                break;
            case "shortlink.click":
                customMetrics.incrementClick();
                break;
            case "bloom.reject":
                customMetrics.incrementBloomReject();
                break;
            case "cache.hit":
                customMetrics.incrementCacheHit();
                break;
            case "cache.miss":
                customMetrics.incrementCacheMiss();
                break;
            case "rocketmq.send":
                customMetrics.incrementRocketmqSend();
                break;
            case "sentinel.block":
                customMetrics.incrementSentinelBlock();
                break;
            default:
                return Result.error("不支持的指标类型：" + metric);
        }

        return Result.success(data);
    }

    @GetMapping("/active")
    public Result<Integer> getActiveShortLinks() {
        // 可以从 Redis 或 DB 查询实际活跃短链数
        return Result.success(12345);
    }
}