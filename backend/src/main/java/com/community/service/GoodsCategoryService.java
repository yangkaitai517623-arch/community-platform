package com.community.service;

import com.community.dto.Result;
import com.community.entity.GoodsCategory;
import com.community.repository.GoodsCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsCategoryService {

    private final GoodsCategoryMapper categoryMapper;

    /**
     * 查询所有分类
     */
    public List<GoodsCategory> listCategories() {
        return categoryMapper.findActiveCategories();
    }

    /**
     * 添加分类
     */
    public Result<Void> addCategory(GoodsCategory category) {
        category.setStatus(1);
        categoryMapper.insert(category);
        return Result.success("分类添加成功", null);
    }

    /**
     * 更新分类
     */
    public Result<Void> updateCategory(GoodsCategory category) {
        GoodsCategory existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            return Result.error("分类不存在");
        }
        categoryMapper.updateById(category);
        return Result.success("分类更新成功", null);
    }

    /**
     * 删除分类
     */
    public Result<Void> deleteCategory(Long id) {
        GoodsCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            return Result.error("分类不存在");
        }
        categoryMapper.deleteById(id);
        return Result.success("分类删除成功", null);
    }
}
