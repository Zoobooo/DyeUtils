package dev.zoobooo.dyeutils.party;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.zoobooo.dyeutils.config.DyeUtilsConfig;

public class PartyList {
	public static List<String> get() {
		return List.copyOf(DyeUtilsConfig.get().partyMembers);
	}

	public static void set(List<String> members) {
		DyeUtilsConfig.setPartyMembers(members);
	}

	public static boolean contains(String ign) {
		return indexOf(get(), ign) >= 0;
	}

	public static boolean add(String ign) {
		String trimmed = ign.trim();
		if (!DyeUtilsConfig.isValidIgn(trimmed)) return false;

		List<String> members = new ArrayList<>(get());
		if (indexOf(members, trimmed) >= 0) return false;

		members.add(trimmed);
		set(members);

		return true;
	}

	public static void clear() {
		set(List.of());
	}

	public static boolean remove(String ign) {
		List<String> members = new ArrayList<>(get());
		int index = indexOf(members, ign);
		if (index < 0) return false;

		members.remove(index);
		set(members);

		return true;
	}

	private static int indexOf(List<String> members, String ign) {
		String needle = ign.trim().toLowerCase(Locale.ROOT);

		for (int i = 0; i < members.size(); i++) {
			if (members.get(i).trim().toLowerCase(Locale.ROOT).equals(needle)) return i;
		}

		return -1;
	}
}
