package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.ForumPost;
import com.community.repository.ForumPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
public class AdminForumController {

    private final ForumPostMapper postMapper;

    @GetMapping
    public Result<Map<String, Object>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ForumPost::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ForumPost::getTitle, keyword)
                    .or()
                    .like(ForumPost::getContent, keyword));
        }
        wrapper.orderByDesc(ForumPost::getCreatedAt);

        Page<ForumPost> pageResult = postMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @PutMapping("/{id}/audit")
    public Result<Void> auditPost(@PathVariable Long id, @RequestParam Integer status) {
        ForumPost post = postMapper.selectById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }
        if (status == null || (status != 1 && status != 2)) {
            return Result.error("审核状态不正确");
        }
        post.setStatus(status);
        postMapper.updateById(post);
        return Result.success("审核成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
