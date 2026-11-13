package com.kingdoms;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;

public final class ModEntities {
	public static final EntityType<SoldierEntity> SOLDIER = register("soldier",
		EntityType.Builder.<SoldierEntity>of(SoldierEntity::new, MobCategory.CREATURE)
			.sized(0.6f, 1.95f).clientTrackingRange(10));

	public static final EntityType<RulerEntity> RULER = register("ruler",
		EntityType.Builder.<RulerEntity>of(RulerEntity::new, MobCategory.CREATURE)
			.sized(0.6f, 1.8f).clientTrackingRange(10));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE,
			Identifier.fromNamespaceAndPath(KingdomsMod.MOD_ID, name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(SOLDIER, PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.FOLLOW_RANGE, 40.0));
		FabricDefaultAttributeRegistry.register(RULER, PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.FOLLOW_RANGE, 32.0));
	}

	private ModEntities() {}
}
