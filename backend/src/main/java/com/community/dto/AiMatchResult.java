package com.community.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiMatchResult {

    /**
     * AI分析的服务类型
     */
    private String serviceType;

    /**
     * AI分析的紧急程度: low / medium / high
     */
    private String urgency;

    /**
     * AI提取的技能标签列表
     */
    private List<String> skillTags;

    /**
     * AI推荐的工人ID列表
     */
    private List<Long> matchedWorkerIds;

    /**
     * AI分析的简要说明
     */
    private String description;

    public AiMatchResult() {}

    public AiMatchResult(String serviceType, String urgency, List<String> skillTags,
                         List<Long> matchedWorkerIds, String description) {
        this.serviceType = serviceType;
        this.urgency = urgency;
        this.skillTags = skillTags;
        this.matchedWorkerIds = matchedWorkerIds;
        this.description = description;
    }
}
