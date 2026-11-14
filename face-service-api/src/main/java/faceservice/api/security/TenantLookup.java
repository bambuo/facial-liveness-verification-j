package faceservice.api.security;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户 code → face_tenants.id 解析（进程内缓存；未注册/停用租户拒绝）。
 */
@Component
public class TenantLookup {

    private final JdbcTemplate jdbc;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public TenantLookup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String idOf(String tenantCode) {
        return cache.computeIfAbsent(tenantCode, code -> {
            var ids = jdbc.queryForList("SELECT id FROM face_tenants WHERE code = ? AND status = 'active'",
                    String.class, code);
            if (ids.isEmpty()) {
                throw new FaceServiceException(ErrorCode.TENANT_DISABLED);
            }
            return ids.getFirst();
        });
    }
}
