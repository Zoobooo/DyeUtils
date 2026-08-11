package dev.zoobooo.dyeutils.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class Ign {
	private static final Pattern VALID = Pattern.compile("^\\w{3,16}$");

	private Ign() {
	}

	public static boolean isValid(String name) {
		return name != null && VALID.matcher(name.trim()).matches();
	}

	public static int indexOf(List<String> names, String ign) {
		String needle = ign.trim().toLowerCase(Locale.ROOT);

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			if (name != null && name.trim().toLowerCase(Locale.ROOT).equals(needle)) return i;
		}

		return -1;
	}

	public static boolean contains(List<String> names, String ign) {
		return indexOf(names, ign) >= 0;
	}

	public static List<String> sanitise(List<String> names) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> cleaned = new ArrayList<>(names.size());

		for (String name : names) {
			if (name == null) continue;
			String trimmed = name.trim();

			if (isValid(trimmed) && seen.add(trimmed.toLowerCase(Locale.ROOT))) {
				cleaned.add(trimmed);
			}
		}

		return cleaned;
	}
}
