package com.kingdoms.client;

import com.kingdoms.KingdomsMod;
import com.kingdoms.RulerEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;

/** Kings and queens use the player model with the wide (Steve) arms. Kings also show their armor. */
public class RulerRenderer extends HumanoidMobRenderer<RulerEntity, KingdomRenderState, HumanoidModel<KingdomRenderState>> {
	public RulerRenderer(EntityRendererProvider.Context ctx) {
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
	public void extractRenderState(RulerEntity e, KingdomRenderState state, float partial) {
		super.extractRenderState(e, state, partial);
		state.texture = Identifier.fromNamespaceAndPath(KingdomsMod.MOD_ID,
			"textures/entity/" + e.faction().id + (e.isQueen() ? "_queen.png" : "_king.png"));
	}

	@Override
	public Identifier getTextureLocation(KingdomRenderState state) {
		return state.texture;
	}
}
