package com.community.controller;

import com.community.dto.Result;
import com.community.entity.GoodsCategory;
import com.community.service.GoodsCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final GoodsCategoryService goodsCategoryService;

    @GetMapping
    public Result<List<GoodsCategory>> listCategories() {
        try {
            List<GoodsCategory> categories = goodsCategoryService.listCategories();
            return Result.success(categories);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
