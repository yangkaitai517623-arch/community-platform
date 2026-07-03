package com.community.controller;

import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.dto.ServiceOrderReviewRequest;
import com.community.entity.RepairRequest;
import com.community.entity.SysUser;
import com.community.service.RepairRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repair-requests")
@RequiredArgsConstructor
public class RepairRequestController {

    private final RepairRequestService repairRequestService;

    @GetMapping
    public Result<PageResult<RepairRequest>> listRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) List<Integer> statuses,
            @RequestParam(required = false) String scope) {
        try {
            Long userId = getCurrentUserId();
            PageResult<RepairRequest> pageResult = repairRequestService.listRequests(page, size, status, statuses, scope, userId);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<PageResult<RepairRequest>> getMyRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long userId = getCurrentUserId();
            PageResult<RepairRequest> pageResult = repairRequestService.getMyRequests(userId, page, size);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> addRequest(@RequestBody RepairRequest request) {
        try {
            Long userId = getCurrentUserId();
            request.setUserId(userId);
            request.setStatus(0);
            return repairRequestService.addRequest(request);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/accept")
    public Result<Void> acceptRequest(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            return repairRequestService.acceptByUser(id, userId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    public Result<Void> completeRequest(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            return repairRequestService.completeRequest(id, userId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel-accept")
    public Result<Void> cancelAccept(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            return repairRequestService.cancelAccept(id, userId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/review")
    public Result<Void> reviewOrder(@PathVariable Long id, @RequestBody ServiceOrderReviewRequest request) {
        try {
            Long userId = getCurrentUserId();
            return repairRequestService.reviewOrder(id, userId, request.getRating(), request.getComment());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> updateRequest(@PathVariable Long id, @RequestBody RepairRequest request) {
        try {
            Long userId = getCurrentUserId();
            RepairRequest existing = repairRequestService.getRequestById(id);
            if (existing == null) {
                return Result.error("Request does not exist");
            }
            if (!existing.getUserId().equals(userId)) {
                return Result.error("You can only edit your own request");
            }
            if (existing.getStatus() != 0) {
                return Result.error("Accepted requests cannot be edited");
            }
            request.setId(id);
            request.setUserId(userId);
            return repairRequestService.updateRequest(request);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRequest(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            RepairRequest existing = repairRequestService.getRequestById(id);
            if (existing == null) {
                return Result.error("Request does not exist");
            }
            if (!existing.getUserId().equals(userId)) {
                return Result.error("You can only delete your own request");
            }
            if (existing.getStatus() != 0) {
                return Result.error("Accepted requests cannot be deleted");
            }
            return repairRequestService.deleteRequest(id);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUser user) {
            return user.getId();
        }
        return null;
    }
}
