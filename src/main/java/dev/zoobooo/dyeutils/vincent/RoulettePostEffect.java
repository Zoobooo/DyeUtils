package dev.zoobooo.dyeutils.vincent;

import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

// The handshake between the reveal and the frame's blur pass, implemented by GuiRenderStateMixin.
//
// Minecraft gives the GUI exactly one blur slot per frame, so the reveal borrows it rather than adding
// another: the effect is armed while drawing and consumed when the renderer reaches the blur, which
// means it can only ever apply to the frame that asked for it.
public interface RoulettePostEffect {
	void dyeutils$armPostEffect(Identifier effect);

	@Nullable Identifier dyeutils$consumePostEffect();
}
