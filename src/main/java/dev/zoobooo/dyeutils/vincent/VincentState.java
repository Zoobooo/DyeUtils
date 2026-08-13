package dev.zoobooo.dyeutils.vincent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;

// Deliberately not part of dyeutils.json: that file holds settings the user chose, this one holds what
// the game told us. Losing it costs a little roulette variety and one repeated reveal, nothing more, so
// it is written on a best-effort basis and never blocks the feature when it cannot be read.
final class VincentState {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Path FILE =
			FabricLoader.getInstance().getConfigDir().resolve(DyeUtils.NAMESPACE + "-vincent.json");

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final VincentState INSTANCE = new VincentState();

	private Data data = new Data();
	private boolean loaded;

	private VincentState() {
	}

	static VincentState get() {
		INSTANCE.load();

		return INSTANCE;
	}

	private void load() {
		if (loaded) return;

		loaded = true;

		if (!Files.isRegularFile(FILE)) return;

		try {
			Data read = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
			if (read != null) data = read;
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("[DyeUtils] Could not read {}, starting from empty.", FILE.getFileName(), e);
		}

		// A hand-edited file can leave any of these null, and every caller below assumes they are not.
		if (data.revealed == null) data.revealed = new ArrayList<>();
		if (data.icons == null) data.icons = new LinkedHashMap<>();

		data.icons.values().removeIf(icon -> icon == null || icon.texture == null);
	}

	void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.warn("[DyeUtils] Could not write {}.", FILE.getFileName(), e);
		}
	}

	// Vincent picks again every SkyBlock year, and the year is written on the dyes themselves, so a year
	// that does not match the stored one is all the signal needed to hide them all again.
	boolean syncYear(int year) {
		if (data.year == year) return false;

		data.year = year;
		data.revealed.clear();

		return true;
	}

	int year() {
		return data.year;
	}

	boolean isRevealed(String dye) {
		return data.revealed.contains(dye);
	}

	boolean reveal(String dye) {
		if (data.revealed.contains(dye)) return false;

		data.revealed.add(dye);

		return true;
	}

	void hideAll() {
		data.revealed.clear();
	}

	boolean putIcon(String name, String texture, int colour) {
		Icon existing = data.icons.get(name);
		if (existing != null && texture.equals(existing.texture) && existing.colour == colour) return false;

		data.icons.put(name, new Icon(texture, colour));

		return true;
	}

	Map<String, Icon> icons() {
		return Collections.unmodifiableMap(data.icons);
	}

	int iconCount() {
		return data.icons.size();
	}

	// Mutable and Gson-shaped rather than records, so a field added later does not break an older file.
	private static final class Data {
		int year;
		List<String> revealed = new ArrayList<>();
		Map<String, Icon> icons = new LinkedHashMap<>();
	}

	static final class Icon {
		String texture;
		int colour;

		Icon() {
		}

		Icon(String texture, int colour) {
			this.texture = texture;
			this.colour = colour;
		}

		String texture() {
			return texture;
		}

		int colour() {
			return colour;
		}
	}
}
