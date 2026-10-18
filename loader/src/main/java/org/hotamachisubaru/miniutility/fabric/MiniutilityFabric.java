package org.hotamachisubaru.miniutility.fabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.hotamachisubaru.miniutility.mod.*;
public final class MiniutilityFabric implements ModInitializer {
 @Override public void onInitialize() {
  ServerLifecycleEvents.SERVER_STARTING.register(ModRuntime::start);
  ServerLifecycleEvents.SERVER_STOPPED.register(ModRuntime::stop);
  ServerTickEvents.END_SERVER_TICK.register(server -> {var r=ModRuntime.get(server); if(r!=null) r.tick();});
  CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->ModRuntime.register(dispatcher));
  ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message,player,params)-> {var r=ModRuntime.get(Compat.server(player)); return r==null || !r.chat(player,message.signedContent());});
  ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{if(entity instanceof ServerPlayer player){var r=ModRuntime.get(Compat.server(player)); if(r!=null)r.recordDeath(player);}});
  ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{var r=ModRuntime.get(server);if(r!=null)r.quit(handler.player);});
 }
}
