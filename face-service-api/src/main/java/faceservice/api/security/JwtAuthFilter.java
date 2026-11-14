package faceservice.api.security;

import faceservice.application.token.TokenService;
import faceservice.domain.security.Principal;
import faceservice.domain.security.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * /v1/** 统一 JWT 验签（签名、过期、aud），把租户身份写入 TenantContext。
 * 租户身份只来自 JWT，不信任请求体中的 tenant 字段。
 */

/**
 * 仅注册到 /v1/*（见 WebConfig），/dev、/actuator 不经过本过滤器。
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokens;
    private final TenantLookup tenants;

    public JwtAuthFilter(TokenService tokens, TenantLookup tenants) {
        this.tokens = tokens;
        this.tenants = tenants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"AUTH_INVALID\",\"message\":\"缺少 Bearer JWT\","
                    + "\"request_id\":\"" + UUID.randomUUID() + "\"}");
            return;
        }
        try {
            Claims claims = tokens.parseAccess(header.substring(7));
            String tenantCode = String.valueOf(claims.get("tenant", String.class));
            TenantContext.set(new Principal(tenants.idOf(tenantCode), tenantCode,
                    claims.get("sub", String.class), claims.get("scene", String.class),
                    claims.get("ref", String.class), claims.getId()));
            try {
                chain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } catch (RuntimeException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"AUTH_INVALID\",\"message\":\"" + e.getMessage() + "\","
                    + "\"request_id\":\"" + UUID.randomUUID() + "\"}");
        }
    }

}
