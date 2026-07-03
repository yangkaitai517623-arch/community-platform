package com.community.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiPriceResult {

    /**
     * AI估价的最低价
     */
    private BigDecimal priceRangeMin;

    /**
     * AI估价的最高价
     */
    private BigDecimal priceRangeMax;

    /**
     * AI估价的建议价
     */
    private BigDecimal estimatedPrice;

    /**
     * AI优化后的商品描述
     */
    private String optimizedDescription;

    /**
     * AI估价的说明/依据
     */
    private String explanation;

    public AiPriceResult() {}

    public AiPriceResult(BigDecimal priceRangeMin, BigDecimal priceRangeMax,
                         BigDecimal estimatedPrice, String optimizedDescription, String explanation) {
        this.priceRangeMin = priceRangeMin;
        this.priceRangeMax = priceRangeMax;
        this.estimatedPrice = estimatedPrice;
        this.optimizedDescription = optimizedDescription;
        this.explanation = explanation;
    }
}
