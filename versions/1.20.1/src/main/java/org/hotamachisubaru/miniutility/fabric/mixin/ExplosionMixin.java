package org.hotamachisubaru.miniutility.fabric.mixin;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Creeper;
import org.hotamachisubaru.miniutility.mod.ModRuntime;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
 @Shadow @Final private Level level;
 @Inject(method="finalizeExplosion",at=@At("HEAD"))
 private void miniutility$protect(boolean particles,CallbackInfo info) {
  Explosion explosion=(Explosion)(Object)this;
  if(explosion.getIndirectSourceEntity() instanceof Creeper && level.getServer()!=null){var r=ModRuntime.get(level.getServer());if(r!=null && r.protectBlocks())explosion.getToBlow().clear();}
 }
}
