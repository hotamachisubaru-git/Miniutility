package org.hotamachisubaru.miniutility.fabric.mixin;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.entity.monster.Creeper;
import org.hotamachisubaru.miniutility.mod.ModRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin {
 @Inject(method="calculateExplodedPositions",at=@At("RETURN"),cancellable=true)
 private void miniutility$protect(CallbackInfoReturnable<List<BlockPos>> result) {
  ServerExplosion explosion=(ServerExplosion)(Object)this;
  if(explosion.getIndirectSourceEntity() instanceof Creeper){var r=ModRuntime.get(explosion.level().getServer());if(r!=null && r.protectBlocks())result.setReturnValue(List.of());}
 }
}
