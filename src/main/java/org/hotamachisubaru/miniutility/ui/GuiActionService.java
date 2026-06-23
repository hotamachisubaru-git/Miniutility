package org.hotamachisubaru.miniutility.ui;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.plugin.Plugin;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.listeners.ChatListener;
import org.hotamachisubaru.miniutility.nicknames.NicknameManager;
import org.hotamachisubaru.miniutility.util.ComponentUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class GuiActionService {

    private final Plugin plugin;
    private final DeathLocationStore deathLocationStore;
    private final ChatListener chatListener;
    private final NicknameManager nicknameManager;
    private final CreeperProtectionService creeperProtectionService;
    private final TrashBoxSessionStore trashBoxSessionStore;

    public GuiActionService(
            Plugin plugin,
            DeathLocationStore deathLocationStore,
            ChatListener chatListener,
            NicknameManager nicknameManager,
            CreeperProtectionService creeperProtectionService,
            TrashBoxSessionStore trashBoxSessionStore
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore, "deathLocationStore");
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.nicknameManager = Objects.requireNonNull(nicknameManager, "nicknameManager");
        this.creeperProtectionService = Objects.requireNonNull(
                creeperProtectionService,
                "creeperProtectionService"
        );
        this.trashBoxSessionStore = Objects.requireNonNull(trashBoxSessionStore, "trashBoxSessionStore");
    }

    public void handleMenuClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        switch (clickedItem.getType()) {
            case ARMOR_STAND -> teleportToDeathLocation(player);
            case ENDER_CHEST -> player.openInventory(player.getEnderChest());
            case CRAFTING_TABLE -> openCraftingTable(player);
            case DROPPER -> player.openInventory(GuiFactory.createTrashBox(player.getUniqueId()));
            case NAME_TAG -> player.openInventory(GuiFactory.createNicknameMenu(player.getUniqueId()));
            case CREEPER_HEAD -> toggleCreeperProtection(player);
            case EXPERIENCE_BOTTLE -> beginExperienceInput(player);
            case COMPASS -> toggleGameMode(player);
            default -> player.sendMessage(ComponentUtil.text("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
        }
    }

    public void handleNicknameClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        switch (clickedItem.getType()) {
            case PAPER -> {
                player.closeInventory();
                chatListener.awaitNicknameInput(player);
                player.sendMessage(ComponentUtil.text("新しいニックネームをチャットに入力してください。", NamedTextColor.AQUA));
            }
            case NAME_TAG -> {
                player.closeInventory();
                chatListener.awaitColorNicknameInput(player);
                player.sendMessage(ComponentUtil.text("色付きのニックネームをチャットに入力してください。例: &6ほたまち", NamedTextColor.AQUA));
            }
            case BARRIER -> {
                nicknameManager.removeNickname(player);
                player.closeInventory();
                player.sendMessage(ComponentUtil.text("ニックネームをリセットしました。", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(ComponentUtil.text("無効な選択です。", NamedTextColor.RED));
        }
    }

    public void handleTrashAction(Player player, Inventory topInventory, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(topInventory, "topInventory");
        Objects.requireNonNull(clickedItem, "clickedItem");
        if (clickedItem.getType() != Material.LIME_WOOL) {
            return;
        }

        trashBoxSessionStore.save(player.getUniqueId(), topInventory);
        player.openInventory(GuiFactory.createTrashConfirm(player.getUniqueId()));
    }

    public void handleTrashConfirmClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        if (clickedItem.getType() == Material.LIME_WOOL) {
            trashBoxSessionStore.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ComponentUtil.text("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
            return;
        }

        if (clickedItem.getType() == Material.RED_WOOL) {
            ItemStack[] snapshot = trashBoxSessionStore.remove(player.getUniqueId());
            player.openInventory(GuiFactory.createTrashBox(player.getUniqueId(), snapshot));
            player.sendMessage(ComponentUtil.text("削除をキャンセルしました。", NamedTextColor.YELLOW));
        }
    }

    private void teleportToDeathLocation(Player player) {
        Optional<Location> storedLocation = deathLocationStore.find(player.getUniqueId());
        if (storedLocation.isEmpty()) {
            player.sendMessage(ComponentUtil.text("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        Location deathLocation = storedLocation.orElseThrow();
        if (deathLocation.getWorld() == null) {
            player.sendMessage(ComponentUtil.text("死亡地点のワールドが見つかりません。", NamedTextColor.RED));
            return;
        }
        UUID playerId = player.getUniqueId();
        player.teleportAsync(deathLocation).whenComplete((succeeded, failure) ->
                notifyTeleportResult(playerId, Boolean.TRUE.equals(succeeded), failure)
        );
    }

    private void notifyTeleportResult(UUID playerId, boolean succeeded, Throwable failure) {
        if (!plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return;
            }
            if (failure != null) {
                plugin.getLogger().log(Level.WARNING, "死亡地点へのテレポートに失敗しました。", failure);
            }
            player.sendMessage(ComponentUtil.text(
                    succeeded ? "死亡地点にワープしました。" : "死亡地点へのワープに失敗しました。",
                    succeeded ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
        });
    }

    private void openCraftingTable(Player player) {
        InventoryView view = MenuType.CRAFTING.create(
                player,
                ComponentUtil.text("作業台")
        );
        player.openInventory(Objects.requireNonNull(view, "crafting inventory view"));
    }

    private void toggleCreeperProtection(Player player) {
        boolean enabled = creeperProtectionService.toggle();
        player.closeInventory();
        player.sendMessage(ComponentUtil.text(
                "クリーパーの爆破によるブロック破壊防止が " + (enabled ? "有効" : "無効") + " になりました。",
                NamedTextColor.GREEN
        ));
    }

    private void beginExperienceInput(Player player) {
        player.closeInventory();
        chatListener.awaitExpInput(player);
        player.sendMessage(ComponentUtil.text("経験値を増減する数値をチャットに入力してください。", NamedTextColor.AQUA));
        player.sendMessage(ComponentUtil.text("例: \"10\" で +10 レベル, \"-5\" で -5 レベル", NamedTextColor.GRAY));
    }

    private void toggleGameMode(Player player) {
        GameMode nextMode = player.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL;
        player.setGameMode(nextMode);
        player.closeInventory();
        player.sendMessage(ComponentUtil.text(
                "ゲームモードを " + (nextMode == GameMode.CREATIVE ? "クリエイティブ" : "サバイバル") + " に変更しました。",
                NamedTextColor.GREEN
        ));
    }
}
