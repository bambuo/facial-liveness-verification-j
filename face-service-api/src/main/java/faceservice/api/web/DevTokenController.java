package faceservice.api.web;

import faceservice.application.token.TokenService;
import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import faceservice.contracts.dto.DevTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * 本地签发 dev JWT：固定 tenant=dev。
 * 双条件：dev profile 且 faceservice.auth.enable-dev-token=true；生产任一条件不满足即 DEV_DISABLED。
 */
@RestController
@RequestMapping("/dev")
@Profile("dev")
public class DevTokenController {

    private final TokenService tokens;
    private final boolean enabled;

    public DevTokenController(TokenService tokens,
                              @Value("${faceservice.auth.enable-dev-token:false}") boolean enabled) {
        this.tokens = tokens;
        this.enabled = enabled;
    }

    @PostMapping("/token")
    public ApiEnvelope token(@RequestBody(required = false) Map<String, Object> body,
                             jakarta.servlet.http.HttpServletRequest request) {
        if (!enabled) {
            throw new FaceServiceException(ErrorCode.DEV_DISABLED);
        }
        String sub = body == null ? null : String.valueOf(body.getOrDefault("sub", ""));
        long exp = Instant.now().plusSeconds(300).getEpochSecond();
        return ApiEnvelope.ok(new DevTokenResponse(tokens.issueDevToken(sub), "dev",
                sub == null || sub.isBlank() ? "dev-user" : sub, exp), request.getHeader("X-Request-Id"));
    }
}
