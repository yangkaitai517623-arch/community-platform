package com.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.dto.Result;
import com.community.entity.SysUser;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/admins")
@RequiredArgsConstructor
public class AdminAdminController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public Result<Map<String, Object>> listAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        if (!isSuperAdmin(getCurrentUser())) {
            return Result.error(403, "Only super admin can view admin accounts");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, 1, 2);

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

    @PostMapping
    public Result<Void> addAdmin(@RequestBody SysUser user) {
        if (!isSuperAdmin(getCurrentUser())) {
            return Result.error(403, "Only super admin can add admin accounts");
        }
        SysUser existing = userMapper.findByUsername(user.getUsername());
        if (existing != null) {
            return Result.error("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(1);
        user.setStatus(1);
        userMapper.insert(user);
        return Result.success("Admin added successfully", null);
    }

    @PutMapping("/{id}")
    public Result<Void> updateAdmin(@PathVariable Long id, @RequestBody SysUser user) {
        SysUser currentUser = getCurrentUser();
        if (!isSuperAdmin(currentUser)) {
            return Result.error(403, "Only super admin can edit admin accounts");
        }
        SysUser existing = userMapper.selectById(id);
        if (existing == null || existing.getRole() == null || (existing.getRole() != 1 && existing.getRole() != 2)) {
            return Result.error("Admin account does not exist");
        }
        if (currentUser.getId().equals(id) && user.getRole() != null && user.getRole() != 2) {
            return Result.error("Cannot downgrade your own super admin role");
        }
        if (user.getRole() != null && user.getRole() != 1 && user.getRole() != 2) {
            return Result.error("Admin role must be admin or super admin");
        }

        user.setId(id);
        user.setPassword(null);
        user.setDeleted(null);
        userMapper.updateById(user);
        return Result.success("Updated successfully", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAdmin(@PathVariable Long id) {
        if (!isSuperAdmin(getCurrentUser())) {
            return Result.error(403, "Only super admin can delete admin accounts");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null || user.getRole() == null || (user.getRole() != 1 && user.getRole() != 2)) {
            return Result.error("Admin account does not exist");
        }
        if (user.getRole() == 2) {
            return Result.error("Cannot delete super admin");
        }
        userMapper.deleteById(id);
        return Result.success("Deleted successfully", null);
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
