package com.example.ojpt.controller;

import com.example.ojpt.common.Result;
import com.example.ojpt.config.JwtProperties;
import com.example.ojpt.converter.AuthConverter;
import com.example.ojpt.dto.LoginRequestDTO;
import com.example.ojpt.dto.PasswordResetRequestDTO;
import com.example.ojpt.dto.RegisterRequestDTO;
import com.example.ojpt.entity.User;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.exception.ErrorCode;
import com.example.ojpt.service.UserService;
import com.example.ojpt.service.PasswordResetRequestService;
import com.example.ojpt.security.JwtService;
import com.example.ojpt.security.LoginUserDetails;
import com.example.ojpt.security.RefreshTokenStore;
import com.example.ojpt.security.SystemRoleScope;
import com.example.ojpt.security.TokenBlacklistService;
import com.example.ojpt.vo.CurrentUserVO;
import com.example.ojpt.vo.LoginResponseVO;
import com.example.ojpt.vo.TokenResponseVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户登录、登出、刷新Token等认证相关接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final PasswordResetRequestService passwordResetRequestService;
    private final AuthConverter authConverter;

    /**
     * 用户登录接口。
     * @param dto 登录请求体（含用户名、密码）
     * @return token 响应体（access token, refresh token 等）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "支持用户名、邮箱或手机号登录")
    public Result<LoginResponseVO> login(@Valid @RequestBody LoginRequestDTO dto) {
        String principalName = resolvePrincipalName(dto.getAccount());

        // 2. 使用 AuthenticationManager 进行身份验证（密码校验等）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(principalName, dto.getPassword()));

        // 3. 将认证对象放入 SecurityContext, 以在后续请求中自动识别身份
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. 获取登录用户详情
        LoginUserDetails principal = (LoginUserDetails) authentication.getPrincipal();
        // 待审核账号（status=2）在密码校验通过后进行拦截，交给异常处理器返回提示
        if (principal.getStatus() != null && principal.getStatus() == 2) {
            throw new org.springframework.security.authentication.DisabledException("账号待审核");
        }
        // 黑名单拦截：被管理员拉黑的账号不允许继续登录签发新 token，并返回封禁剩余时间
        if (tokenBlacklistService.isUserBlacklisted(principal.getUserId())) {
            Long ttl = tokenBlacklistService.getUserBlacklistTtlSeconds(principal.getUserId());
            long remaining = ttl == null ? -1L : ttl;
            throw new com.example.ojpt.exception.UserBannedException("账号已被封禁", remaining);
        }
        
        // 权限变更临时黑名单处理：如果是权限变更导致的临时黑名单，清除标记并允许登录
        // 这样用户重新登录时可以获取包含新权限的新 token
        if (tokenBlacklistService.isPermissionChangeBlacklisted(principal.getUserId())) {
            tokenBlacklistService.removePermissionChangeBlacklist(principal.getUserId());
        }

        // 5. 获取去除"ROLE_"前缀后的角色名称列表
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();

        // 6. 生成 accessToken 和 refreshToken
        JwtService.TokenPair pair = jwtService.generateTokens(principal.getUserId(), principal.getUsername(), roles);

        // 7. 刷新令牌存入 redis 或其他持久层
        refreshTokenStore.store(principal.getUserId(), pair.refreshJti(), pair.refreshToken());

        // 8. 查询用户基础信息（如邮箱、roleType）
        User dbUser = userService.findById(principal.getUserId());
        if (dbUser == null) {
            throw new IllegalStateException("登录用户数据不存在");
        }

        // 9. 封装响应对象：token + 少量用户基础信息（使用 MapStruct）
        LoginResponseVO vo = authConverter.toLoginResponse(
                "Bearer",
                pair,
                jwtProperties.getAccessExpSeconds(),
                jwtProperties.getRefreshExpSeconds(),
                dbUser,
                roles
        );

        // 10. 返回响应
        return Result.ok(vo);
    }

    private String resolvePrincipalName(String account) {
        String loginId = account == null ? "" : account.trim();
        User user;
        if (loginId.contains("@")) {
            user = userService.findByEmail(loginId.toLowerCase());
        } else if (loginId.matches("^1[3-9]\\d{9}$")) {
            user = userService.findByPhone(loginId);
        } else {
            user = userService.findByUsername(loginId);
        }

        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BadCredentialsException("Bad credentials");
        }
        return user.getUsername();
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "支持邮箱或手机号注册，注册成功后直接返回登录态")
    public Result<LoginResponseVO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        User user = userService.register(dto);
        List<String> roles = List.of("USER");
        JwtService.TokenPair pair = jwtService.generateTokens(user.getId(), user.getUsername(), roles);
        refreshTokenStore.store(user.getId(), pair.refreshJti(), pair.refreshToken());

        LoginResponseVO vo = authConverter.toLoginResponse(
                "Bearer",
                pair,
                jwtProperties.getAccessExpSeconds(),
                jwtProperties.getRefreshExpSeconds(),
                user,
                roles
        );

        return Result.ok(vo);
    }

    @PostMapping("/password-reset-requests")
    @Operation(summary = "提交忘记密码申请", description = "提交用户名或邮箱后通知管理员审批重置密码")
    public Result<Void> submitPasswordResetRequest(@Valid @RequestBody PasswordResetRequestDTO dto) {
        passwordResetRequestService.submitRequest(dto.getAccount());
        return Result.ok("已通知管理员，请等待重置");
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用refreshToken刷新accessToken")
    public Result<TokenResponseVO> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.unauthorized("refreshToken不能为空");
        }
        
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken);
        } catch (ExpiredJwtException e) {
            // refreshToken 已过期
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException e) {
            // refreshToken 无效（签名错误、格式错误等）
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        
        if (!"refresh".equals(claims.get("type"))) {
            throw BusinessException.badRequest("无效的refreshToken类型");
        }
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = SystemRoleScope.normalizeRoleCodes(claims.get("roles", List.class));
        String jti = claims.getId();
        Long userId = Long.parseLong(claims.getSubject());

        RefreshTokenStore.ValidateResult validateResult = refreshTokenStore.validateWithRotation(userId, jti, refreshToken);
        if (validateResult == RefreshTokenStore.ValidateResult.ROTATED) {
            // 检测到旧 refreshToken 重用：强制该用户所有会话失效
            refreshTokenStore.deleteAllByUserId(userId);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (validateResult != RefreshTokenStore.ValidateResult.VALID) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 黑名单拦截：被管理员拉黑的账号不允许刷新 token，并返回封禁剩余时间
        if (tokenBlacklistService.isUserBlacklisted(userId)) {
            Long ttl = tokenBlacklistService.getUserBlacklistTtlSeconds(userId);
            long remaining = ttl == null ? -1L : ttl;
            throw new com.example.ojpt.exception.UserBannedException("账号已被封禁", remaining);
        }

        JwtService.TokenPair newPair = jwtService.generateTokens(userId, username, roles);
        // 标记旧 refreshToken 已轮换（用于检测重放）
        refreshTokenStore.markRotated(userId, jti);
        refreshTokenStore.delete(userId, jti);
        refreshTokenStore.store(userId, newPair.refreshJti(), newPair.refreshToken());

        TokenResponseVO vo = new TokenResponseVO(
                "Bearer",
                newPair.accessToken(),
                jwtProperties.getAccessExpSeconds(),
                newPair.refreshToken(),
                jwtProperties.getRefreshExpSeconds()
        );
        return Result.ok(vo);
    }

    /**
     * 使用 accessToken 获取当前登录用户信息（前端"自动登录 / 恢复会话"场景）。
     * 前端在请求头中携带 Authorization: Bearer <accessToken> 即可。
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "根据当前登录的Token获取用户信息")
    public Result<CurrentUserVO> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw BusinessException.unauthorized("未登录");
        }

        // 兼容多种 principal 类型：LoginUserDetails、Long（userId）或 String（username，兼容旧 token）
        Object principalObj = authentication.getPrincipal();
        User user;
        
        if (principalObj instanceof LoginUserDetails loginUserDetails) {
            // 如果 principal 是 LoginUserDetails，直接获取用户
            user = userService.findById(loginUserDetails.getUserId());
        } else if (principalObj instanceof Long userId) {
            // JwtAuthenticationFilter 将 principal 设置为 userId（推荐方式，用户名修改后仍有效）
            user = userService.findById(userId);
        } else if (principalObj instanceof String username) {
            // 兼容旧 token：JwtAuthenticationFilter 可能将 principal 设置为用户名字符串
            user = userService.findByUsername(username);
        } else {
            throw BusinessException.unauthorized("无法识别用户身份");
        }

        if (user == null) {
            throw BusinessException.userNotFound();
        }

        List<String> roles = SystemRoleScope.normalizeRoleCodes(authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList());

        CurrentUserVO vo = new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatar(),
                SystemRoleScope.normalizeRoleType(user.getRoleType()),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        return Result.ok(vo);
    }

    /**
     * 用户登出接口：将当前 access token 加入黑名单，删除 refresh token。
     * 
     * @param request HTTP 请求对象，用于提取 token
     * @return 登出成功响应
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登出当前用户，使Token失效")
    public Result<Void> logout(HttpServletRequest request) {
        // 从请求头中提取 access token
        String authHeader = request.getHeader(org.springframework.http.HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtService.parseToken(token);
                String jti = claims.getId();
                Long userId = Long.parseLong(claims.getSubject());
                
                // 删除 refresh token（如果存在）
                String refreshJti = claims.get("jti_refresh", String.class);
                if (refreshJti != null) {
                    refreshTokenStore.delete(userId, refreshJti);
                }
                
                // 将 access token 加入黑名单
                // 计算 access token 的剩余有效期，使用该值作为黑名单 TTL
                // refreshToken 在这里已经被删除，无法再刷新，所以不需要覆盖 refreshToken 有效期
                if (jti != null) {
                    Date expiration = claims.getExpiration();
                    long remainingSeconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                    if (remainingSeconds > 0) {
                        tokenBlacklistService.addToBlacklist(jti, remainingSeconds);
                    }
                }
            } catch (Exception e) {
                // token 无效，忽略
            }
        }
        
        return Result.ok("登出成功");
    }
}
