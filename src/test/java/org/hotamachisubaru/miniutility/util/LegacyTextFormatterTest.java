package org.hotamachisubaru.miniutility.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTextFormatterTest {

    @Test
    void preservesMixedLegacyColorsAndFormattingThroughStorageConversion() {
        String stored = LegacyTextFormatter.ampersandToSection("&aAlice§l_1&r");
        Component restored = LegacyTextFormatter.legacySection(stored).compact();
        assertEquals("Alice_1", LegacyTextFormatter.plain(restored));
        assertEquals(LegacyTextFormatter.legacy("&aAlice§l_1&r").compact(), restored);
        assertEquals(NamedTextColor.GREEN, restored.color());
        assertEquals("_1", LegacyTextFormatter.plain(Objects.requireNonNull(restored.children().getLast())));
        assertEquals(TextDecoration.State.TRUE, restored.children().getLast().decoration(TextDecoration.BOLD));
        assertTrue(stored.contains("§a"));
        assertTrue(stored.contains("§l"));
    }

    @Test
    void nicknameColorAndDecorationsDoNotBecomeTheMessageParentStyle() {
        Component nickname = Component.text("Alice", NamedTextColor.RED).decorate(TextDecoration.BOLD);
        Component message = Component.text("hello", NamedTextColor.BLUE);
        Component rendered = LegacyTextFormatter.chatMessage(nickname, message);
        assertEquals("Alice » hello", LegacyTextFormatter.plain(rendered));
        assertNull(rendered.color());
        assertEquals(TextDecoration.State.NOT_SET, rendered.decoration(TextDecoration.BOLD));
        assertEquals(message, rendered.children().getLast());
    }

    @Test
    void retainsNullAsEmptyTextContracts() {
        assertEquals(Component.empty(), LegacyTextFormatter.legacy(null));
        assertEquals(Component.empty(), LegacyTextFormatter.legacySection(null));
        assertEquals("", LegacyTextFormatter.ampersandToSection(null));
        assertEquals("", LegacyTextFormatter.plain(LegacyTextFormatter.text(null)));
    }
}
