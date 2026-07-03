package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.AiMatchResult;
import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.RepairOrder;
import com.community.entity.RepairRequest;
import com.community.entity.SysUser;
import com.community.repository.RepairOrderMapper;
import com.community.repository.RepairRequestMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepairRequestService {

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RepairRequestMapper repairRequestMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final SysUserMapper userMapper;
    private final AiService aiService;
    private final NotificationService notificationService;

    public PageResult<RepairRequest> listRequests(int page, int size, Integer status, List<Integer> statuses, String scope, Long userId) {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        if ("pending".equals(scope)) {
            wrapper.eq(RepairRequest::getStatus, 0);
        } else if ("active".equals(scope)) {
            wrapper.in(RepairRequest::getStatus, 1, 2);
            if (userId != null) {
                wrapper.and(w -> w.eq(RepairRequest::getUserId, userId)
                        .or()
                        .eq(RepairRequest::getWorkerId, userId));
            }
        } else if ("completed".equals(scope)) {
            wrapper.eq(RepairRequest::getStatus, 3);
            if (userId != null) {
                wrapper.and(w -> w.eq(RepairRequest::getUserId, userId)
                        .or()
                        .eq(RepairRequest::getWorkerId, userId));
            }
        } else if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(RepairRequest::getStatus, statuses);
            if (!statuses.contains(0) && userId != null) {
                wrapper.and(w -> w.eq(RepairRequest::getUserId, userId)
                        .or()
                        .eq(RepairRequest::getWorkerId, userId));
            }
        } else if (status != null) {
            wrapper.eq(RepairRequest::getStatus, status);
            if (status != 0 && userId != null) {
                wrapper.and(w -> w.eq(RepairRequest::getUserId, userId)
                        .or()
                        .eq(RepairRequest::getWorkerId, userId));
            }
        } else if (userId != null) {
            wrapper.and(w -> w.eq(RepairRequest::getStatus, 0)
                    .or()
                    .eq(RepairRequest::getUserId, userId)
                    .or()
                    .eq(RepairRequest::getWorkerId, userId));
        } else {
            wrapper.eq(RepairRequest::getStatus, 0);
        }
        wrapper.orderByDesc(RepairRequest::getCreatedAt);

        IPage<RepairRequest> pageResult = repairRequestMapper.selectPage(new Page<>(page, size), wrapper);
        enrichRepairOrders(pageResult.getRecords());
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    public RepairRequest getRequestById(Long id) {
        RepairRequest request = repairRequestMapper.selectById(id);
        enrichRepairOrder(request);
        return request;
    }

    public Result<Void> addRequest(RepairRequest request) {
        request.setStatus(0);
        request.setWorkerId(null);
        repairRequestMapper.insert(request);
        enrichWithAi(request);
        return Result.success("Repair request published", null);
    }

    private void enrichWithAi(RepairRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return;
        }
        try {
            AiMatchResult aiResult = aiService.callAiForClassification(request.getDescription(), "repair", request.getId());
            if (aiResult.getSkillTags() != null) {
                request.setAiTags(String.join(",", aiResult.getSkillTags()));
            }
            request.setAiUrgency(aiResult.getUrgency());
            if (aiResult.getServiceType() != null && (request.getRepairType() == null || request.getRepairType().isBlank())) {
                request.setRepairType(aiResult.getServiceType());
            }
            if (aiResult.getUrgency() != null) {
                request.setUrgency(switch (aiResult.getUrgency()) {
                    case "high" -> 1;
                    case "low" -> 3;
                    default -> 2;
                });
            }
            repairRequestMapper.updateById(request);
        } catch (Exception ignored) {
            // AI enrichment must not block publishing.
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> acceptByUser(Long id, Long userId) {
        if (userId == null) {
            return Result.error("Please login first");
        }
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (request.getStatus() == null || request.getStatus() != 0) {
            return Result.error("This request has already been accepted or completed");
        }
        if (request.getUserId() != null && request.getUserId().equals(userId)) {
            return Result.error("You cannot accept your own request");
        }
        if (hasActiveTask(userId, id)) {
            return Result.error("You already have an active repair task");
        }

        LambdaUpdateWrapper<RepairRequest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RepairRequest::getId, id)
                .eq(RepairRequest::getStatus, 0)
                .set(RepairRequest::getWorkerId, userId)
                .set(RepairRequest::getStatus, 1);
        int affected = repairRequestMapper.update(null, updateWrapper);
        if (affected == 0) {
            return Result.error("This request has already been accepted by another user");
        }
        request.setWorkerId(userId);
        request.setStatus(1);
        upsertRepairOrder(request, 0);
        notificationService.sendNotification(request.getUserId(), "检修需求已接单",
                "您的检修需求已被接单：" + request.getTitle(), 2);
        return Result.success("Accepted successfully", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> assignWorkerByAdmin(Long id, Long workerId) {
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("Only pending or active repair requests can be assigned");
        }
        Result<Void> workerCheck = validateWorker(request, workerId);
        if (workerCheck.getCode() != 200) {
            return workerCheck;
        }
        if (hasActiveTask(workerId, id)) {
            return Result.error("This worker already has an active repair task");
        }

        Long oldWorkerId = request.getWorkerId();
        request.setWorkerId(workerId);
        request.setStatus(1);
        repairRequestMapper.updateById(request);
        upsertRepairOrder(request, 0);

        notificationService.sendNotification(request.getUserId(), "检修需求已分配",
                "您的检修需求已由管理员分配：" + request.getTitle(), 2);
        notificationService.sendNotification(workerId, "新的检修任务",
                "管理员已为您分配检修任务：" + request.getTitle(), 2);
        if (oldWorkerId != null && !oldWorkerId.equals(workerId)) {
            notificationService.sendNotification(oldWorkerId, "检修任务已改派",
                    "您的检修任务已被管理员改派：" + request.getTitle(), 2);
        }
        return Result.success("Worker assigned", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelAccept(Long id, Long userId) {
        if (userId == null) {
            return Result.error("Please login first");
        }
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (!userId.equals(request.getWorkerId())) {
            return Result.error("Only the current worker can cancel this task");
        }
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("Current status cannot be cancelled");
        }

        request.setWorkerId(null);
        request.setStatus(0);
        repairRequestMapper.updateById(request);
        deleteRepairOrderByRequestId(id);
        notificationService.sendNotification(request.getUserId(), "检修需求已回到待接单",
                "接单人已取消接单，需求已回到待接单：" + request.getTitle(), 2);
        return Result.success("Accepted repair request cancelled", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (status == null || status < 0 || status > 4) {
            return Result.error("Invalid repair request status");
        }
        if (Integer.valueOf(4).equals(status)) {
            if (Integer.valueOf(3).equals(request.getStatus())) {
                return Result.error("Completed requests cannot be cancelled");
            }
            request.setStatus(4);
            request.setWorkerId(null);
            repairRequestMapper.updateById(request);
            deleteRepairOrderByRequestId(id);
            return Result.success("Status updated", null);
        }
        if (Integer.valueOf(0).equals(status)) {
            if (Integer.valueOf(3).equals(request.getStatus())) {
                return Result.error("Completed requests cannot be returned to pending");
            }
            request.setStatus(0);
            request.setWorkerId(null);
            repairRequestMapper.updateById(request);
            deleteRepairOrderByRequestId(id);
            return Result.success("Status updated", null);
        }
        if ((status == 1 || status == 2 || status == 3) && request.getWorkerId() == null) {
            return Result.error("Assign a worker before changing this request to an active status");
        }

        request.setStatus(status);
        repairRequestMapper.updateById(request);
        if (status == 1 || status == 2) {
            upsertRepairOrder(request, 0);
        } else if (status == 3) {
            upsertRepairOrder(request, 1);
        }
        return Result.success("Status updated", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> completeRequest(Long id, Long userId) {
        if (userId == null) {
            return Result.error("Please login first");
        }
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("Only the publisher can confirm this request");
        }
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            return Result.error("Current status cannot be completed");
        }
        if (request.getWorkerId() == null) {
            return Result.error("This request has no worker");
        }
        request.setStatus(3);
        repairRequestMapper.updateById(request);
        upsertRepairOrder(request, 1);
        notificationService.sendNotification(request.getWorkerId(), "检修需求已确认完成",
                "发布者已确认检修完成：" + request.getTitle(), 2);
        return Result.success("Request completed", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reviewOrder(Long id, Long userId, Integer rating, String comment) {
        if (userId == null) {
            return Result.error("Please login first");
        }
        RepairRequest request = repairRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Repair request does not exist");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("Only the publisher can review this repair order");
        }
        if (!Integer.valueOf(3).equals(request.getStatus())) {
            return Result.error("Only completed repair orders can be reviewed");
        }
        if (rating == null || rating < 1 || rating > 5) {
            return Result.error("Rating must be between 1 and 5");
        }
        RepairOrder order = repairOrderMapper.findByRequestId(id);
        if (order == null) {
            if (request.getWorkerId() == null) {
                return Result.error("This repair request has no worker");
            }
            upsertRepairOrder(request, 1);
            order = repairOrderMapper.findByRequestId(id);
        }
        if (order == null) {
            return Result.error("Repair order does not exist");
        }
        order.setRating(rating);
        order.setComment(comment == null ? null : comment.trim());
        order.setStatus(2);
        repairOrderMapper.updateById(order);
        return Result.success("Review submitted", null);
    }

    public Result<Void> updateRequest(RepairRequest request) {
        RepairRequest existing = repairRequestMapper.selectById(request.getId());
        if (existing == null) {
            return Result.error("Request does not exist");
        }
        request.setUserId(existing.getUserId());
        request.setWorkerId(existing.getWorkerId());
        request.setStatus(existing.getStatus());
        repairRequestMapper.updateById(request);
        return Result.success("Updated successfully", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteRequest(Long id) {
        RepairRequest existing = repairRequestMapper.selectById(id);
        if (existing == null) {
            return Result.error("Request does not exist");
        }
        deleteRepairOrderByRequestId(id);
        repairRequestMapper.deleteById(id);
        return Result.success("Deleted successfully", null);
    }

    public PageResult<RepairRequest> getMyRequests(Long userId, int page, int size) {
        LambdaQueryWrapper<RepairRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(RepairRequest::getUserId, userId)
                .or(q -> q.eq(RepairRequest::getWorkerId, userId)
                        .in(RepairRequest::getStatus, 1, 2, 3)))
                .orderByDesc(RepairRequest::getCreatedAt);

        IPage<RepairRequest> pageResult = repairRequestMapper.selectPage(new Page<>(page, size), wrapper);
        enrichRepairOrders(pageResult.getRecords());
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    private Result<Void> validateWorker(RepairRequest request, Long workerId) {
        if (workerId == null) {
            return Result.error("Please select a repair worker");
        }
        if (request.getUserId() != null && request.getUserId().equals(workerId)) {
            return Result.error("The publisher cannot be assigned as worker");
        }
        SysUser worker = userMapper.selectById(workerId);
        if (worker == null || worker.getStatus() == null || worker.getStatus() != 1) {
            return Result.error("Worker does not exist or is disabled");
        }
        if (worker.getRole() == null || worker.getRole() != 3) {
            return Result.error("Only repair workers can be assigned");
        }
        return Result.success();
    }

    private boolean hasActiveTask(Long workerId, Long excludeRequestId) {
        return repairRequestMapper.countActiveByWorkerIdExcludeRequest(workerId, excludeRequestId) > 0;
    }

    private void enrichRepairOrders(List<RepairRequest> requests) {
        for (RepairRequest request : requests) {
            enrichRepairOrder(request);
        }
    }

    private void enrichRepairOrder(RepairRequest request) {
        if (request == null || request.getId() == null) {
            return;
        }
        RepairOrder order = repairOrderMapper.findByRequestId(request.getId());
        if (order == null) {
            return;
        }
        request.setOrderStatus(order.getStatus());
        request.setOrderRating(order.getRating());
        request.setOrderComment(order.getComment());
    }

    private void upsertRepairOrder(RepairRequest request, Integer orderStatus) {
        RepairOrder order = repairOrderMapper.findByRequestId(request.getId());
        if (order == null) {
            order = new RepairOrder();
            order.setOrderNo(newOrderNo("RO"));
            order.setRequestId(request.getId());
        }
        order.setUserId(request.getUserId());
        order.setWorkerId(request.getWorkerId());
        order.setAmount(resolveRepairAmount(request));
        order.setStatus(orderStatus);
        if (order.getId() == null) {
            repairOrderMapper.insert(order);
        } else {
            repairOrderMapper.updateById(order);
        }
    }

    private BigDecimal resolveRepairAmount(RepairRequest request) {
        if (request.getActualPrice() != null) {
            return request.getActualPrice();
        }
        return request.getEstimatedPrice();
    }

    private void deleteRepairOrderByRequestId(Long requestId) {
        repairOrderMapper.delete(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRequestId, requestId));
    }

    private String newOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(ORDER_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + timestamp + suffix;
    }
}
