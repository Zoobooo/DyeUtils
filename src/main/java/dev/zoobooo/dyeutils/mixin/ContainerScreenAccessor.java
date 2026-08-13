package dev.zoobooo.dyeutils.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import org.jspecify.annotations.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Both fields are protected, and the placeholder covers are drawn from another package, so where the
// menu actually sits on screen has to be asked for rather than recomputed. Recomputing it would also
// quietly disagree with any mod that moves a container screen.
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int dyeutils$leftPos();

	@Accessor("topPos")
	int dyeutils$topPos();

	// Vanilla's hover box is 18x18, not 16x16: isHovering tests mouseX >= x - 1. Reading its answer
	// rather than re-deriving one is the only way the replaced tooltip cannot disagree with the real one
	// along the border of the slot.
	@Accessor("hoveredSlot")
	@Nullable Slot dyeutils$hoveredSlot();
}
