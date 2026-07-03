package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.ErrandOrder;
import com.community.entity.ErrandRequest;
import com.community.repository.ErrandOrderMapper;
import com.community.repository.ErrandRequestMapper;
import com.community.service.ErrandRequestService;
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
@RequestMapping("/api/admin/errand-orders")
@RequiredArgsConstructor
public class AdminErrandOrderController {

    private final ErrandRequestMapper errandRequestMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final ErrandRequestService errandRequestService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<ErrandRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ErrandRequest::getStatus, 1, 2, 3);
        if (status != null) {
            wrapper.eq(ErrandRequest::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ErrandRequest::getTitle, keyword);
        }
        wrapper.orderByDesc(ErrandRequest::getCreatedAt);

        Page<ErrandRequest> pageResult = errandRequestMapper.selectPage(new Page<>(page, size), wrapper);
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
        return errandRequestService.updateStatus(id, status);
    }

    private void enrichOrderFields(Iterable<ErrandRequest> records) {
        for (ErrandRequest request : records) {
            if (request == null || request.getId() == null) {
                continue;
            }
            ErrandOrder order = errandOrderMapper.findByRequestId(request.getId());
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
