package com.tns.tms.shared.util;

import java.util.regex.Pattern;

/**
 * Validates password complexity requirements:
 * - At least 8 characters
 * - At least one uppercase letter
 * - At least one digit
 * - At least one special character
 */
public final class PasswordValidator {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

    private PasswordValidator() {}

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8) return false;
        return UPPERCASE.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }

    public static void validate(String password) {
        if (!isValid(password)) {
            throw new com.tns.tms.shared.exception.ValidationException(
                "Password must be at least 8 characters and include at least one uppercase letter, " +
                "one number, and one special character."
            );
        }
    }
}
