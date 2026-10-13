package org.hotamachisubaru.miniutility.ui.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * GUIメニューのアイテム構成を表す列挙型。
 */
public enum MenuButtonSpec {
    /**
     * 死亡地点へのワープ。
     */
    TELEPORT_TO_DEATH(Material.ARMOR_STAND, "死亡地点にワープ", "死亡地点にワープします。溺れたり、溶岩遊泳した場合は安全な場所にテレポートします。"),

    /**
     * 経験値制御。
     */
    EXPERIENCE_CONTROL(Material.EXPERIENCE_BOTTLE, "経験値制御器", "経験値を制御します"),

    /**
     * ゲームモード制御。
     */
    GAME_MODE_CONTROL(Material.COMPASS, "ゲームモード制御器", "ゲームモードを制御します"),

    /**
     * クリーパー保護の切り替え。
     */
    CREEPER_PROTECTION(Material.CREEPER_HEAD, "クリーパーのブロック破壊を防ぐ", "クリーパーのブロック破壊を防ぎます。有効/無効を切り替えられます。"),

    /**
     * エンダーチェスト。
     */
    ENDER_CHEST(Material.ENDER_CHEST, "エンダーチェスト", "クリックしてエンダーチェストを開く"),

    /**
     * ゴミ箱。
     */
    TRASH_BOX(Material.DROPPER, "ゴミ箱", "クリックしてゴミ箱を開く"),

    /**
     * ニックネーム変更。
     */
    NICKNAME_CHANGE(Material.NAME_TAG, "ニックネームを変更", "クリックしてニックネームを変更"),

    /**
     * どこでも作業台。
     */
    CRAFTING_TABLE(Material.CRAFTING_TABLE, "どこでも作業台", "クリックして作業台を開く"),

    /**
     * ニックネーム入力。
     */
    NICKNAME_INPUT(Material.PAPER, "ニックネーム入力", "クリックして新しいニックネームを入力"),

    /**
     * カラーニックネーム入力。
     */
    COLORED_NICKNAME_INPUT(Material.NAME_TAG, "カラーコード指定", "クリックして色付きニックネームを入力"),

    /**
     * ニックネームリセット。
     */
    NICKNAME_RESET(Material.BARRIER, "リセット", "ニックネームをリセット"),

    /**
     * ゴミ箱捨てるボタン。
     */
    TRASH_DISCARD(Material.LIME_WOOL, "捨てる", "クリックして削除確認へ"),

    /**
     * ゴミ箱確認 - はい。
     */
    TRASH_CONFIRM_YES(Material.LIME_WOOL, "はい", "ゴミ箱を空にする"),

    /**
     * ゴミ箱確認 - いいえ。
     */
    TRASH_CONFIRM_NO(Material.RED_WOOL, "いいえ", "キャンセル");

    private final Material material;
    private final String name;
    private final String lore;

    MenuButtonSpec(Material material, String name, String lore) {
        this.material = Objects.requireNonNull(material, "material");
        this.name = Objects.requireNonNull(name, "name");
        this.lore = Objects.requireNonNull(lore, "lore");
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public String getLore() {
        return lore;
    }

    public boolean matches(@Nullable ItemStack item) {
        return item != null && item.getType() == material;
    }
}
