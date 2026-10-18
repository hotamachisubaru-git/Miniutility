package org.hotamachisubaru.miniutility.registry;

import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * plugin.yml に宣言した Bukkit/Spigot 共通コマンドを登録します。
 */
public final class BukkitCommandRegistrar {
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "menu", "便利機能を開きます。",
            "load", "データベースからニックネームをロードします。",
            "prefixtoggle", "プレフィックス結合の有効無効を切り替えます。"
    );

    private final JavaPlugin plugin;

    public BukkitCommandRegistrar(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(Function<String, TabExecutor> commandFactory, String... commandNames) {
        Objects.requireNonNull(commandFactory, "commandFactory");
        Objects.requireNonNull(commandNames, "commandNames");
        // Validate the entire batch before mutating Paper's command registrations.
        var commands = Arrays.stream(commandNames).map(name -> {
            Objects.requireNonNull(name, "commandName");
            if (!DESCRIPTIONS.containsKey(name)) {
                throw new IllegalArgumentException("未定義のコマンド: " + name);
            }
            return new CommandBinding(name, Objects.requireNonNull(commandFactory.apply(name), "command"));
        }).toList();
        for (var command : commands) {
            var registered = Objects.requireNonNull(plugin.getCommand(command.name()), "Missing plugin.yml command: " + command.name());
            registered.setExecutor(command.command());
            registered.setTabCompleter(command.command());
        }
    }

    @NullMarked
    private record CommandBinding(String name, TabExecutor command) {
    }
}
