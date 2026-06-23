package org.hotamachisubaru.miniutility.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

public final class ComponentUtil {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private ComponentUtil() {
    }

    public static Component text(String message) {
        return Component.text(Objects.requireNonNullElse(message, ""));
    }

    public static Component text(String message, NamedTextColor color) {
        return Component.text(
                Objects.requireNonNullElse(message, ""),
                Objects.requireNonNull(color, "color")
        );
    }

    public static String plain(Component component) {
        return PLAIN_TEXT.serialize(Objects.requireNonNull(component, "component"));
    }

    public static Component legacy(String legacyText) {
        String normalized = normalizeLegacyMarkers(legacyText);
        return normalized.isEmpty() ? Component.empty() : LEGACY_AMPERSAND.deserialize(normalized);
    }

    public static Component legacySection(String legacyText) {
        return legacyText == null || legacyText.isEmpty()
                ? Component.empty()
                : LEGACY_SECTION.deserialize(legacyText);
    }

    public static String ampersandToSection(String legacyText) {
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(normalizeLegacyMarkers(legacyText)));
    }

    public static Component chatMessage(Component displayName, Component message) {
        return Component.empty()
                .append(Objects.requireNonNull(displayName, "displayName"))
                .append(Component.text(" » "))
                .append(Objects.requireNonNull(message, "message"));
    }

    private static String normalizeLegacyMarkers(String legacyText) {
        return Objects.requireNonNullElse(legacyText, "").replace('§', '&');
    }
}
