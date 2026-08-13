package dev.zoobooo.dyeutils.mixin;

import dev.zoobooo.dyeutils.vincent.VincentDyes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While the reveal is playing the menu underneath it does not draw at all.
//
// A translucent scrim over the top is not enough: the menu is a bright grey box and it showed straight
// through, and its tooltip is drawn in the same stratum as the reveal, so the two interleaved. Skipping
// the menu render for those frames is also what SkyOcean does, and it leaves the blurred world behind
// the cards rather than a flat black rectangle.
@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenRenderMixin {
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void dyeutils$hideMenuDuringReveal(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		if (VincentDyes.INSTANCE.hidesMenu((Screen) (Object) this)) ci.cancel();
	}
}
