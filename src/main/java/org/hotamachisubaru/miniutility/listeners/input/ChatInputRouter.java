package org.hotamachisubaru.miniutility.listeners.input;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Validates input on the server thread before completing its owning input session. */
@NullMarked
public final class ChatInputRouter {

    private final PlayerNicknameService nicknameManager;
    private final Consumer<Runnable> serverThreadExecutor;
    private final BiConsumer<Player, InputMessage> messageSender;

    public ChatInputRouter(
            PlayerNicknameService nicknameManager,
            Consumer<Runnable> serverThreadExecutor,
            BiConsumer<Player, InputMessage> messageSender
    ) {
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
        this.serverThreadExecutor = Objects.requireNonNull(serverThreadExecutor, "serverThreadExecutor");
        this.messageSender = Objects.requireNonNull(messageSender, "messageSender");
    }

    /** Retains the standalone API; the listener uses the session-aware overload. */
    public boolean handle(Player player, ChatInputMode state, String input) {
        return handle(player, state, input, action -> Objects.requireNonNull(action).run());
    }

    /** An invalid value remains pending. A valid value completes through the session owner. */
    public boolean handle(Player player, ChatInputMode state, String input, Consumer<Runnable> completion) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(completion, "completion");
        serverThreadExecutor.accept(() -> {
            if (!state.accepts(input)) {
                messageSender.accept(player, state.invalidMessage());
                return;
            }
            completion.accept(switch (state) {
                case EXPERIENCE -> () -> changeExperience(player, Integer.parseInt(input));
                case NICKNAME -> () -> setPlainNickname(player, input);
                case COLORED_NICKNAME -> () -> setColoredNickname(player, input);
            });
        });
        return true;
    }

    private void changeExperience(Player player, int requestedChange) {
        int currentLevel = player.getLevel();
        int appliedChange = Math.max(-currentLevel, Math.min(requestedChange, Integer.MAX_VALUE - currentLevel));
        player.giveExpLevels(appliedChange);
        String signedChange = appliedChange > 0 ? "+" + appliedChange : Integer.toString(appliedChange);
        LegacyTextFormatter.send(player, LegacyTextFormatter.text(
                "経験値レベルを " + signedChange + " しました。現在レベル: " + player.getLevel(),
                NamedTextColor.GREEN
        ));
    }

    private void setPlainNickname(Player player, String nickname) {
        nicknameManager.setNickname(player, nickname);
        LegacyTextFormatter.send(player, LegacyTextFormatter.text(
                "ニックネームを「" + nickname + "」に設定しました。", NamedTextColor.GREEN
        ));
    }

    private void setColoredNickname(Player player, String input) {
        String nickname = LegacyTextFormatter.ampersandToSection(input);
        nicknameManager.setNickname(player, nickname);
        LegacyTextFormatter.send(player, LegacyTextFormatter.text("ニックネームを設定しました: ", NamedTextColor.GREEN)
                .append(LegacyTextFormatter.legacySection(nickname)));
    }

    public record InputMessage(String text, NamedTextColor color) {
        public InputMessage {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(color, "color");
        }
    }
}
