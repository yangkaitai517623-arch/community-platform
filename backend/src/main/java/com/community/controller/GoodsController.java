package com.community.controller;

import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.SecondHandGoods;
import com.community.entity.SysUser;
import com.community.service.SecondHandGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final SecondHandGoodsService secondHandGoodsService;

    @GetMapping
    public Result<PageResult<SecondHandGoods>> listGoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        try {
            PageResult<SecondHandGoods> pageResult = secondHandGoodsService.listGoods(page, size, status);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<SecondHandGoods> getGoods(@PathVariable Long id) {
        try {
            SecondHandGoods goods = secondHandGoodsService.getGoodsById(id);
            if (goods == null) {
                return Result.error("商品不存在");
            }
            return Result.success(goods);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> addGoods(@RequestBody SecondHandGoods goods) {
        try {
            Long userId = getCurrentUserId();
            goods.setSellerId(userId);
            goods.setStatus(0); // 待审核
            return secondHandGoodsService.addGoods(goods);
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
