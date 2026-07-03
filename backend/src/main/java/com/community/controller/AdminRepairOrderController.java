package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.RepairOrder;
import com.community.entity.RepairRequest;
import com.community.repository.RepairOrderMapper;
import com.community.repository.RepairRequestMapper;
import com.community.service.RepairRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/repair-orders")
@RequiredArgsConstructor
public class AdminRepairOrderController {

    private final RepairRequestMapper repairRequestMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final RepairRequestService repairRequestService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RepairRequest::getStatus, 1, 2, 3);
        if (status != null) {
            wrapper.eq(RepairRequest::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(RepairRequest::getTitle, keyword);
        }
        wrapper.orderByDesc(RepairRequest::getCreatedAt);

        Page<RepairRequest> pageResult = repairRequestMapper.selectPage(new Page<>(page, size), wrapper);
        enrichOrderFields(pageResult.getRecords());

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return repairRequestService.updateStatus(id, status);
    }

    private void enrichOrderFields(Iterable<RepairRequest> records) {
        for (RepairRequest request : records) {
            if (request == null || request.getId() == null) {
                continue;
            }
            RepairOrder order = repairOrderMapper.findByRequestId(request.getId());
            if (order == null) {
                request.setOrderStatus(null);
                request.setOrderRating(null);
                request.setOrderComment(null);
                continue;
            }
            request.setOrderStatus(order.getStatus());
            request.setOrderRating(order.getRating());
            request.setOrderComment(order.getComment());
        }
    }
}
