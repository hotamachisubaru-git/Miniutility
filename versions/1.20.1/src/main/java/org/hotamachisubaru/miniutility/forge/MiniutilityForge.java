package org.hotamachisubaru.miniutility.forge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import org.hotamachisubaru.miniutility.mod.*;

@Mod("miniutility")
public final class MiniutilityForge {
    public MiniutilityForge() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener((ServerStartingEvent e) -> ModRuntime.start(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> ModRuntime.stop(e.getServer()));
        bus.addListener((RegisterCommandsEvent e) -> ModRuntime.register(e.getDispatcher()));
        bus.addListener((TickEvent.ServerTickEvent e) -> { if (e.phase == TickEvent.Phase.END) { var r = ModRuntime.get(e.getServer()); if (r != null) r.tick(); } });
        bus.addListener((ServerChatEvent e) -> { var r = ModRuntime.get(Compat.server(e.getPlayer())); if (r != null && r.chat(e.getPlayer(), e.getRawText())) e.setCanceled(true); });
        bus.addListener(EventPriority.LOWEST, false, (LivingDeathEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) r.recordDeath(p); } });
        bus.addListener((ExplosionEvent.Detonate e) -> { if (e.getExplosion().getIndirectSourceEntity() instanceof Creeper) { var r = ModRuntime.get(e.getLevel().getServer()); if (r != null && r.protectBlocks()) e.getAffectedBlocks().clear(); } });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) r.quit(p); } });
        bus.addListener((PlayerEvent.NameFormat e) -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) e.setDisplayname(r.displayName(p)); } });
        bus.addListener((PlayerEvent.TabListNameFormat e) -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) e.setDisplayName(r.displayName(p)); } });
    }
}
