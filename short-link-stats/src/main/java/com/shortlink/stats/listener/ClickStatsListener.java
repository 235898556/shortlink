package com.shortlink.stats.listener;

import com.shortlink.common.dto.ClickStatsDTO;
import com.shortlink.common.metrics.CustomMetrics;      // 改为引用 common
import com.shortlink.stats.service.StatsService;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RocketMQMessageListener(
        topic = "click-stats-topic",
        consumerGroup = "stats-consumer-group",
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        consumeThreadMax = 24
)
public class ClickStatsListener implements RocketMQListener<List<ClickStatsDTO>> {

    private static final Logger log = LoggerFactory.getLogger(ClickStatsListener.class);

    @Autowired
    private StatsService statsService;

    @Autowired
    private CustomMetrics customMetrics;

    @Override
    public void onMessage(List<ClickStatsDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int batchSize = messages.size();

        try {
            statsService.recordClickBatch(messages);

            // 批量增加点击次数（一次性增加 batchSize 条）
            customMetrics.incrementClick(batchSize);   // 现在可以正常编译

            long cost = System.currentTimeMillis() - startTime;
            log.info("成功批量消费点击消息 {} 条，耗时 {} ms", batchSize, cost);

        } catch (Exception e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("批量消费点击统计失败，消息数量: {}，耗时: {} ms", batchSize, cost, e);
            throw new RuntimeException("点击统计批量消费失败", e);
        }
    }
}