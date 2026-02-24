package controllers;

import java.util.regex.Pattern;

public final class InputValidationUtils {
    private static final Pattern MEANINGFUL_CHAR_PATTERN = Pattern.compile("[\\p{L}\\p{N}]");

    private InputValidationUtils() {
    }

    public static boolean hasMeaningfulText(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && MEANINGFUL_CHAR_PATTERN.matcher(trimmed).find();
    }
}
