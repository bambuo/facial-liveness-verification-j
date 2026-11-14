package faceservice.infrastructure.key;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.domain.key.KeyStore;
import faceservice.infrastructure.persistence.TenantKeyEntity;
import faceservice.infrastructure.persistence.mapper.TenantKeyMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 落库密钥库：租户密钥存 face_tenant_keys，key_cipher 由主密钥（环境变量注入）包裹。
 * 密钥不可用一律抛异常，不降级明文。
 */
@Component
public class DbKeyStore implements KeyStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MASTER_KEY_ID = "master";

    private final TenantKeyMapper keys;
    private final SecurityProperties securityProperties;

    public DbKeyStore(TenantKeyMapper keys, SecurityProperties securityProperties) {
        this.keys = keys;
        this.securityProperties = securityProperties;
    }

    @Override
    public byte[] currentKey(String tenantId) {
        try {
            UUID tenantUuid = UUID.fromString(tenantId);
            TenantKeyEntity active = keys.selectActive(tenantUuid);
            if (active != null) {
                return AesGcm.decrypt(active.getKeyCipher(), securityProperties.masterKeyBytes());
            }
            byte[] key = new byte[32];
            RANDOM.nextBytes(key);
            byte[] wrapped = AesGcm.encrypt(key, securityProperties.masterKeyBytes());
            TenantKeyEntity entity = new TenantKeyEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setTenantId(tenantId);
            entity.setKeyVersion(keys.selectMaxVersion(tenantUuid) + 1);
            entity.setKeyCipher(wrapped);
            entity.setStatus("active");
            keys.insert(entity);
            return key;
        } catch (DataAccessException | IllegalStateException e) {
            // 密钥不可用：拒绝写入，不降级明文
            throw new FaceServiceException(ErrorCode.SERVICE_UNAVAILABLE, "密钥库不可用，拒绝处理人脸特征");
        }
    }
}
