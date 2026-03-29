package com.shortlink.stats.service;

import com.shortlink.common.dto.ClickStatsDTO;
import com.shortlink.common.metrics.CustomMetrics;      // 改为引用 common
import com.shortlink.stats.entity.ClickStats;
import com.shortlink.stats.mapper.ClickStatsMapper;
import org.apache.shardingsphere.infra.hint.HintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Autowired
    private ClickStatsMapper clickStatsMapper;

    @Autowired
    private CustomMetrics customMetrics;

    @Transactional(rollbackFor = Exception.class)
    public void recordClickBatch(List<ClickStatsDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) return;

        List<ClickStats> list = dtos.stream()
                .map(this::convert)
                .collect(Collectors.toList());

        try (HintManager hint = HintManager.getInstance()) {
            String month = list.get(0).getClickTime().format(MONTH_FORMATTER);
            hint.addTableShardingValue("t_click_stats", month);

            // 使用 MyBatis-Plus 批量插入（推荐）
            clickStatsMapper.insertBatch(list);   // 或自定义 insertBatch
        }

        customMetrics.incrementClick(list.size());
        log.debug("批量插入点击记录 {} 条", list.size());
    }

    private ClickStats convert(ClickStatsDTO dto) {
        ClickStats stats = new ClickStats();
        stats.setShortCode(dto.getShortCode());
        stats.setClickTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(dto.getClickTime()), ZoneId.systemDefault()));
        stats.setIp(dto.getIp());
        stats.setUserAgent(dto.getUserAgent());
        stats.setReferer(dto.getReferer());
        return stats;
    }
    // 可选：单条记录调用批量方法
    public void recordClick(ClickStatsDTO dto) {
        recordClickBatch(List.of(dto));
    }
}