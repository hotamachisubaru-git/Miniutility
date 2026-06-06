package org.hotamachisubaru.miniutility.Nickname;

import java.util.regex.Pattern;

public final class NicknameValidator {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 16;
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[&§][0-9a-fk-or]");
    private static final Pattern FORBIDDEN_CHARACTER_PATTERN = Pattern.compile("[<>\"'`$\\\\]");

    private NicknameValidator() {
    }

    public static String validatePlain(String input) {
        if (input == null) {
            return null;
        }

        if (input.contains(" ") || input.contains("　")) {
            return null;
        }
        if (input.contains("&") || input.contains("§")) {
            return null;
        }
        if (input.length() < MIN_LENGTH || input.length() > MAX_LENGTH) {
            return null;
        }
        if (FORBIDDEN_CHARACTER_PATTERN.matcher(input).find()) {
            return null;
        }
        return input;
    }

    public static String visibleWithoutLegacyCodes(String input) {
        return LEGACY_CODE_PATTERN.matcher(input == null ? "" : input).replaceAll("");
    }

    public static String stripLeadingLegacyCodes(String input) {
        if (input == null) {
            return null;
        }

        int index = 0;
        while (index + 1 < input.length()) {
            char marker = input.charAt(index);
            char code = Character.toLowerCase(input.charAt(index + 1));
            boolean isLegacyMarker = marker == '§' || marker == '&';
            boolean isLegacyCode =
                    (code >= '0' && code <= '9')
                            || (code >= 'a' && code <= 'f')
                            || code == 'k'
                            || code == 'l'
                            || code == 'm'
                            || code == 'n'
                            || code == 'o'
                            || code == 'r';

            if (!isLegacyMarker || !isLegacyCode) {
                break;
            }
            index += 2;
        }
        return input.substring(index);
    }
}
