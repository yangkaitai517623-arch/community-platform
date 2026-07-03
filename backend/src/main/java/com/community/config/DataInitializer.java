package com.community.config;

import com.community.entity.SysUser;
import com.community.repository.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 启动时只为没有密码的用户设置默认密码
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("正在检查用户密码...");

        String defaultPassword = "admin123";
        String defaultHash = passwordEncoder.encode(defaultPassword);

        userMapper.selectList(null).forEach(user -> {
            // 只有当密码为null、空、或不是有效的BCrypt哈希时才重置
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                SysUser update = new SysUser();
                update.setId(user.getId());
                update.setPassword(defaultHash);
                userMapper.updateById(update);
                log.info("已为用户 {} 设置默认密码", user.getUsername());
            }
        });

        log.info("用户密码检查完成");
    }
}
