package com.example.ojpt.converter;

import com.example.ojpt.entity.User;
import com.example.ojpt.security.JwtService;
import com.example.ojpt.vo.LoginResponseVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 认证相关的 VO 转换（登录响应等）。
 * 原本使用 MapStruct，现改为手写实现，避免编译期依赖与调试成本。
 */
@Component
public class AuthConverter {

    /**
     * 组装登录响应：token 信息 + 用户基础信息。
     */
    public LoginResponseVO toLoginResponse(String tokenType,
                                           JwtService.TokenPair tokenPair,
                                           long accessExpiresIn,
                                           long refreshExpiresIn,
                                           User user,
                                           List<String> roles) {
        if (user == null || tokenPair == null) {
            return null;
        }
        return new LoginResponseVO(
                tokenType,
                tokenPair.accessToken(),
                accessExpiresIn,
                tokenPair.refreshToken(),
                refreshExpiresIn,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                user.getRoleType(),
                roles == null ? List.of() : List.copyOf(roles)
        );
    }
}

