package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.ForumLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ForumLikeMapper extends BaseMapper<ForumLike> {

    @Select("SELECT COUNT(*) FROM forum_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int countByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM forum_like WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);

    @Delete("DELETE FROM forum_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
