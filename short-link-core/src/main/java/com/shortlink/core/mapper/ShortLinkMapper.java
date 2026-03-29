package com.shortlink.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.core.entity.ShortLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLink> {
}