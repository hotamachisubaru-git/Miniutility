package org.hotamachisubaru.miniutility.Command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CommandManager implements CommandExecutor, TabCompleter {

    private final Miniutility miniutility;

    public CommandManager(Miniutility miniutility) {
        this.miniutility = miniutility;
    }

    private static Component colored(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        switch (name) {
            case "menu":
                if (sender instanceof Player player) {
                    GUI.openMenu(player);
                } else {
                    sender.sendMessage(colored("プレイヤーのみ使用できます。", NamedTextColor.RED));
                }
                return true;

            case "load":
                if (!sender.hasPermission("load")) {
                    sender.sendMessage(colored("このコマンドを実行する権限がありません。", NamedTextColor.RED));
                    return true;
                }
                try {
                    miniutility.getNicknameManager().reload(Bukkit.getOnlinePlayers());
                    sender.sendMessage(colored("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(colored("データベース再読み込みに失敗しました: " + e.getMessage(), NamedTextColor.RED));
                }
                return true;

            case "prefixtoggle":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colored("プレイヤーのみ実行可能です。", NamedTextColor.RED));
                    return true;
                }

                NicknameManager nicknameManager = miniutility.getNicknameManager();
                boolean enabled;
                if (args.length == 0 || "toggle".equalsIgnoreCase(args[0])) {
                    enabled = nicknameManager.togglePrefix(player.getUniqueId());
                } else if ("on".equalsIgnoreCase(args[0])) {
                    enabled = nicknameManager.setPrefixEnabled(player.getUniqueId(), true);
                } else if ("off".equalsIgnoreCase(args[0])) {
                    enabled = nicknameManager.setPrefixEnabled(player.getUniqueId(), false);
                } else {
                    player.sendMessage(colored("使い方: /prefixtoggle [on|off|toggle]", NamedTextColor.RED));
                    return true;
                }

                player.sendMessage(colored("Prefixの表示が " + (enabled ? "有効" : "無効") + " になりました。", NamedTextColor.GREEN));
                return true;

            default:
                sender.sendMessage(colored("不明なコマンドです。", NamedTextColor.RED));
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("prefixtoggle".equalsIgnoreCase(command.getName())) {
            if (args.length == 1) {
                List<String> options = new ArrayList<>();
                options.add("on");
                options.add("off");
                options.add("toggle");
                if (args[0] != null && !args[0].isEmpty()) {
                    String head = args[0].toLowerCase(Locale.ROOT);
                    List<String> filtered = new ArrayList<>();
                    for (String option : options) {
                        if (option.startsWith(head)) {
                            filtered.add(option);
                        }
                    }
                    return filtered;
                }
                return options;
            }
        }
        return Collections.emptyList();
    }
}
