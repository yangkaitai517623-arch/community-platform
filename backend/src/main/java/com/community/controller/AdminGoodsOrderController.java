package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.GoodsOrder;
import com.community.repository.GoodsOrderMapper;
import com.community.service.GoodsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/goods-orders")
@RequiredArgsConstructor
public class AdminGoodsOrderController {

    private final GoodsOrderMapper goodsOrderMapper;
    private final GoodsOrderService goodsOrderService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<GoodsOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(GoodsOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(GoodsOrder::getOrderNo, keyword);
        }
        wrapper.orderByDesc(GoodsOrder::getCreatedAt);

        Page<GoodsOrder> pageResult = goodsOrderMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<GoodsOrder> getById(@PathVariable Long id) {
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return goodsOrderService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return goodsOrderService.cancelOrderByAdmin(id);
    }
}
