package org.hotamachisubaru.miniutility.registry;

import io.papermc.paper.command.brigadier.BasicCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Paper のライフサイクルを使って、再登録にも対応するコマンドを登録します。
 */
public final class PaperCommandRegistrar {
    private static final Map<String, String> DESCRIPTIONS = Map.of(
            "menu", "便利機能を開きます。",
            "load", "データベースからニックネームをロードします。",
            "prefixtoggle", "プレフィックス結合の有効無効を切り替えます。"
    );

    private final JavaPlugin plugin;

    public PaperCommandRegistrar(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(Function<String, BasicCommand> commandFactory, String... commandNames) {
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
            plugin.registerCommand(command.name(), DESCRIPTIONS.get(command.name()), command.command());
        }
    }

    @NullMarked
    private record CommandBinding(String name, BasicCommand command) {
    }
}
