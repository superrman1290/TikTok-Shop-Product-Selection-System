package com.tiktokinsight.auth.domain;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= 8
                && password.length() <= 64
                && LETTER.matcher(password).matches()
                && DIGIT.matcher(password).matches();
    }
}
