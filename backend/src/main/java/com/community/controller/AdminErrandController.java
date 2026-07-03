package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.ErrandRequest;
import com.community.entity.Notification;
import com.community.repository.ErrandRequestMapper;
import com.community.repository.NotificationMapper;
import com.community.service.ErrandRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/errand")
@RequiredArgsConstructor
public class AdminErrandController {

    private final ErrandRequestMapper errandRequestMapper;
    private final NotificationMapper notificationMapper;
    private final ErrandRequestService errandRequestService;

    @GetMapping
    public Result<Map<String, Object>> listRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String errandType,
            @RequestParam(required = false) Integer urgency) {

        LambdaQueryWrapper<ErrandRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ErrandRequest::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ErrandRequest::getTitle, keyword)
                    .or()
                    .like(ErrandRequest::getDescription, keyword));
        }
        if (StringUtils.hasText(errandType)) {
            wrapper.eq(ErrandRequest::getErrandType, errandType);
        }
        if (urgency != null) {
            wrapper.eq(ErrandRequest::getUrgency, urgency);
        }
        wrapper.orderByDesc(ErrandRequest::getCreatedAt);

        Page<ErrandRequest> pageResult = errandRequestMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<ErrandRequest> getRequest(@PathVariable Long id) {
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        return Result.success(request);
    }

    @PutMapping("/{id}/assign")
    public Result<Void> assignRunner(@PathVariable Long id, @RequestParam Long runnerId) {
        return errandRequestService.assignRunnerByAdmin(id, runnerId);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return errandRequestService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRequest(@PathVariable Long id, @RequestParam(required = false) String reason) {
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Request does not exist");
        }
        if (request.getUserId() != null) {
            Notification notification = new Notification();
            notification.setUserId(request.getUserId());
            notification.setTitle("需求已删除");
            String content = "您的跑腿需求已被管理员删除：" + request.getTitle();
            if (StringUtils.hasText(reason)) {
                content += "。原因：" + reason;
            }
            notification.setContent(content);
            notification.setType(1);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
        return errandRequestService.deleteRequest(id);
    }
}
