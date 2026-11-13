package com.kingdoms;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KingdomsMod implements ModInitializer {
	public static final String MOD_ID = "kingdoms";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.init();
		CommandRegistrationCallback.EVENT.register(KingdomCommands::register);
		ServerLifecycleEvents.SERVER_STARTED.register(KingdomData::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> KingdomData.get().save());
		ServerTickEvents.END_SERVER_TICK.register(War::tick);
		// Re-apply the 20-heart ruler bonus whenever the player joins.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			KingdomData.get().applyRulerBonus(handler.getPlayer()));
		LOGGER.info("Kingdoms loaded");
	}
}
