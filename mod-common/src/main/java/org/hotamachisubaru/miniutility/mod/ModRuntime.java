package org.hotamachisubaru.miniutility.mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.hotamachisubaru.miniutility.core.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ModRuntime {
    private static final Map<MinecraftServer, ModRuntime> INSTANCES = new ConcurrentHashMap<>();
    private final MinecraftServer server;
    private final ProfileStore profiles;
    private final InputSessions inputs = new InputSessions();
    private final Map<UUID, DeathPoint> deaths = new HashMap<>();
    private final Queue<Runnable> nextTick = new ConcurrentLinkedQueue<>();
    private final Set<UUID> pendingClicks = new HashSet<>();
    private volatile boolean active = true;

    private ModRuntime(MinecraftServer server) throws IOException {
        this.server = server;
        this.profiles = new ProfileStore(server.getWorldPath(LevelResource.ROOT).resolve("miniutility/profiles.properties"));
    }
    public static void start(MinecraftServer server) {
        try { INSTANCES.put(server, new ModRuntime(server)); }
        catch (IOException failure) { throw new IllegalStateException("Miniutilityの保存データを読み込めません。", failure); }
    }
    public static ModRuntime get(MinecraftServer server) { return INSTANCES.get(server); }
    public static void stop(MinecraftServer server) {
        ModRuntime runtime = INSTANCES.remove(server);
        if (runtime != null) {
            runtime.active = false;
            runtime.inputs.clear(); runtime.deaths.clear(); runtime.nextTick.clear(); runtime.pendingClicks.clear();
        }
    }
    public void tick() {
        int count = nextTick.size();
        for (int i = 0; i < count; i++) { Runnable task = nextTick.poll(); if (task != null && active) task.run(); }
    }
    public void quit(ServerPlayer player) { inputs.forget(player.getUUID()); pendingClicks.remove(player.getUUID()); }
    public boolean protectBlocks() { return profiles.protectedBlocks(); }
    public void recordDeath(ServerPlayer player) {
        deaths.put(player.getUUID(), new DeathPoint(Compat.level(player).dimension(), player.getX(), player.getY() + 1,
                player.getZ(), player.getYRot(), player.getXRot()));
    }
    public Component displayName(ServerPlayer player) {
        String realName = player.getName().getString();
        String nickname = profiles.nickname(player.getUUID(), realName);
        String prefix = profiles.prefix(player.getUUID()) ? OptionalPrefix.lookup(player.getUUID()) : "";
        return ModText.legacy(prefix.isEmpty() || nickname.startsWith(prefix) ? nickname : prefix + "&r " + nickname);
    }
    public boolean chat(ServerPlayer player, String text) {
        InputSessions.Session session = inputs.get(player.getUUID());
        if (session != null) {
            nextTick.add(() -> {
                if (!online(player) || !inputs.current(player.getUUID(), session)) return;
                String value = text.trim();
                if ("cancel".equalsIgnoreCase(value) || "キャンセル".equals(value)) {
                    inputs.complete(player.getUUID(), session); tell(player, "入力をキャンセルしました。"); return;
                }
                if (session.mode() == InputSessions.Mode.EXPERIENCE) {
                    try {
                        int delta = NicknameRules.experienceDelta(player.experienceLevel, Integer.parseInt(value));
                        player.giveExperienceLevels(delta);
                        tell(player, "経験値レベルを変更しました。現在: " + player.experienceLevel);
                        inputs.complete(player.getUUID(), session);
                    } catch (NumberFormatException invalid) { tell(player, "整数を入力してください。例: 10 / -5"); }
                } else if (!NicknameRules.valid(value, session.mode() == InputSessions.Mode.COLORED_NICKNAME)) {
                    tell(player, "1〜16文字、文字・数字・_・-で入力してください。cancelで中止できます。");
                } else {
                    safely(player, () -> {
                        profiles.setNickname(player.getUUID(), value);
                        inputs.complete(player.getUUID(), session);
                        tell(player, "ニックネームを設定しました。");
                        Compat.refreshName(player);
                    });
                }
            });
            return true;
        }
        // ChatType binds the player display name; leave normal signed chat delivery to Minecraft.
        return false;
    }
    private boolean online(ServerPlayer player) {
        return active && server.getPlayerList().getPlayer(player.getUUID()) == player;
    }
    public void schedule(ServerPlayer player, AbstractContainerMenu expected, Runnable action) {
        if (!pendingClicks.add(player.getUUID())) return;
        nextTick.add(() -> {
            pendingClicks.remove(player.getUUID());
            if (online(player) && player.containerMenu == expected) safely(player, action::run);
        });
    }
    public void open(ServerPlayer player, UtilityMenu.Page page) {
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new UtilityMenu(id, inventory, this, player, page, null), Component.literal(page.title)));
    }
    public void choose(ServerPlayer player, UtilityMenu.Page page, int slot) {
        if (page == UtilityMenu.Page.MAIN) {
            switch (slot) {
                case 0 -> teleport(player);
                case 2 -> beginInput(player, InputSessions.Mode.EXPERIENCE);
                case 4 -> {
                    player.setGameMode(player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                            ? GameType.CREATIVE : GameType.SURVIVAL);
                    player.closeContainer();
                    tell(player, "ゲームモードを変更しました。");
                }
                case 9 -> safely(player, () -> {
                    profiles.protectedBlocks(!profiles.protectedBlocks());
                    tell(player, "クリーパーのブロック破壊防止: " + (profiles.protectedBlocks() ? "有効" : "無効"));
                    player.closeContainer();
                });
                case 11 -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                        ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()), Component.literal("エンダーチェスト")));
                case 13 -> openTrash(player, new TrashSession());
                case 15 -> open(player, UtilityMenu.Page.NICKNAME);
                case 17 -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                        new CraftingMenu(id, inventory, ContainerLevelAccess.create(Compat.level(player), player.blockPosition())) {
                            @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return p == player; }
                        }, Component.literal("どこでも作業台")));
                default -> { }
            }
        } else if (page == UtilityMenu.Page.NICKNAME) {
            switch (slot) {
                case 2 -> beginInput(player, InputSessions.Mode.NICKNAME);
                case 4 -> beginInput(player, InputSessions.Mode.COLORED_NICKNAME);
                case 6 -> safely(player, () -> {
                    profiles.setNickname(player.getUUID(), null); Compat.refreshName(player);
                    player.closeContainer(); tell(player, "ニックネームをリセットしました。");
                });
                default -> { }
            }
        }
    }
    private void beginInput(ServerPlayer player, InputSessions.Mode mode) {
        inputs.begin(player.getUUID(), mode);
        player.closeContainer();
        tell(player, mode == InputSessions.Mode.EXPERIENCE ? "増減するレベルを入力してください。例: 10 / -5"
                : "ニックネームをチャットへ入力してください。色指定は&aなど。");
        tell(player, "cancel または キャンセル で中止できます。");
    }
    private void teleport(ServerPlayer player) {
        DeathPoint point = deaths.get(player.getUUID());
        if (point == null) { tell(player, "死亡地点がありません。"); return; }
        var level = server.getLevel(point.world());
        if (level == null) { tell(player, "死亡地点のディメンションが読み込まれていません。"); return; }
        player.closeContainer();
        Compat.teleport(player, level, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        tell(player, "死亡地点へワープしました。");
    }
    public boolean openTrash(ServerPlayer player, TrashSession session) {
        return player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new TrashMenu(id, inventory, this, player, session), Component.literal("ゴミ箱"))).isPresent();
    }
    public boolean confirmTrash(ServerPlayer player, TrashSession session) {
        return player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new UtilityMenu(id, inventory, this, player, UtilityMenu.Page.CONFIRM, session),
                Component.literal("本当に捨てますか？"))).isPresent();
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("menu").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ModRuntime runtime = get(Compat.server(player));
            if (runtime != null) runtime.open(player, UtilityMenu.Page.MAIN);
            return runtime == null ? 0 : 1;
        }));
        dispatcher.register(Commands.literal("load").requires(source -> Compat.admin(source)).executes(context -> {
            ModRuntime runtime = get(context.getSource().getServer());
            if (runtime == null) return 0;
            try {
                runtime.profiles.reload();
                for (ServerPlayer player : runtime.server.getPlayerList().getPlayers()) Compat.refreshName(player);
                context.getSource().sendSuccess(() -> Component.literal("ニックネームを再読み込みしました。"), false);
                return 1;
            } catch (IOException failure) {
                context.getSource().sendFailure(Component.literal("再読み込みに失敗しました。保存ファイルを確認してください。"));
                return 0;
            }
        }));
        dispatcher.register(Commands.literal("prefixtoggle")
                .executes(context -> prefix(context.getSource().getPlayerOrException(), null))
                .then(Commands.literal("on").executes(context -> prefix(context.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off").executes(context -> prefix(context.getSource().getPlayerOrException(), false))));
    }
    private static int prefix(ServerPlayer player, Boolean enabled) {
        ModRuntime runtime = get(Compat.server(player));
        if (runtime == null) return 0;
        runtime.safely(player, () -> {
            boolean next = enabled == null ? !runtime.profiles.prefix(player.getUUID()) : enabled;
            runtime.profiles.prefix(player.getUUID(), next);
            Compat.refreshName(player);
            tell(player, "プレフィックス表示: " + (next ? "有効" : "無効"));
        });
        return 1;
    }
    static void tell(ServerPlayer player, String text) { player.sendSystemMessage(Component.literal(text)); }
    private void safely(ServerPlayer player, Action action) {
        try { action.run(); }
        catch (Exception failure) {
            System.getLogger("Miniutility").log(System.Logger.Level.ERROR, "操作に失敗しました", failure);
            tell(player, "操作に失敗しました。サーバーログを確認してください。");
        }
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private record DeathPoint(ResourceKey<Level> world, double x, double y, double z, float yaw, float pitch) { }
}
