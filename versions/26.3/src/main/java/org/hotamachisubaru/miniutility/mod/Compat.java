package org.hotamachisubaru.miniutility.mod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class Compat {
    public static void drop(ServerPlayer player, ItemStack stack) { player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY); }
    private Compat() { }
    public static MinecraftServer server(ServerPlayer player) { return level(player).getServer(); }
    public static ServerLevel level(ServerPlayer player) { return (ServerLevel) player.level(); }
    public static boolean admin(CommandSourceStack source) { return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER); }
    public static void label(ItemStack stack, Component text) { stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, text); }
    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) { player.teleportTo(level, x, y, z, java.util.Set.of(), yaw, pitch, true); }
    public static void refreshName(ServerPlayer player) { player.refreshDisplayName(); player.refreshTabListName(); }
}
