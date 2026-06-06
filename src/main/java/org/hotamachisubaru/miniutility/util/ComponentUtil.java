package org.hotamachisubaru.miniutility.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class ComponentUtil {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private ComponentUtil() {
    }

    public static @NonNull Component empty() {
        return Component.empty();
    }

    public static @NonNull Component text(@Nullable String message) {
        return Component.text(message == null ? "" : message);
    }

    public static @NonNull Component text(@Nullable String message, @Nullable NamedTextColor color) {
        return Component.text(message == null ? "" : message, color);
    }

    public static @NonNull String plain(@Nullable Component component) {
        return component == null ? "" : PLAIN_TEXT.serialize(component);
    }

    public static @NonNull Component legacy(@Nullable String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_AMPERSAND.deserialize(normalizeLegacyMarkers(legacyText));
    }

    public static @NonNull Component legacySection(@Nullable String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SECTION.deserialize(legacyText);
    }

    public static @NonNull String serializeSection(@Nullable Component component) {
        return LEGACY_SECTION.serialize(component == null ? Component.empty() : component);
    }

    public static @NonNull String ampersandToSection(@Nullable String legacyText) {
        return serializeSection(LEGACY_AMPERSAND.deserialize(normalizeLegacyMarkers(legacyText)));
    }

    public static @NonNull Component append(@NonNull Component component, @NonNull Component child) {
        return component.append(child);
    }

    public static @NonNull Component chatMessage(@Nullable Component displayName, @Nullable Component message) {
        return empty()
                .append(displayName == null ? empty() : displayName)
                .append(text(" » "))
                .append(message == null ? empty() : message);
    }

    private static @NonNull String normalizeLegacyMarkers(@Nullable String legacyText) {
        return legacyText == null ? "" : legacyText.replace('§', '&');
    }
}
