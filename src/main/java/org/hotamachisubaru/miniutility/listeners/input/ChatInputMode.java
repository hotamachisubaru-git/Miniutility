package org.hotamachisubaru.miniutility.listeners.input;

import net.kyori.adventure.text.format.NamedTextColor;
import org.hotamachisubaru.miniutility.nicknames.NicknameInputValidator;

/** The validation and retry message for one chat-input mode. */
public enum ChatInputMode {
    NICKNAME("無効なニックネームです。1〜16文字、記号は _- のみ使用可。空白不可。"),
    COLORED_NICKNAME("無効なニックネームです。1〜16文字、記号は _- のみ、空白不可。"),
    EXPERIENCE("数値を入力してください。例: 99 または -5");

    private final ChatInputRouter.InputMessage invalidMessage;

    ChatInputMode(String invalidMessage) {
        this.invalidMessage = new ChatInputRouter.InputMessage(invalidMessage, NamedTextColor.RED);
    }

    public boolean accepts(String input) {
        return switch (this) {
            case NICKNAME -> NicknameInputValidator.isValidPlainNickname(input);
            case COLORED_NICKNAME -> NicknameInputValidator.isValidColoredNickname(input);
            case EXPERIENCE -> isInteger(input);
        };
    }

    public ChatInputRouter.InputMessage invalidMessage() {
        return invalidMessage;
    }

    private static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}