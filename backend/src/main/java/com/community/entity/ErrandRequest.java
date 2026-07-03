package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿需求表
 */
@Data
@TableName("errand_request")
public class ErrandRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String errandType;

    private String pickupAddress;

    private String deliveryAddress;

    private Integer urgency;

    private BigDecimal reward;

    private String aiTags;

    private String aiUrgency;

    /**
     * 状态: 0-待接单 1-已接单 2-配送中 3-已完成 4-已取消
     */
    private Integer status;

    private Long runnerId;

    @TableField(exist = false)
    private Integer orderStatus;

    @TableField(exist = false)
    private Integer orderRating;

    @TableField(exist = false)
    private String orderComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
