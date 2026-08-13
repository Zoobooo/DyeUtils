package dev.zoobooo.dyeutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.zoobooo.dyeutils.util.Failsafe;
import dev.zoobooo.dyeutils.vincent.RoulettePostEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;


// Swaps the GUI's blur pass for the reveal's own post chain on the one frame that asked for it, and
// otherwise calls the blur exactly as it would have been called.
//
// WrapOperation rather than a plain Redirect, because SkyOcean wraps this exact call for its own dungeon
// chest case opening and a Redirect would claim the call site exclusively. Wrappers compose; whoever
// arms an effect for the frame gets it, and with neither armed the original blur runs untouched.
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	@Shadow
	@Final
	private GuiRenderState renderState;

	@WrapOperation(method = "draw",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
	private void dyeutils$revealPostEffect(GameRenderer renderer, Operation<Void> original) {
		Identifier effect = this.renderState instanceof RoulettePostEffect armed
				? armed.dyeutils$consumePostEffect()
				: null;

		if (effect == null) {
			// Deliberately not guarded. With nothing armed this wrapper is meant to be transparent, and
			// swallowing whatever the blur throws would hide a problem from every other mod on this call.
			original.call(renderer);

			return;
		}

		try {
			Minecraft client = Minecraft.getInstance();
			PostChain chain = client.getShaderManager().getPostChain(effect, LevelTargetBundle.MAIN_TARGETS);

			// A resource pack could have removed it, or it could have failed to compile. Either way the
			// reveal still plays, just without the blur.
			if (chain == null) {
				original.call(renderer);

				return;
			}

			chain.process(client.getMainRenderTarget(),
					((GameRendererAccessor) renderer).dyeutils$resourcePool());
		} catch (Throwable t) {
			Failsafe.report("Vincent reveal post effect", t);

			// Once the frame's blur slot is taken the frame is ours to finish. If falling back to the
			// real blur also fails, as it does when a reload has left the blur chain missing, the reveal
			// loses its shader rather than the frame breaking.
			try {
				original.call(renderer);
			} catch (Throwable fallback) {
				Failsafe.report("Vincent reveal blur fallback", fallback);
			}
		}
	}
}
