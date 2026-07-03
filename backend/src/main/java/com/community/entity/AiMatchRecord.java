package com.community.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI服务匹配记录表
 */
@Data
@TableName("ai_match_record")
public class AiMatchRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestType;

    private Long requestId;

    private String rawDescription;

    private String aiServiceType;

    private String aiUrgency;

    private String aiSkills;

    private String matchedWorkers;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
