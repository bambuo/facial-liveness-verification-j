package faceservice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * face_tenant_keys 持久化实体（POJO：MyBatis-Plus 需要空构造与 getter/setter）。
 */
@TableName("face_tenant_keys")
public class TenantKeyEntity {

    private String id;
    private String tenantId;
    private Integer keyVersion;
    private byte[] keyCipher;
    private String status;
    private java.time.OffsetDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public byte[] getKeyCipher() {
        return keyCipher;
    }

    public void setKeyCipher(byte[] keyCipher) {
        this.keyCipher = keyCipher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
