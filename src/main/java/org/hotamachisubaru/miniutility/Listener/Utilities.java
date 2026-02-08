package org.hotamachisubaru.miniutility.Listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;
import org.hotamachisubaru.miniutility.MiniutilityLoader;
import org.hotamachisubaru.miniutility.Nickname.NicknameDatabase;
import org.hotamachisubaru.miniutility.Nickname.NicknameManager;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Utilities implements Listener {

    private static final Set<UUID> recentlyTeleported = new HashSet<>();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private final MiniutilityLoader plugin;

    public Utilities(MiniutilityLoader plugin) {
        this.plugin = plugin;
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    @EventHandler(ignoreCancelled = true)
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getClickedInventory() == null) return;
        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) return;

        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof GuiHolder guiHolder)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (guiHolder.getType()) {
            case MENU:
                event.setCancelled(true);
                handleUtilityBox(player, clicked, event);
                break;
            case TRASH:
                handleTrashBox(player, clicked, event);
                break;
            case TRASH_CONFIRM:
                handleTrashConfirm(player, clicked, event);
                break;
            case NICKNAME:
                handleNicknameMenu(player, clicked, event);
                break;
            default:
                break;
        }
    }

    private void handleUtilityBox(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        switch (clickedItem.getType()) {
            case ARMOR_STAND:
                event.setCancelled(true);
                teleportToDeathLocation(player);
                break;
            case ENDER_CHEST:
                event.setCancelled(true);
                player.openInventory(player.getEnderChest());
                break;
            case CRAFTING_TABLE:
                event.setCancelled(true);
                var craftingView = MenuType.CRAFTING.create(player, Component.text("作業台"));
                if (craftingView != null) {
                    player.openInventory(craftingView);
                }
                break;
            case DROPPER:
                event.setCancelled(true);
                openTrashBox(player);
                break;
            case NAME_TAG:
                event.setCancelled(true);
                GUI.NicknameMenu(player);
                break;
            case CREEPER_HEAD: {
                event.setCancelled(true);
                CreeperProtectionListener cp = plugin.getMiniutility().getCreeperProtectionListener();
                boolean nowEnabled = cp.toggle();
                player.sendMessage(colored("クリーパーの爆破によるブロック破壊防止が " + (nowEnabled ? "有効" : "無効") + " になりました。", NamedTextColor.GREEN));
                player.closeInventory();
                break;
            }
            case EXPERIENCE_BOTTLE:
                event.setCancelled(true);
                player.closeInventory();
                Chat.setWaitingForExpInput(player, true);
                player.sendMessage(colored("経験値を増減する数値をチャットに入力してください。", NamedTextColor.AQUA));
                player.sendMessage(colored("例: \"10\" で +10レベル, \"-5\" で -5レベル", NamedTextColor.GRAY));
                break;
            case COMPASS:
                event.setCancelled(true);
                GameMode current = player.getGameMode();
                if (current == GameMode.SURVIVAL) {
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(colored("ゲームモードをクリエイティブに変更しました。", NamedTextColor.GREEN));
                } else {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.sendMessage(colored("ゲームモードをサバイバルに変更しました。", NamedTextColor.GREEN));
                }
                player.closeInventory();
                break;
            default:
                event.setCancelled(true);
                player.sendMessage(colored("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
                break;
        }
    }

    private void handleTrashBox(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();
        if (rawSlot == 53 && clickedItem.getType() == Material.LIME_WOOL) {
            event.setCancelled(true);
            openTrashConfirm(player);
            return;
        }
        event.setCancelled(false);
    }

    private static void openTrashConfirm(Player player) {
        GuiHolder h = new GuiHolder(GuiType.TRASH_CONFIRM, player.getUniqueId());
        Inventory confirm = Bukkit.createInventory(h, 9, Component.text("本当に捨てますか？"));
        h.bind(confirm);

        confirm.setItem(3, createMenuItem(Material.LIME_WOOL, "§aはい"));
        confirm.setItem(5, createMenuItem(Material.RED_WOOL, "§cいいえ"));
        player.openInventory(confirm);
    }

    private void handleTrashConfirm(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        event.setCancelled(true);

        if (clickedItem.getType() == Material.LIME_WOOL) {
            Inventory last = player.getOpenInventory().getTopInventory();
            for (int i = 0; i < 53; i++) last.setItem(i, null);
            player.closeInventory();
            player.sendMessage(colored("ゴミ箱のアイテムをすべて削除しました。", NamedTextColor.GREEN));
            return;
        }

        if (clickedItem.getType() == Material.RED_WOOL) {
            player.closeInventory();
            player.sendMessage(colored("削除をキャンセルしました。", NamedTextColor.YELLOW));
        }
    }

    private void handleNicknameMenu(Player player, ItemStack clickedItem, InventoryClickEvent event) {
        event.setCancelled(true);

        switch (clickedItem.getType()) {
            case PAPER:
                player.sendMessage(colored("新しいニックネームをチャットに入力してください。", NamedTextColor.AQUA));
                Chat.setWaitingForNickname(player, true);
                player.closeInventory();
                break;
            case NAME_TAG:
                player.sendMessage(colored("色付きのニックネームをチャットで入力してください。例: &6ほたまち", NamedTextColor.AQUA));
                Chat.setWaitingForColorInput(player, true);
                player.closeInventory();
                break;
            case BARRIER:
                NicknameDatabase.deleteNickname(player);
                NicknameManager.updateDisplayName(player);
                player.sendMessage(colored("ニックネームをリセットしました。", NamedTextColor.GREEN));
                player.closeInventory();
                break;
            default:
                player.sendMessage(colored("無効な選択です。", NamedTextColor.RED));
                break;
        }
    }

    private void teleportToDeathLocation(Player player) {
        if (plugin.getMiniutility() == null) {
            player.sendMessage(colored("プラグイン初期化中です。", NamedTextColor.RED));
            return;
        }

        Location deathLocation = plugin.getMiniutility().getDeathLocation(player.getUniqueId());
        if (deathLocation == null) {
            player.sendMessage(colored("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        try {
            player.getClass().getMethod("teleportAsync", Location.class).invoke(player, deathLocation);
        } catch (Throwable t) {
            player.teleport(deathLocation);
        }

        if (recentlyTeleported.add(player.getUniqueId())) {
            player.sendMessage(colored("死亡地点にワープしました。", NamedTextColor.GREEN));
            FoliaUtil.runLater(plugin, () -> recentlyTeleported.remove(player.getUniqueId()), 20L);
        }
    }

    private static ItemStack createMenuItem(Material material, String legacyName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY_SECTION.deserialize(legacyName));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openTrashBox(Player player) {
        GuiHolder h = new GuiHolder(GuiType.TRASH, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("ゴミ箱"));
        h.bind(inv);

        inv.setItem(53, createMenuItem(Material.LIME_WOOL, "§c捨てる"));
        player.openInventory(inv);
    }
}
