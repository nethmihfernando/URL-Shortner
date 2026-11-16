package com.example.urlshortener.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates random Base62 short codes (0-9, A-Z, a-z). */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(@Value("${app.short-code-length:7}") int length) {
        if (length < 4 || length > 32) {
            throw new IllegalArgumentException("app.short-code-length must be between 4 and 32");
        }
        this.length = length;
    }

    public String generate() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
