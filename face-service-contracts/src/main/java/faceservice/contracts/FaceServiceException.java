package faceservice.contracts;

/**
 * 平台业务异常，由全局异常处理器统一转换为响应契约。
 */
public class FaceServiceException extends RuntimeException {

    private final ErrorCode code;

    public FaceServiceException(ErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public FaceServiceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
