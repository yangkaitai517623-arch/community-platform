package com.community.service;

import com.community.dto.AiMatchResult;
import com.community.dto.AiPriceResult;
import com.community.entity.AiMatchRecord;
import com.community.entity.AiPriceRecord;
import com.community.repository.AiMatchRecordMapper;
import com.community.repository.AiPriceRecordMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiMatchRecordMapper aiMatchRecordMapper;
    private final AiPriceRecordMapper aiPriceRecordMapper;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:your-api-key}")
    private String apiKey;

    @Value("${ai.api-url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public AiMatchResult classifyServiceRequest(String description) {
        return classifyServiceRequest(description, "service", null);
    }

    public AiMatchResult classifyServiceRequest(String description, String requestType, Long requestId) {
        String safeDescription = description == null ? "" : description;
        String serviceType = analyzeServiceType(safeDescription);
        String urgency = analyzeUrgency(safeDescription);
        List<String> skillTags = extractSkillTags(safeDescription);

        AiMatchResult result = new AiMatchResult();
        result.setServiceType(serviceType);
        result.setUrgency(urgency);
        result.setSkillTags(skillTags);
        result.setMatchedWorkerIds(Collections.emptyList());
        result.setDescription("AI分析完成: 服务类型=" + serviceType + ", 紧急程度=" + urgency);

        saveAiMatchRecord(requestType, requestId, safeDescription, result);
        return result;
    }

    public AiPriceResult estimateGoodsPrice(String goodsName, String condition, BigDecimal originalPrice) {
        return estimateGoodsPrice(goodsName, condition, originalPrice, null);
    }

    public AiPriceResult estimateGoodsPrice(String goodsName, String condition, BigDecimal originalPrice, Long goodsId) {
        BigDecimal basePrice = originalPrice == null ? BigDecimal.ZERO : originalPrice;
        BigDecimal discountRate = calculateDiscountRate(condition);
        BigDecimal estimatedPrice = basePrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal priceRangeMin = estimatedPrice.multiply(new BigDecimal("0.8")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal priceRangeMax = estimatedPrice.multiply(new BigDecimal("1.2")).setScale(2, RoundingMode.HALF_UP);

        AiPriceResult result = new AiPriceResult();
        result.setPriceRangeMin(priceRangeMin);
        result.setPriceRangeMax(priceRangeMax);
        result.setEstimatedPrice(estimatedPrice);
        result.setOptimizedDescription(generateOptimizedDescription(goodsName, condition));
        result.setExplanation("基于商品名称、成色和原价的规则兜底估价");

        saveAiPriceRecord(goodsId, goodsName, condition, basePrice, result);
        return result;
    }

    public AiMatchResult callAiForClassification(String description, String type) {
        return callAiForClassification(description, type, null);
    }

    public AiMatchResult callAiForClassification(String description, String type, Long requestId) {
        String typeDesc = "repair".equals(type) ? "检修" : "跑腿";
        String prompt = """
                你是社区服务平台的智能分类助手。请分析下面的%s需求描述，提取服务类型、紧急程度和技能标签。
                需求描述: %s

                只返回JSON，格式如下:
                {
                  "serviceType": "具体服务类型",
                  "urgency": "high/medium/low",
                  "skillTags": ["技能标签1", "技能标签2"],
                  "description": "一句话总结"
                }
                """.formatted(typeDesc, description);

        try {
            AiMatchResult result = parseClassifyResponse(callAiApi(prompt));
            saveAiMatchRecord(type, requestId, description, result);
            return result;
        } catch (Exception e) {
            log.warn("AI classification failed, fallback to rule engine: {}", e.getMessage());
            return classifyServiceRequest(description, type, requestId);
        }
    }

    public AiPriceResult callAiForEstimation(String goodsName, String condition, BigDecimal originalPrice) {
        return callAiForEstimation(goodsName, condition, originalPrice, null);
    }

    public AiPriceResult callAiForEstimation(String goodsName, String condition, BigDecimal originalPrice, Long goodsId) {
        String prompt = """
                你是二手商品估价助手。请根据商品名称、成色和原价给出合理估价区间，并优化商品描述。
                商品名称: %s
                商品成色: %s
                原价: %s

                只返回JSON，格式如下:
                {
                  "estimatedPrice": 100,
                  "priceRangeMin": 80,
                  "priceRangeMax": 120,
                  "optimizedDescription": "优化后的商品描述"
                }
                """.formatted(goodsName, condition, originalPrice);

        try {
            AiPriceResult result = parseEstimateResponse(callAiApi(prompt), originalPrice);
            saveAiPriceRecord(goodsId, goodsName, condition, originalPrice, result);
            return result;
        } catch (Exception e) {
            log.warn("AI price estimation failed, fallback to rule engine: {}", e.getMessage());
            return estimateGoodsPrice(goodsName, condition, originalPrice, goodsId);
        }
    }

    private String callAiApi(String prompt) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 500);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", "你是专业AI助手，请严格返回JSON格式数据。");
        messages.addObject()
                .put("role", "user")
                .put("content", prompt);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(objectMapper.writeValueAsString(payload), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AI API调用失败: " + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("AI API返回为空");
            }
            JsonNode json = objectMapper.readTree(body.string());
            JsonNode choices = json.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IOException("AI API返回缺少choices");
            }
            return choices.get(0).path("message").path("content").asText();
        }
    }

    private AiMatchResult parseClassifyResponse(String response) {
        try {
            JsonNode json = objectMapper.readTree(extractJson(response));
            AiMatchResult result = new AiMatchResult();
            result.setServiceType(json.path("serviceType").asText("其他服务"));
            result.setUrgency(json.path("urgency").asText("medium"));
            result.setDescription(json.path("description").asText("AI分析完成"));

            List<String> skills = new ArrayList<>();
            JsonNode skillsNode = json.path("skillTags");
            if (skillsNode.isArray()) {
                skillsNode.forEach(node -> skills.add(node.asText()));
            }
            if (skills.isEmpty()) {
                skills.add("通用服务");
            }
            result.setSkillTags(skills);
            result.setMatchedWorkerIds(Collections.emptyList());
            return result;
        } catch (Exception e) {
            log.warn("Parse AI classification response failed: {}", e.getMessage());
            return classifyServiceRequest(response, "service", null);
        }
    }

    private AiPriceResult parseEstimateResponse(String response, BigDecimal originalPrice) {
        try {
            JsonNode json = objectMapper.readTree(extractJson(response));
            AiPriceResult result = new AiPriceResult();
            result.setEstimatedPrice(new BigDecimal(json.path("estimatedPrice").asText("0")));
            result.setPriceRangeMin(new BigDecimal(json.path("priceRangeMin").asText("0")));
            result.setPriceRangeMax(new BigDecimal(json.path("priceRangeMax").asText("0")));
            result.setOptimizedDescription(json.path("optimizedDescription").asText(""));
            result.setExplanation("AI智能估价");
            return result;
        } catch (Exception e) {
            log.warn("Parse AI price response failed: {}", e.getMessage());
            return estimateGoodsPrice("", "", originalPrice);
        }
    }

    private String analyzeServiceType(String description) {
        String desc = description == null ? "" : description.toLowerCase();
        if (containsAny(desc, "电脑", "重装系统", "网络", "打印机", "wifi", "路由器")) {
            return "电脑维修";
        }
        if (containsAny(desc, "水电", "水管", "漏水", "电路", "灯", "开关", "插座", "水龙头")) {
            return "水电维修";
        }
        if (containsAny(desc, "家电", "空调", "冰箱", "洗衣机", "电视", "热水器")) {
            return "家电维修";
        }
        if (containsAny(desc, "家具", "桌子", "椅子", "柜子", "门窗", "锁")) {
            return "家具维修";
        }
        if (containsAny(desc, "快递", "取件", "代拿", "代买", "代送", "跑腿")) {
            return "跑腿服务";
        }
        if (containsAny(desc, "清洁", "打扫", "保洁", "清洗")) {
            return "清洁服务";
        }
        return "其他服务";
    }

    private String analyzeUrgency(String description) {
        String desc = description == null ? "" : description.toLowerCase();
        if (containsAny(desc, "紧急", "立刻", "马上", "急", "漏水", "断电", "着火")) {
            return "high";
        }
        if (containsAny(desc, "尽快", "今天", "比较急")) {
            return "medium";
        }
        return "low";
    }

    private List<String> extractSkillTags(String description) {
        List<String> tags = new ArrayList<>();
        String desc = description == null ? "" : description.toLowerCase();

        addTagIfContains(tags, desc, "电脑", "电脑维修");
        addTagIfContains(tags, desc, "系统", "系统安装");
        addTagIfContains(tags, desc, "网络", "网络调试");
        addTagIfContains(tags, desc, "wifi", "网络调试");
        addTagIfContains(tags, desc, "水电", "水电维修");
        addTagIfContains(tags, desc, "漏水", "水管维修");
        addTagIfContains(tags, desc, "电路", "电路维修");
        addTagIfContains(tags, desc, "灯", "灯具安装");
        addTagIfContains(tags, desc, "空调", "空调维修");
        addTagIfContains(tags, desc, "冰箱", "冰箱维修");
        addTagIfContains(tags, desc, "洗衣机", "洗衣机维修");
        addTagIfContains(tags, desc, "门窗", "门窗维修");
        addTagIfContains(tags, desc, "锁", "锁具维修");
        addTagIfContains(tags, desc, "快递", "跑腿服务");
        addTagIfContains(tags, desc, "代买", "跑腿服务");

        if (tags.isEmpty()) {
            tags.add("通用服务");
        }
        return tags;
    }

    private void addTagIfContains(List<String> tags, String text, String keyword, String tag) {
        if (text.contains(keyword) && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    private BigDecimal calculateDiscountRate(String condition) {
        if (condition == null) {
            return new BigDecimal("0.50");
        }
        return switch (condition.trim().toLowerCase()) {
            case "全新", "new" -> new BigDecimal("0.90");
            case "几乎全新", "like new" -> new BigDecimal("0.80");
            case "轻微使用痕迹" -> new BigDecimal("0.70");
            case "明显使用痕迹" -> new BigDecimal("0.55");
            case "严重磨损" -> new BigDecimal("0.35");
            default -> new BigDecimal("0.50");
        };
    }

    private String generateOptimizedDescription(String goodsName, String condition) {
        String name = goodsName == null || goodsName.isBlank() ? "该商品" : goodsName;
        String level = condition == null || condition.isBlank() ? "正常使用" : condition;
        return "【" + level + "】" + name + "，功能正常，适合社区自提交易。价格合理，欢迎咨询。";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        int start = text.indexOf("```json");
        if (start >= 0) {
            start += 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void saveAiMatchRecord(String requestType, Long requestId, String description, AiMatchResult result) {
        if (requestId == null || result == null) {
            return;
        }
        AiMatchRecord record = new AiMatchRecord();
        record.setRequestType(requestType);
        record.setRequestId(requestId);
        record.setRawDescription(description);
        record.setAiServiceType(result.getServiceType());
        record.setAiUrgency(result.getUrgency());
        if (result.getSkillTags() != null) {
            record.setAiSkills(String.join(",", result.getSkillTags()));
        }
        if (result.getMatchedWorkerIds() != null) {
            record.setMatchedWorkers(result.getMatchedWorkerIds().toString());
        }
        aiMatchRecordMapper.insert(record);
    }

    private void saveAiPriceRecord(Long goodsId, String goodsName, String condition,
                                   BigDecimal originalPrice, AiPriceResult result) {
        if (goodsId == null || result == null) {
            return;
        }
        AiPriceRecord record = new AiPriceRecord();
        record.setGoodsId(goodsId);
        record.setGoodsName(goodsName);
        record.setConditionLevel(condition);
        record.setOriginalPrice(originalPrice);
        record.setAiEstimatedPrice(result.getEstimatedPrice());
        record.setAiPriceRangeMin(result.getPriceRangeMin());
        record.setAiPriceRangeMax(result.getPriceRangeMax());
        record.setAiDescription(result.getOptimizedDescription());
        aiPriceRecordMapper.insert(record);
    }
}
