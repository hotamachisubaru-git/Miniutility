package org.hotamachisubaru.miniutility.GUI;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.plugin.Plugin;
import org.hotamachisubaru.miniutility.Listener.Chat;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.creeper.CreeperProtectionService;
import org.hotamachisubaru.miniutility.death.DeathLocationStore;
import org.hotamachisubaru.miniutility.util.ComponentUtil;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Objects;

public final class GuiActionService {

    private static final int TRASH_CONFIRM_SLOT = 53;

    private final Plugin plugin;
    private final DeathLocationStore deathLocationStore;
    private final Chat chatListener;
    private final NicknameManager nicknameManager;
    private final CreeperProtectionService creeperProtectionService;
    private final TrashBoxSessionStore trashBoxSessionStore;

    public GuiActionService(
            Plugin plugin,
            DeathLocationStore deathLocationStore,
            Chat chatListener,
            NicknameManager nicknameManager,
            CreeperProtectionService creeperProtectionService,
            TrashBoxSessionStore trashBoxSessionStore
    ) {
        this.plugin = Objects.requireNonNull(plugin);
        this.deathLocationStore = Objects.requireNonNull(deathLocationStore);
        this.chatListener = Objects.requireNonNull(chatListener);
        this.nicknameManager = Objects.requireNonNull(nicknameManager);
        this.creeperProtectionService = Objects.requireNonNull(creeperProtectionService);
        this.trashBoxSessionStore = Objects.requireNonNull(trashBoxSessionStore);
    }

    public void handleMenuClick(Player player, ItemStack clickedItem) {
        switch (clickedItem.getType()) {
            case ARMOR_STAND -> teleportToDeathLocation(player);
            case ENDER_CHEST -> player.openInventory(player.getEnderChest());
            case CRAFTING_TABLE -> openCraftingTable(player);
            case DROPPER -> player.openInventory(Objects.requireNonNull(GUI.createTrashBox(player.getUniqueId(), null)));
            case NAME_TAG -> player.openInventory(Objects.requireNonNull(GUI.createNicknameMenu(player.getUniqueId())));
            case CREEPER_HEAD -> toggleCreeperProtection(player);
            case EXPERIENCE_BOTTLE -> beginExperienceInput(player);
            case COMPASS -> toggleGameMode(player);
            default -> player.sendMessage(ComponentUtil.text("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
        }
    }

    public void handleNicknameClick(Player player, ItemStack clickedItem) {
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

    public boolean handleTrashClick(Player player, Inventory topInventory, ItemStack clickedItem, int rawSlot) {
        if (rawSlot != TRASH_CONFIRM_SLOT || clickedItem.getType() != Material.LIME_WOOL) {
            return false;
        }

        trashBoxSessionStore.save(player.getUniqueId(), topInventory);
        player.openInventory(Objects.requireNonNull(GUI.createTrashConfirm(player.getUniqueId())));
        return true;
    }

    public void handleTrashConfirmClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.LIME_WOOL) {
            trashBoxSessionStore.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ComponentUtil.text("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
            return;
        }

        if (clickedItem.getType() == Material.RED_WOOL) {
            ItemStack[] snapshot = trashBoxSessionStore.remove(player.getUniqueId());
            player.openInventory(Objects.requireNonNull(GUI.createTrashBox(player.getUniqueId(), snapshot)));
            player.sendMessage(ComponentUtil.text("削除をキャンセルしました。", NamedTextColor.YELLOW));
        }
    }

    private void teleportToDeathLocation(Player player) {
        Location deathLocation = deathLocationStore.get(player.getUniqueId());
        if (deathLocation == null || deathLocation.getWorld() == null) {
            player.sendMessage(ComponentUtil.text("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), () -> {
            player.teleportAsync(deathLocation);
            player.sendMessage(ComponentUtil.text("死亡地点にワープしました。", NamedTextColor.GREEN));
        });
    }

    private void openCraftingTable(Player player) {
        InventoryView view = MenuType.CRAFTING.create(
                Objects.requireNonNull(player),
                ComponentUtil.text("作業台")
        );
        player.openInventory(Objects.requireNonNull(view));
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
