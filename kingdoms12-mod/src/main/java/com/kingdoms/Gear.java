package com.kingdoms;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/** Visible, enchanted diamond gear for both factions. */
public final class Gear {
	public static ItemStack enchanted(Level level, ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> key, int lvl) {
		Holder<Enchantment> ench = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		stack.enchant(ench, lvl);
		return stack;
	}

	public static void equipArmor(Mob mob) {
		Level level = mob.level();
		set(mob, EquipmentSlot.HEAD, enchanted(level, new ItemStack(Items.DIAMOND_HELMET), Enchantments.PROTECTION, 3));
		set(mob, EquipmentSlot.CHEST, enchanted(level, new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 3));
		set(mob, EquipmentSlot.LEGS, enchanted(level, new ItemStack(Items.DIAMOND_LEGGINGS), Enchantments.PROTECTION, 3));
		set(mob, EquipmentSlot.FEET, enchanted(level, new ItemStack(Items.DIAMOND_BOOTS), Enchantments.PROTECTION, 3));
	}

	public static void set(Mob mob, EquipmentSlot slot, ItemStack stack) {
		mob.setItemSlot(slot, stack);
		mob.setDropChance(slot, 0.0f);
	}

	private Gear() {}
}
