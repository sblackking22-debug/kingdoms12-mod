package com.kingdoms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.EnumSet;

public class SoldierEntity extends PathfinderMob implements RangedAttackMob {
	private static final EntityDataAccessor<Integer> FACTION =
		SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> RANK =
		SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FEMALE =
		SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);

	public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FACTION, 0);
		builder.define(RANK, 0);
		builder.define(FEMALE, false);
	}

	public Faction faction() { return Faction.byOrdinal(entityData.get(FACTION)); }
	public Rank rank() { return Rank.byOrdinal(entityData.get(RANK)); }
	public boolean isFemale() { return entityData.get(FEMALE); }

	public void init(Faction f, Rank r, boolean female) {
		entityData.set(FACTION, f.ordinal());
		entityData.set(FEMALE, female);
		setRank(r);
		Gear.equipArmor(this);
		setCustomName(Component.literal((female ? "Female " : "") + capitalize(f.id) + " " + capitalize(r.name())));
	}

	public void setRank(Rank r) {
		entityData.set(RANK, r.ordinal());
		switch (r) {
			case SWORDSMAN -> Gear.set(this, EquipmentSlot.MAINHAND,
				Gear.enchanted(level(), new ItemStack(Items.DIAMOND_SWORD),
					net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2));
			case AXEMAN -> Gear.set(this, EquipmentSlot.MAINHAND,
				Gear.enchanted(level(), new ItemStack(Items.DIAMOND_AXE),
					net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2));
			case ARCHER -> Gear.set(this, EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
			default -> Gear.set(this, EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		}
		// Rebuild goals so archers shoot and melee ranks swing.
		this.goalSelector.removeAllGoals(g -> true);
		registerGoals();
		if (entityData.get(FACTION) >= 0) {
			setCustomName(Component.literal((isFemale() ? "Female " : "") + capitalize(faction().id) + " " + capitalize(r.name())));
		}
	}

	private static String capitalize(String s) {
		s = s.toLowerCase();
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		if (rank() == Rank.ARCHER) {
			this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 25, 16.0f));
		} else {
			this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
		}
		this.goalSelector.addGoal(4, new MarchToEnemyKingGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
			(target, level) -> isEnemy(target)));
	}

	boolean isEnemy(LivingEntity other) {
		Faction mine = faction();
		if (other instanceof SoldierEntity s) return s.faction() != mine;
		if (other instanceof RulerEntity r) return r.faction() != mine && !r.isQueen();
		if (other instanceof Player) {
			KingdomData d = KingdomData.get();
			// The player is an enemy only if pledged to the opposing side.
			return d.playerFaction != null && d.playerFaction != mine;
		}
		return false;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(level() instanceof ServerLevel sl)) return;
		Arrow arrow = new Arrow(sl, this, new ItemStack(Items.ARROW), null);
		double dx = target.getX() - getX();
		double dy = target.getEyeY() - 1.1 - arrow.getY();
		double dz = target.getZ() - getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		arrow.shoot(dx, dy + flat * 0.2, dz, 1.6f, 6.0f);
		sl.addFreshEntity(arrow);
	}

	/** Right-click with a gold ingot as a ruler who has a free throne: this woman becomes a queen. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level().isClientSide() || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
		KingdomData d = KingdomData.get();
		ItemStack held = player.getItemInHand(hand);
		if (isFemale() && held.is(Items.GOLD_INGOT) && d.isPlayerRulerOf(faction())) {
			if (d.freeThrones <= 0) {
				sp.sendSystemMessage(Component.literal("You have no free throne. Take one from a queen first."));
				return InteractionResult.SUCCESS;
			}
			d.freeThrones--;
			d.save();
			RulerEntity queen = ModEntities.RULER.create((ServerLevel) level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
			if (queen != null) {
				queen.setPos(getX(), getY(), getZ());
				queen.initRuler(faction(), true);
				queen.owner = sp.getUUID();
				level().addFreshEntity(queen);
				held.shrink(1);
				sp.sendSystemMessage(Component.literal("A new queen sits beside you."));
				discard();
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		super.addAdditionalSaveData(out);
		out.putInt("Faction", entityData.get(FACTION));
		out.putInt("Rank", entityData.get(RANK));
		out.putBoolean("Female", entityData.get(FEMALE));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		super.readAdditionalSaveData(in);
		entityData.set(FACTION, in.getIntOr("Faction", 0));
		entityData.set(RANK, in.getIntOr("Rank", 0));
		entityData.set(FEMALE, in.getBooleanOr("Female", false));
	}

	@Override
	public boolean removeWhenFarAway(double dist) {
		return false;
	}

	/** Idle soldiers of AI-controlled factions slowly walk toward the enemy king. */
	static class MarchToEnemyKingGoal extends Goal {
		private final SoldierEntity mob;
		private int cooldown;

		MarchToEnemyKingGoal(SoldierEntity mob) {
			this.mob = mob;
			setFlags(EnumSet.of(Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (mob.getTarget() != null) return false;
			KingdomData d = KingdomData.get();
			if (d.isPlayerRulerOf(mob.faction())) return false; // the player commands this army
			return --cooldown <= 0 && War.enemyKingPos(mob) != null;
		}

		@Override
		public void start() {
			cooldown = 600 + mob.getRandom().nextInt(600);
			BlockPos p = War.enemyKingPos(mob);
			if (p != null) mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.8);
		}

		@Override
		public boolean canContinueToUse() {
			return mob.getTarget() == null && !mob.getNavigation().isDone();
		}
	}
}
