package org.hotamachisubaru.miniutility.nicknames;

import java.util.Objects;
import java.util.regex.Pattern;

public final class NicknameValidator {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 16;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[\\p{L}\\p{M}\\p{N}_-]+");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)[&§][0-9a-fk-or]");

    private NicknameValidator() {
    }

    public static boolean isValidPlainNickname(String nickname) {
        if (nickname == null) {
            return false;
        }

        int length = nickname.codePointCount(0, nickname.length());
        return length >= MIN_LENGTH
                && length <= MAX_LENGTH
                && ALLOWED_CHARACTERS.matcher(nickname).matches();
    }

    public static String visibleWithoutLegacyCodes(String nickname) {
        return LEGACY_CODE.matcher(Objects.requireNonNull(nickname, "nickname")).replaceAll("");
    }
}
