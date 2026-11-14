package faceservice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import faceservice.infrastructure.persistence.TenantKeyEntity;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * face_tenant_keys 数据访问：当前生效密钥取版本最大的一条，其余 CRUD 走 BaseMapper。
 */
public interface TenantKeyMapper extends BaseMapper<TenantKeyEntity> {

    /**
     * 查租户当前生效（active）且版本最高的密钥。
     */
    TenantKeyEntity selectActive(@Param("tenantId") UUID tenantId);

    /**
     * 查租户已用的最大密钥版本，无记录时返回 0。
     */
    int selectMaxVersion(@Param("tenantId") UUID tenantId);
}
