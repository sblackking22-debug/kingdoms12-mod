package com.kingdoms;

import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class KingdomCommands {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext ctx, Commands.CommandSelection sel) {
		dispatcher.register(Commands.literal("kingdom")
			.then(Commands.literal("found")
				.then(Commands.argument("faction", StringArgumentType.word()).executes(KingdomCommands::found)))
			.then(Commands.literal("pledge")
				.then(Commands.argument("faction", StringArgumentType.word()).executes(KingdomCommands::pledge)))
			.then(Commands.literal("status").executes(KingdomCommands::status))
			.then(Commands.literal("rank")
				.then(Commands.argument("rank", StringArgumentType.word()).executes(KingdomCommands::rank)))
			.then(Commands.literal("queen")
				.then(Commands.argument("action", StringArgumentType.word())
					.then(Commands.argument("id", StringArgumentType.word()).executes(KingdomCommands::queen)))));
	}

	private static int found(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		Faction f = Faction.byName(StringArgumentType.getString(c, "faction"));
		if (f == null) { p.sendSystemMessage(Component.literal("Use: villager or pillager")); return 0; }
		boolean ok = Realms.found((ServerLevel) p.level(), p.blockPosition(), f);
		p.sendSystemMessage(Component.literal(ok
			? "A " + f.id + " kingdom is founded here."
			: "The " + f.id + "s already have a king in this world."));
		return ok ? 1 : 0;
	}

	private static int pledge(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		KingdomData d = KingdomData.get();
		Faction f = Faction.byName(StringArgumentType.getString(c, "faction"));
		if (f == null) { p.sendSystemMessage(Component.literal("Use: villager or pillager")); return 0; }
		if (d.playerFaction != null) {
			p.sendSystemMessage(Component.literal("You already serve the " + d.playerFaction.id + "s."));
			return 0;
		}
		d.playerFaction = f;
		d.save();
		p.sendSystemMessage(Component.literal("You pledge to the " + f.id + "s. Beat their king down to one heart, then right-click him."));
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		KingdomData d = KingdomData.get();
		int queens = 0, soldiers = 0;
		if (d.playerFaction != null) {
			for (Entity e : ((ServerLevel) p.level()).getAllEntities()) {
				if (e instanceof RulerEntity r && r.isQueen() && p.getUUID().equals(r.owner)) queens++;
				if (e instanceof SoldierEntity s && s.faction() == d.playerFaction) soldiers++;
			}
		}
		p.sendSystemMessage(Component.literal("Faction: " + (d.playerFaction == null ? "none" : d.playerFaction.id)
			+ " | Ruler: " + d.playerRuler + " | Queens: " + queens + "/" + KingdomData.MAX_QUEENS
			+ " | Free thrones: " + d.freeThrones + " | Soldiers: " + soldiers));
		return 1;
	}

	/** Promote or demote the nearest soldier of your faction within 5 blocks. Use "recruit" to demote. */
	private static int rank(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		KingdomData d = KingdomData.get();
		if (!d.playerRuler) { p.sendSystemMessage(Component.literal("Only a ruler can grant ranks.")); return 0; }
		Rank r = Rank.byName(StringArgumentType.getString(c, "rank"));
		if (r == null) { p.sendSystemMessage(Component.literal("Ranks: recruit, swordsman, axeman, archer")); return 0; }
		SoldierEntity best = null;
		double bd = 25.0;
		for (SoldierEntity s : p.level().getEntitiesOfClass(SoldierEntity.class, new AABB(p.blockPosition()).inflate(5))) {
			if (s.faction() != d.playerFaction) continue;
			double dist = s.distanceToSqr(p);
			if (dist < bd) { bd = dist; best = s; }
		}
		if (best == null) { p.sendSystemMessage(Component.literal("No soldier of yours within 5 blocks.")); return 0; }
		best.setRank(r);
		p.sendSystemMessage(Component.literal("Soldier set to " + r.name().toLowerCase() + "."));
		return 1;
	}

	private static int queen(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		ServerLevel level = (ServerLevel) p.level();
		String action = StringArgumentType.getString(c, "action");
		UUID id;
		try { id = UUID.fromString(StringArgumentType.getString(c, "id")); }
		catch (IllegalArgumentException e) { return 0; }
		Entity e = level.getEntity(id);
		if (!(e instanceof RulerEntity q) || !q.isQueen() || !p.getUUID().equals(q.owner)) return 0;
		if (q.distanceToSqr(p) > 100) { p.sendSystemMessage(Component.literal("She is too far away.")); return 0; }

		switch (action) {
			case "toast" -> {
				level.playSound(null, q.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.6f, 1.4f);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.getX(), q.getY() + 1.5, q.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
				favor(level, p, q, 4);
			}
			case "dine" -> {
				p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
				p.getFoodData().eat(8, 0.8f);
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.getX(), q.getY() + 1.5, q.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
				favor(level, p, q, 6);
			}
			case "kiss" -> {
				level.sendParticles(ParticleTypes.HEART, q.getX(), q.getY() + 1.8, q.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
				level.playSound(null, q.blockPosition(), SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1f, 1.2f);
				favor(level, p, q, 8);
			}
			case "sleep" -> {
				p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
				p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
				level.sendParticles(ParticleTypes.HEART, q.getX(), q.getY() + 1.8, q.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
				p.sendSystemMessage(Component.literal("You rest well beside her."));
				favor(level, p, q, 10);
			}
			case "execute" -> q.execute(p);
			default -> { return 0; }
		}
		return 1;
	}

	/** Spending time with one queen makes her happier and every other queen of yours more jealous. */
	private static void favor(ServerLevel level, ServerPlayer p, RulerEntity chosen, int amount) {
		chosen.loyalty = Math.min(100, chosen.loyalty + amount);
		chosen.jealousy = Math.max(0, chosen.jealousy - 15);
		for (Entity e : level.getAllEntities()) {
			if (e instanceof RulerEntity o && o != chosen && o.isQueen() && p.getUUID().equals(o.owner)) {
				o.jealousy = Math.min(100, o.jealousy + 10);
			}
		}
	}

	private KingdomCommands() {}
}
