package org.hotamachisubaru.miniutility.commands;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.ui.GuiFactory;
import org.hotamachisubaru.miniutility.util.ComponentUtil;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class MiniutilityCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PREFIX_OPTIONS = List.of("on", "off");

    private final Miniutility plugin;

    public MiniutilityCommand(Miniutility plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "menu" -> openMenu(sender);
            case "load" -> reloadNicknames(sender);
            case "prefixtoggle" -> configurePrefix(sender, arguments);
            default -> false;
        };
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ComponentUtil.text("プレイヤーのみ使用できます。", NamedTextColor.RED));
            return true;
        }

        player.openInventory(GuiFactory.createMenu(player.getUniqueId()));
        return true;
    }

    private boolean reloadNicknames(CommandSender sender) {
        try {
            plugin.getNicknameManager().reload();
            sender.sendMessage(ComponentUtil.text("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "ニックネームデータの再読み込みに失敗しました。", exception);
            sender.sendMessage(ComponentUtil.text("ニックネームデータの再読み込みに失敗しました。", NamedTextColor.RED));
        }
        return true;
    }

    private boolean configurePrefix(CommandSender sender, String[] arguments) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ComponentUtil.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
            return true;
        }

        try {
            boolean enabled = resolvePrefixState(player, arguments);
            player.sendMessage(ComponentUtil.text(
                    "プレフィックスの表示が " + (enabled ? "有効" : "無効") + " になりました。",
                    NamedTextColor.GREEN
            ));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(ComponentUtil.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private boolean resolvePrefixState(Player player, String[] arguments) {
        if (arguments.length == 0) {
            return plugin.getNicknameManager().togglePrefix(player.getUniqueId());
        }
        if (arguments.length != 1) {
            throw usageError();
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
            default -> throw usageError();
        };
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] arguments
    ) {
        if (!command.getName().equalsIgnoreCase("prefixtoggle") || arguments.length != 1) {
            return List.of();
        }

        String prefix = arguments[0].toLowerCase(Locale.ROOT);
        return PREFIX_OPTIONS.stream()
                .filter(option -> option.startsWith(prefix))
                .toList();
    }

    private static IllegalArgumentException usageError() {
        return new IllegalArgumentException("使用法: /prefixtoggle [on|off]（on=有効、off=無効）");
    }
}
