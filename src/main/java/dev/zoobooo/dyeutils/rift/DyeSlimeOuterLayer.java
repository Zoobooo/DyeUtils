package dev.zoobooo.dyeutils.rift;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Vanilla's outer layer hardcodes the slime texture instead of asking the renderer for it,
// and the outer shell is the only part you can see.
public class DyeSlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
	private final SlimeModel model;

	public DyeSlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> parent, EntityModelSet models) {
		super(parent);

		this.model = new SlimeModel(models.bakeLayer(ModelLayers.SLIME_OUTER));
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SlimeRenderState state,
			float yRot, float xRot) {
		boolean outlineOnly = state.appearsGlowing() && state.isInvisible;

		if (state.isInvisible && !outlineOnly) return;

		Identifier texture = DyeSlimeRenderer.textureFor(state);
		RenderType renderType = outlineOnly ? RenderTypes.outline(texture) : RenderTypes.entityTranslucent(texture);

		collector.order(1).submitModel(this.model, state, poseStack, renderType, light,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor, null);
	}
}
