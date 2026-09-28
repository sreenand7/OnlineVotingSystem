package com.voting;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PBKDF2 PIN hashing using the JDK's built-in crypto only.
 * Stored format: pbkdf2$<iterations>$<base64 salt>$<base64 hash>
 */
public final class PasswordHasher {

    private static final String ALGORITHM  = "PBKDF2WithHmacSHA256";
    private static final String PREFIX     = "pbkdf2";
    private static final int    ITERATIONS = 65_536;
    private static final int    KEY_LENGTH = 256;
    private static final int    SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(String pin) {
        if (pin == null || pin.isBlank())
            throw new IllegalArgumentException("PIN must not be null or blank.");
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(pin.toCharArray(), salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean verify(String pin, String storedHash) {
        if (pin == null || storedHash == null || storedHash.isBlank()) return false;
        String[] parts = storedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt     = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual   = derive(pin.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] pin, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PIN hashing is unavailable on this system.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
