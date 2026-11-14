package faceservice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * face_subjects 持久化实体（POJO：MyBatis-Plus 需要空构造与 getter/setter）。
 */
@TableName("face_subjects")
public class SubjectEntity {

    private String id;
    private String tenantId;
    private String externalSubjectId;
    private String status;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime deletedAt;

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

    public String getExternalSubjectId() {
        return externalSubjectId;
    }

    public void setExternalSubjectId(String externalSubjectId) {
        this.externalSubjectId = externalSubjectId;
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

    public java.time.OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(java.time.OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
