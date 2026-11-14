package faceservice.infrastructure.key;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全配置：主密钥只从环境变量注入（SECURITY_MASTER_KEY，Base64）。
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(String masterKey) {

    public byte[] masterKeyBytes() {
        if (masterKey == null || masterKey.isBlank()) {
            throw new IllegalStateException("SECURITY_MASTER_KEY 未配置（Base64 32 字节）");
        }
        return java.util.Base64.getDecoder().decode(masterKey);
    }
}
