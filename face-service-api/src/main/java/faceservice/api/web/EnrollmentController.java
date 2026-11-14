package faceservice.api.web;

import faceservice.application.enroll.EnrollmentService;
import faceservice.contracts.dto.EnrollResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 建立/更新人脸档案：challenge + 活体帧，存加密特征。
 */
@RestController
@RequestMapping("/v1")
public class EnrollmentController {

    private final EnrollmentService enrollments;

    public EnrollmentController(EnrollmentService enrollments) {
        this.enrollments = enrollments;
    }

    static void require(List<MultipartFile> frames) {
        if (frames == null || frames.isEmpty()) {
            throw new faceservice.contracts.FaceServiceException(faceservice.contracts.ErrorCode.NO_FACE, "未收到任何帧");
        }
        if (frames.size() > 15) {
            throw new faceservice.contracts.FaceServiceException(faceservice.contracts.ErrorCode.AUTH_INVALID, "帧数超过 15");
        }
    }

    static List<byte[]> toBytes(List<MultipartFile> frames) throws IOException {
        List<byte[]> out = new ArrayList<>(frames.size());
        for (MultipartFile f : frames) {
            out.add(f.getBytes());
        }
        return out;
    }

    @PostMapping("/enrollments")
    public ApiEnvelope enroll(@RequestParam("challenge_id") String challengeId,
                              @RequestParam(value = "device_id", required = false) String deviceId,
                              @RequestParam("frames") List<MultipartFile> frames,
                              jakarta.servlet.http.HttpServletRequest http) throws IOException {
        require(frames);
        String rid = http.getHeader("X-Request-Id");
        EnrollResponse response = enrollments.enroll(toBytes(frames), challengeId, deviceId, rid);
        return ApiEnvelope.ok(response, rid);
    }
}
