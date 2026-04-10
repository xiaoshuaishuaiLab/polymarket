package com.shuai.polymarket.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuai.polymarket.bot.entity.Market;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Market Table Mapper 接口
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Mapper
public interface MarketMapper extends BaseMapper<Market> {

}
