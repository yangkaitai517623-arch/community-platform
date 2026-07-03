package com.community.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.entity.ForumComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ForumCommentMapper extends BaseMapper<ForumComment> {

    @Select("SELECT * FROM forum_comment WHERE post_id = #{postId} AND status = 1 AND deleted = 0 ORDER BY created_at ASC")
    List<ForumComment> findByPostId(@Param("postId") Long postId);

    @Select("SELECT * FROM forum_comment WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<ForumComment> findByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM forum_comment WHERE post_id = #{postId} AND status = 1 AND deleted = 0")
    int countPublishedByPostId(@Param("postId") Long postId);
}
