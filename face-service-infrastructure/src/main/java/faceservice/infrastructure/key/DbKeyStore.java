package faceservice.infrastructure.key;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.domain.key.KeyStore;
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

    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final SecurityProperties securityProperties;

    public DbKeyStore(org.springframework.jdbc.core.JdbcTemplate jdbc, SecurityProperties securityProperties) {
        this.jdbc = jdbc;
        this.securityProperties = securityProperties;
    }

    @Override
    public byte[] currentKey(String tenantId) {
        try {
            var rows = jdbc.query(
                    "SELECT key_cipher FROM face_tenant_keys WHERE tenant_id = ? AND status = 'active' "
                            + "ORDER BY key_version DESC LIMIT 1",
                    (rs, n) -> rs.getBytes(1), UUID.fromString(tenantId));
            if (!rows.isEmpty()) {
                return AesGcm.decrypt(rows.getFirst(), securityProperties.masterKeyBytes());
            }
            byte[] key = new byte[32];
            RANDOM.nextBytes(key);
            byte[] wrapped = AesGcm.encrypt(key, securityProperties.masterKeyBytes());
            int version = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(key_version), 0) FROM face_tenant_keys WHERE tenant_id = ?",
                    Integer.class, UUID.fromString(tenantId)) + 1;
            jdbc.update("INSERT INTO face_tenant_keys (id, tenant_id, key_version, key_cipher, status, created_at) "
                            + "VALUES (?, ?, ?, ?, 'active', now())",
                    UUID.randomUUID(), UUID.fromString(tenantId), version, wrapped);
            return key;
        } catch (DataAccessException | IllegalStateException e) {
            // 密钥不可用：拒绝写入，不降级明文
            throw new FaceServiceException(ErrorCode.SERVICE_UNAVAILABLE, "密钥库不可用，拒绝处理人脸特征");
        }
    }
}
