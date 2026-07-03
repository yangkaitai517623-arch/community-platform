package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.CommentVO;
import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.ForumComment;
import com.community.entity.ForumLike;
import com.community.entity.ForumPost;
import com.community.entity.SysUser;
import com.community.repository.ForumCommentMapper;
import com.community.repository.ForumLikeMapper;
import com.community.repository.ForumPostMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumPostMapper forumPostMapper;
    private final ForumCommentMapper forumCommentMapper;
    private final ForumLikeMapper forumLikeMapper;
    private final SysUserMapper userMapper;

    /**
     * 分页查询已发布帖子列表。
     */
    public PageResult<ForumPost> listPosts(int page, int size, Long userId) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getStatus, 1)
                .orderByDesc(ForumPost::getCreatedAt);

        IPage<ForumPost> pageResult = forumPostMapper.selectPage(new Page<>(page, size), wrapper);
        for (ForumPost post : pageResult.getRecords()) {
            post.setCommentCount(forumCommentMapper.countPublishedByPostId(post.getId()));
            post.setLiked(userId != null && forumLikeMapper.countByPostIdAndUserId(post.getId(), userId) > 0);
        }
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    /**
     * 根据ID获取帖子详情。
     */
    public ForumPost getPostById(Long id, Long userId) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post != null) {
            post.setLiked(userId != null && forumLikeMapper.countByPostIdAndUserId(post.getId(), userId) > 0);
            int viewCount = post.getViewCount() == null ? 0 : post.getViewCount();
            post.setViewCount(viewCount + 1);
            forumPostMapper.updateById(post);
        }
        return post;
    }

    /**
     * 浏览量允许重复累计。
     */
    public Result<Integer> increaseViewCount(Long id) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return Result.error("帖子不存在或未发布");
        }
        int viewCount = post.getViewCount() == null ? 0 : post.getViewCount();
        post.setViewCount(viewCount + 1);
        forumPostMapper.updateById(post);
        return Result.success(post.getViewCount());
    }

    /**
     * 点赞切换：未点赞则点赞，已点赞则取消。
     */
    @Transactional
    public Result<Map<String, Object>> likePost(Long id, Long userId) {
        if (userId == null) {
            return Result.unauthorized();
        }
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null || post.getStatus() == null || post.getStatus() != 1) {
            return Result.error("帖子不存在或未发布");
        }

        int likeCount = post.getLikeCount() == null ? 0 : post.getLikeCount();
        if (forumLikeMapper.countByPostIdAndUserId(id, userId) > 0) {
            forumLikeMapper.deleteByPostIdAndUserId(id, userId);
            post.setLikeCount(Math.max(0, likeCount - 1));
            forumPostMapper.updateById(post);
            return Result.success(buildLikeResult(post.getLikeCount(), false));
        }

        ForumLike like = new ForumLike();
        like.setPostId(id);
        like.setUserId(userId);
        try {
            forumLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            return Result.success(buildLikeResult(likeCount, true));
        }

        post.setLikeCount(likeCount + 1);
        forumPostMapper.updateById(post);
        return Result.success(buildLikeResult(post.getLikeCount(), true));
    }

    private Map<String, Object> buildLikeResult(Integer likeCount, boolean liked) {
        Map<String, Object> data = new HashMap<>();
        data.put("likeCount", likeCount == null ? 0 : likeCount);
        data.put("liked", liked);
        return data;
    }

    /**
     * 发布帖子。
     */
    public Result<Void> addPost(ForumPost post) {
        if (post.getStatus() == null) {
            post.setStatus(1);
        }
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        forumPostMapper.insert(post);
        return Result.success("帖子发布成功", null);
    }

    /**
     * 更新帖子。
     */
    public Result<Void> updatePost(ForumPost post) {
        ForumPost existing = forumPostMapper.selectById(post.getId());
        if (existing == null) {
            return Result.error("帖子不存在");
        }
        forumPostMapper.updateById(post);
        return Result.success("帖子更新成功", null);
    }

    /**
     * 删除帖子。
     */
    public Result<Void> deletePost(Long id) {
        ForumPost existing = forumPostMapper.selectById(id);
        if (existing == null) {
            return Result.error("帖子不存在");
        }
        forumPostMapper.deleteById(id);
        return Result.success("帖子删除成功", null);
    }

    /**
     * 添加评论。
     */
    public Result<Void> addComment(ForumComment comment) {
        if (comment.getStatus() == null) {
            comment.setStatus(1);
        }
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        forumCommentMapper.insert(comment);

        ForumPost post = forumPostMapper.selectById(comment.getPostId());
        if (post != null) {
            int commentCount = forumCommentMapper.countPublishedByPostId(comment.getPostId());
            post.setCommentCount(commentCount);
            forumPostMapper.updateById(post);
        }

        return Result.success("评论发布成功", null);
    }

    /**
     * 获取帖子的评论列表。
     */
    public List<CommentVO> getCommentsByPostId(Long postId) {
        List<ForumComment> comments = forumCommentMapper.findByPostId(postId);
        List<CommentVO> result = new ArrayList<>();
        for (ForumComment comment : comments) {
            CommentVO vo = new CommentVO();
            vo.setId(comment.getId());
            vo.setPostId(comment.getPostId());
            vo.setUserId(comment.getUserId());
            vo.setContent(comment.getContent());
            vo.setLikeCount(comment.getLikeCount());
            vo.setStatus(comment.getStatus());
            vo.setCreatedAt(comment.getCreatedAt());
            SysUser user = userMapper.selectById(comment.getUserId());
            vo.setUserName(user != null ? user.getRealName() : null);
            result.add(vo);
        }
        return result;
    }

    /**
     * 审核帖子。
     */
    public Result<Void> auditPost(Long id, Integer status) {
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null) {
            return Result.error("帖子不存在");
        }
        post.setStatus(status);
        forumPostMapper.updateById(post);
        return Result.success("审核操作成功", null);
    }
}
