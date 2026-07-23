package com.tiktokinsight.datasource.administration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataSourceCrypto {
    private final String masterKey;
    public DataSourceCrypto(@Value("${DATA_SOURCE_MASTER_KEY:}") String masterKey) { this.masterKey = masterKey; }
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return null;
        try { byte[] iv = new byte[12]; new SecureRandom().nextBytes(iv); Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key(), "AES"), new GCMParameterSpec(128, iv)); byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)); return Base64.getEncoder().encodeToString(iv) + "." + Base64.getEncoder().encodeToString(encrypted); }
        catch (Exception ex) { throw new IllegalStateException("Unable to encrypt data source secret", ex); }
    }
    private byte[] key() throws Exception { if (masterKey.isBlank()) throw new IllegalStateException("DATA_SOURCE_MASTER_KEY must be configured"); return MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8)); }
}
