package faceservice.api.web;

/**
 * 统一响应契约：{code, message, data, request_id}，code=0 成功，失败为错误码字符串。
 */
public record ApiEnvelope(Object code, String message, Object data, String requestId) {

    public static ApiEnvelope ok(Object data, String requestId) {
        return new ApiEnvelope(0, "ok", data, requestId);
    }

    public static ApiEnvelope error(String errorCode, String message, String requestId) {
        return new ApiEnvelope(errorCode, message, null, requestId);
    }
}
