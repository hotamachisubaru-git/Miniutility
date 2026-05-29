package org.hotamachisubaru.miniutility.Command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.MiniutilityLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CommandManager implements CommandExecutor, TabCompleter {

    private final MiniutilityLoader plugin;

    public CommandManager(MiniutilityLoader plugin) {
        this.plugin = plugin;
    }

    private static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "menu":
                if (sender instanceof Player player) {
                    player.openInventory(Objects.requireNonNull(GUI.createMenu(player.getUniqueId())));
                } else {
                    sender.sendMessage(colored("プレイヤーのみ使用できます。", NamedTextColor.RED));
                }
                return true;

            case "load":
                try {
                    plugin.getNicknameManager().reload();
                    sender.sendMessage(colored("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
                } catch (Throwable throwable) {
                    sender.sendMessage(colored("データベース再読み込みに失敗しました: " + throwable.getMessage(), NamedTextColor.RED));
                }
                return true;

            case "prefixtoggle":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colored("プレイヤーのみ実行可能です。", NamedTextColor.RED));
                    return true;
                }

                try {
                    boolean enabled = resolvePrefixState(player, args);
                    player.sendMessage(colored("Prefixの表示が " + (enabled ? "有効" : "無効") + " になりました。", NamedTextColor.GREEN));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(colored(exception.getMessage(), NamedTextColor.RED));
                } catch (Throwable throwable) {
                    sender.sendMessage(colored("Prefixの切り替えに失敗しました: " + throwable.getMessage(), NamedTextColor.RED));
                }
                return true;

            default:
                sender.sendMessage(colored("不明なコマンドです。", NamedTextColor.RED));
                return false;
        }
    }

    private boolean resolvePrefixState(Player player, String[] args) {
        if (args.length == 0) {
            return plugin.getNicknameManager().togglePrefix(player.getUniqueId());
        }

        String option = args[0].toLowerCase();
        if ("on".equals(option)) {
            plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), true);
            return true;
        }
        if ("off".equals(option)) {
            plugin.getNicknameManager().setPrefixEnabled(player.getUniqueId(), false);
            return false;
        }
        throw new IllegalArgumentException("使用法: /prefixtoggle [on|off]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("prefixtoggle".equalsIgnoreCase(command.getName()) && args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("on");
            options.add("off");

            String head = args[0] == null ? "" : args[0].toLowerCase();
            if (head.isEmpty()) {
                return options;
            }

            List<String> filtered = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(head)) {
                    filtered.add(option);
                }
            }
            return filtered;
        }
        return Collections.emptyList();
    }
}
