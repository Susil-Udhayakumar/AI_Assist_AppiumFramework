package io.framework.knowledge;

import io.framework.core.exception.FrameworkException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Short stable hashes for signatures/fingerprints. */
final class Hashes {

    private Hashes() {
    }

    static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new FrameworkException("SHA-1 unavailable", e);
        }
    }
}
