package faceservice.api.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.infrastructure.persistence.TenantEntity;
import faceservice.infrastructure.persistence.mapper.TenantMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户 code → face_tenants.id 解析（进程内缓存；未注册/停用租户拒绝）。
 */
@Component
public class TenantLookup {

    private final TenantMapper tenants;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public TenantLookup(TenantMapper tenants) {
        this.tenants = tenants;
    }

    public String idOf(String tenantCode) {
        return cache.computeIfAbsent(tenantCode, code -> {
            TenantEntity tenant = tenants.selectOne(Wrappers.<TenantEntity>lambdaQuery()
                    .select(TenantEntity::getId)
                    .eq(TenantEntity::getCode, code)
                    .eq(TenantEntity::getStatus, "active"));
            if (tenant == null) {
                throw new FaceServiceException(ErrorCode.TENANT_DISABLED);
            }
            return tenant.getId();
        });
    }
}
