package dev.zoobooo.dyeutils.vincent;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

// One card of the reveal strip, ported from SkyOcean's DungeonCard.kt under the MIT licence -- the full
// notice is on RouletteSpin, which carries the rest of the port.
//
// Drawn in card-local 24x18 coordinates so the caller scales the pose rather than the numbers. The
// accent is the dye's own colour rather than SkyOcean's rarity colour: every dye has a hex, and a card
// in the colour of the dye it is showing beats four rarity colours shared between sixty-seven dyes.
final class DyeCard {
	private static final int BACKGROUND = 0x80303030;

	private static final int WASH_ALPHA = 0x60000000;

	private DyeCard() {
	}

	static void render(GuiGraphicsExtractor graphics, ItemStack stack, int colour) {
		int rgb = colour & 0xFFFFFF;

		graphics.fillGradient(0, 0, RouletteSpin.CARD_WIDTH, RouletteSpin.CARD_HEIGHT - 1,
				BACKGROUND, WASH_ALPHA | rgb);

		// A solid bar along the bottom, which the 4x scale turns into a 4px underline.
		graphics.fill(0, RouletteSpin.CARD_HEIGHT - 1, RouletteSpin.CARD_WIDTH, RouletteSpin.CARD_HEIGHT,
				0xFF000000 | rgb);

		graphics.item(stack, RouletteSpin.CARD_WIDTH / 2 - 8, RouletteSpin.CARD_HEIGHT / 2 - 8);
	}
}
