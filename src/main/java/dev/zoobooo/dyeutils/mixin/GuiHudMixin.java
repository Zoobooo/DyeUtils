package dev.zoobooo.dyeutils.mixin;

import dev.zoobooo.dyeutils.vincent.VincentDyes;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No HUD while the reveal is playing.
//
// The hotbar, hearts and crosshair are extracted every frame whether or not a screen is open, and what
// normally hides them is the menu's own dimming. Dimming is not enough for the crosshair: it is drawn
// bright white and reads straight through a translucent layer, so the only way to be rid of it is to not
// draw it. Cancelling the whole HUD also takes the hotbar and hearts, which a modal reveal wants gone.
@Mixin(Gui.class)
abstract class GuiHudMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void dyeutils$hideHudDuringReveal(GuiGraphicsExtractor graphics, DeltaTracker delta,
			CallbackInfo ci) {
		if (VincentDyes.INSTANCE.isRevealing()) ci.cancel();
	}

	// Stopped on its own as well as through the cancel above, because the crosshair is the one piece of
	// the HUD bright enough to read straight through a translucent overlay.
	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void dyeutils$hideCrosshairDuringReveal(GuiGraphicsExtractor graphics, DeltaTracker delta,
			CallbackInfo ci) {
		if (VincentDyes.INSTANCE.isRevealing()) ci.cancel();
	}
}
