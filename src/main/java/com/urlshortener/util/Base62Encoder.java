package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final SecureRandom RANDOM = new SecureRandom();

    public String encode(long number) {
        if (number < 0) {
            throw new IllegalArgumentException("Number must be non-negative: " + number);
        }

        if (number == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            sb.append(ALPHABET.charAt((int) (number % BASE)));
            number /= BASE;
        }
        return sb.reverse().toString();
    }

    public long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Input string must not be null or empty");
        }

        long result = 0;
        for (char c : encoded.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + index;
        }
        return result;
    }

    public String generateRandom(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(BASE)));
        }
        return sb.toString();
    }

    public boolean isValidBase62(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}
