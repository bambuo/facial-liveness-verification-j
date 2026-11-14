package faceservice.contracts;

/**
 * 错误码（与 docs/需求/0001/0001-人脸核验平台-实施方案.md §9 对齐，值即对外契约，不得随意增改）。
 */
public enum ErrorCode {
    AUTH_INVALID(401),
    TENANT_DISABLED(403),
    QUOTA_EXCEEDED(429),
    CHALLENGE_EXPIRED(409),
    CHALLENGE_REPLAY(409),
    NO_FACE(422),
    MULTI_FACE(422),
    LOW_QUALITY(422),
    LIVENESS_FAILED(422),
    ACTION_MISMATCH(422),
    FACE_MISMATCH(422),
    FACE_NOT_ENROLLED(404),
    GALLERY_NOT_FOUND(404),
    DOCUMENT_UNSUPPORTED(422),
    DOCUMENT_LOW_CONFIDENCE(422),
    DOCUMENT_INVALID(422),
    SERVICE_UNAVAILABLE(503),
    DEV_DISABLED(404);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
