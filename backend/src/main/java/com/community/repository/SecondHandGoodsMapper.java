package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.SecondHandGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SecondHandGoodsMapper extends BaseMapper<SecondHandGoods> {

    @Select("SELECT * FROM second_hand_goods WHERE seller_id = #{sellerId} AND deleted = 0 ORDER BY created_at DESC")
    List<SecondHandGoods> findBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT * FROM second_hand_goods WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<SecondHandGoods> findByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM second_hand_goods WHERE status = 1 AND deleted = 0")
    int countOnSale();

    @Select("SELECT * FROM second_hand_goods WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<SecondHandGoods> findRecent(@Param("limit") int limit);
}
