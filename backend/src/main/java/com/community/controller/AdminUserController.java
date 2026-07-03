package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.SysUser;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserMapper userMapper;

    @GetMapping
    public Result<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer role) {

        SysUser currentUser = getCurrentUser();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null) {
            if (!isSuperAdmin(currentUser) && role != 0) {
                return Result.error(403, "Only super admin can view admin accounts");
            }
            wrapper.eq(SysUser::getRole, role);
        } else {
            wrapper.eq(SysUser::getRole, 0);
        }

        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getRealName, keyword)
                    .or()
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<SysUser> getUser(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("User does not exist");
        }
        if (!canManageUser(user)) {
            return Result.error(403, "Only super admin can view admin accounts");
        }
        return Result.success(user);
    }

    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser existing = userMapper.selectById(id);
        if (existing == null) {
            return Result.error("User does not exist");
        }
        if (!canManageUser(existing)) {
            return Result.error(403, "Only super admin can manage admin accounts");
        }

        user.setId(id);
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        user.setDeleted(null);
        userMapper.updateById(user);
        return Result.success("Updated successfully", null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("User does not exist");
        }
        if (!canManageUser(user)) {
            return Result.error(403, "Only super admin can manage admin account status");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        return Result.success("Status updated successfully", null);
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestParam Integer role) {
        SysUser currentUser = getCurrentUser();
        if (!isSuperAdmin(currentUser)) {
            return Result.error(403, "Only super admin can change user roles");
        }
        if (role == null || role < 0 || role > 2) {
            return Result.error("Invalid role");
        }
        if (currentUser.getId().equals(id) && role != 2) {
            return Result.error("Cannot downgrade your own super admin role");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("User does not exist");
        }
        user.setRole(role);
        userMapper.updateById(user);
        return Result.success("Role updated successfully", null);
    }

    private boolean canManageUser(SysUser targetUser) {
        return targetUser != null
                && (targetUser.getRole() == null
                || targetUser.getRole() == 0
                || isSuperAdmin(getCurrentUser()));
    }

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUser) {
            return (SysUser) authentication.getPrincipal();
        }
        return null;
    }

    private boolean isSuperAdmin(SysUser user) {
        return user != null && user.getRole() != null && user.getRole() == 2;
    }
}
