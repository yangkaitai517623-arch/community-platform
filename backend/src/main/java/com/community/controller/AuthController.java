package com.community.controller;

import com.community.dto.LoginRequest;
import com.community.dto.LoginResponse;
import com.community.dto.Result;
import com.community.entity.SysUser;
import com.community.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            return Result.success(loginResponse);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        try {
            // 使用 RegisterRequest 进行注册
            com.community.dto.RegisterRequest request = new com.community.dto.RegisterRequest();
            request.setUsername(user.getUsername());
            request.setPassword(user.getPassword());
            request.setRealName(user.getRealName());
            request.setPhone(user.getPhone());
            request.setEmail(user.getEmail());
            request.setBuilding(user.getBuilding());
            request.setRoom(user.getRoom());
            return userService.register(request);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
