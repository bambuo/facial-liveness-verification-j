package faceservice.domain.security;

/**
 * 已验证的调用方身份（全部来自 JWT/签名，不信任请求体中的租户）。
 */
public record Principal(
        String tenantId,
        String tenantCode,
        String sub,
        String scene,
        String ref,
        String jti) {
}
