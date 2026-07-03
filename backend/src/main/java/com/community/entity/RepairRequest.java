package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检修需求表
 */
@Data
@TableName("repair_request")
public class RepairRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String description;

    private String repairType;

    private String location;

    private Integer urgency;

    private String aiTags;

    private String aiUrgency;

    private String images;

    /**
     * 状态: 0-待接单 1-已接单 2-检修中 3-已完成 4-已取消
     */
    private Integer status;

    private Long workerId;

    private BigDecimal estimatedPrice;

    private BigDecimal actualPrice;

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
