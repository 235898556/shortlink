package com.shortlink.common.dto;



import lombok.Data;
import java.io.Serializable;

@Data
public class ClickStatsDTO implements Serializable {
    private String shortCode;       // 短码
    private Long clickTime;         // 点击时间戳（毫秒）
    private String ip;              // 客户端IP
    private String userAgent;       // User-Agent
    private String referer;         // 来源页面
    // 可根据需要扩展其他字段
}
