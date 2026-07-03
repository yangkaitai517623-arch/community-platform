package com.community.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("errand_order")
public class ErrandOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long requestId;

    private Long userId;

    private Long runnerId;

    private BigDecimal amount;

    /**
     * 0-in progress, 1-completed, 2-reviewed.
     */
    private Integer status;

    private Integer rating;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
