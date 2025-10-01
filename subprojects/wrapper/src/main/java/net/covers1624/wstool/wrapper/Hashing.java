package net.covers1624.wstool.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by covers1624 on 1/11/24.
 */
public final class Hashing {

    public static final String SHA1 = "SHA-1";
    public static final String SHA256 = "SHA-256";

    public static String hashFile(String alg, Path file) throws IOException {
        MessageDigest digest = digest(alg);
        addFileBytes(digest, file);
        return toString(digest);
    }

    public static MessageDigest digest(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Digest not found: " + alg, ex);
        }
    }

    public static void addUTFBytes(MessageDigest digest, String str) {
        digest.update(str.getBytes(StandardCharsets.UTF_8));
    }

    public static void tryAddFileBytes(MessageDigest digest, Path file) throws IOException {
        if (Files.exists(file)) {
            addFileBytes(digest, file);
        }
    }

    public static void addFileBytes(MessageDigest digest, Path file) throws IOException {
        try (InputStream is = Files.newInputStream(file)) {
            addStreamBytes(digest, is);
        }
    }

    public static void addStreamBytes(MessageDigest digest, InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        int len;
        while ((len = is.read(bytes)) != -1) {
            digest.update(bytes, 0, len);
        }
    }

    public static String toString(MessageDigest digest) {
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            sb.append(Character.forDigit(b >> 4 & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
