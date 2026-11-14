package faceservice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * face_profiles 持久化实体（POJO：MyBatis-Plus 需要空构造与 getter/setter）。
 */
@TableName("face_profiles")
public class FaceProfileEntity {

    private String id;
    private String tenantId;
    private String subjectId;
    private byte[] embeddingCipher;
    private Integer keyVersion;
    private String modelVersion;
    private String status;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime updatedAt;

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

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public byte[] getEmbeddingCipher() {
        return embeddingCipher;
    }

    public void setEmbeddingCipher(byte[] embeddingCipher) {
        this.embeddingCipher = embeddingCipher;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
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

    public java.time.OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
