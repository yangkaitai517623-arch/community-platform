package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 论坛帖子表
 */
@Data
@TableName("forum_post")
public class ForumPost {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String images;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    /**
     * 状态: 0-草稿 1-已发布 2-已屏蔽
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 当前登录用户是否已点赞，仅用于接口返回。
     */
    @TableField(exist = false)
    private Boolean liked;

    /**
     * 发帖人展示名，仅用于接口返回。
     */
    @TableField(exist = false)
    private String authorName;
}
