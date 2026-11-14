package faceservice.infrastructure.key;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM：密文 = nonce(12B) || cipherText+tag。JDK 内置，无第三方依赖。
 */
public final class AesGcm {

    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private AesGcm() {
    }

    public static byte[] encrypt(byte[] plain, byte[] key) {
        try {
            byte[] nonce = new byte[NONCE_LEN];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(key), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plain);
            byte[] out = new byte[NONCE_LEN + encrypted.length];
            System.arraycopy(nonce, 0, out, 0, NONCE_LEN);
            System.arraycopy(encrypted, 0, out, NONCE_LEN, encrypted.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("aes-gcm encrypt failed", e);
        }
    }

    public static byte[] decrypt(byte[] sealed, byte[] key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey(key),
                    new GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_LEN));
            return cipher.doFinal(sealed, NONCE_LEN, sealed.length - NONCE_LEN);
        } catch (Exception e) {
            throw new IllegalStateException("aes-gcm decrypt failed", e);
        }
    }

    /**
     * 密钥字节不满足 32 字节时 SHA-256 拉齐（主密钥为口令派生场景）。
     */
    private static SecretKeySpec aesKey(byte[] key) {
        try {
            byte[] k = key.length == 32 ? key
                    : Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(key), 32);
            return new SecretKeySpec(k, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("aes key derivation failed", e);
        }
    }
}
