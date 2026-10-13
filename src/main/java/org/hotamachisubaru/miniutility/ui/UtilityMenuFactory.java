package org.hotamachisubaru.miniutility.ui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.action.MenuButtonItemFactory;
import org.hotamachisubaru.miniutility.ui.holder.OwnedMenuInventoryHolder;
import org.hotamachisubaru.miniutility.ui.holder.UtilityMenuKind;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * GUIインベントリを作成するファクトリクラス。
 * <p>
 * 各メソッドが固有のGUIタイプを生成し、{@link OwnedMenuInventoryHolder} にバインドします。
 */
@NullMarked
public final class UtilityMenuFactory {

    private UtilityMenuFactory() {
    }

    /**
     * メインメニューのインベントリを作成します。
     */
    public static Inventory createMenu(UUID owner) {
        return createInventory(UtilityMenuKind.MENU, owner);
    }

    /**
     * ニックネーム変更メニューのインベントリを作成します。
     */
    public static Inventory createNicknameMenu(UUID owner) {
        return createInventory(UtilityMenuKind.NICKNAME, owner);
    }

    /**
     * ゴミ箱のインベントリを作成します。
     */
    public static Inventory createTrashBox(UUID owner) {
        return createTrashBox(owner, new @Nullable ItemStack[0]);
    }

    /**
     * コンテンツ付きのゴミ箱インベントリを作成します。
     */
    public static Inventory createTrashBox(UUID owner, @Nullable ItemStack[] contents) {
        Objects.requireNonNull(contents, "contents");
        Inventory inventory = createInventory(UtilityMenuKind.TRASH, owner);
        for (int slot = 0; slot < Math.min(UtilityMenuKind.TRASH.getContentSlots(), contents.length); slot++) {
            ItemStack item = contents[slot];
            inventory.setItem(slot, item == null || item.getType().isAir() ? null : item.clone());
        }
        return inventory;
    }

    /**
     * ゴミ箱削除確認のインベントリを作成します。
     */
    public static Inventory createTrashConfirm(UUID owner) {
        return createInventory(UtilityMenuKind.TRASH_CONFIRM, owner);
    }

    private static Inventory createInventory(UtilityMenuKind type, UUID owner) {
        OwnedMenuInventoryHolder holder = new OwnedMenuInventoryHolder(type, owner);
        Inventory inventory = Objects.requireNonNull(Bukkit.createInventory(
                holder, type.getSize(), LegacyTextFormatter.text(type.getTitle())
        ));
        holder.bind(inventory);
        type.getButtons().forEach((slot, button) -> inventory.setItem(slot, MenuButtonItemFactory.createGuiItem(button)));
        return inventory;
    }
}
