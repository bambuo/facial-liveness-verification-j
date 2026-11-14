package faceservice.application.challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.domain.security.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * challenge：一次性，绑定 tenant/sub/mode/device，TTL 默认 5 分钟。
 * Redis key: face:challenge:{id}，value 为 JSON，EX 到期自动清理。
 */
@Service
public class ChallengeService {

    private static final List<String> ACTION_POOL = List.of("turn_left", "turn_right", "mouth_open");
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final long ttlSeconds;
    private final int framesPerAction;
    private final int actionsCount;
    private final boolean actionVerifyEnabled;
    public ChallengeService(StringRedisTemplate redis, ObjectMapper mapper,
                            @Value("${faceservice.challenge.ttl-seconds:300}") long ttlSeconds,
                            @Value("${faceservice.challenge.frames-per-action:5}") int framesPerAction,
                            @Value("${faceservice.challenge.actions-count:3}") int actionsCount,
                            @Value("${faceservice.challenge.action-verify-enabled:true}") boolean actionVerifyEnabled) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
        this.framesPerAction = Math.max(2, Math.min(8, framesPerAction));
        this.actionsCount = Math.max(1, Math.min(3, actionsCount));
        this.actionVerifyEnabled = actionVerifyEnabled;
    }

    private static String key(String id) {
        return "face:challenge:" + id;
    }

    public faceservice.contracts.dto.ChallengeResponse create(String scene, String mode, String gallery, String deviceId) {
        if (scene == null || scene.isBlank() || mode == null || mode.isBlank() || deviceId == null || deviceId.isBlank()) {
            throw new FaceServiceException(ErrorCode.AUTH_INVALID, "scene/mode/device_id 必填");
        }
        var principal = TenantContext.require();
        // gallery 属于 identify 场景，本期 1:N 未开放，先拒绝避免误用
        if (gallery != null && !gallery.isBlank()) {
            throw new FaceServiceException(ErrorCode.GALLERY_NOT_FOUND, "1:N 检索本期未开放");
        }
        // 动作序列：straight 打底（兼作张嘴基线段）+ 随机动作；关闭校验时退化为正视单段
        List<String> actions = new java.util.ArrayList<>();
        actions.add("straight");
        if (actionVerifyEnabled && actionsCount > 1) {
            var pool = new java.util.ArrayList<>(ACTION_POOL);
            java.util.Collections.shuffle(pool);
            actions.addAll(pool.subList(0, Math.min(pool.size(), actionsCount - 1)));
        }
        int totalFrames = Math.min(15, framesPerAction * actions.size());
        Challenge challenge = new Challenge(UUID.randomUUID().toString(), principal.tenantId(), principal.sub(),
                scene, principal.ref(), mode, deviceId, totalFrames, UUID.randomUUID().toString(),
                Instant.now().plusSeconds(ttlSeconds), actions, framesPerAction);
        try {
            String json = mapper.writeValueAsString(challenge);
            Boolean ok = redis.opsForValue().setIfAbsent(key(challenge.id()), json, Duration.ofSeconds(ttlSeconds));
            if (!Boolean.TRUE.equals(ok)) {
                throw new FaceServiceException(ErrorCode.CHALLENGE_REPLAY);
            }
        } catch (FaceServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new FaceServiceException(ErrorCode.SERVICE_UNAVAILABLE, "挑战写入失败");
        }
        return new faceservice.contracts.dto.ChallengeResponse(challenge.id(), actions,
                challenge.frameCount(), challenge.expiresAt().toString(), challenge.nonce());
    }

    /**
     * 取出并立即删除（一次性）；校验租户/主体/模式/设备绑定。
     */
    public Challenge consume(String challengeId, String mode, String deviceId) {
        String key = key(challengeId);
        String json = redis.opsForValue().get(key);
        if (json == null) {
            throw new FaceServiceException(ErrorCode.CHALLENGE_EXPIRED);
        }
        redis.delete(key);
        Challenge challenge;
        try {
            challenge = mapper.readValue(json, Challenge.class);
        } catch (Exception e) {
            throw new FaceServiceException(ErrorCode.SERVICE_UNAVAILABLE, "挑战数据损坏");
        }
        var principal = TenantContext.require();
        if (!principal.tenantId().equals(challenge.tenantId()) || !principal.sub().equals(challenge.sub())) {
            throw new FaceServiceException(ErrorCode.CHALLENGE_REPLAY, "挑战归属校验失败");
        }
        if (!challenge.mode().equals(mode)) {
            throw new FaceServiceException(ErrorCode.CHALLENGE_REPLAY, "挑战模式不匹配");
        }
        if (!challenge.deviceId().equals(deviceId)) {
            throw new FaceServiceException(ErrorCode.CHALLENGE_REPLAY, "设备绑定不一致");
        }
        return challenge;
    }

    public record Challenge(String id, String tenantId, String sub, String scene, String ref,
                            String mode, String deviceId, int frameCount, String nonce, Instant expiresAt,
                            List<String> actions, int framesPerAction) {
    }
}
