package dev.zoobooo.dyeutils.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import dev.zoobooo.dyeutils.DyeUtils;
import net.azureaaron.dandelion.api.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

public class DyeUtilsConfig {
	private static final Pattern VALID_IGN = Pattern.compile("^\\w{3,16}$");

	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(DyeUtils.NAMESPACE + ".json");
	private static final ConfigManager<DyeUtilsConfig> MANAGER =
			ConfigManager.create(DyeUtilsConfig.class, CONFIG_FILE, UnaryOperator.identity());

	public List<String> partyMembers = new ArrayList<>();
	public String inviteCommandPrefix = "/p invite ";
	public boolean hideCommandsFromChat = true;
	public boolean bacteSkin = true;
	public boolean autoDisband = true;

	public static ConfigManager<DyeUtilsConfig> manager() {
		return MANAGER;
	}

	public static DyeUtilsConfig get() {
		return MANAGER.instance();
	}

	public static void load() {
		MANAGER.load();
	}

	public static void setPartyMembers(List<String> members) {
		// get() returns Dandelion's patched copy, which must never be written to.
		MANAGER.unpatchedInstance().partyMembers = new ArrayList<>(members);
		MANAGER.save();
	}

	public static boolean isValidIgn(String name) {
		return name != null && VALID_IGN.matcher(name.trim()).matches();
	}

	public static List<String> sanitise(List<String> names) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> cleaned = new ArrayList<>(names.size());

		for (String name : names) {
			if (name == null) continue;
			String trimmed = name.trim();

			if (VALID_IGN.matcher(trimmed).matches() && seen.add(trimmed.toLowerCase(Locale.ROOT))) {
				cleaned.add(trimmed);
			}
		}

		return cleaned;
	}
}
