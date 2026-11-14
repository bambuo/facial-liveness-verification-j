package faceservice.domain.security;

/**
 * 请求级租户上下文（由 JWT 过滤器写入，MyBatis-Plus 租户拦截器读取）。
 */
public final class TenantContext {

    private static final ThreadLocal<Principal> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Principal principal) {
        HOLDER.set(principal);
    }

    public static Principal require() {
        Principal p = HOLDER.get();
        if (p == null) {
            throw new IllegalStateException("tenant context missing; JWT filter must run first");
        }
        return p;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
