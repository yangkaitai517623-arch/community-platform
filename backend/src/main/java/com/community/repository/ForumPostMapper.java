package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.ForumPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ForumPostMapper extends BaseMapper<ForumPost> {

    @Select("SELECT * FROM forum_post WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ForumPost> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM forum_post WHERE status = 1 ORDER BY created_at DESC")
    List<ForumPost> findPublished();

    @Select("SELECT * FROM forum_post ORDER BY created_at DESC LIMIT #{limit}")
    List<ForumPost> findRecent(@Param("limit") int limit);
}
