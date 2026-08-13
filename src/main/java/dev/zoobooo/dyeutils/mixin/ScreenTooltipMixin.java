package dev.zoobooo.dyeutils.mixin;

import dev.zoobooo.dyeutils.vincent.VincentDyes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The one window this feature needs and Fabric's screen events cannot give: after every slot has been
// drawn, but before the deferred tooltip is flushed.
//
// Fabric's afterExtract wraps the whole of extractRenderStateWithTooltipAndSubtitles, so it runs after
// the tooltip has already been drawn and cleared -- too late to replace a tooltip, and early enough
// that a cover drawn there would paint over the tooltip of whichever slot is hovered.
@Mixin(Screen.class)
abstract class ScreenTooltipMixin {
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
	private void dyeutils$beforeTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float partialTick, CallbackInfo ci) {
		VincentDyes.INSTANCE.beforeTooltips((Screen) (Object) this, graphics, mouseX, mouseY);
	}
}
