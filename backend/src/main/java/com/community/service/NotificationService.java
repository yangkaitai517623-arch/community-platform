package com.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.PageResult;
import com.community.dto.Result;
import com.community.entity.Notification;
import com.community.repository.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 分页查询用户通知列表
     */
    public PageResult<Notification> listNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);

        IPage<Notification> pageResult = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), page, size);
    }

    /**
     * 标记单条通知为已读
     */
    public Result<Void> markAsRead(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            return Result.error("通知不存在");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        return Result.success("标记已读", null);
    }

    /**
     * 标记用户所有通知为已读
     */
    public Result<Void> markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
        return Result.success("全部已读", null);
    }

    /**
     * 统计用户未读通知数量
     */
    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 发送通知
     */
    public Result<Void> sendNotification(Long userId, String title, String content, Integer type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(normalizeType(type));
        notification.setIsRead(0);
        notificationMapper.insert(notification);
        return Result.success("通知发送成功", null);
    }

    private Integer normalizeType(Integer type) {
        if (type == null) {
            return 1;
        }
        return switch (type) {
            case 2 -> 2;
            case 3 -> 3;
            default -> 1;
        };
    }
}
