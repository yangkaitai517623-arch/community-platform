package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.GoodsOrder;
import com.community.entity.SecondHandGoods;
import com.community.repository.GoodsOrderMapper;
import com.community.repository.SecondHandGoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoodsOrderService {

    public static final int STATUS_PENDING_CONFIRM = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CANCELLED = 4;

    private final GoodsOrderMapper goodsOrderMapper;
    private final SecondHandGoodsMapper goodsMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> createOrder(GoodsOrder order, Long buyerId) {
        if (buyerId == null) {
            return Result.error("请先登录");
        }
        if (order == null || order.getGoodsId() == null) {
            return Result.error("商品ID不能为空");
        }

        SecondHandGoods goods = goodsMapper.selectById(order.getGoodsId());
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            return Result.error("商品已售出或已下架");
        }
        if (buyerId.equals(goods.getSellerId())) {
            return Result.error("不能购买自己的商品");
        }

        LambdaUpdateWrapper<SecondHandGoods> goodsUpdate = new LambdaUpdateWrapper<>();
        goodsUpdate.eq(SecondHandGoods::getId, goods.getId())
                .eq(SecondHandGoods::getStatus, 1)
                .set(SecondHandGoods::getStatus, 2)
                .set(SecondHandGoods::getBuyerId, buyerId);
        int affected = goodsMapper.update(null, goodsUpdate);
        if (affected == 0) {
            return Result.error("商品已被其他用户购买");
        }

        order.setBuyerId(buyerId);
        order.setSellerId(goods.getSellerId());
        order.setAmount(goods.getSellingPrice());
        order.setOrderNo(generateOrderNo());
        order.setStatus(STATUS_PENDING_CONFIRM);
        goodsOrderMapper.insert(order);
        return Result.success("购买成功，等待卖家确认", null);
    }

    public PageResult<GoodsOrder> listOrders(int page, int size) {
        LambdaQueryWrapper<GoodsOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(GoodsOrder::getCreatedAt);
        IPage<GoodsOrder> pageResult = goodsOrderMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    public GoodsOrder getOrderById(Long id) {
        return goodsOrderMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> confirmOrder(Long id, Long sellerId) {
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (sellerId == null || !sellerId.equals(order.getSellerId())) {
            return Result.error("只有卖家可以确认订单");
        }
        if (!isStatus(order, STATUS_PENDING_CONFIRM)) {
            return Result.error("只有待确认订单可以确认");
        }
        order.setStatus(STATUS_CONFIRMED);
        goodsOrderMapper.updateById(order);
        return Result.success("订单已确认", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> completeOrder(Long id, Long buyerId) {
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (buyerId == null || !buyerId.equals(order.getBuyerId())) {
            return Result.error("只有买家可以确认收货");
        }
        if (!isStatus(order, STATUS_CONFIRMED)) {
            return Result.error("只有已确认订单可以完成");
        }
        order.setStatus(STATUS_COMPLETED);
        goodsOrderMapper.updateById(order);
        return Result.success("已确认收货", null);
    }

    public Result<Map<String, Object>> getMyOrders(Long userId) {
        if (userId == null) {
            return Result.error("请先登录");
        }

        LambdaQueryWrapper<GoodsOrder> buyerWrapper = new LambdaQueryWrapper<>();
        buyerWrapper.eq(GoodsOrder::getBuyerId, userId).orderByDesc(GoodsOrder::getCreatedAt);

        LambdaQueryWrapper<GoodsOrder> sellerWrapper = new LambdaQueryWrapper<>();
        sellerWrapper.eq(GoodsOrder::getSellerId, userId).orderByDesc(GoodsOrder::getCreatedAt);

        Map<String, Object> result = new HashMap<>();
        result.put("buyerOrders", goodsOrderMapper.selectList(buyerWrapper));
        result.put("sellerOrders", goodsOrderMapper.selectList(sellerWrapper));
        return Result.success(result);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteOrder(Long id, Long userId) {
        if (userId == null) {
            return Result.error("请先登录");
        }
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            return Result.error("只能取消自己的订单");
        }
        return cancelPendingOrder(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateStatus(Long id, Integer status) {
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!isValidStatus(status)) {
            return Result.error("订单状态不正确");
        }
        if (status == STATUS_CANCELLED) {
            return cancelPendingOrder(order);
        }
        if (isStatus(order, STATUS_CANCELLED)) {
            return Result.error("已取消订单不能修改状态");
        }
        if (status == STATUS_PENDING_CONFIRM && !isStatus(order, STATUS_PENDING_CONFIRM)) {
            return Result.error("订单不能回退为待确认");
        }
        if (status == STATUS_CONFIRMED && !isStatus(order, STATUS_PENDING_CONFIRM)) {
            return Result.error("只有待确认订单可以确认");
        }
        if (status == STATUS_COMPLETED && !isStatus(order, STATUS_CONFIRMED)) {
            return Result.error("只有已确认订单可以完成");
        }

        order.setStatus(status);
        goodsOrderMapper.updateById(order);
        return Result.success("订单状态更新成功", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancelOrderByAdmin(Long id) {
        GoodsOrder order = goodsOrderMapper.selectById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return cancelPendingOrder(order);
    }

    private Result<Void> cancelPendingOrder(GoodsOrder order) {
        if (isStatus(order, STATUS_CANCELLED)) {
            return Result.error("订单已取消，请勿重复操作");
        }
        if (!isStatus(order, STATUS_PENDING_CONFIRM)) {
            return Result.error("只能取消待确认订单");
        }

        SecondHandGoods goods = goodsMapper.selectById(order.getGoodsId());
        if (goods != null && order.getBuyerId() != null && order.getBuyerId().equals(goods.getBuyerId())) {
            goods.setStatus(1);
            goods.setBuyerId(null);
            goodsMapper.updateById(goods);
        }

        order.setStatus(STATUS_CANCELLED);
        goodsOrderMapper.updateById(order);
        return Result.success("订单已取消", null);
    }

    private boolean isValidStatus(Integer status) {
        return status != null
                && (status == STATUS_PENDING_CONFIRM
                || status == STATUS_CONFIRMED
                || status == STATUS_COMPLETED
                || status == STATUS_CANCELLED);
    }

    private boolean isStatus(GoodsOrder order, int status) {
        return order != null && Integer.valueOf(status).equals(order.getStatus());
    }

    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
