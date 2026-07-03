package com.community.dto;

import com.community.entity.ErrandRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErrandRequestVO extends ErrandRequest {

    /**
     * 发布者姓名
     */
    private String publisherName;

    /**
     * 接单人姓名
     */
    private String runnerName;

    public ErrandRequestVO() {}

    public ErrandRequestVO(ErrandRequest request) {
        this.setId(request.getId());
        this.setUserId(request.getUserId());
        this.setTitle(request.getTitle());
        this.setDescription(request.getDescription());
        this.setErrandType(request.getErrandType());
        this.setPickupAddress(request.getPickupAddress());
        this.setDeliveryAddress(request.getDeliveryAddress());
        this.setUrgency(request.getUrgency());
        this.setReward(request.getReward());
        this.setAiTags(request.getAiTags());
        this.setAiUrgency(request.getAiUrgency());
        this.setStatus(request.getStatus());
        this.setRunnerId(request.getRunnerId());
        this.setOrderStatus(request.getOrderStatus());
        this.setOrderRating(request.getOrderRating());
        this.setOrderComment(request.getOrderComment());
        this.setCreatedAt(request.getCreatedAt());
        this.setUpdatedAt(request.getUpdatedAt());
    }
}
