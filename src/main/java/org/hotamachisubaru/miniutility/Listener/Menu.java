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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.GUI.GUI;
import org.hotamachisubaru.miniutility.GUI.holder.GuiHolder;
import org.hotamachisubaru.miniutility.GUI.holder.GuiType;
import org.hotamachisubaru.miniutility.Miniutility;
import org.hotamachisubaru.miniutility.MiniutilityLoader;
import org.hotamachisubaru.miniutility.util.FoliaUtil;

import java.util.Objects;

public final class Menu implements Listener {

    private final MiniutilityLoader plugin;
    private final Miniutility miniutility;
    private final Chat chatListener;
    private final TrashListener trashListener;

    public Menu(MiniutilityLoader plugin, Miniutility miniutility, Chat chatListener, TrashListener trashListener) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.miniutility = Objects.requireNonNull(miniutility, "miniutility");
        this.chatListener = Objects.requireNonNull(chatListener, "chatListener");
        this.trashListener = Objects.requireNonNull(trashListener, "trashListener");
    }

    private static Component colored(String message, NamedTextColor color) {
        return Component.text(message, color);
    }

    @EventHandler(ignoreCancelled = true)
    public void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) {
            return;
        }

        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof GuiHolder guiHolder)) {
            return;
        }
        if (guiHolder.getType() != GuiType.MENU) {
            return;
        }

        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        handleUtilityBox(player, clickedItem);
    }

    private void handleUtilityBox(Player player, ItemStack clickedItem) {
        switch (clickedItem.getType()) {
            case ARMOR_STAND:
                teleportToDeathLocation(player);
                break;
            case ENDER_CHEST:
                player.openInventory(player.getEnderChest());
                break;
            case CRAFTING_TABLE:
                player.openWorkbench(null, true);
                break;
            case DROPPER:
                trashListener.openTrashBox(player);
                break;
            case NAME_TAG:
                GUI.openNicknameMenu(player);
                break;
            case CREEPER_HEAD:
                boolean nowEnabled = miniutility.getCreeperProtectionListener().toggle();
                player.sendMessage(colored(
                        "クリーパーの爆破によるブロック破壊防止が " + (nowEnabled ? "有効" : "無効") + " になりました。",
                        NamedTextColor.GREEN
                ));
                player.closeInventory();
                break;
            case EXPERIENCE_BOTTLE:
                player.closeInventory();
                chatListener.beginExpInput(player);
                player.sendMessage(colored("経験値を増減する数値をチャットに入力してください。", NamedTextColor.AQUA));
                player.sendMessage(colored("例: \"10\" で +10レベル, \"-5\" で -5レベル", NamedTextColor.GRAY));
                break;
            case COMPASS:
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
                player.sendMessage(colored("このアイテムにはアクションが設定されていません。", NamedTextColor.RED));
                break;
        }
    }

    private void teleportToDeathLocation(Player player) {
        Location location = DeathListener.getLastDeathLocation(player, miniutility);
        if (location == null) {
            player.sendMessage(colored("死亡地点が見つかりません。", NamedTextColor.RED));
            return;
        }

        FoliaUtil.runAtPlayer(
                plugin, player.getUniqueId(),
                () -> {
                    DeathListener.teleportToDeathLocation(player, location);
                    player.sendMessage(colored("死亡地点にワープしました。", NamedTextColor.GREEN));
                }
        );
    }

}
