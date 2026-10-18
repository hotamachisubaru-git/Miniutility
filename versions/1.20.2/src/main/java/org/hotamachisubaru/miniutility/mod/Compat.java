package org.hotamachisubaru.miniutility.mod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class Compat {
    public static void drop(ServerPlayer player, ItemStack stack) { player.drop(stack, false, true); }
    private Compat() { }
    public static MinecraftServer server(ServerPlayer player) { return level(player).getServer(); }
    public static ServerLevel level(ServerPlayer player) { return (ServerLevel) player.level(); }
    public static boolean admin(CommandSourceStack source) { return source.hasPermission(2); }
    public static void label(ItemStack stack, Component text) { stack.setHoverName(text); }
    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) { player.teleportTo(level, x, y, z, yaw, pitch); }
    public static void refreshName(ServerPlayer player) { server(player).getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                java.util.EnumSet.of(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), java.util.List.of(player))); }
}
