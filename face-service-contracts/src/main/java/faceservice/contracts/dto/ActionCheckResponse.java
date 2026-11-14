package faceservice.contracts.dto;

/**
 * POST /v1/action-check 响应：单动作预检结果（引导用，不消费挑战）。
 */
public record ActionCheckResponse(boolean satisfied, String detail) {
}
