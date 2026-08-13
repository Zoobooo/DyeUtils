package dev.zoobooo.dyeutils.vincent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.mixin.GuiGraphicsAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import org.joml.Matrix3x2fStack;

// The reveal itself, ported from SkyOcean's DungeonGamblingRenderer.kt under the MIT licence (notice on
// RouletteSpin). Every number lives in RouletteSpin; this only draws.
final class DyeRoulette {
	// Vanilla's own menu dimming is left in place underneath this, so the two stack. Half black was too
	// much between them and a quarter too little.
	private static final int SCRIM = 0x66000000;

	private static final int MARKER = 0xFFFFFFFF;

	private static final int MARKER_SHADOW = 0xC0000000;

	private static final int MARKER_ROWS = 5;

	// Breathing room between the point of the arrow and the top of the cards.
	private static final int MARKER_GAP = 2;

	// Clearance between the bottom of the name and the top of the marker, in unscaled text rows.
	private static final int WINNER_TEXT_GAP = 6;

	private static final Identifier EFFECT = DyeUtils.id("dye_roulette");

	private final Screen screen;
	private final DyeIcons.Dye winner;
	private final int multiplier;
	private final List<DyeIcons.Dye> cards;
	private final long startedAt;
	private final long duration;
	private final int offset;

	// Monotonic, so a frame that jumps several cards clicks once rather than not at all.
	private int lastSound = -1;

	private DyeRoulette(Screen screen, DyeIcons.Dye winner, int multiplier, long duration,
			RandomGenerator random) {
		this.screen = screen;
		this.winner = winner;
		this.multiplier = multiplier;
		this.duration = duration;
		this.startedAt = Util.getMillis();
		this.offset = RouletteSpin.randomOffset(random);

		// The winner is left in the filler pool as well, so it can scroll past before it lands.
		this.cards = RouletteStrip.build(winner, DyeIcons.pool(), random);
	}

	static DyeRoulette start(Screen screen, DyeIcons.Dye winner, int multiplier, long duration) {
		return new DyeRoulette(screen, winner, multiplier, duration, ThreadLocalRandom.current());
	}

	boolean isFor(Screen other) {
		return screen == other;
	}

	String winnerName() {
		return winner.name();
	}

	/** @return whether the reveal is still playing. */
	boolean draw(GuiGraphicsExtractor graphics) {
		float raw = RouletteSpin.rawProgress(Util.getMillis() - startedAt, duration);
		float progress = RouletteSpin.progress(raw);

		if (RouletteSpin.finished(raw, progress)) return false;

		float endOffset = RouletteSpin.endOffset(progress);

		click(endOffset);

		// Its own stratum, so nothing already drawn can sort itself in among the cards. Within one
		// stratum the renderer batches by pipeline and texture rather than honouring draw order.
		graphics.nextStratum();

		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), SCRIM);

		int stripX = RouletteSpin.stripX(graphics.guiWidth(), endOffset, offset);
		int stripY = RouletteSpin.stripY(graphics.guiHeight());
		int last = RouletteSpin.lastVisible(stripX, graphics.guiWidth());

		Matrix3x2fStack pose = graphics.pose();

		for (int index = RouletteSpin.firstVisible(stripX); index <= last; index++) {
			DyeIcons.Dye card = cards.get(index);

			pose.pushMatrix();
			pose.translate(stripX + RouletteSpin.cardX(index), stripY);
			pose.scale(RouletteSpin.ITEM_SCALE, RouletteSpin.ITEM_SCALE);

			DyeCard.render(graphics, card.stack(), card.colour());

			pose.popMatrix();
		}

		marker(graphics);

		applyPostEffect(graphics);

		// Drawn after the effect is armed, so it lands in the fresh stratum on top and stays sharp.
		if (RouletteSpin.showWinner(progress)) announce(graphics, progress);

		return true;
	}

	// Everything drawn so far becomes the post chain's input; everything after it lands in a new stratum
	// on top. The zero-alpha fill is what forces that stratum to exist.
	private static void applyPostEffect(GuiGraphicsExtractor graphics) {
		GuiRenderState state = ((GuiGraphicsAccessor) graphics).dyeutils$guiRenderState();

		if (!(state instanceof RoulettePostEffect armed)) return;

		graphics.nextStratum();

		armed.dyeutils$armPostEffect(EFFECT);

		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0);
	}

	// An arrowhead above the strip, pointing down at the card it landed on. SkyOcean puts a red bar under
	// the row; above and white reads as a pointer rather than as something that went wrong, and either
	// way it stays off the icons.
	private static void marker(GuiGraphicsExtractor graphics) {
		int centre = graphics.guiWidth() / 2;
		int tip = RouletteSpin.stripY(graphics.guiHeight()) - MARKER_GAP;

		for (int row = 0; row < MARKER_ROWS; row++) {
			// Widest at the top, one pixel at the bottom, with the point touching the top of the cards.
			int half = MARKER_ROWS - 1 - row;
			int y = tip - MARKER_ROWS + row;

			graphics.fill(centre - half - 1, y, centre + half + 2, y + 1, MARKER_SHADOW);
			graphics.fill(centre - half, y, centre + half + 1, y + 1, MARKER);
		}
	}

	// One click per card crossing the marker: rapid at the start, a crawl by the end.
	private void click(float endOffset) {
		int index = RouletteSpin.soundIndex(endOffset);
		if (index <= lastSound) return;

		lastSound = index;

		Minecraft.getInstance().getSoundManager()
				.play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 2.0F, 1.0F));
	}

	private void announce(GuiGraphicsExtractor graphics, float progress) {
		Component label = Component.literal(winner.name()).withColor(winner.colour())
				.append(Component.literal("  " + multiplier + "x")
						.withStyle(multiplier >= 3 ? ChatFormatting.GOLD : ChatFormatting.GREEN));

		float scale = RouletteSpin.winnerScale(progress);
		Font font = Minecraft.getInstance().font;

		// Anchored off the bottom of the text rather than the top, so as it grows from 1x to 3x it rises
		// away from the cards instead of growing down into them. The marker sits in between, so its
		// height comes out of the budget too or the name lands on top of the arrow.
		float baseline = RouletteSpin.stripY(graphics.guiHeight())
				- MARKER_ROWS - MARKER_GAP - (WINNER_TEXT_GAP + font.lineHeight) * scale;

		Matrix3x2fStack pose = graphics.pose();

		pose.pushMatrix();
		pose.translate(graphics.guiWidth() / 2f, baseline);
		pose.scale(scale, scale);

		graphics.centeredText(font, label, 0, 0, 0xFFFFFFFF);

		pose.popMatrix();
	}
}
