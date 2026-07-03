package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI商品估价记录表
 */
@Data
@TableName("ai_price_record")
public class AiPriceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long goodsId;

    private String goodsName;

    private String conditionLevel;

    private BigDecimal originalPrice;

    private BigDecimal aiEstimatedPrice;

    private BigDecimal aiPriceRangeMin;

    private BigDecimal aiPriceRangeMax;

    private String aiDescription;

    private String similarHistory;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
