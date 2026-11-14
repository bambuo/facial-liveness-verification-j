package faceservice.api.web;

import faceservice.application.challenge.ActionVerifier;
import faceservice.contracts.dto.ActionCheckResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 单动作预检（联调/接入辅助）：页面分段引导时实时询问"该动作是否已完成"。
 * 不消费 challenge、不写库；最终判定仍以 enroll/verifications 的整单校验为准。
 */
@RestController
@RequestMapping("/v1")
public class ActionCheckController {

    private final ActionVerifier verifier;

    public ActionCheckController(ActionVerifier verifier) {
        this.verifier = verifier;
    }

    @PostMapping("/action-check")
    public ApiEnvelope check(@RequestParam("action") String action,
                             @RequestParam("frames") List<MultipartFile> frames,
                             @RequestParam(value = "baseline_frames", required = false) List<MultipartFile> baselineFrames,
                             jakarta.servlet.http.HttpServletRequest http) throws IOException {
        var result = verifier.checkAction(action, EnrollmentController.toBytes(frames),
                baselineFrames == null ? null : EnrollmentController.toBytes(baselineFrames));
        return ApiEnvelope.ok(new ActionCheckResponse(result.satisfied(), result.detail()),
                http.getHeader("X-Request-Id"));
    }
}
