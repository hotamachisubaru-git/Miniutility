package org.hotamachisubaru.miniutility.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.ui.UtilityMenuFactory;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

/** Paper の BasicCommand を使用した、既存の便利機能コマンド。 */
public final class UtilityCommandHandlers {

    private static final List<String> PREFIX_OPTIONS = List.of("on", "off");
    private static final String LOAD_PERMISSION = "miniutility.load";
    private static final String PREFIX_USAGE = "使用法: /prefixtoggle [on|off]（on=有効、off=無効）";

    private final MiniutilityPlugin plugin;

    public UtilityCommandHandlers(MiniutilityPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** 各登録名に対応するアダプターを作り、名前・権限・補完を同じ定義で処理する。 */
    public BasicCommand commandAdapter(String commandName) {
        String name = Objects.requireNonNull(commandName, "commandName").toLowerCase(Locale.ROOT);
        if (!List.of("menu", "load", "prefixtoggle").contains(name)) {
            throw new IllegalArgumentException("Unknown Miniutility command: " + commandName);
        }
        return new NamedCommand(name);
    }

    private void openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyTextFormatter.text("プレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }

        player.openInventory(UtilityMenuFactory.createMenu(player.getUniqueId()));
    }

    private void reloadNicknames(CommandSender sender) {
        try {
            plugin.getNicknameManager().reload();
            sender.sendMessage(LegacyTextFormatter.text("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "ニックネームデータの再読み込みに失敗しました。", exception);
            sender.sendMessage(LegacyTextFormatter.text("ニックネームデータの再読み込みに失敗しました。", NamedTextColor.RED));
        }
    }

    private void configurePrefix(CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyTextFormatter.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return;
        }

        if (arguments.length > 1 || (arguments.length == 1
                && PREFIX_OPTIONS.stream().noneMatch(option -> option.equalsIgnoreCase(arguments[0])))) {
            sender.sendMessage(LegacyTextFormatter.text(PREFIX_USAGE, NamedTextColor.RED));
            return;
        }

        try {
            boolean enabled = resolvePrefixState(player, arguments);
            player.sendMessage(LegacyTextFormatter.text(
                    "プレフィックスの表示が " + (enabled ? "有効" : "無効") + " になりました。",
                    NamedTextColor.GREEN
            ));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "プレフィックスの表示設定の変更に失敗しました。", exception);
            sender.sendMessage(LegacyTextFormatter.text("プレフィックスの表示設定の変更に失敗しました。", NamedTextColor.RED));
        }
    }

    private boolean resolvePrefixState(Player player, String[] arguments) {
        if (arguments.length == 0) {
            return plugin.getNicknameManager().togglePrefix(player.getUniqueId());
        }
        if (arguments.length != 1) {
            throw new IllegalArgumentException(PREFIX_USAGE);
        }

        return switch (arguments[0].toLowerCase(Locale.ROOT)) {
            case "on" -> {
                plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), true);
                yield true;
            }
            case "off" -> {
                plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), false);
                yield false;
            }
            default -> throw new IllegalArgumentException(PREFIX_USAGE);
        };
    }

    private final class NamedCommand implements BasicCommand {

        private final String name;

        private NamedCommand(String name) {
            this.name = name;
        }

        @Override
        public void execute(CommandSourceStack source, String[] arguments) {
            CommandSender sender = source.getSender();
            // /execute の実行対象ではなく、従来どおり実際のコマンド送信者を操作する。
            if (!canUse(sender)) {
                sender.sendMessage(LegacyTextFormatter.text("このコマンドを使用する権限がありません。", NamedTextColor.RED));
                return;
            }
            switch (name) {
                case "menu" -> openMenu(sender);
                case "load" -> reloadNicknames(sender);
                case "prefixtoggle" -> configurePrefix(sender, arguments);
                default -> throw new IllegalStateException("Unregistered command: " + name);
            }
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] arguments) {
            if (!canUse(source.getSender()) || !name.equals("prefixtoggle") || arguments.length > 1) {
                return List.of();
            }
            String prefix = arguments.length == 0 ? "" : arguments[0].toLowerCase(Locale.ROOT);
            return PREFIX_OPTIONS.stream().filter(option -> option.startsWith(prefix)).toList();
        }

        @Override
        public @Nullable String permission() {
            return name.equals("load") ? LOAD_PERMISSION : null;
        }
    }
}
