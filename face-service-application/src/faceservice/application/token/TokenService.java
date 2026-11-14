package faceservice.application.token;

import faceservice.contracts.ErrorCode;
import faceservice.contracts.FaceServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;

/**
 * JWT 签发与验签（jjwt，HS256）。
 * 端上 token：业务后端/平台 dev 签发，5 分钟，aud=face-service；
 * 结果令牌 verification_token：平台签发，一次性 jti，业务后端验签后消费。
 */
@Component
public class TokenService {

    private static final String AUDIENCE = "face-service";
    private static final long TTL_SECONDS = 300;

    private final SecretKey accessKey;
    private final SecretToken resultToken;

    public TokenService(@Value("${faceservice.auth.dev-signing-key}") String devSigningKey,
                        @Value("${faceservice.auth.result-token-signing-key}") String resultSigningKey) {
        this.accessKey = key(devSigningKey);
        this.resultToken = new SecretToken(key(resultSigningKey));
    }

    private static SecretKey key(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new javax.crypto.spec.SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * dev token（仅 dev profile 的 /dev/token 使用），tenant 固定 dev，sub 默认 dev-user。
     */
    public String issueDevToken(String sub) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", "dev");
        claims.put("sub", sub == null || sub.isBlank() ? "dev-user" : sub);
        claims.put("scene", "kyc");
        claims.put("ops", List.of("match", "liveness", "enroll", "verify", "identify", "document", "delete"));
        claims.put("jti", UUID.randomUUID().toString());
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("dev").audience().add(AUDIENCE).and()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TTL_SECONDS)))
                .signWith(accessKey)
                .compact();
    }

    /**
     * 端上 JWT 验签 + 过期校验（aud/iss）。
     */
    public Claims parseAccess(String token) {
        try {
            return Jwts.parser().verifyWith(accessKey).requireAudience(AUDIENCE).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new FaceServiceException(ErrorCode.AUTH_INVALID, "JWT 无效、过期或签名错误");
        }
    }

    /**
     * 结果令牌：平台签名，含一次性 jti，5 分钟有效。
     */
    public String signVerificationToken(String tenant, String sub, String scene, String ref,
                                        String result, String liveness, double score) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", tenant);
        claims.put("sub", sub);
        claims.put("scene", scene);
        if (ref != null) {
            claims.put("ref", ref);
        }
        claims.put("result", result);
        claims.put("liveness", liveness);
        claims.put("score", score);
        claims.put("jti", UUID.randomUUID().toString());
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("face-service").audience().add(tenant).and()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TTL_SECONDS)))
                .signWith(resultToken.key())
                .compact();
    }

    private record SecretToken(SecretKey key) {
    }
}
