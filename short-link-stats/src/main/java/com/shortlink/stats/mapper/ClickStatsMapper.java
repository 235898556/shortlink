package com.shortlink.stats.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.stats.entity.ClickStats;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface ClickStatsMapper extends BaseMapper<ClickStats> {
    int insertBatch(@Param("list") List<ClickStats> list);
}