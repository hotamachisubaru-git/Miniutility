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
import org.bukkit.plugin.IllegalPluginAccessException;
import org.hotamachisubaru.miniutility.creeper.CreeperBlockProtection;
import org.hotamachisubaru.miniutility.death.PlayerDeathLocations;
import org.hotamachisubaru.miniutility.listeners.PlayerChatListener;
import org.hotamachisubaru.miniutility.nicknames.PlayerNicknameService;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.item.MenuButtonSpec;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class UtilityMenuActions {

    private final Plugin plugin;
    private final PlayerDeathLocations deathLocationStore;
    private final PlayerChatListener chatListener;
    private final PlayerNicknameService nicknameManager;
    private final CreeperBlockProtection creeperProtectionService;
    private final TrashInventorySessions trashBoxSessionStore;
    private final Set<UUID> pendingClicks = new HashSet<>();

    public UtilityMenuActions(
            Plugin plugin,
            PlayerDeathLocations deathLocationStore,
            PlayerChatListener chatListener,
            PlayerNicknameService nicknameManager,
            CreeperBlockProtection creeperProtectionService,
            TrashInventorySessions trashBoxSessionStore
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

    /**
     * InventoryClickEvent のインベントリ更新が完了してから、同じ画面のボタンを実行します。
     */
    public void scheduleClick(Player player, Inventory sourceInventory, int rawSlot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sourceInventory, "sourceInventory");
        if (!(sourceInventory.getHolder(false) instanceof OwnedMenuInventoryHolder holder)
                || !holder.isOwner(player.getUniqueId())) {
            return;
        }
        MenuButtonSpec button = holder.getType().getButton(rawSlot);
        if (button == null || !button.matches(sourceInventory.getItem(rawSlot)) || !plugin.isEnabled()) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (!pendingClicks.add(playerId)) {
            return;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                pendingClicks.remove(playerId);
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != sourceInventory
                        || !button.matches(sourceInventory.getItem(rawSlot))) {
                    return;
                }
                ItemStack clickedItem = Objects.requireNonNull(sourceInventory.getItem(rawSlot)).clone();
                switch (holder.getType()) {
                    case MENU -> handleMenuClick(player, clickedItem);
                    case NICKNAME -> handleNicknameClick(player, clickedItem);
                    case TRASH -> handleTrashAction(player, sourceInventory, clickedItem);
                    case TRASH_CONFIRM -> handleTrashConfirmClick(player, clickedItem);
                }
            });
        } catch (IllegalPluginAccessException failure) {
            pendingClicks.remove(playerId);
            if (plugin.isEnabled()) {
                throw failure;
            }
        }
    }

    public void handleMenuClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        switch (clickedItem.getType()) {
            case ARMOR_STAND -> teleportToDeathLocation(player);
            case ENDER_CHEST -> player.openInventory(player.getEnderChest());
            case CRAFTING_TABLE -> openCraftingTable(player);
            case DROPPER -> player.openInventory(UtilityMenuFactory.createTrashBox(player.getUniqueId()));
            case NAME_TAG -> player.openInventory(UtilityMenuFactory.createNicknameMenu(player.getUniqueId()));
            case CREEPER_HEAD -> toggleCreeperProtection(player);
            case EXPERIENCE_BOTTLE -> beginExperienceInput(player);
            case COMPASS -> toggleGameMode(player);
            default -> player.sendMessage(LegacyTextFormatter.text("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
        }
    }

    public void handleNicknameClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        switch (clickedItem.getType()) {
            case PAPER -> {
                player.closeInventory();
                chatListener.awaitNicknameInput(player);
                player.sendMessage(LegacyTextFormatter.text("新しいニックネームをチャットに入力してください。", NamedTextColor.AQUA));
            }
            case NAME_TAG -> {
                player.closeInventory();
                chatListener.awaitColorNicknameInput(player);
                player.sendMessage(LegacyTextFormatter.text("色付きのニックネームをチャットに入力してください。例: &6ほたまち", NamedTextColor.AQUA));
            }
            case BARRIER -> {
                nicknameManager.removeNickname(player);
                player.closeInventory();
                player.sendMessage(LegacyTextFormatter.text("ニックネームをリセットしました。", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(LegacyTextFormatter.text("無効な選択です。", NamedTextColor.RED));
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
        player.openInventory(UtilityMenuFactory.createTrashConfirm(player.getUniqueId()));
    }

    public void handleTrashConfirmClick(Player player, ItemStack clickedItem) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(clickedItem, "clickedItem");
        if (clickedItem.getType() == Material.LIME_WOOL) {
            trashBoxSessionStore.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(LegacyTextFormatter.text("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
            return;
        }

        if (clickedItem.getType() == Material.RED_WOOL) {
            ItemStack[] snapshot = trashBoxSessionStore.remove(player.getUniqueId());
            player.openInventory(UtilityMenuFactory.createTrashBox(player.getUniqueId(), snapshot));
            player.sendMessage(LegacyTextFormatter.text("削除をキャンセルしました。", NamedTextColor.YELLOW));
        }
    }

    private void teleportToDeathLocation(Player player) {
        Optional<Location> storedLocation = deathLocationStore.find(player.getUniqueId());
        if (storedLocation.isEmpty()) {
            player.sendMessage(LegacyTextFormatter.text("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        Location deathLocation = storedLocation.orElseThrow();
        if (!deathLocation.isWorldLoaded()) {
            player.sendMessage(LegacyTextFormatter.text("死亡地点のワールドが見つかりません。", NamedTextColor.RED));
            return;
        }
        try {
            player.teleportAsync(deathLocation).whenComplete((succeeded, failure) ->
                    notifyTeleportResult(player, Boolean.TRUE.equals(succeeded), failure)
            );
        } catch (IllegalArgumentException | IllegalStateException failure) {
            notifyTeleportResult(player, false, failure);
        }
    }

    private void notifyTeleportResult(Player originalPlayer, boolean succeeded, @Nullable Throwable failure) {
        if (!plugin.isEnabled()) {
            return;
        }

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (failure != null) {
                    plugin.getLogger().log(Level.WARNING, "死亡地点へのテレポートに失敗しました。", failure);
                }
                if (!originalPlayer.isOnline()) {
                    return;
                }
                originalPlayer.sendMessage(LegacyTextFormatter.text(
                        succeeded ? "死亡地点にワープしました。" : "死亡地点へのワープに失敗しました。",
                        succeeded ? NamedTextColor.GREEN : NamedTextColor.RED
                ));
            });
        } catch (IllegalPluginAccessException disabledDuringCompletion) {
            if (plugin.isEnabled()) {
                throw disabledDuringCompletion;
            }
        }
    }

    private void openCraftingTable(Player player) {
        InventoryView view = MenuType.CRAFTING.builder()
                .title(LegacyTextFormatter.text("作業台"))
                .checkReachable(false)
                .build(player);
        player.openInventory(view);
    }

    private void toggleCreeperProtection(Player player) {
        boolean enabled = creeperProtectionService.toggle();
        player.closeInventory();
        player.sendMessage(LegacyTextFormatter.text(
                "クリーパーの爆破によるブロック破壊防止が " + (enabled ? "有効" : "無効") + " になりました。",
                NamedTextColor.GREEN
        ));
    }

    private void beginExperienceInput(Player player) {
        player.closeInventory();
        chatListener.awaitExpInput(player);
        player.sendMessage(LegacyTextFormatter.text("経験値を増減する数値をチャットに入力してください。", NamedTextColor.AQUA));
        player.sendMessage(LegacyTextFormatter.text("例: \"10\" で +10 レベル, \"-5\" で -5 レベル", NamedTextColor.GRAY));
    }

    private void toggleGameMode(Player player) {
        GameMode nextMode = player.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL;
        player.setGameMode(nextMode);
        player.closeInventory();
        player.sendMessage(LegacyTextFormatter.text(
                "ゲームモードを " + (nextMode == GameMode.CREATIVE ? "クリエイティブ" : "サバイバル") + " に変更しました。",
                NamedTextColor.GREEN
        ));
    }
}
