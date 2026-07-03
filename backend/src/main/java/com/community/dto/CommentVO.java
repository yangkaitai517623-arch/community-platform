package com.community.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Integer likeCount;
    private Integer status;
    private LocalDateTime createdAt;

    // 关联字段
    private String userName;      // 评论者姓名
    private String postTitle;     // 所属帖子标题

    public CommentVO() {}
}
