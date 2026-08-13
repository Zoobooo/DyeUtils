package dev.zoobooo.dyeutils.vincent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteSpinTest {
	private static final int GUI_WIDTH = 854;

	@Test
	void theGapBetweenCardsEscapesTheScale() {
		// The point of the arithmetic: 5 real pixels between cards, not 5 scaled ones.
		assertEquals(RouletteSpin.CARD_WIDTH * RouletteSpin.ITEM_SCALE + RouletteSpin.ITEM_GAP,
				RouletteSpin.FULL_CARD_WIDTH);
		assertEquals(RouletteSpin.CARD_HEIGHT * RouletteSpin.ITEM_SCALE, RouletteSpin.FULL_CARD_HEIGHT);
	}

	@Test
	void theEaseSpansTheUnitIntervalAndIsClampedOutsideIt() {
		assertEquals(0f, RouletteSpin.ease(0f), 1.0e-6f);
		assertEquals(0.5f, RouletteSpin.ease(0.5f), 1.0e-6f);
		assertEquals(1f, RouletteSpin.ease(1f), 1.0e-6f);
		assertEquals(1f, RouletteSpin.ease(4f), 1.0e-6f);
		assertEquals(0f, RouletteSpin.ease(-4f), 1.0e-6f);
	}

	@Test
	void theEaseNeverGoesBackwards() {
		float previous = -1f;

		for (int step = 0; step <= 1000; step++) {
			float eased = RouletteSpin.ease(step / 1000f);

			assertTrue(eased >= previous, "ease dipped at " + step);
			previous = eased;
		}
	}

	@Test
	void theStripIsAlreadyMovingOnTheFirstFrame() {
		assertEquals(RouletteSpin.HEAD_START, RouletteSpin.progress(0f), 1.0e-6f);
		assertTrue(RouletteSpin.endOffset(RouletteSpin.progress(0f)) > RouletteSpin.FULL_CARD_WIDTH);
	}

	@Test
	void theSpinLandsAtThreeQuartersLeavingTheRestAsTheHold() {
		assertEquals(1f, RouletteSpin.progress(0.75f), 1.0e-6f);
		assertTrue(RouletteSpin.showWinner(RouletteSpin.progress(0.75f)));

		// Landed, but not over: the hold has to run before the reveal is finished.
		assertFalse(RouletteSpin.finished(0.75f, RouletteSpin.progress(0.75f)));
		assertTrue(RouletteSpin.finished(1f, RouletteSpin.progress(1f)));
	}

	@Test
	void theWinningCardTravelsExactlyItsOwnIndex() {
		assertEquals(RouletteSpin.WINNER_INDEX * RouletteSpin.FULL_CARD_WIDTH,
				RouletteSpin.endOffset(1f), 1.0e-3f);
	}

	@Test
	void theMarkerAlwaysEndsUpSomewhereOnTheWinningCard() {
		Random random = new Random(1234L);
		Set<Integer> landings = new HashSet<>();

		for (int attempt = 0; attempt < 500; attempt++) {
			int offset = RouletteSpin.randomOffset(random);
			int stripX = RouletteSpin.stripX(GUI_WIDTH, RouletteSpin.endOffset(1f), offset);
			int winnerLeft = stripX + RouletteSpin.cardX(RouletteSpin.WINNER_INDEX);
			int marker = GUI_WIDTH / 2;

			assertTrue(marker >= winnerLeft, "marker fell short of the winning card");
			assertTrue(marker < winnerLeft + RouletteSpin.CARD_WIDTH * RouletteSpin.ITEM_SCALE,
					"marker overran the winning card");
			landings.add(marker - winnerLeft);
		}

		// The point of the jitter is that the card does not stop in the same place twice, not that it
		// avoids the centre -- an offset of +16 lands dead centre and that is fine.
		assertTrue(landings.size() > 10, "the landing barely moved: " + landings);
	}

	@Test
	void theJitterStaysWithinOneBandEitherWay() {
		Random random = new Random(99L);
		boolean sawNegative = false;
		boolean sawPositive = false;

		for (int attempt = 0; attempt < 500; attempt++) {
			int offset = RouletteSpin.randomOffset(random);

			int band = 4 * RouletteSpin.ITEM_SCALE;

			assertTrue(Math.abs(offset) >= band && Math.abs(offset) <= band * 2 - 1,
					"jitter out of band: " + offset);

			sawNegative |= offset < 0;
			sawPositive |= offset > 0;
		}

		assertTrue(sawNegative && sawPositive, "the jitter only ever went one way");
	}

	@Test
	void everyCardBeforeTheWinnerClicksOnceAndTheCountNeverFalls() {
		int clicks = 0;
		int last = 0;

		for (int step = 0; step <= 10_000; step++) {
			float raw = step / 10_000f;
			int index = RouletteSpin.soundIndex(RouletteSpin.endOffset(RouletteSpin.progress(raw)));

			assertTrue(index >= last, "the sound index went backwards");

			if (index > last) clicks += index - last;

			last = index;
		}

		assertEquals(RouletteSpin.WINNER_INDEX, clicks);
	}

	@Test
	void aZeroLengthAnimationIsAlreadyOver() {
		assertEquals(1f, RouletteSpin.rawProgress(0L, 0L), 1.0e-6f);
	}

	@Test
	void theWinningCardIsOnScreenWhenTheSpinStops() {
		int stripX = RouletteSpin.stripX(GUI_WIDTH, RouletteSpin.endOffset(1f), 20);

		assertTrue(RouletteSpin.firstVisible(stripX) <= RouletteSpin.WINNER_INDEX);
		assertTrue(RouletteSpin.lastVisible(stripX, GUI_WIDTH) >= RouletteSpin.WINNER_INDEX);
	}

	@Test
	void cullingNeverLeavesTheStrip() {
		for (int step = 0; step <= 200; step++) {
			int stripX = RouletteSpin.stripX(GUI_WIDTH, RouletteSpin.endOffset(step / 200f), 0);
			int first = RouletteSpin.firstVisible(stripX);
			int last = RouletteSpin.lastVisible(stripX, GUI_WIDTH);

			assertTrue(first >= 0 && first < RouletteSpin.CARDS, "first out of range: " + first);
			assertTrue(last >= 0 && last < RouletteSpin.CARDS, "last out of range: " + last);
			assertTrue(first <= last, "culling inverted at step " + step);
		}
	}

	@Test
	void theWinnerTextGrowsFromOneToThreeOverTheLastSliver() {
		assertEquals(1f, RouletteSpin.winnerScale(RouletteSpin.WINNER_TEXT_AT), 1.0e-6f);
		assertEquals(3f, RouletteSpin.winnerScale(1f), 1.0e-6f);
		assertEquals(2f, RouletteSpin.winnerScale(0.98f), 1.0e-3f);
	}
}
