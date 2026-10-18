package org.hotamachisubaru.miniutility.fabric.mixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.hotamachisubaru.miniutility.mod.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class PlayerNameMixin {
 @Inject(method="getDisplayName",at=@At("RETURN"),cancellable=true)
 private void miniutility$name(CallbackInfoReturnable<Component> result) {
  if((Object)this instanceof ServerPlayer player){var runtime=ModRuntime.get(Compat.server(player));if(runtime!=null)result.setReturnValue(runtime.displayName(player));}
 }
}
