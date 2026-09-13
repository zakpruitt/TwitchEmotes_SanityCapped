package com.zakpruitt.sanitycapped.image;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Hashes {

    public static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    /**
     * Differing bits between two dHashes; 64 when either is missing.
     */
    public static int hamming(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return Long.SIZE;
        }
        return Long.bitCount(Long.parseUnsignedLong(a, 16) ^ Long.parseUnsignedLong(b, 16));
    }
}
