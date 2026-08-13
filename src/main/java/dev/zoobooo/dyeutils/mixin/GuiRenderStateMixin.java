package dev.zoobooo.dyeutils.mixin;

import dev.zoobooo.dyeutils.vincent.RoulettePostEffect;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements RoulettePostEffect {
	@Shadow
	private int firstStratumAfterBlur;

	@Shadow
	public abstract void blurBeforeThisStratum();

	@Unique
	private @Nullable Identifier dyeutils$effect;

	@Override
	public void dyeutils$armPostEffect(Identifier effect) {
		// blurBeforeThisStratum throws "Can only blur once per frame" if the slot is already taken, and
		// the vanilla menu background blur takes it whenever blurriness is 1 or more, which is the
		// default. Handing the claim back to ourselves first is the entire reason this mixin exists.
		this.firstStratumAfterBlur = Integer.MAX_VALUE;

		blurBeforeThisStratum();

		this.dyeutils$effect = effect;
	}

	@Override
	public @Nullable Identifier dyeutils$consumePostEffect() {
		Identifier effect = this.dyeutils$effect;

		this.dyeutils$effect = null;

		return effect;
	}

	// Consuming it at the blur is what keeps this to one frame, and this is the second belt: a frame that
	// arms the effect but never reaches the blur cannot leave it armed for the next screen. Without one
	// of the two, every GUI in the game keeps the reveal shader until the client restarts.
	@Inject(method = "reset", at = @At("HEAD"))
	private void dyeutils$dropPostEffect(CallbackInfo ci) {
		this.dyeutils$effect = null;
	}
}
