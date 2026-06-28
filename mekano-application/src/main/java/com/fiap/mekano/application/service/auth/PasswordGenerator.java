package com.fiap.mekano.application.service.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PasswordGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%&*()-_=+[]{}";

    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private PasswordGenerator() {
    }

    public static String generate() {
        return generate(12);
    }

    public static String generate(int length) {
        if (length < 12) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 12 caracteres");
        }

        List<Character> chars = new ArrayList<>();

        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SYMBOLS));

        while (chars.size() < length) {
            chars.add(randomChar(ALL));
        }

        Collections.shuffle(chars, RANDOM);

        StringBuilder password = new StringBuilder();

        for (Character c : chars) {
            password.append(c);
        }

        return password.toString();
    }

    private static char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}