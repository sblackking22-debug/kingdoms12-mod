package com.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Slow background war between the two factions while the player does not rule them.
 * Every few minutes each AI faction sends one reinforcement from its king toward the enemy king.
 */
public final class War {
	private static final int INTERVAL_TICKS = 6000;   // 5 minutes
	private static final int MAX_SOLDIERS_PER_FACTION = 40;

	public static BlockPos enemyKingPos(SoldierEntity s) {
		java.util.UUID id = KingdomData.get().kings.get(s.faction().enemy());
		if (id == null || !(s.level() instanceof ServerLevel sl)) return null;
		Entity king = sl.getEntity(id);
		return king == null ? null : king.blockPosition();
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % INTERVAL_TICKS != 0) return;
		ServerLevel level = server.overworld();
		KingdomData d = KingdomData.get();
		for (Faction f : Faction.values()) {
			if (d.isPlayerRulerOf(f)) continue; // the player commands this one now
			java.util.UUID kid = d.kings.get(f);
			if (kid == null) continue;
			Entity king = level.getEntity(kid);
			if (king == null) continue;
			int count = 0;
			for (Entity e : level.getAllEntities()) {
				if (e instanceof SoldierEntity s && s.faction() == f) count++;
			}
			if (count >= MAX_SOLDIERS_PER_FACTION) continue;
			Realms.spawnSoldier(level, king.blockPosition(), f, level.getRandom().nextInt(4) == 0);
		}
	}

	private War() {}
}
