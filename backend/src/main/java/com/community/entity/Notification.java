package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知表
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    /**
     * 通知类型: 1-系统通知 2-订单通知 3-活动通知
     */
    private Integer type;

    /**
     * 是否已读: 0-未读 1-已读
     */
    private Integer isRead;

    @TableField(exist = false)
    private String receiver;

    @TableField(exist = false)
    private String receiverName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
