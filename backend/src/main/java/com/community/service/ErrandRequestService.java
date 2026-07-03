package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.AiMatchResult;
import com.community.dto.ErrandRequestVO;
import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.ErrandOrder;
import com.community.entity.ErrandRequest;
import com.community.entity.SysUser;
import com.community.repository.ErrandOrderMapper;
import com.community.repository.ErrandRequestMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ErrandRequestService {

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ErrandRequestMapper errandRequestMapper;
    private final ErrandOrderMapper errandOrderMapper;
    private final SysUserMapper userMapper;
    private final AiService aiService;
    private final NotificationService notificationService;

    public PageResult<ErrandRequestVO> listRequests(int page, int size, Integer status, List<Integer> statuses, String scope, Long userId) {
        LambdaQueryWrapper<ErrandRequest> wrapper = new LambdaQueryWrapper<>();
        if ("pending".equals(scope)) {
            wrapper.eq(ErrandRequest::getStatus, 0);
        } else if ("active".equals(scope)) {
            wrapper.in(ErrandRequest::getStatus, 1, 2);
            if (userId != null) {
                wrapper.and(w -> w.eq(ErrandRequest::getUserId, userId)
                        .or()
                        .eq(ErrandRequest::getRunnerId, userId));
            }
        } else if ("completed".equals(scope)) {
            wrapper.eq(ErrandRequest::getStatus, 3);
            if (userId != null) {
                wrapper.and(w -> w.eq(ErrandRequest::getUserId, userId)
                        .or()
                        .eq(ErrandRequest::getRunnerId, userId));
            }
        } else if (statuses != null && !statuses.isEmpty()) {
            wrapper.in(ErrandRequest::getStatus, statuses);
            if (!statuses.contains(0) && userId != null) {
                wrapper.and(w -> w.eq(ErrandRequest::getUserId, userId)
                        .or()
                        .eq(ErrandRequest::getRunnerId, userId));
            }
        } else if (status != null) {
            wrapper.eq(ErrandRequest::getStatus, status);
            if (status != 0 && userId != null) {
                wrapper.and(w -> w.eq(ErrandRequest::getUserId, userId)
                        .or()
                        .eq(ErrandRequest::getRunnerId, userId));
            }
        } else if (userId != null) {
            wrapper.and(w -> w.eq(ErrandRequest::getStatus, 0)
                    .or()
                    .eq(ErrandRequest::getUserId, userId)
                    .or()
                    .eq(ErrandRequest::getRunnerId, userId));
        } else {
            wrapper.eq(ErrandRequest::getStatus, 0);
        }
        wrapper.orderByDesc(ErrandRequest::getCreatedAt);

        IPage<ErrandRequest> pageResult = errandRequestMapper.selectPage(new Page<>(page, size), wrapper);
        enrichErrandOrders(pageResult.getRecords());
        return new PageResult<>(toVoList(pageResult.getRecords()), pageResult.getTotal(), page, size);
    }

    public ErrandRequest getRequestById(Long id) {
        ErrandRequest request = errandRequestMapper.selectById(id);
        enrichErrandOrder(request);
        return request;
    }

    public Result<Void> addRequest(ErrandRequest request) {
        request.setStatus(0);
        request.setRunnerId(null);
        errandRequestMapper.insert(request);
        enrichWithAi(request);
        return Result.success("Errand request published", null);
    }

    private void enrichWithAi(ErrandRequest request) {
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            return;
        }
        try {
            AiMatchResult aiResult = aiService.callAiForClassification(request.getDescription(), "errand", request.getId());
            if (aiResult.getSkillTags() != null) {
                request.setAiTags(String.join(",", aiResult.getSkillTags()));
            }
            request.setAiUrgency(aiResult.getUrgency());
            if (aiResult.getUrgency() != null) {
                request.setUrgency(switch (aiResult.getUrgency()) {
                    case "high" -> 1;
                    case "low" -> 3;
                    default -> 2;
                });
            }
            errandRequestMapper.updateById(request);
        } catch (Exception ignored) {
            // AI enrichment must not block publishing.
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> acceptRequest(Long id, Long runnerId) {
        if (runnerId == null) {
            return Result.error("Please login first");
        }
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (request.getStatus() == null || request.getStatus() != 0) {
            return Result.error("This request has already been accepted or completed");
        }
        if (request.getUserId() != null && request.getUserId().equals(runnerId)) {
            return Result.error("You cannot accept your own request");
        }
        if (hasActiveTask(runnerId, id)) {
            return Result.error("You already have an active errand task");
        }

        LambdaUpdateWrapper<ErrandRequest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ErrandRequest::getId, id)
                .eq(ErrandRequest::getStatus, 0)
                .set(ErrandRequest::getRunnerId, runnerId)
                .set(ErrandRequest::getStatus, 1);
        int affected = errandRequestMapper.update(null, updateWrapper);
        if (affected == 0) {
            return Result.error("This request has already been accepted by another user");
        }
        request.setRunnerId(runnerId);
        request.setStatus(1);
        upsertErrandOrder(request, 0);
        notificationService.sendNotification(request.getUserId(), "跑腿需求已接单",
                "您的跑腿需求已被接单：" + request.getTitle(), 2);
        return Result.success("Accepted successfully", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> assignRunnerByAdmin(Long id, Long runnerId) {
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("Only pending or active errand requests can be assigned");
        }
        Result<Void> runnerCheck = validateRunner(request, runnerId);
        if (runnerCheck.getCode() != 200) {
            return runnerCheck;
        }
        if (hasActiveTask(runnerId, id)) {
            return Result.error("This runner already has an active errand task");
        }

        Long oldRunnerId = request.getRunnerId();
        request.setRunnerId(runnerId);
        request.setStatus(1);
        errandRequestMapper.updateById(request);
        upsertErrandOrder(request, 0);

        notificationService.sendNotification(request.getUserId(), "跑腿需求已分配",
                "您的跑腿需求已由管理员分配：" + request.getTitle(), 2);
        notificationService.sendNotification(runnerId, "新的跑腿任务",
                "管理员已为您分配跑腿任务：" + request.getTitle(), 2);
        if (oldRunnerId != null && !oldRunnerId.equals(runnerId)) {
            notificationService.sendNotification(oldRunnerId, "跑腿任务已改派",
                    "您的跑腿任务已被管理员改派：" + request.getTitle(), 2);
        }
        return Result.success("Runner assigned", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelAccept(Long id, Long runnerId) {
        if (runnerId == null) {
            return Result.error("Please login first");
        }
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (!runnerId.equals(request.getRunnerId())) {
            return Result.error("Only the current runner can cancel this accepted order");
        }
        if (request.getStatus() == null || (request.getStatus() != 1 && request.getStatus() != 2)) {
            return Result.error("Current status cannot be cancelled");
        }

        request.setRunnerId(null);
        request.setStatus(0);
        errandRequestMapper.updateById(request);
        deleteErrandOrderByRequestId(id);
        notificationService.sendNotification(request.getUserId(), "跑腿需求已回到待接单",
                "接单人已取消接单，需求已回到待接单：" + request.getTitle(), 2);
        return Result.success("Accepted errand has been cancelled", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (status == null || status < 0 || status > 4) {
            return Result.error("Invalid errand request status");
        }
        if (Integer.valueOf(4).equals(status)) {
            if (Integer.valueOf(3).equals(request.getStatus())) {
                return Result.error("Completed requests cannot be cancelled");
            }
            request.setStatus(4);
            request.setRunnerId(null);
            errandRequestMapper.updateById(request);
            deleteErrandOrderByRequestId(id);
            return Result.success("Status updated", null);
        }
        if (Integer.valueOf(0).equals(status)) {
            if (Integer.valueOf(3).equals(request.getStatus())) {
                return Result.error("Completed requests cannot be returned to pending");
            }
            request.setStatus(0);
            request.setRunnerId(null);
            errandRequestMapper.updateById(request);
            deleteErrandOrderByRequestId(id);
            return Result.success("Status updated", null);
        }
        if ((status == 1 || status == 2 || status == 3) && request.getRunnerId() == null) {
            return Result.error("Assign a runner before changing this request to an active status");
        }

        request.setStatus(status);
        errandRequestMapper.updateById(request);
        if (status == 1 || status == 2) {
            upsertErrandOrder(request, 0);
        } else if (status == 3) {
            upsertErrandOrder(request, 1);
        }
        return Result.success("Status updated", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> completeRequest(Long id, Long runnerId) {
        if (runnerId == null) {
            return Result.error("Please login first");
        }
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (!runnerId.equals(request.getRunnerId())) {
            return Result.error("Only the runner can complete this request");
        }
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            return Result.error("Current status cannot be completed");
        }
        request.setStatus(3);
        errandRequestMapper.updateById(request);
        upsertErrandOrder(request, 1);
        notificationService.sendNotification(request.getUserId(), "跑腿需求已完成",
                "您的跑腿需求已完成：" + request.getTitle(), 2);
        return Result.success("Request completed", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reviewOrder(Long id, Long userId, Integer rating, String comment) {
        if (userId == null) {
            return Result.error("Please login first");
        }
        ErrandRequest request = errandRequestMapper.selectById(id);
        if (request == null) {
            return Result.error("Errand request does not exist");
        }
        if (!userId.equals(request.getUserId())) {
            return Result.error("Only the publisher can review this errand order");
        }
        if (!Integer.valueOf(3).equals(request.getStatus())) {
            return Result.error("Only completed errand orders can be reviewed");
        }
        if (rating == null || rating < 1 || rating > 5) {
            return Result.error("Rating must be between 1 and 5");
        }
        ErrandOrder order = errandOrderMapper.findByRequestId(id);
        if (order == null) {
            if (request.getRunnerId() == null) {
                return Result.error("This errand request has no runner");
            }
            upsertErrandOrder(request, 1);
            order = errandOrderMapper.findByRequestId(id);
        }
        if (order == null) {
            return Result.error("Errand order does not exist");
        }
        order.setRating(rating);
        order.setComment(comment == null ? null : comment.trim());
        order.setStatus(2);
        errandOrderMapper.updateById(order);
        return Result.success("Review submitted", null);
    }

    public Result<Void> updateRequest(ErrandRequest request) {
        ErrandRequest existing = errandRequestMapper.selectById(request.getId());
        if (existing == null) {
            return Result.error("Request does not exist");
        }
        request.setUserId(existing.getUserId());
        request.setRunnerId(existing.getRunnerId());
        request.setStatus(existing.getStatus());
        errandRequestMapper.updateById(request);
        return Result.success("Updated successfully", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteRequest(Long id) {
        ErrandRequest existing = errandRequestMapper.selectById(id);
        if (existing == null) {
            return Result.error("Request does not exist");
        }
        deleteErrandOrderByRequestId(id);
        errandRequestMapper.deleteById(id);
        return Result.success("Deleted successfully", null);
    }

    public PageResult<ErrandRequestVO> getMyRequests(Long userId, int page, int size) {
        LambdaQueryWrapper<ErrandRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ErrandRequest::getUserId, userId)
                .or(q -> q.eq(ErrandRequest::getRunnerId, userId)
                        .in(ErrandRequest::getStatus, 1, 2, 3)))
                .orderByDesc(ErrandRequest::getCreatedAt);

        IPage<ErrandRequest> pageResult = errandRequestMapper.selectPage(new Page<>(page, size), wrapper);
        enrichErrandOrders(pageResult.getRecords());
        return new PageResult<>(toVoList(pageResult.getRecords()), pageResult.getTotal(), page, size);
    }

    public List<ErrandRequest> getUrgentRequests() {
        return errandRequestMapper.findUrgent();
    }

    private List<ErrandRequestVO> toVoList(List<ErrandRequest> requests) {
        List<ErrandRequestVO> voList = new ArrayList<>();
        for (ErrandRequest request : requests) {
            ErrandRequestVO vo = new ErrandRequestVO(request);
            SysUser publisher = userMapper.selectById(request.getUserId());
            if (publisher != null) {
                vo.setPublisherName(publisher.getRealName());
            }
            if (request.getRunnerId() != null) {
                SysUser runner = userMapper.selectById(request.getRunnerId());
                if (runner != null) {
                    vo.setRunnerName(runner.getRealName());
                }
            }
            voList.add(vo);
        }
        return voList;
    }

    private void enrichErrandOrders(List<ErrandRequest> requests) {
        for (ErrandRequest request : requests) {
            enrichErrandOrder(request);
        }
    }

    private void enrichErrandOrder(ErrandRequest request) {
        if (request == null || request.getId() == null) {
            return;
        }
        ErrandOrder order = errandOrderMapper.findByRequestId(request.getId());
        if (order == null) {
            return;
        }
        request.setOrderStatus(order.getStatus());
        request.setOrderRating(order.getRating());
        request.setOrderComment(order.getComment());
    }

    private Result<Void> validateRunner(ErrandRequest request, Long runnerId) {
        if (runnerId == null) {
            return Result.error("Please select a runner");
        }
        if (request.getUserId() != null && request.getUserId().equals(runnerId)) {
            return Result.error("The publisher cannot be assigned as runner");
        }
        SysUser runner = userMapper.selectById(runnerId);
        if (runner == null || runner.getStatus() == null || runner.getStatus() != 1) {
            return Result.error("Runner does not exist or is disabled");
        }
        return Result.success();
    }

    private boolean hasActiveTask(Long runnerId, Long excludeRequestId) {
        return errandRequestMapper.countActiveByRunnerIdExcludeRequest(runnerId, excludeRequestId) > 0;
    }

    private void upsertErrandOrder(ErrandRequest request, Integer orderStatus) {
        ErrandOrder order = errandOrderMapper.findByRequestId(request.getId());
        if (order == null) {
            order = new ErrandOrder();
            order.setOrderNo(newOrderNo("EO"));
            order.setRequestId(request.getId());
        }
        order.setUserId(request.getUserId());
        order.setRunnerId(request.getRunnerId());
        order.setAmount(request.getReward());
        order.setStatus(orderStatus);
        if (order.getId() == null) {
            errandOrderMapper.insert(order);
        } else {
            errandOrderMapper.updateById(order);
        }
    }

    private void deleteErrandOrderByRequestId(Long requestId) {
        errandOrderMapper.delete(new LambdaQueryWrapper<ErrandOrder>()
                .eq(ErrandOrder::getRequestId, requestId));
    }

    private String newOrderNo(String prefix) {
        String timestamp = LocalDateTime.now().format(ORDER_TIME_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + timestamp + suffix;
    }
}
