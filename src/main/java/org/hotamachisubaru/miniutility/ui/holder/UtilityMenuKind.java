package org.hotamachisubaru.miniutility.ui.holder;

import org.hotamachisubaru.miniutility.ui.item.MenuButtonSpec;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public enum UtilityMenuKind {
    MENU(27, "メニュー", 0, Map.of(
            0, MenuButtonSpec.TELEPORT_TO_DEATH,
            2, MenuButtonSpec.EXPERIENCE_CONTROL,
            4, MenuButtonSpec.GAME_MODE_CONTROL,
            9, MenuButtonSpec.CREEPER_PROTECTION,
            11, MenuButtonSpec.ENDER_CHEST,
            13, MenuButtonSpec.TRASH_BOX,
            15, MenuButtonSpec.NICKNAME_CHANGE,
            17, MenuButtonSpec.CRAFTING_TABLE
    )),
    NICKNAME(9, "ニックネームを変更", 0, Map.of(
            2, MenuButtonSpec.NICKNAME_INPUT,
            4, MenuButtonSpec.COLORED_NICKNAME_INPUT,
            6, MenuButtonSpec.NICKNAME_RESET
    )),
    TRASH(54, "ゴミ箱", 53, Map.of(53, MenuButtonSpec.TRASH_DISCARD)),
    TRASH_CONFIRM(9, "本当に捨てますか？", 0, Map.of(
            3, MenuButtonSpec.TRASH_CONFIRM_YES,
            5, MenuButtonSpec.TRASH_CONFIRM_NO
    ));

    private final int size;
    private final String title;
    private final int contentSlots;
    private final Map<Integer, MenuButtonSpec> buttons;

    UtilityMenuKind(int size, String title, int contentSlots, Map<Integer, MenuButtonSpec> buttons) {
        this.size = size;
        this.title = title;
        this.contentSlots = contentSlots;
        this.buttons = Map.copyOf(buttons);
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }

    public int getContentSlots() {
        return contentSlots;
    }

    public boolean isContentSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < contentSlots;
    }

    public Map<Integer, MenuButtonSpec> getButtons() {
        return buttons;
    }

    public @Nullable MenuButtonSpec getButton(int rawSlot) {
        return buttons.get(rawSlot);
    }
}
