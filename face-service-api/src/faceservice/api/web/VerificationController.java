package faceservice.api.web;

import faceservice.application.verify.VerificationService;
import faceservice.contracts.dto.VerifyResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 档案 1:1：challenge + 活体帧，与已录入档案比对并签发结果令牌。
 */
@RestController
@RequestMapping("/v1")
public class VerificationController {

    private final VerificationService verifications;

    public VerificationController(VerificationService verifications) {
        this.verifications = verifications;
    }

    @PostMapping("/verifications")
    public ApiEnvelope verify(@RequestParam("challenge_id") String challengeId,
                              @RequestParam(value = "device_id", required = false) String deviceId,
                              @RequestParam("frames") List<MultipartFile> frames,
                              jakarta.servlet.http.HttpServletRequest http) throws IOException {
        EnrollmentController.require(frames);
        String rid = http.getHeader("X-Request-Id");
        VerifyResponse response = verifications.verify(EnrollmentController.toBytes(frames), challengeId, deviceId, rid);
        return ApiEnvelope.ok(response, rid);
    }
}
