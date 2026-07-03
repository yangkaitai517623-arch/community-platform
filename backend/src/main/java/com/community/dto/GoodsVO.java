package com.community.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GoodsVO {

    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private String conditionLevel;
    private String images;        // JSON数组
    private String image;         // 第一张图片URL（方便前端使用）
    private Long sellerId;
    private String sellerName;    // 卖家姓名
    private Integer status;
    private Integer viewCount;
    private LocalDateTime createdAt;
}
