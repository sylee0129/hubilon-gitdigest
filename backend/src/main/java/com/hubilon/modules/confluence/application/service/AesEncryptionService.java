package com.hubilon.modules.confluence.application.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DELIMITER = ":";

    @Value("${CONFLUENCE_TOKEN_SECRET:}")
    private String secretBase64;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "환경변수 CONFLUENCE_TOKEN_SECRET이 설정되지 않았습니다. 애플리케이션을 시작할 수 없습니다.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "CONFLUENCE_TOKEN_SECRET은 32바이트(Base64 인코딩)여야 합니다. 현재: " + keyBytes.length + "바이트");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("AesEncryptionService 초기화 완료");
    }

    /**
     * 평문을 AES-256-GCM으로 암호화한다.
     *
     * @return Base64(IV) + ":" + Base64(CipherText+AuthTag)
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes());

            return Base64.getEncoder().encodeToString(iv)
                    + DELIMITER
                    + Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception e) {
            throw new IllegalStateException("토큰 암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 암호화된 문자열(Base64(IV):Base64(CipherText+AuthTag))을 복호화한다.
     * 복호화된 값은 절대 로그에 출력하지 않는다.
     */
    public String decrypt(String encrypted) {
        try {
            String[] parts = encrypted.split(DELIMITER, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("올바르지 않은 암호화 형식입니다.");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherBytes));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 복호화 중 오류가 발생했습니다.", e);
        }
    }
}
