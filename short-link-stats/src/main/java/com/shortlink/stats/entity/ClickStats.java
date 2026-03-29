package com.shortlink.stats.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_click_stats")
public class ClickStats {
    private Long id;
    private String shortCode;
    private LocalDateTime clickTime;
    private String ip;
    private String userAgent;
    private String referer;
}