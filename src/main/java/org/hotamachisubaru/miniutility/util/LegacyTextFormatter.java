package org.hotamachisubaru.miniutility.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Shared immutable Adventure serializers used by both server and chat threads. */
public final class LegacyTextFormatter {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();
    private static final Component CHAT_SEPARATOR = Component.text(" » ");

    private LegacyTextFormatter() {
    }

    public static Component text(@Nullable String message) {
        return Component.text(orEmpty(message));
    }

    public static Component text(@Nullable String message, NamedTextColor color) {
        return Component.text(orEmpty(message), Objects.requireNonNull(color, "color"));
    }

    public static String plain(Component component) {
        return PLAIN.serialize(Objects.requireNonNull(component, "component"));
    }

    /** Existing data can contain either & or section-sign legacy formatting. */
    public static Component legacy(@Nullable String legacyText) {
        return deserialize(AMPERSAND, normalizeLegacyMarkers(legacyText));
    }

    public static Component legacySection(@Nullable String legacyText) {
        return deserialize(SECTION, orEmpty(legacyText));
    }

    public static String ampersandToSection(@Nullable String legacyText) {
        return SECTION.serialize(legacy(legacyText));
    }

    /** Appending siblings keeps a nickname's color from becoming the message's parent style. */
    public static Component chatMessage(Component displayName, Component message) {
        return Component.empty()
                .append(Objects.requireNonNull(displayName, "displayName"))
                .append(CHAT_SEPARATOR)
                .append(Objects.requireNonNull(message, "message"));
    }

    private static Component deserialize(LegacyComponentSerializer serializer, String text) {
        return text.isEmpty() ? Component.empty() : serializer.deserialize(text);
    }

    private static String normalizeLegacyMarkers(@Nullable String text) {
        return orEmpty(text).replace('§', '&');
    }

    private static String orEmpty(@Nullable String text) {
        return Objects.requireNonNullElse(text, "");
    }
}