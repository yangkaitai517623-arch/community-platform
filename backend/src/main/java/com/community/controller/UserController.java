package com.community.controller;

import com.community.dto.Result;
import com.community.entity.SysUser;
import com.community.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<SysUser> getProfile() {
        try {
            Long userId = getCurrentUserId();
            SysUser user = userService.getUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        try {
            Long userId = getCurrentUserId();
            user.setId(userId);
            return userService.updateUser(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        try {
            Long userId = getCurrentUserId();
            String oldPassword = params.get("oldPassword");
            String newPassword = params.get("newPassword");
            return userService.changePassword(userId, oldPassword, newPassword);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从SecurityContext获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SysUser) {
            SysUser user = (SysUser) authentication.getPrincipal();
            return user.getId();
        }
        return null;
    }
}
