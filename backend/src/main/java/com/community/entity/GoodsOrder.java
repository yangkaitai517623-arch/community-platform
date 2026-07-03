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
@TableName("goods_order")
public class GoodsOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long goodsId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal amount;

    /**
     * 状态: 0-待确认 1-已确认 2-已完成 4-已取消
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
