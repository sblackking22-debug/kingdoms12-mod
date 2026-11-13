package com.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

/**
 * Stage 1 realm spawning: instead of generated castles, /kingdom found places a
 * king, a queen and a small army at the player's position. Stage 2 will replace this
 * with world-generated castles, outposts and mansions.
 */
public final class Realms {
	public static SoldierEntity spawnSoldier(ServerLevel level, BlockPos near, Faction f, boolean female) {
		SoldierEntity s = ModEntities.SOLDIER.create(level, EntitySpawnReason.COMMAND);
		if (s == null) return null;
		double ox = level.getRandom().nextInt(7) - 3;
		double oz = level.getRandom().nextInt(7) - 3;
		s.setPos(near.getX() + 0.5 + ox, near.getY(), near.getZ() + 0.5 + oz);
		Rank[] pool = {Rank.SWORDSMAN, Rank.AXEMAN, Rank.ARCHER};
		s.init(f, pool[level.getRandom().nextInt(pool.length)], female);
		level.addFreshEntity(s);
		return s;
	}

	public static boolean found(ServerLevel level, BlockPos pos, Faction f) {
		KingdomData d = KingdomData.get();
		if (d.kings.containsKey(f)) return false; // one king per faction per world

		RulerEntity king = ModEntities.RULER.create(level, EntitySpawnReason.COMMAND);
		RulerEntity queen = ModEntities.RULER.create(level, EntitySpawnReason.COMMAND);
		if (king == null || queen == null) return false;
		king.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		king.initRuler(f, false);
		queen.setPos(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5);
		queen.initRuler(f, true);
		level.addFreshEntity(king);
		level.addFreshEntity(queen);
		d.kings.put(f, king.getUUID());
		d.save();

		for (int i = 0; i < 6; i++) spawnSoldier(level, pos, f, i < 2);
		return true;
	}

	private Realms() {}
}
