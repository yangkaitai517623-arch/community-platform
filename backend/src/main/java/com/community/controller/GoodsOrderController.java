package com.community.controller;

import com.community.dto.Result;
import com.community.entity.GoodsOrder;
import com.community.entity.SysUser;
import com.community.service.GoodsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/goods-orders")
@RequiredArgsConstructor
public class GoodsOrderController {

    private final GoodsOrderService goodsOrderService;

    /**
     * 买家创建订单
     */
    @PostMapping
    public Result<Void> createOrder(@RequestBody GoodsOrder order) {
        try {
            return goodsOrderService.createOrder(order, getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 卖家确认订单
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirmOrder(@PathVariable Long id) {
        try {
            return goodsOrderService.confirmOrder(id, getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 买家确认收货
     */
    @PutMapping("/{id}/complete")
    public Result<Void> completeOrder(@PathVariable Long id) {
        try {
            return goodsOrderService.completeOrder(id, getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取我的订单（买家+卖家）
     */
    @GetMapping("/my")
    public Result<Map<String, Object>> getMyOrders() {
        try {
            return goodsOrderService.getMyOrders(getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        try {
            return goodsOrderService.deleteOrder(id, getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUser) {
            SysUser user = (SysUser) authentication.getPrincipal();
            return user.getId();
        }
        return null;
    }
}
