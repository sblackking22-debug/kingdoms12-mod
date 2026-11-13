package com.kingdoms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelResource;

/**
 * World-wide kingdom state, stored in kingdoms.properties in the world folder.
 * Singleplayer only: one player, one faction, one king per faction.
 */
public final class KingdomData {
	public static final int MAX_QUEENS = 5;
	private static final Identifier RULER_HEALTH =
		Identifier.fromNamespaceAndPath(KingdomsMod.MOD_ID, "ruler_health");

	private static KingdomData INSTANCE = new KingdomData();

	public Faction playerFaction = null;   // the faction the player pledged to
	public boolean playerRuler = false;    // true once the player holds a throne
	public final Map<Faction, UUID> kings = new EnumMap<>(Faction.class);
	public int freeThrones = 0;            // thrones taken from queens killed by hand
	private Path file;

	public static KingdomData get() {
		return INSTANCE;
	}

	public static void load(MinecraftServer server) {
		KingdomData d = new KingdomData();
		d.file = server.getWorldPath(LevelResource.ROOT).resolve("kingdoms.properties");
		if (Files.exists(d.file)) {
			Properties p = new Properties();
			try (InputStream in = Files.newInputStream(d.file)) {
				p.load(in);
				d.playerFaction = Faction.byName(p.getProperty("playerFaction"));
				d.playerRuler = Boolean.parseBoolean(p.getProperty("playerRuler", "false"));
				d.freeThrones = Integer.parseInt(p.getProperty("freeThrones", "0"));
				for (Faction f : Faction.values()) {
					String k = p.getProperty("king." + f.id);
					if (k != null && !k.isEmpty()) d.kings.put(f, UUID.fromString(k));
				}
			} catch (IOException | IllegalArgumentException e) {
				KingdomsMod.LOGGER.warn("Could not read kingdoms.properties", e);
			}
		}
		INSTANCE = d;
	}

	public void save() {
		if (file == null) return;
		Properties p = new Properties();
		if (playerFaction != null) p.setProperty("playerFaction", playerFaction.id);
		p.setProperty("playerRuler", Boolean.toString(playerRuler));
		p.setProperty("freeThrones", Integer.toString(freeThrones));
		for (Map.Entry<Faction, UUID> e : kings.entrySet()) {
			p.setProperty("king." + e.getKey().id, e.getValue().toString());
		}
		try (OutputStream out = Files.newOutputStream(file)) {
			p.store(out, "Kingdoms mod state");
		} catch (IOException e) {
			KingdomsMod.LOGGER.warn("Could not write kingdoms.properties", e);
		}
	}

	/** The player may challenge a king only if pledged to that king's faction and not already ruling. */
	public boolean canChallenge(ServerPlayer player, Faction f) {
		return playerFaction == f && !playerRuler;
	}

	public boolean isPlayerRulerOf(Faction f) {
		return playerRuler && playerFaction == f;
	}

	/** +20 max health (so 20 hearts total) while on the throne. */
	public void applyRulerBonus(ServerPlayer player) {
		AttributeInstance inst = player.getAttribute(Attributes.MAX_HEALTH);
		if (inst == null) return;
		inst.removeModifier(RULER_HEALTH);
		if (playerRuler) {
			inst.addPermanentModifier(
				new AttributeModifier(RULER_HEALTH, 20.0, AttributeModifier.Operation.ADD_VALUE));
		}
	}
}
