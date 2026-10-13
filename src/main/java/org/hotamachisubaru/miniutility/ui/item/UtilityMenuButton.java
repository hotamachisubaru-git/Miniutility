package org.hotamachisubaru.miniutility.ui.item;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.action.MenuButtonItemFactory;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * GUIメニューアイテムを表現する不変クラス。
 */
public final class UtilityMenuButton {

    private final MenuButtonSpec menuItem;
    private final Component name;
    private final Component lore;

    private UtilityMenuButton(MenuButtonSpec menuItem) {
        this(menuItem, null, null);
    }

    private UtilityMenuButton(MenuButtonSpec menuItem, @Nullable Component nameOverride, @Nullable Component loreOverride) {
        this.menuItem = Objects.requireNonNull(menuItem, "menuItem");
        this.name = nameOverride != null ? nameOverride : LegacyTextFormatter.text(menuItem.getName());
        this.lore = loreOverride != null ? loreOverride : LegacyTextFormatter.text(menuItem.getLore());
    }

    /**
     * デフォルトのメニューアイテムからボタンを作成します。
     */
    public static UtilityMenuButton of(MenuButtonSpec menuItem) {
        return new UtilityMenuButton(menuItem);
    }

    /**
     * カスタム名・説明文のボタンを作成します。
     */
    public static UtilityMenuButton of(MenuButtonSpec menuItem, @Nullable Component name, @Nullable Component lore) {
        return new UtilityMenuButton(menuItem, name, lore);
    }

    /**
     * GUIアイテムとしてレンダリングします。
     */
    public ItemStack toItemStack() {
        return MenuButtonItemFactory.createGuiItem(menuItem.getMaterial(), name, lore);
    }

    public MenuButtonSpec getMenuItem() {
        return menuItem;
    }
}
