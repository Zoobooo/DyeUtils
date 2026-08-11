package dev.zoobooo.dyeutils.party;

import java.util.List;

import dev.zoobooo.dyeutils.config.DyeUtilsConfig;

public class Favourites {
	private static final NameList STORE =
			new NameList(() -> DyeUtilsConfig.get().favourites, DyeUtilsConfig::setFavourites);

	public static List<String> get() {
		return STORE.get();
	}

	public static void set(List<String> names) {
		STORE.set(names);
	}

	public static boolean contains(String ign) {
		return STORE.contains(ign);
	}

	public static boolean add(String ign) {
		return STORE.add(ign);
	}

	public static void clear() {
		STORE.clear();
	}

	public static boolean remove(String ign) {
		return STORE.remove(ign);
	}

	public static boolean toggle(String ign) {
		return STORE.toggle(ign);
	}
}
