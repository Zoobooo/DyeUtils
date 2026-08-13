package dev.zoobooo.dyeutils.mixin;

import dev.zoobooo.dyeutils.vincent.VincentDyes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The chest panel is drawn here rather than in extractRenderState, so hiding the menu behind a reveal
// takes both.
//
// Cancelled after the super call rather than at the head, so the panel goes but vanilla's full screen
// dimming stays. That dimming darkens the world behind the cards and follows the player's own menu
// background blur setting instead of overriding it.
@Mixin(ContainerScreen.class)
abstract class ContainerBackgroundMixin {
	@Inject(method = "extractBackground",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;"
							+ "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
					shift = At.Shift.AFTER),
			cancellable = true)
	private void dyeutils$hideMenuBackgroundDuringReveal(GuiGraphicsExtractor graphics, int mouseX,
			int mouseY, float partialTick, CallbackInfo ci) {
		if (VincentDyes.INSTANCE.hidesMenu((Screen) (Object) this)) ci.cancel();
	}
}
