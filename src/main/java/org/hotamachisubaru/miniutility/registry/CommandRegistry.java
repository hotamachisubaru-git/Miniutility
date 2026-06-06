package org.hotamachisubaru.miniutility.registry;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandRegistry {

    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(CommandExecutor executor, String... commandNames) {
        TabCompleter tabCompleter = executor instanceof TabCompleter completer ? completer : null;
        for (String commandName : commandNames) {
            register(commandName, executor, tabCompleter);
        }
    }


    private void register(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        if (commandName == null) {
            throw new IllegalArgumentException("コマンド名は null にできません。");
        }
        PluginCommand command = plugin.getCommand(commandName);
        if (command == null) {
            throw new IllegalStateException("plugin.yml にコマンド " + commandName + " が定義されていません。");
        }

        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
