package com.community.controller;

import com.community.dto.AiMatchResult;
import com.community.dto.AiPriceResult;
import com.community.dto.Result;
import com.community.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /**
     * AI需求智能分类与匹配
     * 用户发布跑腿/检修需求时，自动提取服务类型、紧急程度和技能标签
     */
    @PostMapping("/classify")
    public Result<AiMatchResult> classifyRequest(@RequestBody Map<String, String> params) {
        try {
            String description = params.get("description");
            String type = params.getOrDefault("type", "errand");

            if (description == null || description.isEmpty()) {
                return Result.error("描述不能为空");
            }

            // 调用AI API进行智能分类
            AiMatchResult result = aiService.callAiForClassification(description, type);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI分类失败: " + e.getMessage());
        }
    }

    /**
     * AI二手商品估价与描述优化
     * 分析商品信息，给出估价区间并优化商品描述
     */
    @PostMapping("/estimate-price")
    public Result<AiPriceResult> estimatePrice(@RequestBody Map<String, Object> params) {
        try {
            String goodsName = (String) params.get("goodsName");
            String condition = (String) params.get("condition");
            BigDecimal originalPrice = new BigDecimal(params.get("originalPrice").toString());

            if (goodsName == null || goodsName.isEmpty()) {
                return Result.error("商品名称不能为空");
            }

            // 调用AI API进行智能估价
            AiPriceResult result = aiService.callAiForEstimation(goodsName, condition, originalPrice);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI估价失败: " + e.getMessage());
        }
    }
}
