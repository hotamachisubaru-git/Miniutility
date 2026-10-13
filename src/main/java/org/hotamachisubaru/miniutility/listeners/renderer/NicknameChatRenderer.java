package org.hotamachisubaru.miniutility.listeners.renderer;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** Renders the immutable name/message components without consulting mutable server state. */
@NullMarked
public final class NicknameChatRenderer implements ChatRenderer, ChatRenderer.ViewerUnaware {

    private final @Nullable Component displayName;

    /** Uses Paper's source display-name snapshot if no plugin snapshot is available yet. */
    public NicknameChatRenderer() {
        this.displayName = null;
    }

    public NicknameChatRenderer(Component displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
        return renderMessage(sourceDisplayName, message);
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message) {
        return renderMessage(sourceDisplayName, message);
    }

    private Component renderMessage(Component sourceDisplayName, Component message) {
        Component renderedName = displayName;
        if (renderedName == null) {
            renderedName = Objects.requireNonNull(sourceDisplayName, "sourceDisplayName");
        }
        return LegacyTextFormatter.chatMessage(renderedName, message);
    }
}
