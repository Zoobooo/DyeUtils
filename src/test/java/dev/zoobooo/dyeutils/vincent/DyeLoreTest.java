package dev.zoobooo.dyeutils.vincent;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DyeLoreTest {
	// Byzantium as it reads in the menu, wrapped across two lines exactly as Hypixel sends it.
	private static final List<String> BYZANTIUM = List.of(
			"\u00a78Hex #702963",
			"",
			"\u00a77Dropped from killing a \u00a75Voidgloom",
			"\u00a75Seraph\u00a77.",
			"",
			"\u00a7eThis dye is \u00a7a2x\u00a7e as common during",
			"\u00a7dSkyBlock Year \u00a75507\u00a7e!",
			"",
			"\u00a77Global drops: \u00a7e134",
			"\u00a77You've dropped: \u00a760");

	private static final List<String> CELADON = List.of(
			"\u00a78Hex #ACE1AF",
			"",
			"\u00a7eThis dye is \u00a7a3x\u00a7e as common during",
			"\u00a7dSkyBlock Year \u00a75507\u00a7e!");

	private static final List<String> UNBOOSTED = List.of(
			"\u00a78Hex #50C878",
			"",
			"\u00a77Dropped from mining \u00a7aCritters\u00a77.",
			"",
			"\u00a77Global drops: \u00a7e12");

	@Test
	void readsATwoTimesBoostAndItsYear() {
		DyeLore.Boost boost = DyeLore.boost(BYZANTIUM);

		assertEquals(2, boost.multiplier());
		assertEquals(507, boost.year());
	}

	@Test
	void readsAThreeTimesBoost() {
		assertEquals(3, DyeLore.boost(CELADON).multiplier());
	}

	@Test
	void anUnboostedDyeHasNoBoost() {
		assertNull(DyeLore.boost(UNBOOSTED));
	}

	@Test
	void aHalfWrittenBoostLineIsNotABoost() {
		assertNull(DyeLore.boost(List.of("\u00a7eThis dye is \u00a7a2x\u00a7e as common during")));
		assertNull(DyeLore.boost(List.of("This dye is 2x as common during SkyBlock Year!")));
	}

	@Test
	void theRainbowSpellingOfDyesDoesNotBreakFlattening() {
		// Hypixel colours the word "dyes" one letter at a time on the sign in the same menu.
		assertEquals("Vincent picks dyes that he will use!",
				DyeLore.flatten(List.of("\u00a77Vincent picks \u00a7cd\u00a76y\u00a7ee\u00a7as",
						"\u00a77that he will use!")));
	}

	@Test
	void readsTheHexColour() {
		assertEquals(0xACE1AF, DyeLore.hex(CELADON).getAsInt());
		assertEquals(0x702963, DyeLore.colour(BYZANTIUM));
	}

	@Test
	void aShortHexIsNotAColour() {
		assertTrue(DyeLore.hex(List.of("\u00a78Hex #ACE1A")).isEmpty());
	}

	@Test
	void fallsBackToRarityThenWhite() {
		List<String> inventoryDye = List.of("\u00a77Changes the colour of an armor piece", "",
				"\u00a76\u00a7lLEGENDARY DYE");

		assertEquals(0xFFAA00, DyeLore.colour(inventoryDye));
		assertEquals(DyeLore.DEFAULT_COLOUR, DyeLore.colour(List.of("\u00a77Nothing to go on")));
	}

	@Test
	void theBucketAccessoryIsNotADye() {
		// Slot 48 of the same menu is "Bucket of Dye", which ends in " Dye" like everything else there.
		assertFalse(DyeLore.isDyeName("\u00a76Bucket of Dye"));
		assertTrue(DyeLore.isDyeName("\u00a7aCeladon Dye"));
	}

	@Test
	void anAnimatedDyeStillGetsAColour() {
		// Animated dyes name two colours and carry no Hex line at all, so a hex must never be the thing
		// that makes something count as a dye.
		List<String> animated = List.of(
				"\u00a77Animates the color of an armor piece",
				"\u00a77between \u00a78#21262A\u00a77 and \u00a78#202429\u00a77!",
				"",
				"\u00a7d\u00a7lMYTHIC DYE");

		assertTrue(DyeLore.hex(animated).isEmpty());
		assertEquals(0x21262A, DyeLore.colour(animated));
	}
}
