package faceservice.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 平台自有配置属性（mica.ai.face.* 由 mica starter 的 FaceProperties 承接）。
 */
@ConfigurationProperties(prefix = "faceservice")
public record AppProperties(Auth auth, Face face, Challenge challenge) {

    public record Auth(String devSigningKey, String resultTokenSigningKey, boolean enableDevToken) {
    }

    public record Face(double matchThreshold, double livenessThreshold, double liveRatioThreshold) {
    }

    public record Challenge(long ttlSeconds, int frameCount) {
    }
}
