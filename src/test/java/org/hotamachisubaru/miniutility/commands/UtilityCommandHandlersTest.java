package org.hotamachisubaru.miniutility.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.hotamachisubaru.miniutility.MiniutilityPlugin;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.ui.UtilityMenuFactory;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UtilityCommandHandlersTest {

    private MiniutilityPlugin plugin;
    private PlayerNicknameService manager;
    private UtilityCommandHandlers commands;

    @BeforeEach
    void setUp() {
        plugin = mock(MiniutilityPlugin.class);
        manager = mock(PlayerNicknameService.class);
        when(plugin.getNicknameManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        commands = new UtilityCommandHandlers(plugin);
    }

    @Test
    void loadRequiresExistingPermissionIncludingDirectAdapterCalls() throws CommandSyntaxException {
        BasicCommand command = commands.commandAdapter("load");
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
    void menuOpensTheSendersInventoryAndConsoleCannotOperateTheExecutor() throws CommandSyntaxException {
        BasicCommand command = commands.commandAdapter("menu");
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        Inventory inventory = mock(Inventory.class);
        try (var factory = mockStatic(UtilityMenuFactory.class)) {
            factory.when(() -> UtilityMenuFactory.createMenu(playerId)).thenReturn(inventory);
            command.execute(source(player), new String[0]);
            verify(player).openInventory(inventory);

            CommandSender console = mock(CommandSender.class);
            CommandSourceStack source = source(console);
            when(source.getExecutor()).thenReturn(player);
            command.execute(source, new String[0]);
            verify(player, times(1)).openInventory(inventory);
            assertEquals("プレイヤーのみ使用できます。", message(console));
        }
    }

    @Test
    void prefixToggleOnOffAndInvalidArgumentsKeepExistingBehavior() throws CommandSyntaxException {
        BasicCommand command = commands.commandAdapter("prefixtoggle");
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(manager.togglePrefix(playerId)).thenReturn(true);
        CommandSourceStack source = source(player);

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
        verify(player, times(2)).sendMessage(any(Component.class));
    }

    @Test
    void prefixCompletionsFilterTheFirstArgumentAndNeverFallBackToPlayerNames() throws CommandSyntaxException {
        BasicCommand command = commands.commandAdapter("PREFIXTOGGLE");
        CommandSourceStack source = source(mock(Player.class));
        assertEquals(List.of("on", "off"), List.copyOf(command.suggest(source, new String[0])));
        assertEquals(List.of("on"), List.copyOf(command.suggest(source, new String[]{"ON"})));
        assertTrue(command.suggest(source, new String[]{"on", ""}).isEmpty());
        assertTrue(commands.commandAdapter("menu").suggest(source, new String[]{""}).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> commands.commandAdapter("unknown"));
    }

    @Test
    void reloadFailureReportsFailureAndNeverClaimsSuccess() throws CommandSyntaxException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("miniutility.load")).thenReturn(true);
        doThrow(new IllegalStateException("Database unavailable")).when(manager).reload();

        commands.commandAdapter("load").execute(source(sender), new String[0]);

        assertEquals("ニックネームデータの再読み込みに失敗しました。", message(sender));
    }

    private static CommandSourceStack source(CommandSender sender) {
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);
        return source;
    }

    private static String message(CommandSender sender) {
        ArgumentCaptor<Component> captured = ArgumentCaptor.forClass(Component.class);
        verify(sender).sendMessage(captured.capture());
        return LegacyTextFormatter.plain(captured.getValue());
    }
}
