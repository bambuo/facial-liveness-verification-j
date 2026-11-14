package faceservice.domain.key;

/**
 * 租户密钥抽象：落库实现（face_tenant_keys + 主密钥包裹）。密钥不可用必须抛异常，不降级明文。
 */
public interface KeyStore {

    /**
     * 取当前激活版本租户密钥（32 字节）；首次访问自动生成并落库。
     */
    byte[] currentKey(String tenantId);
}
