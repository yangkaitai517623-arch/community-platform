package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.ForumPost;
import com.community.entity.SysUser;
import com.community.repository.ForumPostMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/forum")
@RequiredArgsConstructor
public class AdminForumController {

    private final ForumPostMapper postMapper;
    private final SysUserMapper userMapper;

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
        fillAuthorNames(pageResult.getRecords());

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

    private void fillAuthorNames(List<ForumPost> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        List<Long> userIds = posts.stream()
                .map(ForumPost::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> authorNameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, this::getUserDisplayName));
        for (ForumPost post : posts) {
            post.setAuthorName(authorNameMap.get(post.getUserId()));
        }
    }

    private String getUserDisplayName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }
}
