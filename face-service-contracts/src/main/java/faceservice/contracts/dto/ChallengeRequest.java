package faceservice.contracts.dto;

/**
 * POST /v1/challenges 请求体（Jackson 全局 SNAKE_CASE）。
 */
public record ChallengeRequest(String scene, String mode, String gallery, String deviceId) {
}
