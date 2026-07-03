package com.community.controller;

import com.community.dto.PageResult;
import com.community.dto.CommentVO;
import com.community.dto.Result;
import com.community.entity.ForumComment;
import com.community.entity.ForumPost;
import com.community.entity.SysUser;
import com.community.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;

    @GetMapping("/posts")
    public Result<PageResult<ForumPost>> listPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            PageResult<ForumPost> pageResult = forumService.listPosts(page, size, getCurrentUserId());
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/posts/{id}")
    public Result<ForumPost> getPost(@PathVariable Long id) {
        try {
            ForumPost post = forumService.getPostById(id, getCurrentUserId());
            if (post == null) {
                return Result.error("帖子不存在");
            }
            return Result.success(post);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/posts/{id}/view")
    public Result<Integer> increaseView(@PathVariable Long id) {
        try {
            return forumService.increaseViewCount(id);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/posts/{id}/like")
    public Result<Map<String, Object>> likePost(@PathVariable Long id) {
        try {
            return forumService.likePost(id, getCurrentUserId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/posts")
    public Result<Void> addPost(@RequestBody ForumPost post) {
        try {
            Long userId = getCurrentUserId();
            post.setUserId(userId);
            post.setStatus(1); // 已发布
            return forumService.addPost(post);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/posts/{id}/comments")
    public Result<List<CommentVO>> getComments(@PathVariable Long id) {
        try {
            List<CommentVO> comments = forumService.getCommentsByPostId(id);
            return Result.success(comments);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/comments")
    public Result<Void> addComment(@RequestBody ForumComment comment) {
        try {
            Long userId = getCurrentUserId();
            comment.setUserId(userId);
            comment.setStatus(1); // 已发布
            return forumService.addComment(comment);
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
