package dev.zoobooo.dyeutils.util;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnTest {
	@Test
	void acceptsRealUsernames() {
		assertTrue(Ign.isValid("GlactiX"));
		assertTrue(Ign.isValid("kartoffel84"));
		assertTrue(Ign.isValid("a_b"));
		assertTrue(Ign.isValid("sixteencharacter"));
		assertTrue(Ign.isValid("  padded  "), "surrounding space is trimmed before checking");
	}

	@Test
	void rejectsEverythingElse() {
		assertFalse(Ign.isValid(null));
		assertFalse(Ign.isValid(""));
		assertFalse(Ign.isValid("ab"), "two characters is below Mojang's minimum");
		assertFalse(Ign.isValid("seventeencharacte"), "seventeen characters is above the maximum");
		assertFalse(Ign.isValid("has space"));
		assertFalse(Ign.isValid("dash-not-allowed"));
	}

	@Test
	void sanitiseDropsBlankAndMalformedNames() {
		assertEquals(List.of("GlactiX"), Ign.sanitise(Arrays.asList("", "  ", "ab", "has space", "GlactiX")));
	}

	@Test
	void sanitiseKeepsTheFirstSpellingOfADuplicate() {
		assertEquals(List.of("GlactiX"), Ign.sanitise(List.of("GlactiX", "glactix", "GLACTIX")));
	}

	@Test
	void sanitiseKeepsListOrder() {
		assertEquals(List.of("aaa", "bbb", "ccc"), Ign.sanitise(List.of("aaa", "bbb", "ccc")));
	}

	@Test
	void sanitiseSurvivesNullEntries() {
		assertEquals(List.of("GlactiX"), Ign.sanitise(Arrays.asList(null, "GlactiX", null)));
	}

	@Test
	void containsIgnoresCaseAndSurroundingSpace() {
		List<String> names = List.of("GlactiX", "kartoffel84");

		assertTrue(Ign.contains(names, "glactix"));
		assertTrue(Ign.contains(names, "  GLACTIX  "));
		assertFalse(Ign.contains(names, "someoneelse"));
	}

	@Test
	void indexOfFindsThePosition() {
		assertEquals(1, Ign.indexOf(List.of("aaa", "bbb", "ccc"), "BBB"));
		assertEquals(-1, Ign.indexOf(List.of("aaa"), "zzz"));
	}

	@Test
	void containsToleratesAHalfTypedList() {
		List<String> buffer = Arrays.asList("", null, "ab", "GlactiX");

		assertTrue(Ign.contains(buffer, "glactix"));
		assertFalse(Ign.contains(buffer, "nobody"));
	}
}
