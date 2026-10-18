package org.hotamachisubaru.miniutility.forge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.*;
import net.minecraftforge.event.server.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import org.hotamachisubaru.miniutility.mod.*;

@Mod("miniutility")
public final class MiniutilityForge {
    public MiniutilityForge(FMLJavaModLoadingContext context) {
        ServerStartingEvent.BUS.addListener(e -> ModRuntime.start(e.getServer()));
        ServerStoppedEvent.BUS.addListener(e -> ModRuntime.stop(e.getServer()));
        RegisterCommandsEvent.BUS.addListener(e -> ModRuntime.register(e.getDispatcher()));
        TickEvent.ServerTickEvent.Post.BUS.addListener(e -> { var r = ModRuntime.get(e.getServer()); if (r != null) r.tick(); });
        ServerChatEvent.BUS.addListener(e -> { var r = ModRuntime.get(Compat.server(e.getPlayer())); return r != null && r.chat(e.getPlayer(), e.getRawText()); });
        LivingDeathEvent.BUS.addListener((e, cancelled) -> { if (!cancelled && e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) r.recordDeath(p); } });
        ExplosionEvent.Detonate.BUS.addListener(e -> { if (e.getExplosion().getIndirectSourceEntity() instanceof Creeper) { var r = ModRuntime.get(e.getLevel().getServer()); if (r != null && r.protectBlocks()) e.getAffectedBlocks().clear(); } });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) r.quit(p); } });
        PlayerEvent.NameFormat.BUS.addListener(e -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) e.setDisplayname(r.displayName(p)); } });
        PlayerEvent.TabListNameFormat.BUS.addListener(e -> { if (e.getEntity() instanceof ServerPlayer p) { var r = ModRuntime.get(Compat.server(p)); if (r != null) e.setDisplayName(r.displayName(p)); } });
    }
}
