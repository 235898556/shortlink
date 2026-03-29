package com.shortlink.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "short-link-monitor")  // name 对应服务名（在 Nacos 中注册的服务名）
public interface MonitorClient {

    @PostMapping("/monitor/inc")
    void increment(@RequestParam("metric") String metric);
}