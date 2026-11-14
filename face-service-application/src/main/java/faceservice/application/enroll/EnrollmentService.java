package faceservice.application.enroll;

import faceservice.application.challenge.ActionVerifier;
import faceservice.application.challenge.ChallengeService;
import faceservice.application.challenge.ChallengeService.Challenge;
import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.contracts.dto.EnrollResponse;
import faceservice.domain.face.FaceEngine;
import faceservice.domain.face.LivenessStatus;
import faceservice.domain.face.LivenessVote;
import faceservice.domain.key.KeyStore;
import faceservice.domain.security.Principal;
import faceservice.domain.security.TenantContext;
import faceservice.infrastructure.key.AesGcm;
import faceservice.infrastructure.persistence.FaceProfileEntity;
import faceservice.infrastructure.persistence.SubjectEntity;
import faceservice.infrastructure.persistence.VerificationSessionEntity;
import faceservice.infrastructure.persistence.mapper.FaceProfileMapper;
import faceservice.infrastructure.persistence.mapper.SubjectMapper;
import faceservice.infrastructure.persistence.mapper.VerificationSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 人脸档案录入：活体投票 → 特征提取 → AES-GCM 加密落库（只存特征，不存原始帧）。
 */
@Service
public class EnrollmentService {

    private final ChallengeService challenges;
    private final ActionVerifier actionVerifier;
    private final FaceEngine engine;
    private final KeyStore keyStore;
    private final SubjectMapper subjects;
    private final FaceProfileMapper profiles;
    private final VerificationSessionMapper sessions;

    public EnrollmentService(ChallengeService challenges, ActionVerifier actionVerifier, FaceEngine engine, KeyStore keyStore,
                             SubjectMapper subjects, FaceProfileMapper profiles,
                             VerificationSessionMapper sessions) {
        this.challenges = challenges;
        this.actionVerifier = actionVerifier;
        this.engine = engine;
        this.keyStore = keyStore;
        this.subjects = subjects;
        this.profiles = profiles;
        this.sessions = sessions;
    }

    private static byte[] floatsToBytes(float[] vector) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(vector.length * 4);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }

    @Transactional
    public EnrollResponse enroll(List<byte[]> frames, String challengeId, String deviceId, String requestId) {
        Challenge challenge = challenges.consume(challengeId, "enroll", deviceId);
        Principal principal = TenantContext.require();

        if (frames == null || frames.isEmpty()) {
            throw new FaceServiceException(ErrorCode.NO_FACE, "未收到任何帧");
        }

        LivenessVote vote = engine.checkLiveness(frames);
        if (vote.status() == LivenessStatus.SPOOF) {
            recordSession(challenge, deviceId, requestId, "failed", vote);
            throw new FaceServiceException(ErrorCode.LIVENESS_FAILED);
        }
        if (vote.status() == LivenessStatus.FUZZY) {
            recordSession(challenge, deviceId, requestId, "failed", vote);
            throw new FaceServiceException(ErrorCode.LOW_QUALITY);
        }

        actionVerifier.verify(challenge.actions(), challenge.framesPerAction(), frames);

        float[] embedding = engine.extractLargest(frames);
        String modelVersion = engine.modelVersion();

        SubjectEntity subject = findOrCreateSubject(principal);
        byte[] tenantKey = keyStore.currentKey(principal.tenantId());
        byte[] cipher = AesGcm.encrypt(floatsToBytes(embedding), tenantKey);

        OffsetDateTime now = OffsetDateTime.now();
        // 同一主体只保留一份 active 档案：旧档案标记 deleted（覆盖式录入，保留审计）
        var existing = profiles.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FaceProfileEntity>()
                .eq(FaceProfileEntity::getSubjectId, subject.getId())
                .eq(FaceProfileEntity::getStatus, "active"));
        for (FaceProfileEntity old : existing) {
            profiles.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FaceProfileEntity>()
                    .eq(FaceProfileEntity::getId, old.getId())
                    .set(FaceProfileEntity::getStatus, "deleted")
                    .set(FaceProfileEntity::getUpdatedAt, now));
        }
        String profileId = UUID.randomUUID().toString();
        FaceProfileEntity profile = new FaceProfileEntity();
        profile.setId(profileId);
        profile.setTenantId(principal.tenantId());
        profile.setSubjectId(subject.getId());
        profile.setEmbeddingCipher(cipher);
        profile.setKeyVersion(1);
        profile.setModelVersion(modelVersion);
        profile.setStatus("active");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profiles.insert(profile);
        recordSession(challenge, deviceId, requestId, "passed", vote);
        return new EnrollResponse(profileId, "active", modelVersion);
    }

    private SubjectEntity findOrCreateSubject(Principal principal) {
        var found = subjects.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SubjectEntity>()
                .eq(SubjectEntity::getExternalSubjectId, principal.sub())
                .eq(SubjectEntity::getStatus, "active"));
        if (!found.isEmpty()) {
            return found.getFirst();
        }
        SubjectEntity subject = new SubjectEntity();
        subject.setId(UUID.randomUUID().toString());
        subject.setTenantId(principal.tenantId());
        subject.setExternalSubjectId(principal.sub());
        subject.setStatus("active");
        subject.setCreatedAt(OffsetDateTime.now());
        subjects.insert(subject);
        return subject;
    }

    private void recordSession(Challenge challenge, String deviceId, String requestId, String status, LivenessVote vote) {
        VerificationSessionEntity session = new VerificationSessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setChallengeId(challenge.id());
        session.setTenantId(challenge.tenantId());
        session.setScene(challenge.scene());
        session.setRef(challenge.ref());
        session.setMode(challenge.mode());
        session.setStatus(status);
        session.setLivenessStatus(vote.status().name().toLowerCase());
        session.setClarity(java.math.BigDecimal.valueOf(vote.minScore()).setScale(5, java.math.RoundingMode.HALF_UP));
        session.setReality(java.math.BigDecimal.valueOf(vote.liveRatio()).setScale(5, java.math.RoundingMode.HALF_UP));
        session.setDeviceId(deviceId);
        session.setRequestId(requestId);
        session.setExpiresAt(OffsetDateTime.now().plusSeconds(300));
        session.setCreatedAt(OffsetDateTime.now());
        session.setCompletedAt(OffsetDateTime.now());
        sessions.insert(session);
    }
}
