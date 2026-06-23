package org.hotamachisubaru.miniutility.registry;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CommandRegistry {

    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(CommandExecutor executor, String... commandNames) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(commandNames, "commandNames");
        TabCompleter tabCompleter = executor instanceof TabCompleter completer ? completer : null;
        for (String commandName : commandNames) {
            register(commandName, executor, tabCompleter);
        }
    }


    private void register(String commandName, CommandExecutor executor, TabCompleter tabCompleter) {
        Objects.requireNonNull(commandName, "commandName");
        PluginCommand command = plugin.getCommand(commandName);
        if (command == null) {
            throw new IllegalStateException("plugin.yml にコマンド " + commandName + " が定義されていません。");
        }

        command.setExecutor(executor);
        command.setTabCompleter(tabCompleter);
    }
}
