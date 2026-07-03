package com.community.dto;

import com.community.entity.SysUser;
import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private SysUser user;

    public LoginResponse(String token, SysUser user) {
        this.token = token;
        this.user = user;
    }
}
