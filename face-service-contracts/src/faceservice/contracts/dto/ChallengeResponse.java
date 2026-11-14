package faceservice.contracts.dto;

/**
 * POST /v1/challenges 响应。
 */
public record ChallengeResponse(
        String challengeId,
        java.util.List<String> actions,
        int frameCount,
        String expiresAt,
        String nonce) {
}
