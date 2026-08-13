package dev.zoobooo.dyeutils.vincent;

import java.util.random.RandomGenerator;

// The scroll curve of the reveal, ported from SkyOcean's dungeon chest case opening
// (https://github.com/meowdding/SkyOcean, features/gambling/dungeons/DungeonGamblingRenderer.kt), which
// is MIT licensed:
//
//   Copyright (c) meowdding and the SkyOcean contributors
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//   associated documentation files (the "Software"), to deal in the Software without restriction,
//   including without limitation the rights to use, copy, modify, merge, publish, distribute,
//   sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions:
//
//   The above copyright notice and this permission notice shall be included in all copies or
//   substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//   NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//   DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
//   OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
// The arithmetic is kept apart from the drawing so the curve can be tested in a plain JVM, the same way
// InvitePacer holds the invite pacing.
final class RouletteSpin {
	// Two, where SkyOcean uses four. Their cards hold 16x16 item sprites; a SkyBlock dye is a player head
	// whose visible faces are only 8x8 texels of a 64x64 skin, and that is the whole source -- there is no
	// higher resolution version of the art anywhere. Magnifying it further only makes the texels bigger,
	// and the player's own GUI scale multiplies whatever is chosen here on top.
	static final int ITEM_SCALE = 2;
	static final int CARDS = 50;

	// Ten cards of runway past the winner, so the strip never runs out on the right hand side.
	static final int WINNER_INDEX = CARDS - 10;

	static final int CARD_WIDTH = 24;
	static final int CARD_HEIGHT = 18;

	// The gap is added after the card is scaled, on purpose: cards sit 5 real pixels apart rather than
	// 5 scaled ones.
	static final int ITEM_GAP = 5;
	static final int FULL_CARD_WIDTH = CARD_WIDTH * ITEM_SCALE + ITEM_GAP;
	static final int FULL_CARD_HEIGHT = CARD_HEIGHT * ITEM_SCALE;

	// easeInOutCirc barely moves for its first tenth, so the spin starts a quarter of the way in and is
	// already going on the first frame. The side effect is the useful part: it arrives a quarter early
	// too, and that tail becomes the hold on the winner.
	static final float HEAD_START = 0.25f;
	static final float WINNER_TEXT_AT = 0.96f;

	static final float WINNER_TEXT_SCALE = 3f;

	// SkyOcean's landing, in units of the scale: the marker ends up a third of the way into the winning
	// card, jittered by up to a sixth either way, so it stops somewhere in a band rather than in the
	// same place every time.
	private static final int MARKER_INSET = 8 * ITEM_SCALE;
	private static final int MARKER_JITTER = 4 * ITEM_SCALE;

	private RouletteSpin() {
	}

	static float rawProgress(long elapsedMillis, long durationMillis) {
		if (durationMillis <= 0L) return 1f;

		return (float) elapsedMillis / durationMillis;
	}

	static float progress(float rawProgress) {
		return Math.clamp(rawProgress + HEAD_START, 0f, 1f);
	}

	// easeInOutCirc. Clamped because the square roots go imaginary either side of the unit interval.
	static float ease(float t) {
		float clamped = Math.clamp(t, 0f, 1f);

		if (clamped < 0.5f) return (float) ((1 - Math.sqrt(1 - Math.pow(2 * clamped, 2))) / 2);

		return (float) ((Math.sqrt(1 - Math.pow(-2 * clamped + 2, 2)) + 1) / 2);
	}

	static float endOffset(float progress) {
		return WINNER_INDEX * FULL_CARD_WIDTH * ease(progress);
	}

	// One click per card crossing the marker, so it rattles at the start and slows to a crawl. Callers
	// keep the last index and only play a sound when this rises, which also swallows a frame that
	// happens to jump several cards at once.
	static int soundIndex(float endOffset) {
		return (int) endOffset / FULL_CARD_WIDTH;
	}

	static boolean finished(float rawProgress, float progress) {
		return progress >= WINNER_TEXT_AT && rawProgress >= 1f;
	}

	static boolean showWinner(float progress) {
		return progress >= WINNER_TEXT_AT;
	}

	static float winnerScale(float progress) {
		float span = (progress - WINNER_TEXT_AT) / (1f - WINNER_TEXT_AT);

		return 1f + Math.clamp(span, 0f, 1f) * (WINNER_TEXT_SCALE - 1f);
	}

	static int randomOffset(RandomGenerator random) {
		int magnitude = MARKER_JITTER + random.nextInt(MARKER_JITTER);

		return random.nextBoolean() ? magnitude : -magnitude;
	}

	static int stripX(int guiWidth, float endOffset, int randomOffset) {
		return Math.round(guiWidth / 2f - endOffset - (MARKER_INSET + randomOffset));
	}

	static int stripY(int guiHeight) {
		return (guiHeight - FULL_CARD_HEIGHT) / 2;
	}

	static int cardX(int index) {
		return index * FULL_CARD_WIDTH;
	}

	// SkyOcean draws all fifty cards. Six or so are ever on screen, and each one is a player head with
	// its own model and skin, so the rest are worth skipping.
	static int firstVisible(int stripX) {
		return Math.clamp((-stripX - CARD_WIDTH * ITEM_SCALE) / FULL_CARD_WIDTH - 1, 0, CARDS - 1);
	}

	static int lastVisible(int stripX, int guiWidth) {
		return Math.clamp((guiWidth - stripX) / FULL_CARD_WIDTH + 1, 0, CARDS - 1);
	}


}
