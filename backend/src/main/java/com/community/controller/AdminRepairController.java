package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.Notification;
import com.community.entity.RepairRequest;
import com.community.repository.NotificationMapper;
import com.community.repository.RepairRequestMapper;
import com.community.service.RepairRequestService;
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
@RequestMapping("/api/admin/repair")
@RequiredArgsConstructor
public class AdminRepairController {

    private final RepairRequestMapper repairRequestMapper;
    private final NotificationMapper notificationMapper;
    private final RepairRequestService repairRequestService;

    @GetMapping
    public Result<Map<String, Object>> listRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer urgency) {

        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RepairRequest::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(RepairRequest::getTitle, keyword)
                    .or()
                    .like(RepairRequest::getDescription, keyword));
        }
        if (urgency != null) {
            wrapper.eq(RepairRequest::getUrgency, urgency);
        }
        wrapper.orderByDesc(RepairRequest::getCreatedAt);

        Page<RepairRequest> pageResult = repairRequestMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<RepairRequest> getRequest(@PathVariable Long id) {
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        return Result.success(request);
    }

    @PutMapping("/{id}/assign")
    public Result<Void> assignWorker(@PathVariable Long id, @RequestParam Long workerId) {
        return repairRequestService.assignWorkerByAdmin(id, workerId);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return repairRequestService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRequest(@PathVariable Long id, @RequestParam(required = false) String reason) {
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Request does not exist");
        }
        if (request.getUserId() != null) {
            Notification notification = new Notification();
            notification.setUserId(request.getUserId());
            notification.setTitle("需求已删除");
            String content = "您的检修需求已被管理员删除：" + request.getTitle();
            if (StringUtils.hasText(reason)) {
                content += "。原因：" + reason;
            }
            notification.setContent(content);
            notification.setType(1);
            notification.setIsRead(0);
            notificationMapper.insert(notification);
        }
        return repairRequestService.deleteRequest(id);
    }
}
