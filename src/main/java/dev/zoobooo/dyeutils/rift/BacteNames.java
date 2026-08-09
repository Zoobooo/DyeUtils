package dev.zoobooo.dyeutils.rift;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BacteNames {
	private static final Set<String> PHASES = Set.of("B", "Ba", "Bac", "Bact", "Bacte");

	// The level prefix and health suffix make a one-letter name safe to match: "[Lv10] B 1,000/1,000"
	// is unmistakably a mob tag, a bare "B" would not be.
	private static final Pattern MOB_TAG = Pattern.compile("\\[Lv\\d+]\\s+(\\S+)\\s+[\\d,]+/[\\d,]+");

	private BacteNames() {
	}

	public static boolean isBacte(String nameTag) {
		if (nameTag == null) return false;

		Matcher matcher = MOB_TAG.matcher(nameTag);

		return matcher.find() && PHASES.contains(matcher.group(1));
	}
}
