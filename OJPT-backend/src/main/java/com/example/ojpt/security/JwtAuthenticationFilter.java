package com.example.ojpt.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * 过滤器核心逻辑：解析并校验请求头中的 JWT access token，
     * 若通过则从 token 中提取用户ID与角色，构建认证信息注入 SecurityContext；
     * 若无效或无 token，则交给后续链路或权限校验。
     * 
     * 注意：使用 userId 作为 principal，而不是 username，因为 userId 是稳定的，
     * 不会因为用户名修改而改变，确保即使用户修改了用户名，token 仍然有效。
     *
     * @param request      当前 HTTP 请求
     * @param response     当前 HTTP 响应
     * @param filterChain  过滤器链
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.parseToken(token);
            if (!Objects.equals(claims.get("type"), "access")) {
                // token 类型不是 access，清除认证信息，让后续链路处理
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            
            // 黑名单检查：先检查用户级黑名单，再检查权限变更临时黑名单，最后检查 token 级黑名单
            Long userId = Long.parseLong(claims.getSubject());
            if (tokenBlacklistService.isUserBlacklisted(userId)) {
                // 用户被永久封禁，返回 401（由 AuthenticationEntryPoint 统一处理）
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            
            if (tokenBlacklistService.isPermissionChangeBlacklisted(userId)) {
                // 用户在权限变更临时黑名单中，拒绝使用旧 token，必须重新登录
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            
            String jti = claims.getId();
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                // token 在黑名单中（已登出），返回 401
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            
            // 使用 userId 作为 principal，而不是 username，因为 userId 是稳定的，不会因为用户名修改而改变
            // username 存储在 claims 中，仅用于显示，不作为身份标识
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            List<SimpleGrantedAuthority> authorities = roles == null ? List.of() :
                    roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList());

            // 使用 userId 作为 principal，确保即使用户修改了用户名，token 仍然有效
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // token 过期、签名无效或其他解析错误
            // 清除认证信息，让 AuthenticationEntryPoint 统一返回 401
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

