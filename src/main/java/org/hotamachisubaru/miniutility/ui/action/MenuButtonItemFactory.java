package org.hotamachisubaru.miniutility.ui.action;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.hotamachisubaru.miniutility.ui.item.MenuButtonSpec;
import org.hotamachisubaru.miniutility.util.LegacyTextFormatter;

import java.util.List;
import java.util.Objects;

/**
 * GUIメニューのアイテムを作成するためのファクトリクラス。
 */
public final class MenuButtonItemFactory {

    private MenuButtonItemFactory() {
    }

    /**
     * GUIメニューアイテムを作成します。
     */
    public static ItemStack createGuiItem(MenuButtonSpec menuItem) {
        Objects.requireNonNull(menuItem, "menuItem");
        return createGuiItem(
                menuItem.getMaterial(),
                LegacyTextFormatter.text(menuItem.getName(), NamedTextColor.YELLOW),
                LegacyTextFormatter.text(menuItem.getLore(), NamedTextColor.GRAY)
        );
    }

    /**
     * GUIメニューアイテムを作成します。
     */
    public static ItemStack createGuiItem(Material material, String name, String lore) {
        return createGuiItem(
                material,
                LegacyTextFormatter.text(name, NamedTextColor.YELLOW),
                LegacyTextFormatter.text(lore, NamedTextColor.GRAY)
        );
    }

    /**
     * GUIメニューアイテムを作成します。
     */
    public static ItemStack createGuiItem(Material material, Component name, Component... lore) {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");
        if (!material.isItem() || material.isAir()) {
            throw new IllegalArgumentException("GUI buttons must use a non-air item material.");
        }
        List<Component> loreLines = List.of(lore);

        ItemStack item = new ItemStack(material);
        boolean edited = item.editMeta(meta -> {
            meta.displayName(name);
            if (!loreLines.isEmpty()) {
                meta.lore(loreLines);
            }
        });
        if (!edited) {
            throw new IllegalStateException("Could not create GUI item metadata for " + material + '.');
        }
        return item;
    }
}
