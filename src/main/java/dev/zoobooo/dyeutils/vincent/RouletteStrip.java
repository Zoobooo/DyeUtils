package dev.zoobooo.dyeutils.vincent;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

// The strip the reveal scrolls: filler either side of the one card that matters, which is planted at the
// index the spin is built to stop on. Kept free of Minecraft so the placement can be tested.
final class RouletteStrip {
	private RouletteStrip() {
	}

	static <T> List<T> build(T winner, List<T> pool, RandomGenerator random) {
		List<T> strip = new ArrayList<>(RouletteSpin.CARDS);

		for (int index = 0; index < RouletteSpin.CARDS; index++) {
			// An empty pool still has to produce a strip: on a fresh install nothing has been learned
			// yet, so the reveal is the winner over and over rather than nothing at all.
			strip.add(pool.isEmpty() ? winner : pool.get(random.nextInt(pool.size())));
		}

		strip.set(RouletteSpin.WINNER_INDEX, winner);

		return List.copyOf(strip);
	}
}
