package com.community.config;

import com.community.entity.SysUser;
import com.community.repository.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final SysUserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token)) {
            boolean isValid = jwtConfig.validateToken(token);
            log.debug("JWT token valid: {}, uri: {}", isValid, request.getRequestURI());

            if (isValid) {
                Long userId = jwtConfig.getUserIdFromToken(token);
                Integer role = jwtConfig.getRoleFromToken(token);

                SysUser user = userMapper.selectById(userId);
                if (user != null) {
                    if (user.getStatus() == 1) {
                        String authority = switch (role == null ? 0 : role) {
                            case 2 -> "ROLE_SUPER_ADMIN";
                            case 1 -> "ROLE_ADMIN";
                            case 3 -> "ROLE_WORKER";
                            default -> "ROLE_USER";
                        };

                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(authority))
                            );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT authentication set for userId: {}, authority: {}", userId, authority);
                    } else {
                        log.debug("JWT user is disabled, userId: {}", userId);
                    }
                } else {
                    log.debug("JWT user not found, userId: {}", userId);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
