package com.shortlink.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_short_link")
public class ShortLink {
    private Long id;
    @TableField("short_code")// 主键ID（全局唯一）
    private String shortCode;       // 短码（如 abc123）
    private String longUrl;         // 原始URL
    private Integer status;         // 状态 0-正常 1-禁用
    private LocalDateTime createTime;
    private LocalDateTime expireTime; // 过期时间
    // 扩展字段：用户ID、域名等
}