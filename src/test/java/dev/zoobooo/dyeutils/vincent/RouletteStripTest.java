package dev.zoobooo.dyeutils.vincent;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteStripTest {
	private static final List<String> POOL = List.of("Celadon", "Byzantium", "Aquamarine", "Bone", "Jade");

	@Test
	void theStripIsFiftyCardsLong() {
		assertEquals(RouletteSpin.CARDS, RouletteStrip.build("winner", POOL, new Random(1L)).size());
	}

	@Test
	void theWinnerSitsWhereTheSpinStops() {
		List<String> strip = RouletteStrip.build("winner", POOL, new Random(2L));

		assertEquals("winner", strip.get(RouletteSpin.WINNER_INDEX));
	}

	@Test
	void anEmptyPoolStillProducesAStrip() {
		// A fresh install has learned no dyes yet, and the reveal still has to work.
		List<String> strip = RouletteStrip.build("winner", List.of(), new Random(3L));

		assertEquals(RouletteSpin.CARDS, strip.size());
		assertTrue(strip.stream().allMatch("winner"::equals));
	}

	@Test
	void theFillerIsDrawnFromThePool() {
		List<String> strip = RouletteStrip.build("winner", POOL, new Random(4L));

		for (int index = 0; index < strip.size(); index++) {
			if (index == RouletteSpin.WINNER_INDEX) continue;

			assertTrue(POOL.contains(strip.get(index)), "unexpected filler: " + strip.get(index));
		}
	}

	@Test
	void theSameSeedGivesTheSameStrip() {
		assertEquals(RouletteStrip.build("w", POOL, new Random(5L)),
				RouletteStrip.build("w", POOL, new Random(5L)));
	}
}
