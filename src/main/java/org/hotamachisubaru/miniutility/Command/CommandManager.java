package org.hotamachisubaru.miniutility.Command;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.util.ComponentUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CommandManager implements CommandExecutor, TabCompleter {

    private final Miniutility plugin;

    public CommandManager(Miniutility plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        switch (name) {
            case "menu":
                if (sender instanceof Player player) {
                    player.openInventory(Objects.requireNonNull(GUI.createMenu(player.getUniqueId())));
                } else {
                    sender.sendMessage(ComponentUtil.text("プレイヤーのみ使用できます。", NamedTextColor.RED));
                }
                return true;

            case "load":
                try {
                    plugin.getNicknameManager().reload();
                    sender.sendMessage(ComponentUtil.text("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
                } catch (RuntimeException exception) {
                    sender.sendMessage(ComponentUtil.text("データベース再読み込みに失敗しました: " + exception.getMessage(), NamedTextColor.RED));
                }
                return true;

            case "prefixtoggle":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ComponentUtil.text("プレイヤーのみ実行可能です。", NamedTextColor.RED));
                    return true;
                }

                try {
                    boolean enabled = resolvePrefixState(player, args);
                    player.sendMessage(ComponentUtil.text("プレフィックスの表示が " + (enabled ? "有効" : "無効") + " になりました。", NamedTextColor.GREEN));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ComponentUtil.text(exception.getMessage(), NamedTextColor.RED));
                } catch (RuntimeException exception) {
                    sender.sendMessage(ComponentUtil.text("プレフィックスの切り替えに失敗しました: " + exception.getMessage(), NamedTextColor.RED));
                }
                return true;

            default:
                sender.sendMessage(ComponentUtil.text("不明なコマンドです。", NamedTextColor.RED));
                return false;
        }
    }

    private boolean resolvePrefixState(Player player, String[] args) {
        if (args.length == 0) {
            return plugin.getNicknameManager().togglePrefix(player.getUniqueId());
        }

        String option = args[0].toLowerCase(Locale.ROOT);
        if ("on".equals(option)) {
            plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), true);
            return true;
        }
        if ("off".equals(option)) {
            plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), false);
            return false;
        }
        throw new IllegalArgumentException("使用法: /prefixtoggle [on|off]（on=有効、off=無効）");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("prefixtoggle".equalsIgnoreCase(command.getName()) && args.length == 1) {
            List<String> options = List.of("on", "off");

            String head = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
            if (head.isEmpty()) {
                return options;
            }

            return options.stream()
                    .filter(option -> option.startsWith(head))
                    .toList();
        }
        return Collections.emptyList();
    }
}
