package com.shortlink.core.controller;   // 建议放在 controller 包下

import com.shortlink.core.service.ShortLinkRedirectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class RedirectController {

    @Autowired
    private ShortLinkRedirectService redirectService;

    /**
     * 短链重定向接口（核心高并发入口）
     */
    @GetMapping("/{shortCode}")
    public void redirect(@PathVariable String shortCode,
                         HttpServletRequest request,      // 新增：用于传递IP、UA等统计信息
                         HttpServletResponse response) throws IOException {

        // 调用优化后的服务方法（带 request 参数）
        String longUrl = redirectService.getLongUrl(shortCode, request);

        if (longUrl == null || "NULL".equals(longUrl)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "短链不存在或已过期");
            return;
        }

        // 执行 302 重定向
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        response.setHeader("Location", longUrl);

        // 可选：添加一些安全/统计相关的响应头
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
    }
}