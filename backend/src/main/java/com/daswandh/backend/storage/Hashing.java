package com.daswandh.backend.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {

    private Hashing() {}

    // A "fingerprint" of a file: identical bytes always give the identical hash.
    public static String sha256Hex(byte[] data) {
        return digestHex("SHA-256", data);
    }

    // Only used because Cloudinary's upload-signature scheme requires SHA-1.
    // It is never used to protect anything on its own.
    public static String sha1Hex(String data) {
        return digestHex("SHA-1", data.getBytes(StandardCharsets.UTF_8));
    }

    private static String digestHex(String algorithm, byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }
}