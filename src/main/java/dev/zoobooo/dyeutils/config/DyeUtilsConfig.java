package dev.zoobooo.dyeutils.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import dev.zoobooo.dyeutils.DyeUtils;
import net.azureaaron.dandelion.api.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

public class DyeUtilsConfig {
	private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve(DyeUtils.NAMESPACE + ".json");
	private static final ConfigManager<DyeUtilsConfig> MANAGER =
			ConfigManager.create(DyeUtilsConfig.class, CONFIG_FILE, UnaryOperator.identity());

	public List<String> partyMembers = new ArrayList<>();
	public List<String> favourites = new ArrayList<>();
	public String inviteCommandPrefix = "/p invite ";
	public boolean hideCommandsFromChat = true;
	public boolean bacteSkin = true;
	public boolean autoDisband = true;
	public boolean vincentReveal = true;
	public int vincentRevealSeconds = 3;

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

	public static void setFavourites(List<String> names) {
		// get() returns Dandelion's patched copy, which must never be written to.
		MANAGER.unpatchedInstance().favourites = new ArrayList<>(names);
		MANAGER.save();
	}
}
