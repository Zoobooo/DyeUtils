package dev.zoobooo.dyeutils.rift;

import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Slime;

import org.slf4j.Logger;

public class DyeSlimeRenderer extends SlimeRenderer {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Identifier TEXTURE = DyeUtils.id("textures/entity/bacte.png");

	// Since 1.21.2 the texture is chosen from the render state, and the entity is out of scope by then.
	public static class DyeSlimeRenderState extends SlimeRenderState {
		public boolean isBacte;
	}

	public DyeSlimeRenderer(EntityRendererProvider.Context context) {
		super(context);

		// Vanilla's outer layer hardcodes the slime texture instead of asking the renderer for it,
		// and the outer shell is the only part you can see.
		if (!this.layers.removeIf(SlimeOuterLayer.class::isInstance)) {
			// Vanilla would have restructured the renderer. Say so rather than silently retexturing only
			// the hidden inner cube.
			LOGGER.warn("[DyeUtils] No vanilla slime outer layer to replace; Bacte will render untouched.");
		}

		addLayer(new DyeSlimeOuterLayer(this, context.getModelSet()));
	}

	static Identifier textureFor(SlimeRenderState state) {
		return state instanceof DyeSlimeRenderState dyeState && dyeState.isBacte ? TEXTURE : SLIME_LOCATION;
	}

	@Override
	public SlimeRenderState createRenderState() {
		return new DyeSlimeRenderState();
	}

	@Override
	public void extractRenderState(Slime entity, SlimeRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);

		if (state instanceof DyeSlimeRenderState dyeState) {
			dyeState.isBacte = BacteTracker.INSTANCE.isBacte(entity);
		}
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return textureFor(state);
	}
}
