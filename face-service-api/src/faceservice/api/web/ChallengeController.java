package faceservice.api.web;

import faceservice.application.challenge.ChallengeService;
import faceservice.contracts.dto.ChallengeRequest;
import faceservice.contracts.dto.ChallengeResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 下发动作、帧数、nonce 与过期时间；challenge 一次性。
 */
@RestController
@RequestMapping("/v1")
public class ChallengeController {

    private final ChallengeService challenges;

    public ChallengeController(ChallengeService challenges) {
        this.challenges = challenges;
    }

    @PostMapping("/challenges")
    public ApiEnvelope create(@RequestBody ChallengeRequest request, jakarta.servlet.http.HttpServletRequest http) {
        ChallengeResponse response = challenges.create(request.scene(), request.mode(),
                request.gallery(), request.deviceId());
        return ApiEnvelope.ok(response, http.getHeader("X-Request-Id"));
    }
}
