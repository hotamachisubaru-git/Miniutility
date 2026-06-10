package org.hotamachisubaru.miniutility.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public final class ComponentUtil {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private ComponentUtil() {
    }

    public static @NotNull Component empty() {
        return Component.empty();
    }

    public static @NotNull Component text(@Nullable String message) {
        return Component.text(message == null ? "" : message);
    }

    public static @NotNull Component text(@Nullable String message, @Nullable NamedTextColor color) {
        return Component.text(message == null ? "" : message, color);
    }

    public static @NotNull String plain(@Nullable Component component) {
        return component == null ? "" : PLAIN_TEXT.serialize(component);
    }

    public static @NotNull Component legacy(@Nullable String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_AMPERSAND.deserialize(normalizeLegacyMarkers(legacyText));
    }

    public static @NotNull Component legacySection(@Nullable String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SECTION.deserialize(legacyText);
    }

    public static @NotNull String serializeSection(@Nullable Component component) {
        return LEGACY_SECTION.serialize(component == null ? Component.empty() : component);
    }

    public static @NotNull String ampersandToSection(@Nullable String legacyText) {
        return serializeSection(LEGACY_AMPERSAND.deserialize(normalizeLegacyMarkers(legacyText)));
    }

    public static @NotNull Component append(@NotNull Component component, @NotNull Component child) {
        return component.append(child);
    }

    public static @NotNull Component chatMessage(@NotNull Component displayName, @NotNull Component message) {
        return empty()
                .append(displayName)
                .append(text(" » "))
                .append(message);
    }

    private static @NotNull String normalizeLegacyMarkers(@Nullable String legacyText) {
        return legacyText == null ? "" : legacyText.replace('§', '&');
    }
}
