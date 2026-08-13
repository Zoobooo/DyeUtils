package dev.zoobooo.dyeutils.vincent;

import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

// Everything the reveal needs is written on the dye itself, so none of this has to know the date. The
// boost line names the multiplier and the SkyBlock year together, which is what lets a stored year that
// no longer matches stand in for "Vincent has picked again".
final class DyeLore {
	// Hypixel wraps the sentence, so it arrives as "This dye is 2x as common during" with
	// "SkyBlock Year 507!" on the line below. The lore is flattened before matching for that reason.
	private static final Pattern BOOST =
			Pattern.compile("This dye is ([0-9]{1,2})x as common during SkyBlock Year ([0-9]{1,5})!");

	private static final Pattern HEX = Pattern.compile("Hex #([0-9A-Fa-f]{6})");

	// Animated dyes describe themselves as animating "between #048F95 and #088D97" with no Hex line
	// at all, so the first colour mentioned anywhere is the second-best answer.
	private static final Pattern ANY_HEX = Pattern.compile("#([0-9A-Fa-f]{6})");

	// Only ever present on an inventory dye. Compendium entries carry no rarity line, which is why the
	// hex is the primary colour source and this is only the fallback.
	private static final Pattern RARITY = Pattern.compile(
			"(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|VERY SPECIAL|SPECIAL|DIVINE) DYE");

	private static final Pattern FORMATTING = Pattern.compile("§.");

	private static final Pattern RUN_OF_SPACES = Pattern.compile(" {2,}");

	private static final String DYE_SUFFIX = " Dye";

	// Vincent's bucket accessory also ends in " Dye", and sits in the same menu as the dyes it is named
	// after.
	private static final String BUCKET_PREFIX = "Bucket of";

	static final int DEFAULT_COLOUR = 0xFFFFFF;

	private DyeLore() {
	}

	record Boost(int multiplier, int year) {
	}

	static String strip(@Nullable String text) {
		if (text == null) return "";

		return FORMATTING.matcher(text).replaceAll("");
	}

	// Joined rather than searched line by line, because the boost sentence spans two lines and where it
	// breaks moves with the length of the dye's name.
	static String flatten(List<String> lore) {
		StringBuilder joined = new StringBuilder();

		for (String line : lore) {
			String stripped = strip(line).trim();
			if (stripped.isEmpty()) continue;

			if (!joined.isEmpty()) joined.append(' ');

			joined.append(stripped);
		}

		return RUN_OF_SPACES.matcher(joined).replaceAll(" ");
	}

	static @Nullable Boost boost(List<String> lore) {
		Matcher matcher = BOOST.matcher(flatten(lore));
		if (!matcher.find()) return null;

		try {
			return new Boost(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
		} catch (NumberFormatException e) {
			// Unreachable while the digit caps above stay inside int, but a silent miss beats a throw in
			// a render path.
			return null;
		}
	}

	static OptionalInt hex(List<String> lore) {
		Matcher matcher = HEX.matcher(flatten(lore));
		if (!matcher.find()) return OptionalInt.empty();

		return OptionalInt.of(Integer.parseInt(matcher.group(1), 16));
	}

	static OptionalInt anyHex(List<String> lore) {
		Matcher matcher = ANY_HEX.matcher(flatten(lore));
		if (!matcher.find()) return OptionalInt.empty();

		return OptionalInt.of(Integer.parseInt(matcher.group(1), 16));
	}

	static int rarityColour(List<String> lore) {
		Matcher matcher = RARITY.matcher(flatten(lore));
		if (!matcher.find()) return DEFAULT_COLOUR;

		return switch (matcher.group(1)) {
			case "UNCOMMON" -> 0x55FF55;
			case "RARE" -> 0x5555FF;
			case "EPIC" -> 0xAA00AA;
			case "LEGENDARY" -> 0xFFAA00;
			case "MYTHIC" -> 0xFF55FF;
			case "SPECIAL", "VERY SPECIAL" -> 0xFF5555;
			case "DIVINE" -> 0x55FFFF;
			default -> DEFAULT_COLOUR;
		};
	}

	// The accent a roulette card is drawn in: the dye's own colour, so each card is the colour of the
	// thing it shows, falling back to rarity and then to white.
	static int colour(List<String> lore) {
		OptionalInt hex = hex(lore);
		if (hex.isPresent()) return hex.getAsInt();

		OptionalInt any = anyHex(lore);
		if (any.isPresent()) return any.getAsInt();

		return rarityColour(lore);
	}

	static boolean isDyeName(@Nullable String name) {
		String stripped = strip(name).trim();

		return stripped.endsWith(DYE_SUFFIX) && !stripped.startsWith(BUCKET_PREFIX);
	}

}
