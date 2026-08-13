package dev.zoobooo.dyeutils.vincent;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// What Vincent is hiding this year, read fresh from the open menu every frame. Three slot reads and a
// regex over a dozen short lines costs nothing, and rebuilding removes every way the snapshot could go
// stale -- the menu is filled in by packets that arrive after the screen does, and a dye can be
// swapped mid-year by a patch.
record HiddenDyes(int year, List<Hidden> entries) {
	// Vincent has used 29, 31 and 33 since the menu was added, but which of the three is the 3x one
	// moves, so the multiplier is always read off the dye rather than inferred from where it sits.
	private static final List<Integer> BOOSTED_SLOTS = List.of(29, 31, 33);

	private static final HiddenDyes NONE = new HiddenDyes(0, List.of());

	record Hidden(Slot slot, String name, int colour, int multiplier) {
	}

	static HiddenDyes of(ContainerScreen screen) {
		AbstractContainerMenu menu = screen.getMenu();
		List<Hidden> found = new ArrayList<>(BOOSTED_SLOTS.size());
		int year = 0;

		for (int index : BOOSTED_SLOTS) {
			// A menu that is not the one this expects must fall out here rather than throw.
			if (index >= menu.slots.size()) continue;

			Slot slot = menu.slots.get(index);
			ItemStack stack = slot.getItem();

			DyeIcons.Dye dye = DyeIcons.read(stack);
			if (dye == null) continue;

			DyeLore.Boost boost = DyeLore.boost(DyeIcons.lore(stack));
			if (boost == null) continue;

			year = boost.year();
			found.add(new Hidden(slot, dye.name(), dye.colour(), boost.multiplier()));
		}

		// No boost line anywhere means this is not a menu worth touching, so nothing is hidden and
		// nothing is intercepted. That is the whole degradation path if Hypixel rebuilds the menu.
		if (found.isEmpty()) return NONE;

		return new HiddenDyes(year, List.copyOf(found));
	}

	boolean isEmpty() {
		return entries.isEmpty();
	}
}
