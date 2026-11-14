package faceservice.contracts.dto;

/**
 * POST /v1/enrollments 响应。
 */
public record EnrollResponse(String profileId, String status, String modelVersion) {
}
