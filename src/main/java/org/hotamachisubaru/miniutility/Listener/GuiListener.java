package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.MiniutilityLoader;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiListener implements Listener {

    private final MiniutilityLoader plugin;
    private final Map<UUID, ItemStack[]> trashSnapshots = new ConcurrentHashMap<>();

    public GuiListener(MiniutilityLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();
        if (!(holder instanceof GuiHolder guiHolder)) {
            return;
        }
        if (event.getClickedInventory() != topInventory) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        switch (guiHolder.getType()) {
            case MENU -> handleMenuClick(player, clickedItem, event);
            case NICKNAME -> handleNicknameClick(player, clickedItem, event);
            case TRASH -> handleTrashClick(player, topInventory, clickedItem, event);
            case TRASH_CONFIRM -> handleTrashConfirmClick(player, clickedItem, event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        trashSnapshots.remove(event.getPlayer().getUniqueId());
    }

    private void handleMenuClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        event.setCancelled(true);

        switch (clickedItem.getType()) {
            case ARMOR_STAND -> teleportToDeathLocation(player);
            case ENDER_CHEST -> player.openInventory(player.getEnderChest());
            case CRAFTING_TABLE -> openCraftingTable(player);
            case DROPPER -> player.openInventory(Objects.requireNonNull(GUI.createTrashBox(player.getUniqueId(), null)));
            case NAME_TAG -> player.openInventory(Objects.requireNonNull(GUI.createNicknameMenu(player.getUniqueId())));
            case CREEPER_HEAD -> toggleCreeperProtection(player);
            case EXPERIENCE_BOTTLE -> beginExperienceInput(player);
            case COMPASS -> toggleGameMode(player);
            default -> player.sendMessage(colored("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
        }
    }

    private void handleNicknameClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        event.setCancelled(true);

        Chat chatListener = plugin.getChatListener();
        NicknameManager nicknameManager = plugin.getNicknameManager();

        switch (clickedItem.getType()) {
            case PAPER -> {
                player.closeInventory();
                chatListener.awaitNicknameInput(player);
                player.sendMessage(colored("新しいニックネームをチャットに入力してください。", NamedTextColor.AQUA));
            }
            case NAME_TAG -> {
                player.closeInventory();
                chatListener.awaitColorNicknameInput(player);
                player.sendMessage(colored("色付きのニックネームをチャットに入力してください。例: &6ほたまち", NamedTextColor.AQUA));
            }
            case BARRIER -> {
                nicknameManager.removeNickname(player);
                player.closeInventory();
                player.sendMessage(colored("ニックネームをリセットしました。", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(colored("無効な選択です。", NamedTextColor.RED));
        }
    }

    private void handleTrashClick(Player player, Inventory topInventory, ItemStack clickedItem, InventoryClickEvent event) {
        if (event.getRawSlot() == 53 && clickedItem.getType() == Material.LIME_WOOL) {
            event.setCancelled(true);
            trashSnapshots.put(player.getUniqueId(), snapshotTrashContents(topInventory));
            player.openInventory(Objects.requireNonNull(GUI.createTrashConfirm(player.getUniqueId())));
            return;
        }

        event.setCancelled(false);
    }

    private void handleTrashConfirmClick(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        event.setCancelled(true);

        if (clickedItem.getType() == Material.LIME_WOOL) {
            trashSnapshots.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(colored("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
            return;
        }

        if (clickedItem.getType() == Material.RED_WOOL) {
            ItemStack[] snapshot = trashSnapshots.remove(player.getUniqueId());
            player.openInventory(Objects.requireNonNull(GUI.createTrashBox(player.getUniqueId(), snapshot)));
            player.sendMessage(colored("削除をキャンセルしました。", NamedTextColor.YELLOW));
        }
    }

    private void teleportToDeathLocation(Player player) {
        Location deathLocation = plugin.getDeathLocation(player.getUniqueId());
        if (deathLocation == null || deathLocation.getWorld() == null) {
            player.sendMessage(colored("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        FoliaUtil.runAtPlayer(plugin, player.getUniqueId(), () -> {
            player.teleportAsync(deathLocation);
            player.sendMessage(colored("死亡地点にワープしました。", NamedTextColor.GREEN));
        });
    }

    private void openCraftingTable(Player player) {
        InventoryView view = MenuType.CRAFTING.create(
                Objects.requireNonNull(player),
                Objects.requireNonNull(Component.text("作業台"))
        );
        player.openInventory(Objects.requireNonNull(view));
    }

    private void toggleCreeperProtection(Player player) {
        boolean enabled = plugin.getCreeperProtectionListener().toggle();
        player.closeInventory();
        player.sendMessage(colored(
                "クリーパーの爆破によるブロック破壊防止が " + (enabled ? "有効" : "無効") + " になりました。",
                NamedTextColor.GREEN
        ));
    }

    private void beginExperienceInput(Player player) {
        player.closeInventory();
        plugin.getChatListener().awaitExpInput(player);
        player.sendMessage(colored("経験値を増減する数値をチャットに入力してください。", NamedTextColor.AQUA));
        player.sendMessage(colored("例: \"10\" で +10 レベル, \"-5\" で -5 レベル", NamedTextColor.GRAY));
    }

    private void toggleGameMode(Player player) {
        GameMode nextMode = player.getGameMode() == GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL;
        player.setGameMode(nextMode);
        player.closeInventory();
        player.sendMessage(colored(
                "ゲームモードを " + (nextMode == GameMode.CREATIVE ? "クリエイティブ" : "サバイバル") + " に変更しました。",
                NamedTextColor.GREEN
        ));
    }

    private static ItemStack[] snapshotTrashContents(Inventory inventory) {
        ItemStack[] snapshot = new ItemStack[53];
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack item = inventory.getItem(slot);
            snapshot[slot] = item == null ? null : item.clone();
        }
        return snapshot;
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }
}
