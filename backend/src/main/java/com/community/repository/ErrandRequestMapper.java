package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.ErrandRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ErrandRequestMapper extends BaseMapper<ErrandRequest> {

    @Select("SELECT * FROM errand_request WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<ErrandRequest> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM errand_request WHERE status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<ErrandRequest> findByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM errand_request WHERE status = 0 AND deleted = 0")
    int countPending();

    @Select("SELECT * FROM errand_request WHERE urgency = 1 AND status = 0 AND deleted = 0 ORDER BY created_at DESC")
    List<ErrandRequest> findUrgent();

    @Select("SELECT COUNT(*) FROM errand_request " +
            "WHERE runner_id = #{runnerId} " +
            "AND status IN (1, 2) " +
            "AND deleted = 0 " +
            "AND id <> #{excludeRequestId}")
    int countActiveByRunnerIdExcludeRequest(@Param("runnerId") Long runnerId,
                                            @Param("excludeRequestId") Long excludeRequestId);

    @Select("SELECT * FROM errand_request WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<ErrandRequest> findRecent(@Param("limit") int limit);
}
