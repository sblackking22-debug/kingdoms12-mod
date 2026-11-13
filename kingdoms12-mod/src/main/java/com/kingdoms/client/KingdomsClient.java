package com.kingdoms.client;

import com.kingdoms.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class KingdomsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.SOLDIER, SoldierRenderer::new);
		EntityRendererRegistry.register(ModEntities.RULER, RulerRenderer::new);
	}
}
