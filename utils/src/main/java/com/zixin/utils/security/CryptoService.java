package com.zixin.utils.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

public class CryptoService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final Map<String, String> keyMap;
    private final String currentVersion;

    public CryptoService(Map<String, String> keyMap, String currentVersion) {
        this.keyMap = keyMap;
        this.currentVersion = currentVersion;
    }

    /**
     * 加密
     */
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) return plainText;

        String keyStr = keyMap.get(currentVersion);
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);

        // 随机IV
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGO);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

        return currentVersion + ":" + Base64.getEncoder().encodeToString(result);
    }

    /**
     * 解密（支持多版本key + 兼容旧数据）
     */
    public String decrypt(String encrypted) throws Exception {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;

        if (!encrypted.contains(":")) {
            return legacyDecrypt(encrypted);
        }

        String[] parts = encrypted.split(":", 2);
        String version = parts[0];
        String base64 = parts[1];

        String keyStr = keyMap.get(version);
        if (keyStr == null) {
            throw new RuntimeException("Unknown key version: " + version);
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        byte[] allBytes = Base64.getDecoder().decode(base64);

        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherBytes = new byte[allBytes.length - IV_LENGTH];

        System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH);
        System.arraycopy(allBytes, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance(ALGO);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 兼容旧 ECB 数据
     */
    private String legacyDecrypt(String encrypted) {
        try {
            byte[] key = "ChronicCare2024!".getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));

            byte[] decoded = Base64.getDecoder().decode(encrypted);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encrypted;
        }
    }
}