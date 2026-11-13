package com.kingdoms;

import java.util.UUID;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Kings (role 0) and queens (role 1). Player-like models, rendered client-side.
 * A king can never be killed until he surrenders and is deposed.
 * Queens carry loyalty and jealousy for the court system.
 */
public class RulerEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> FACTION =
		SynchedEntityData.defineId(RulerEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ROLE =
		SynchedEntityData.defineId(RulerEntity.class, EntityDataSerializers.INT);

	public UUID owner = null;        // set when this queen belongs to the player
	public boolean surrendered = false;
	public boolean deposed = false;
	public int loyalty = 50;
	public int jealousy = 0;
	private boolean executed = false;

	public RulerEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FACTION, 0);
		builder.define(ROLE, 0);
	}

	public Faction faction() { return Faction.byOrdinal(entityData.get(FACTION)); }
	public boolean isQueen() { return entityData.get(ROLE) == 1; }
	public boolean isKing() { return !isQueen(); }

	public void initRuler(Faction f, boolean queen) {
		entityData.set(FACTION, f.ordinal());
		entityData.set(ROLE, queen ? 1 : 0);
		setCustomName(Component.literal(capitalize(f.id) + (queen ? " Queen" : " King")));
		if (!queen) {
			Gear.equipArmor(this);
			Gear.set(this, EquipmentSlot.MAINHAND,
				Gear.enchanted(level(), new ItemStack(Items.DIAMOND_SWORD),
					net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 4));
		}
	}

	private static String capitalize(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
	}

	// ---------------------------------------------------------------- overthrow

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isKing() && !deposed) {
			if (surrendered) return false; // invulnerable at 1 HP until the player takes the throne
			if (getHealth() - amount <= 1.0f) {
				Entity attacker = source.getEntity();
				setHealth(1.0f);
				if (attacker instanceof ServerPlayer p && KingdomData.get().canChallenge(p, faction())) {
					surrendered = true; // hurtServer ignores all damage while surrendered
					setTarget(null);
					getNavigation().stop();
					p.sendSystemMessage(Component.literal(
						"The King yields at one heart! Right-click him to take the throne."));
				}
				return false; // kings are never killed outright
			}
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		KingdomData data = KingdomData.get();

		if (isKing() && surrendered && !deposed) {
			takeThrone(sp, data);
			return InteractionResult.SUCCESS;
		}
		if (isQueen() && owner != null && owner.equals(sp.getUUID())) {
			sendMenu(sp);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private void takeThrone(ServerPlayer sp, KingdomData data) {
		deposed = true;
		surrendered = false;
		setCustomName(Component.literal("Deposed " + capitalize(faction().id) + " King"));
		data.playerRuler = true;
		data.playerFaction = faction();
		data.kings.put(faction(), sp.getUUID()); // the player is now the king of this faction
		data.applyRulerBonus(sp);
		// The old king's queens become the player's queens.
		int n = 0;
		for (Entity e : ((ServerLevel) level()).getAllEntities()) {
			if (e instanceof RulerEntity q && q.isQueen() && q.faction() == faction() && q.owner == null && n < KingdomData.MAX_QUEENS) {
				q.owner = sp.getUUID();
				n++;
			}
		}
		data.save();
		sp.sendSystemMessage(Component.literal("You are now the King of the " + capitalize(faction().id) + "s! "
			+ n + " queen(s) pledge to you. The deposed king can be killed by hand."));
	}

	// ---------------------------------------------------------------- queen death

	@Override
	public void die(DamageSource source) {
		if (isQueen() && !executed && owner == null && source.getEntity() instanceof ServerPlayer p) {
			KingdomData d = KingdomData.get();
			if (d.freeThrones < KingdomData.MAX_QUEENS) d.freeThrones++;
			d.save();
			p.sendSystemMessage(Component.literal(
				"You seize the queen's throne. Free thrones: " + d.freeThrones + ". Right-click a woman with a gold ingot to crown her."));
		}
		super.die(source);
	}

	public void execute(ServerPlayer by) {
		executed = true;
		KingdomData d = KingdomData.get();
		if (d.freeThrones < KingdomData.MAX_QUEENS) d.freeThrones++;
		d.save();
		by.sendSystemMessage(Component.literal("The queen has been executed. Her throne is free. Free thrones: " + d.freeThrones));
		hurtServer((ServerLevel) level(), damageSources().genericKill(), 1000f);
	}

	// ---------------------------------------------------------------- queen menu

	private void sendMenu(ServerPlayer sp) {
		String id = getUUID().toString();
		MutableComponent m = Component.literal(getName().getString() + "  (loyalty " + loyalty + ", jealousy " + jealousy + ")\n");
		for (String a : new String[] {"toast", "dine", "kiss", "sleep", "execute"}) {
			final String act = a;
			m.append(Component.literal("[" + Character.toUpperCase(a.charAt(0)) + a.substring(1) + "] ")
				.withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand("/kingdom queen " + act + " " + id))));
		}
		sp.sendSystemMessage(m);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) return;
		// Kings slowly heal back from the brink.
		if (isKing() && !deposed && tickCount % 100 == 0 && getHealth() < getMaxHealth()) heal(2.0f);
		// Queen jealousy cools down, and complains when it gets high.
		if (isQueen() && owner != null && tickCount % 1200 == 0) {
			if (jealousy > 0) jealousy--;
			if (jealousy >= 80) {
				Player p = level().getPlayerByUUID(owner);
				if (p instanceof ServerPlayer sp) {
					sp.sendSystemMessage(Component.literal(getName().getString()
						+ " is jealous and unhappy. Spend time with her."));
				}
				loyalty = Math.max(0, loyalty - 3);
			}
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("Faction", entityData.get(FACTION));
		out.putInt("Role", entityData.get(ROLE));
		out.putBoolean("Surrendered", surrendered);
		out.putBoolean("Deposed", deposed);
		out.putInt("Loyalty", loyalty);
		out.putInt("Jealousy", jealousy);
		if (owner != null) out.putString("Owner", owner.toString());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		entityData.set(FACTION, in.getIntOr("Faction", 0));
		entityData.set(ROLE, in.getIntOr("Role", 0));
		surrendered = in.getBooleanOr("Surrendered", false);
		deposed = in.getBooleanOr("Deposed", false);
		loyalty = in.getIntOr("Loyalty", 50);
		jealousy = in.getIntOr("Jealousy", 0);
		String o = in.getStringOr("Owner", "");
		owner = o.isEmpty() ? null : UUID.fromString(o);
	}

	@Override
	public boolean removeWhenFarAway(double dist) {
		return false;
	}
}
