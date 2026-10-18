package org.hotamachisubaru.miniutility.mod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class TrashSession {
    final SimpleContainer contents = new SimpleContainer(54);
    private boolean finished;
    public void discard() { if (!finished) { finished = true; contents.clearContent(); } }
    public void giveBack(ServerPlayer owner) {
        if (finished) return;
        finished = true;
        for (int i = 0; i < 53; i++) {
            ItemStack stack = contents.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                owner.getInventory().add(stack);
                if (!stack.isEmpty()) Compat.drop(owner, stack);
            }
        }
        contents.clearContent();
    }
}
