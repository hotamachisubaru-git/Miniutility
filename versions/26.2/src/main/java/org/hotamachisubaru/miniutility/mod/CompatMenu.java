package org.hotamachisubaru.miniutility.mod;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
/** Bridges the native click enum without exposing version-specific types to the shared menus. */
public abstract class CompatMenu extends AbstractContainerMenu {
 protected CompatMenu(MenuType<?> type,int id) { super(type,id); }
 @Override public final void clicked(int slot,int button,ContainerInput type,Player player) {
  if (!interceptClick(slot,button,type == ContainerInput.PICKUP,player)) super.clicked(slot,button,type,player);
 }
 protected abstract boolean interceptClick(int slot,int button,boolean pickup,Player player);
}
