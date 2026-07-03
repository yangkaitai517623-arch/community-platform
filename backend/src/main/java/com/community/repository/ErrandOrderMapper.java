package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.ErrandOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErrandOrderMapper extends BaseMapper<ErrandOrder> {

    @Select("SELECT * FROM errand_order WHERE request_id = #{requestId} LIMIT 1")
    ErrandOrder findByRequestId(@Param("requestId") Long requestId);

    @Select("SELECT * FROM errand_order WHERE runner_id = #{runnerId} ORDER BY created_at DESC")
    List<ErrandOrder> findByRunnerId(@Param("runnerId") Long runnerId);

    @Select("SELECT COUNT(*) FROM errand_order WHERE status = #{status}")
    int countByStatus(@Param("status") Integer status);
}
