package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品表
 */
@Data
@TableName("second_hand_goods")
public class SecondHandGoods {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private Long categoryId;

    private BigDecimal originalPrice;

    private BigDecimal sellingPrice;

    private BigDecimal aiEstimatedPrice;

    private String conditionLevel;

    private String images;

    private Long sellerId;

    private Long buyerId;

    /**
     * 状态: 0-待售 1-已售 2-已下架
     */
    private Integer status;

    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
