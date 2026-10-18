package org.hotamachisubaru.miniutility.nicknames;
import org.hotamachisubaru.miniutility.core.NicknameRules;
import org.jspecify.annotations.Nullable;
import java.util.Objects;
/** Validates visible Unicode characters independently of legacy colors. */
public final class NicknameInputValidator {
 private NicknameInputValidator() { }
 public static boolean isValidPlainNickname(@Nullable String nickname) { return NicknameRules.valid(nickname,false); }
 public static boolean isValidColoredNickname(String nickname) { return NicknameRules.valid(nickname,true); }
 public static String visibleWithoutLegacyCodes(String nickname) { return NicknameRules.visible(Objects.requireNonNull(nickname,"nickname")); }
}
