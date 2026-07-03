package com.community.controller;

import com.community.dto.Result;
import com.community.entity.GoodsCategory;
import com.community.repository.GoodsCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminGoodsCategoryController {

    private final GoodsCategoryMapper categoryMapper;

    @GetMapping
    public Result<List<GoodsCategory>> listCategories() {
        List<GoodsCategory> categories = categoryMapper.selectList(null);
        return Result.success(categories);
    }

    @PostMapping
    public Result<Void> addCategory(@RequestBody GoodsCategory category) {
        log.info("添加分类: {}", category);
        category.setStatus(1);
        categoryMapper.insert(category);
        return Result.success("分类添加成功", null);
    }

    @PutMapping("/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody GoodsCategory category) {
        log.info("更新分类: id={}, category={}", id, category);
        GoodsCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.success("分类更新成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        log.info("删除分类: id={}", id);
        categoryMapper.deleteById(id);
        return Result.success("分类删除成功", null);
    }
}
