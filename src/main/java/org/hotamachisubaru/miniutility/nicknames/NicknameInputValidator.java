package org.hotamachisubaru.miniutility.nicknames;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

/** Validates the visible 1–16 Unicode characters, independently of legacy colors. */
public final class NicknameInputValidator {

    private static final int MAX_LENGTH = 16;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[\\p{L}\\p{M}\\p{N}_-]+");
    private static final Pattern LEGACY_CODE = Pattern.compile("[&§][0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private NicknameInputValidator() {
    }

    public static boolean isValidPlainNickname(@Nullable String nickname) {
        return nickname != null
                && !nickname.isEmpty()
                && nickname.codePointCount(0, nickname.length()) <= MAX_LENGTH
                && ALLOWED_CHARACTERS.matcher(nickname).matches();
    }

    public static boolean isValidColoredNickname(String nickname) {
        return isValidPlainNickname(visibleWithoutLegacyCodes(nickname));
    }

    public static String visibleWithoutLegacyCodes(String nickname) {
        return LEGACY_CODE.matcher(Objects.requireNonNull(nickname, "nickname")).replaceAll("");
    }
}