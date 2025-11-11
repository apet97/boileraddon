package com.example.rules.web;

import java.security.SecureRandom;
import java.util.Base64;

public final class Nonce {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Nonce() {
    }

    public static String create() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
