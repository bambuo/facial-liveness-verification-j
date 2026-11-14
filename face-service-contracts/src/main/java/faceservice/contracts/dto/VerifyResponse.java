package faceservice.contracts.dto;

/**
 * POST /v1/verifications 响应。result: passed/failed；liveness: real/spoof/fuzzy。
 */
public record VerifyResponse(String result, Double score, String liveness, String verificationToken) {
}
