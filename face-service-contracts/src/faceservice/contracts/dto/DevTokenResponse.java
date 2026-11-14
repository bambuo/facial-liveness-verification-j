package faceservice.contracts.dto;

/**
 * dev token 签发结果（仅 Development/dev profile）。
 */
public record DevTokenResponse(String token, String tenant, String sub, long expiresAt) {
}
