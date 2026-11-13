package com.kingdoms.client;

import com.kingdoms.KingdomsMod;
import com.kingdoms.SoldierEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;

/** Soldiers use the player model and wear their enchanted diamond armor visibly. */
public class SoldierRenderer extends HumanoidMobRenderer<SoldierEntity, KingdomRenderState, HumanoidModel<KingdomRenderState>> {
	public SoldierRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer<>(this,
			net.minecraft.client.renderer.entity.ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, ctx.getModelSet(), HumanoidModel::new),
			ctx.getEquipmentRenderer()));
	}

	@Override
	public KingdomRenderState createRenderState() {
		return new KingdomRenderState();
	}

	@Override
	public void extractRenderState(SoldierEntity e, KingdomRenderState state, float partial) {
		super.extractRenderState(e, state, partial);
		state.texture = Identifier.fromNamespaceAndPath(KingdomsMod.MOD_ID,
			"textures/entity/" + e.faction().id + (e.isFemale() ? "_soldier_f.png" : "_soldier.png"));
	}

	@Override
	public Identifier getTextureLocation(KingdomRenderState state) {
		return state.texture;
	}
}
