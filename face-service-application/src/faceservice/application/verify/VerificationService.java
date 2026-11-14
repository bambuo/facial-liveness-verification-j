package faceservice.application.verify;

import faceservice.application.challenge.ActionVerifier;
import faceservice.application.challenge.ChallengeService;
import faceservice.application.challenge.ChallengeService.Challenge;
import faceservice.application.token.TokenService;
import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.contracts.dto.VerifyResponse;
import faceservice.domain.face.FaceEngine;
import faceservice.domain.face.LivenessStatus;
import faceservice.domain.face.LivenessVote;
import faceservice.domain.key.KeyStore;
import faceservice.domain.security.Principal;
import faceservice.domain.security.TenantContext;
import faceservice.infrastructure.key.AesGcm;
import faceservice.infrastructure.persistence.FaceProfileEntity;
import faceservice.infrastructure.persistence.SubjectEntity;
import faceservice.infrastructure.persistence.mapper.FaceProfileMapper;
import faceservice.infrastructure.persistence.mapper.SubjectMapper;
import faceservice.infrastructure.persistence.mapper.VerificationSessionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 档案 1:1 认证：活体投票 → 特征比对 → 签发一次性结果令牌。
 */
@Service
public class VerificationService {

    private final ChallengeService challenges;
    private final ActionVerifier actionVerifier;
    private final FaceEngine engine;
    private final KeyStore keyStore;
    private final TokenService tokens;
    private final SubjectMapper subjectMapper;
    private final FaceProfileMapper profiles;
    private final VerificationSessionMapper sessions;
    private final double matchThreshold;

    public VerificationService(ChallengeService challenges, ActionVerifier actionVerifier, FaceEngine engine, KeyStore keyStore,
                               TokenService tokens, SubjectMapper subjectMapper, FaceProfileMapper profiles,
                               VerificationSessionMapper sessions,
                               @Value("${faceservice.face.match-threshold:0.35}") double matchThreshold) {
        this.challenges = challenges;
        this.actionVerifier = actionVerifier;
        this.engine = engine;
        this.keyStore = keyStore;
        this.tokens = tokens;
        this.subjectMapper = subjectMapper;
        this.profiles = profiles;
        this.sessions = sessions;
        this.matchThreshold = matchThreshold;
    }

    private static Double round(double score) {
        return java.math.BigDecimal.valueOf(score).setScale(5, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private static float[] bytesToFloats(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < out.length; i++) {
            out[i] = buffer.getFloat();
        }
        return out;
    }

    @Transactional
    public VerifyResponse verify(List<byte[]> frames, String challengeId, String deviceId, String requestId) {
        Challenge challenge = challenges.consume(challengeId, "verify", deviceId);
        Principal principal = TenantContext.require();

        if (frames == null || frames.isEmpty()) {
            throw new FaceServiceException(ErrorCode.NO_FACE, "未收到任何帧");
        }

        LivenessVote vote = engine.checkLiveness(frames);
        if (vote.status() == LivenessStatus.SPOOF) {
            recordSession(challenge, principal, deviceId, requestId, "failed", null, null, vote);
            throw new FaceServiceException(ErrorCode.LIVENESS_FAILED);
        }
        if (vote.status() == LivenessStatus.FUZZY) {
            recordSession(challenge, principal, deviceId, requestId, "failed", null, null, vote);
            throw new FaceServiceException(ErrorCode.LOW_QUALITY);
        }

        var subject = subjectMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SubjectEntity>()
                        .eq(SubjectEntity::getExternalSubjectId, principal.sub())
                        .eq(SubjectEntity::getStatus, "active")
                        .last("LIMIT 1"));
        String subjectId = subject.isEmpty() ? null : subject.getFirst().getId();
        if (subjectId == null) {
            throw new FaceServiceException(ErrorCode.FACE_NOT_ENROLLED);
        }
        actionVerifier.verify(challenge.actions(), challenge.framesPerAction(), frames);

        FaceProfileEntity profile = profiles.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FaceProfileEntity>()
                        .eq(FaceProfileEntity::getSubjectId, subjectId)
                        .eq(FaceProfileEntity::getStatus, "active")
                        .last("LIMIT 1"));
        if (profile == null) {
            throw new FaceServiceException(ErrorCode.FACE_NOT_ENROLLED);
        }
        if (!engine.modelVersion().equals(profile.getModelVersion())) {
            throw new FaceServiceException(ErrorCode.SERVICE_UNAVAILABLE, "特征版本不一致，请重新录入");
        }

        float[] probe = engine.extractLargest(frames);
        float[] stored = bytesToFloats(AesGcm.decrypt(profile.getEmbeddingCipher(),
                keyStore.currentKey(principal.tenantId())));
        double score = engine.compare(probe, stored);
        boolean passed = score >= matchThreshold;
        recordSession(challenge, principal, deviceId, requestId, passed ? "passed" : "failed",
                subjectId, score, vote);
        if (!passed) {
            throw new FaceServiceException(ErrorCode.FACE_MISMATCH);
        }
        String token = tokens.signVerificationToken(principal.tenantCode(), principal.sub(),
                challenge.scene(), challenge.ref(), "passed", "real", score);
        return new VerifyResponse("passed", round(score), "real", token);
    }

    private void recordSession(Challenge challenge, Principal principal, String deviceId, String requestId,
                               String status, String subjectId, Double score, LivenessVote vote) {
        faceservice.infrastructure.persistence.VerificationSessionEntity session =
                new faceservice.infrastructure.persistence.VerificationSessionEntity();
        session.setId(UUID.randomUUID().toString());
        session.setChallengeId(challenge.id());
        session.setTenantId(challenge.tenantId());
        session.setSubjectId(subjectId);
        session.setScene(challenge.scene());
        session.setRef(challenge.ref());
        session.setMode(challenge.mode());
        session.setStatus(status);
        session.setScore(score == null ? null : java.math.BigDecimal.valueOf(score).setScale(5, java.math.RoundingMode.HALF_UP));
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
