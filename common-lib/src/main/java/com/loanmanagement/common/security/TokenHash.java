package com.loanmanagement.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * One-way hashing utility for token material stored outside the JWT itself.
 */
public final class TokenHash {

    private TokenHash() {
    }

    public static String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Token value must not be blank");
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
