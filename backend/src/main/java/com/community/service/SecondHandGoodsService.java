package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.PageResult;
import com.community.dto.AiPriceResult;
import com.community.dto.Result;
import com.community.entity.SecondHandGoods;
import com.community.repository.SecondHandGoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SecondHandGoodsService {

    private final SecondHandGoodsMapper goodsMapper;
    private final AiService aiService;

    /**
     * 分页查询商品列表
     */
    public PageResult<SecondHandGoods> listGoods(int page, int size, Integer status) {
        LambdaQueryWrapper<SecondHandGoods> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SecondHandGoods::getStatus, status);
        }
        wrapper.orderByDesc(SecondHandGoods::getCreatedAt);

        IPage<SecondHandGoods> pageResult = goodsMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    /**
     * 根据ID获取商品详情
     */
    public SecondHandGoods getGoodsById(Long id) {
        return goodsMapper.selectById(id);
    }

    /**
     * 发布商品
     */
    public Result<Void> addGoods(SecondHandGoods goods) {
        goods.setStatus(0); // 待售
        goods.setViewCount(0);
        if (goods.getSellingPrice() == null) {
            BigDecimal basePrice = goods.getOriginalPrice() == null ? BigDecimal.ZERO : goods.getOriginalPrice();
            goods.setSellingPrice(basePrice.multiply(new BigDecimal("0.50")));
        }
        goodsMapper.insert(goods);
        enrichWithAi(goods);
        return Result.success("商品发布成功", null);
    }

    private void enrichWithAi(SecondHandGoods goods) {
        if (goods.getTitle() == null || goods.getTitle().isBlank() || goods.getOriginalPrice() == null) {
            return;
        }
        try {
            AiPriceResult aiResult = aiService.callAiForEstimation(
                    goods.getTitle(),
                    goods.getConditionLevel(),
                    goods.getOriginalPrice(),
                    goods.getId()
            );
            goods.setAiEstimatedPrice(aiResult.getEstimatedPrice());
            if (goods.getSellingPrice() == null || goods.getSellingPrice().signum() <= 0) {
                goods.setSellingPrice(aiResult.getEstimatedPrice());
            }
            if ((goods.getDescription() == null || goods.getDescription().isBlank())
                    && aiResult.getOptimizedDescription() != null) {
                goods.setDescription(aiResult.getOptimizedDescription());
            }
            goodsMapper.updateById(goods);
        } catch (Exception ignored) {
            // AI增强不能阻断商品发布。
        }
    }

    /**
     * 更新商品信息
     */
    public Result<Void> updateGoods(SecondHandGoods goods) {
        SecondHandGoods existing = goodsMapper.selectById(goods.getId());
        if (existing == null) {
            return Result.error("商品不存在");
        }
        goodsMapper.updateById(goods);
        return Result.success("商品更新成功", null);
    }

    /**
     * 审核商品
     */
    public Result<Void> auditGoods(Long id, Integer status) {
        SecondHandGoods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        goods.setStatus(status);
        goodsMapper.updateById(goods);
        return Result.success("审核操作成功", null);
    }

    /**
     * 删除商品
     */
    public Result<Void> deleteGoods(Long id) {
        SecondHandGoods existing = goodsMapper.selectById(id);
        if (existing == null) {
            return Result.error("商品不存在");
        }
        goodsMapper.deleteById(id);
        return Result.success("商品删除成功", null);
    }

    /**
     * 查询我的商品（分页）
     */
    public PageResult<SecondHandGoods> getMyGoods(Long sellerId, int page, int size) {
        LambdaQueryWrapper<SecondHandGoods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecondHandGoods::getSellerId, sellerId)
                .orderByDesc(SecondHandGoods::getCreatedAt);

        IPage<SecondHandGoods> pageResult = goodsMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }
}
