package faceservice.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * face_verification_sessions 持久化实体（POJO：MyBatis-Plus 需要空构造与 getter/setter）。
 */
@TableName("face_verification_sessions")
public class VerificationSessionEntity {

    private String id;
    private String challengeId;
    private String tenantId;
    private String subjectId;
    private String scene;
    private String ref;
    private String mode;
    private String status;
    private java.math.BigDecimal score;
    private String livenessStatus;
    private java.math.BigDecimal clarity;
    private java.math.BigDecimal reality;
    private String deviceId;
    private String requestId;
    private java.time.OffsetDateTime expiresAt;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
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

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.math.BigDecimal getScore() {
        return score;
    }

    public void setScore(java.math.BigDecimal score) {
        this.score = score;
    }

    public String getLivenessStatus() {
        return livenessStatus;
    }

    public void setLivenessStatus(String livenessStatus) {
        this.livenessStatus = livenessStatus;
    }

    public java.math.BigDecimal getClarity() {
        return clarity;
    }

    public void setClarity(java.math.BigDecimal clarity) {
        this.clarity = clarity;
    }

    public java.math.BigDecimal getReality() {
        return reality;
    }

    public void setReality(java.math.BigDecimal reality) {
        this.reality = reality;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public java.time.OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(java.time.OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public java.time.OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(java.time.OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
