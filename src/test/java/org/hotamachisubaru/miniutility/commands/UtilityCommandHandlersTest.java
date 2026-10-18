package org.hotamachisubaru.miniutility.commands;

import org.hotamachisubaru.miniutility.commands.UtilityCommandHandlers.NamedCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.ui.UtilityMenuFactory;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@NullMarked
class UtilityCommandHandlersTest {

    private final MiniutilityPlugin plugin = mock(MiniutilityPlugin.class);
    private final PlayerNicknameService manager = mock(PlayerNicknameService.class);
    private final UtilityCommandHandlers commands = new UtilityCommandHandlers(plugin);

    @BeforeEach
    void setUp() {
        when(plugin.getNicknameManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
    }

    @Test
    void loadRequiresExistingPermissionIncludingDirectAdapterCalls() {
        NamedCommand command = commands.commandAdapter("load");
        CommandSender sender = mock(CommandSender.class);
        assertEquals("miniutility.load", command.permission());
        assertFalse(command.canUse(sender));

        command.execute(source(sender), new String[0]);
        verify(manager, never()).reload();
        when(sender.hasPermission("miniutility.load")).thenReturn(true);
        assertTrue(command.canUse(sender));
        command.execute(source(sender), new String[0]);
        verify(manager).reload();
    }

    @Test
    void menuOpensTheSendersInventoryAndConsoleCannotOperateTheExecutor() {
        NamedCommand command = commands.commandAdapter("menu");
        Player player = mock(Player.class);
        UUID playerId = Objects.requireNonNull(UUID.randomUUID());
        when(player.getUniqueId()).thenReturn(playerId);
        Inventory inventory = mock(Inventory.class);
        try (var factory = mockStatic(UtilityMenuFactory.class)) {
            factory.when(() -> UtilityMenuFactory.createMenu(playerId)).thenReturn(inventory);
            command.execute(source(player), new String[0]);
            verify(player).openInventory(inventory);

            CommandSender console = mock(CommandSender.class);
            CommandSender source = source(console);
            command.execute(source, new String[0]);
            verify(player, times(1)).openInventory(inventory);
            assertEquals("プレイヤーのみ使用できます。", message(console));
        }
    }

    @Test
    void prefixToggleOnOffAndInvalidArgumentsKeepExistingBehavior() {
        NamedCommand command = commands.commandAdapter("prefixtoggle");
        Player player = mock(Player.class);
        UUID playerId = Objects.requireNonNull(UUID.randomUUID());
        when(player.getUniqueId()).thenReturn(playerId);
        when(manager.togglePrefix(playerId)).thenReturn(true);
        CommandSender source = source(player);

        command.execute(source, new String[0]);
        verify(manager).togglePrefix(playerId);
        command.execute(source, new String[]{"ON"});
        verify(manager).setPrefixEnabled(playerId, true);
        command.execute(source, new String[]{"off"});
        verify(manager).setPrefixEnabled(playerId, false);
        clearInvocations(manager, player);

        command.execute(source, new String[]{"invalid"});
        command.execute(source, new String[]{"on", "off"});
        verifyNoInteractions(manager);
        verify(player, times(2)).sendMessage(anyString());
    }

    @Test
    void prefixCompletionsFilterTheFirstArgumentAndNeverFallBackToPlayerNames() {
        NamedCommand command = commands.commandAdapter("PREFIXTOGGLE");
        CommandSender source = source(mock(Player.class));
        assertEquals(List.of("on", "off"), List.copyOf(command.suggest(source, new String[0])));
        assertEquals(List.of("on"), List.copyOf(command.suggest(source, new String[]{"ON"})));
        assertTrue(command.suggest(source, new String[]{"on", ""}).isEmpty());
        assertTrue(commands.commandAdapter("menu").suggest(source, new String[]{""}).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> commands.commandAdapter("unknown"));
    }

    @Test
    void reloadFailureReportsFailureAndNeverClaimsSuccess() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("miniutility.load")).thenReturn(true);
        doThrow(new IllegalStateException("Database unavailable")).<PlayerNicknameService>when(manager).reload();

        commands.commandAdapter("load").execute(source(sender), new String[0]);

        assertEquals("ニックネームデータの再読み込みに失敗しました。", message(sender));
    }

    private static CommandSender source(CommandSender sender) {
        return sender;
    }

    private static String message(CommandSender sender) {
        var captured = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captured.capture());
        return org.bukkit.ChatColor.stripColor(captured.getValue());
    }
}
