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
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

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
                    GUI.openMenu(player);
                } else {
                    sender.sendMessage(colored("プレイヤーのみ使用できます。", NamedTextColor.RED));
                }
                return true;

            case "load":
                try {
                    NicknameDatabase.reload();
                    sender.sendMessage(colored("ニックネームデータを再読み込みしました。", NamedTextColor.GREEN));
                } catch (Throwable e) {
                    sender.sendMessage(colored("データベース再読み込みに失敗しました: " + e.getMessage(), NamedTextColor.RED));
                }
                return true;

            case "prefixtoggle":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(colored("プレイヤーのみ実行可能です。", NamedTextColor.RED));
                    return true;
                }
                Player player = (Player) sender;
                boolean enabled;
                try {
                    enabled = plugin.getMiniutility().getNicknameManager().togglePrefix(player.getUniqueId());
                } catch (Throwable e) {
                    sender.sendMessage(colored("Prefixの切り替えに失敗しました: " + e.getMessage(), NamedTextColor.RED));
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
                List<String> options = new ArrayList<String>();
                options.add("on");
                options.add("off");
                if (args[0] != null && !args[0].isEmpty()) {
                    String head = args[0].toLowerCase();
                    List<String> filtered = new ArrayList<String>();
                    for (String o : options) if (o.startsWith(head)) filtered.add(o);
                    return filtered;
                }
                return options;
            }
        }
        return Collections.emptyList();
    }
}
