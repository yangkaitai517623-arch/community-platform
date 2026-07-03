package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.GoodsOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsOrderMapper extends BaseMapper<GoodsOrder> {

    @Select("SELECT * FROM goods_order WHERE buyer_id = #{buyerId} ORDER BY created_at DESC")
    List<GoodsOrder> findByBuyerId(@Param("buyerId") Long buyerId);

    @Select("SELECT * FROM goods_order WHERE seller_id = #{sellerId} ORDER BY created_at DESC")
    List<GoodsOrder> findBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT COUNT(*) FROM goods_order WHERE status = #{status}")
    int countByStatus(@Param("status") Integer status);
}
