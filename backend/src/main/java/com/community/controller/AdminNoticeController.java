package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.Notification;
import com.community.entity.SysUser;
import com.community.repository.NotificationMapper;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type) {

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Notification::getTitle, keyword)
                    .or()
                    .like(Notification::getContent, keyword));
        }
        if (type != null) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);

        Page<Notification> pageResult = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        enrichReceiver(pageResult.getRecords());

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Notification notification) {
        notification.setIsRead(0);

        // 如果接收者为空，发送给所有用户
        if (notification.getUserId() == null) {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getStatus, 1)
                    .ne(SysUser::getRole, 2)); // 排除超级管理员
            for (SysUser user : users) {
                Notification notif = new Notification();
                notif.setUserId(user.getId());
                notif.setTitle(notification.getTitle());
                notif.setContent(notification.getContent());
                notif.setType(notification.getType());
                notif.setIsRead(0);
                notificationMapper.insert(notif);
            }
            return Result.success("通知已发送给所有用户", null);
        } else {
            notificationMapper.insert(notification);
            return Result.success("通知发送成功", null);
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        notificationMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    private void enrichReceiver(List<Notification> notifications) {
        for (Notification notification : notifications) {
            if (notification.getUserId() == null) {
                notification.setReceiver("全部居民");
                notification.setReceiverName("全部居民");
                continue;
            }
            SysUser receiver = userMapper.selectById(notification.getUserId());
            if (receiver == null) {
                String label = "用户ID: " + notification.getUserId();
                notification.setReceiver(label);
                notification.setReceiverName(label);
                continue;
            }
            String name = StringUtils.hasText(receiver.getRealName())
                    ? receiver.getRealName()
                    : receiver.getUsername();
            notification.setReceiver(name);
            notification.setReceiverName(name);
        }
    }
}
