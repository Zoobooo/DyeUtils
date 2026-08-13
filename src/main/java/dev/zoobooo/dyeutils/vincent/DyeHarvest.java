package dev.zoobooo.dyeutils.vincent;

import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.util.Failsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;

// The dye table is learned rather than shipped. Every dye in the game is in Vincent's compendium, and
// any menu holding dyes teaches it a few more, so a dye added to SkyBlock turns up on the roulette with
// no mod update and no dye artwork bundled or downloaded from anywhere.
//
// Scanned every tick rather than on a slower cadence: the compendium can be paged through faster than
// twice a second, and a skipped page is a page of dyes never learned.
public final class DyeHarvest {
	public static final DyeHarvest INSTANCE = new DyeHarvest();

	private static final Logger LOGGER = LogUtils.getLogger();

	private DyeHarvest() {
	}

	public void tick(Minecraft client) {
		if (!DyeUtils.config().vincentReveal) return;
		if (!(client.screen instanceof AbstractContainerScreen<?> screen)) return;

		Failsafe.run("Vincent dye harvest", () -> harvest(screen));
	}

	private void harvest(AbstractContainerScreen<?> screen) {
		VincentState state = VincentState.get();
		int learned = 0;

		// Every slot, the player's own inventory included: a dye being carried is as good a source of
		// its own icon as the compendium is.
		for (Slot slot : screen.getMenu().slots) {
			ItemStack stack = slot.getItem();

			DyeIcons.Dye dye = DyeIcons.read(stack);
			if (dye == null) continue;

			String texture = DyeIcons.texture(stack);
			if (texture == null) continue;

			if (state.putIcon(dye.name(), texture, dye.colour())) learned++;
		}

		if (learned == 0) return;

		state.save();
		LOGGER.info("[DyeUtils] Learned {} dye icon(s) from {}; {} known.", learned,
				screen.getTitle().getString(), state.iconCount());
	}
}
