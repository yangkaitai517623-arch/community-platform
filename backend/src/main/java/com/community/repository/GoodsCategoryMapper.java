package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.GoodsCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsCategoryMapper extends BaseMapper<GoodsCategory> {

    @Select("SELECT * FROM goods_category WHERE status = 1 ORDER BY sort_order")
    List<GoodsCategory> findActiveCategories();
}
