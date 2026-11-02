package com.llucascr.identify_service;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/** Shared temporary JWT signing keys for Spring context tests. */
final class TestJwtKeys {
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestJwtKeys() {}

    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.public.key", () -> writePem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded()));
        registry.add("jwt.private.key", () -> writePem("PRIVATE KEY", KEY_PAIR.getPrivate().getEncoded()));
    }

    private static KeyPair generateKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate temporary JWT test keys", exception);
        }
    }

    private static String writePem(String type, byte[] bytes) {
        try {
            Path file = Files.createTempFile("identify-test-key-", ".pem");
            file.toFile().deleteOnExit();
            Files.writeString(file, "-----BEGIN " + type + "-----\n"
                    + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                    + "\n-----END " + type + "-----\n");
            return file.toUri().toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write temporary JWT test key", exception);
        }
    }
}
