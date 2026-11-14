package faceservice.api.web;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 统一异常 → 响应契约；错误码值即契约，不在 handler 层改写。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static String messageOf(FaceServiceException e) {
        String m = e.getMessage();
        return m == null || m.equals(e.code().name()) ? e.code().name() : m;
    }

    @ExceptionHandler(FaceServiceException.class)
    public ResponseEntity<ApiEnvelope> business(FaceServiceException e, jakarta.servlet.http.HttpServletRequest req) {
        return ResponseEntity.status(e.code().httpStatus())
                .body(ApiEnvelope.error(e.code().name(), messageOf(e), req.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiEnvelope> tooLarge(MaxUploadSizeExceededException e, jakarta.servlet.http.HttpServletRequest req) {
        return ResponseEntity.status(413).body(ApiEnvelope.error("LOW_QUALITY", "上传体积超限", req.getHeader("X-Request-Id")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope> unexpected(Exception e, jakarta.servlet.http.HttpServletRequest req) {
        log.error("unhandled error on {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(503).body(ApiEnvelope.error(ErrorCode.SERVICE_UNAVAILABLE.name(),
                "服务内部错误", req.getHeader("X-Request-Id")));
    }
}
