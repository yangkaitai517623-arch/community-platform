package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.GoodsVO;
import com.community.dto.Result;
import com.community.entity.GoodsCategory;
import com.community.entity.SecondHandGoods;
import com.community.entity.SysUser;
import com.community.repository.GoodsCategoryMapper;
import com.community.repository.SecondHandGoodsMapper;
import com.community.repository.SysUserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/goods")
@RequiredArgsConstructor
public class AdminGoodsController {

    private final SecondHandGoodsMapper goodsMapper;
    private final SysUserMapper userMapper;
    private final GoodsCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Result<Map<String, Object>> listGoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<SecondHandGoods> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SecondHandGoods::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SecondHandGoods::getTitle, keyword);
        }
        wrapper.orderByDesc(SecondHandGoods::getCreatedAt);

        Page<SecondHandGoods> pageResult = goodsMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为VO
        List<GoodsVO> voList = new ArrayList<>();
        for (SecondHandGoods goods : pageResult.getRecords()) {
            GoodsVO vo = new GoodsVO();
            vo.setId(goods.getId());
            vo.setTitle(goods.getTitle());
            vo.setDescription(goods.getDescription());
            vo.setCategoryId(goods.getCategoryId());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setSellingPrice(goods.getSellingPrice());
            vo.setConditionLevel(goods.getConditionLevel());
            vo.setImages(goods.getImages());
            vo.setSellerId(goods.getSellerId());
            vo.setStatus(goods.getStatus());
            vo.setViewCount(goods.getViewCount());
            vo.setCreatedAt(goods.getCreatedAt());

            // 解析图片，取第一张
            if (goods.getImages() != null && !goods.getImages().isEmpty()) {
                try {
                    JsonNode imagesNode = objectMapper.readTree(goods.getImages());
                    if (imagesNode.isArray() && imagesNode.size() > 0) {
                        vo.setImage(imagesNode.get(0).asText());
                    }
                } catch (Exception e) {
                    vo.setImage(goods.getImages());
                }
            }

            // 查询卖家姓名
            SysUser seller = userMapper.selectById(goods.getSellerId());
            vo.setSellerName(seller != null ? seller.getRealName() : "未知");

            // 查询分类名称
            GoodsCategory category = categoryMapper.selectById(goods.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : "未分类");

            voList.add(vo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<GoodsVO> getGoods(@PathVariable Long id) {
        SecondHandGoods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }

        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setTitle(goods.getTitle());
        vo.setDescription(goods.getDescription());
        vo.setCategoryId(goods.getCategoryId());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setSellingPrice(goods.getSellingPrice());
        vo.setConditionLevel(goods.getConditionLevel());
        vo.setImages(goods.getImages());
        vo.setSellerId(goods.getSellerId());
        vo.setStatus(goods.getStatus());
        vo.setViewCount(goods.getViewCount());
        vo.setCreatedAt(goods.getCreatedAt());

        SysUser seller = userMapper.selectById(goods.getSellerId());
        vo.setSellerName(seller != null ? seller.getRealName() : "未知");

        return Result.success(vo);
    }

    @PutMapping("/{id}/audit")
    public Result<Void> auditGoods(@PathVariable Long id, @RequestParam Integer status) {
        SecondHandGoods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (goods.getStatus() != 0) {
            return Result.error("只有待审核商品可以审核");
        }
        if (status == null || (status != 1 && status != 4)) {
            return Result.error("审核状态不正确");
        }
        goods.setStatus(status);
        goodsMapper.updateById(goods);
        return Result.success("审核成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SecondHandGoods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        goods.setStatus(status);
        goodsMapper.updateById(goods);
        return Result.success("状态更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteGoods(@PathVariable Long id) {
        goodsMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
