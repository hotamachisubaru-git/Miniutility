package org.hotamachisubaru.miniutility.nicknames;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NicknameInputValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"Subaru", "ほしまち", "Player_1-2", "é", "é", "１２３", "abcdefghijklmnop", "𠀋"})
    void acceptsUnicodeLettersMarksNumbersAndSixteenCodePoints(String nickname) {
        assertTrue(NicknameInputValidator.isValidPlainNickname(nickname));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abcdefghijklmnopq", "a b", "a.b", "a/b", "&aAlice", "§aAlice", "😀", "\\", "{"})
    void rejectsSpacesUnsupportedPunctuationAndFormattingInPlainMode(String nickname) {
        assertFalse(NicknameInputValidator.isValidPlainNickname(nickname));
    }

    @Test
    void measuresSupplementaryCharactersAsSingleCodePoints() {
        assertTrue(NicknameInputValidator.isValidPlainNickname("𠀋".repeat(16)));
        assertFalse(NicknameInputValidator.isValidPlainNickname("𠀋".repeat(17)));
        assertFalse(NicknameInputValidator.isValidPlainNickname(null));
    }

    @Test
    void coloredModeStripsOnlyRecognizedLegacyCodes() {
        assertEquals("ほしまち", NicknameInputValidator.visibleWithoutLegacyCodes("&Bほし§Lまち&r"));
        assertTrue(NicknameInputValidator.isValidColoredNickname("&Bほし§Lまち&r"));
        assertFalse(NicknameInputValidator.isValidColoredNickname("&zAlice"));
        assertFalse(NicknameInputValidator.isValidColoredNickname("&a"));
        assertFalse(NicknameInputValidator.isValidColoredNickname("&aAlice Bob"));
    }
}