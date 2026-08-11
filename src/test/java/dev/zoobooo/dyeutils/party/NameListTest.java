package dev.zoobooo.dyeutils.party;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameListTest {
	private final List<String> storage = new ArrayList<>();
	private final NameList names = new NameList(() -> storage, values -> {
		storage.clear();
		storage.addAll(values);
	});

	@Test
	void addsAValidName() {
		assertTrue(names.add("GlactiX"));
		assertEquals(List.of("GlactiX"), names.get());
	}

	@Test
	void refusesAMalformedName() {
		assertFalse(names.add("ab"));
		assertTrue(names.get().isEmpty());
	}

	@Test
	void refusesADuplicateWhateverItsCase() {
		assertTrue(names.add("GlactiX"));
		assertFalse(names.add("glactix"));

		assertEquals(List.of("GlactiX"), names.get(), "the first spelling is the one that stays");
	}

	@Test
	void containsIgnoresCaseAndSurroundingSpace() {
		names.add("GlactiX");

		assertTrue(names.contains("glactix"));
		assertTrue(names.contains("  GLACTIX  "));
		assertFalse(names.contains("someoneelse"));
	}

	@Test
	void removesRegardlessOfCase() {
		names.add("GlactiX");

		assertTrue(names.remove("GLACTIX"));
		assertTrue(names.get().isEmpty());
	}

	@Test
	void removingSomethingAbsentChangesNothing() {
		names.add("GlactiX");

		assertFalse(names.remove("someoneelse"));
		assertEquals(List.of("GlactiX"), names.get());
	}

	@Test
	void togglePutsANameOnThenTakesItOff() {
		assertTrue(names.toggle("GlactiX"), "first press adds");
		assertEquals(List.of("GlactiX"), names.get());

		assertFalse(names.toggle("glactix"), "second press removes, matching case-insensitively");
		assertTrue(names.get().isEmpty());
	}

	@Test
	void toggleReportsFailureForAMalformedName() {
		assertFalse(names.toggle("ab"));
		assertTrue(names.get().isEmpty());
	}

	@Test
	void keepsInsertionOrder() {
		names.add("aaa");
		names.add("bbb");
		names.add("ccc");

		assertEquals(List.of("aaa", "bbb", "ccc"), names.get());
	}

	@Test
	void getIsNotAWayIntoTheStorage() {
		names.add("GlactiX");

		List<String> copy = names.get();

		assertThrows(UnsupportedOperationException.class, () -> copy.add("sneaky"));
		assertEquals(List.of("GlactiX"), names.get());
	}
}
