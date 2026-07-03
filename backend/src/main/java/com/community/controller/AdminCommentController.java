package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.CommentVO;
import com.community.dto.Result;
import com.community.entity.ForumComment;
import com.community.entity.ForumPost;
import com.community.entity.SysUser;
import com.community.repository.ForumCommentMapper;
import com.community.repository.ForumPostMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final ForumCommentMapper commentMapper;
    private final SysUserMapper userMapper;
    private final ForumPostMapper postMapper;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<ForumComment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ForumComment::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ForumComment::getContent, keyword);
        }
        wrapper.orderByDesc(ForumComment::getCreatedAt);

        Page<ForumComment> pageResult = commentMapper.selectPage(new Page<>(page, size), wrapper);

        // 转换为VO，关联查询用户名和帖子标题
        List<CommentVO> voList = new ArrayList<>();
        for (ForumComment comment : pageResult.getRecords()) {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setPostId(comment.getPostId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setLikeCount(comment.getLikeCount());
            vo.setStatus(comment.getStatus());
            vo.setCreatedAt(comment.getCreatedAt());

            // 查询用户名
            SysUser user = userMapper.selectById(comment.getUserId());
            vo.setUserName(user != null ? user.getRealName() : "未知用户");

            // 查询帖子标题
            ForumPost post = postMapper.selectById(comment.getPostId());
            vo.setPostTitle(post != null ? post.getTitle() : "已删除");

            voList.add(vo);
        }

        Map<String, Object> result = Map.of(
            "records", voList,
            "total", pageResult.getTotal(),
            "page", page,
            "size", size
        );

        return Result.success(result);
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestParam Integer status) {
        ForumComment comment = commentMapper.selectById(id);
        if (comment == null) {
            return Result.error("评论不存在");
        }
        comment.setStatus(status);
        commentMapper.updateById(comment);
        return Result.success("审核成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
