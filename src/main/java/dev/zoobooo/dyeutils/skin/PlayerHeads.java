package dev.zoobooo.dyeutils.skin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.config.DyeUtilsConfig;
import dev.zoobooo.dyeutils.util.Ign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.PlayerSkin;

import org.slf4j.Logger;

// SkinManager reads the textures property off the profile locally, so attaching it ourselves
// makes the whole vanilla pipeline -- download, cache, legacy skin conversion -- run unchanged.
public final class PlayerHeads {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int FAILURE_LIMIT = 5;

	private static final Map<String, Head> HEADS = new ConcurrentHashMap<>();

	private static volatile boolean offline;
	private static int failures;

	private PlayerHeads() {
	}

	public static Supplier<PlayerSkin> lookup(String ign) {
		Head head = head(ign);

		return head::skin;
	}

	public static void prefetch(Collection<String> igns) {
		if (offline) return;

		List<String> pending = new ArrayList<>();

		for (String ign : igns) {
			String name = ign.trim();
			if (!Ign.isValid(name)) continue;

			// Claimed in the same step, so two screens opening at once cannot ask twice.
			if (head(name).claim()) pending.add(name);
		}

		if (pending.isEmpty()) return;

		Util.nonCriticalIoPool().execute(() -> resolve(pending));
	}

	private static Head head(String ign) {
		return HEADS.computeIfAbsent(ign.trim().toLowerCase(Locale.ROOT), key -> new Head(ign.trim()));
	}

	private static void resolve(List<String> names) {
		Map<String, UUID> ids = MojangProfiles.ids(names);

		if (ids.isEmpty()) {
			noteFailure();

			return;
		}

		noteSuccess();

		for (String name : names) {
			UUID id = ids.get(name.toLowerCase(Locale.ROOT));
			if (id == null) continue;

			Optional<MojangProfiles.Textures> textures = MojangProfiles.textures(id);
			if (textures.isEmpty()) continue;

			apply(name, id, textures.get());
		}
	}

	private static void apply(String name, UUID id, MojangProfiles.Textures textures) {
		Multimap<String, Property> properties = ArrayListMultimap.create();
		properties.put("textures", new Property("textures", textures.value(), textures.signature()));

		GameProfile profile = new GameProfile(id, name, new PropertyMap(properties));

		Minecraft client = Minecraft.getInstance();

		client.execute(() -> head(name).resolved(
				client.getSkinManager().createLookup(profile, /* requireSecure = */ false)));
	}

	private static synchronized void noteFailure() {
		if (++failures < FAILURE_LIMIT) return;

		offline = true;
		LOGGER.warn("[DyeUtils] Player head lookups have failed {} times in a row, so they are off for "
				+ "this session. Heads will show as the default skin.", FAILURE_LIMIT);
	}

	private static synchronized void noteSuccess() {
		failures = 0;
	}

	private static final class Head {
		private final Supplier<PlayerSkin> fallback;

		private volatile Supplier<PlayerSkin> resolved;
		private volatile boolean asked;

		Head(String name) {
			PlayerSkin defaultSkin = DefaultPlayerSkin.get(offlineId(name));

			this.fallback = () -> defaultSkin;
		}

		PlayerSkin skin() {
			Supplier<PlayerSkin> current = resolved;

			return current == null ? fallback.get() : current.get();
		}

		synchronized boolean claim() {
			if (asked) return false;

			asked = true;

			return true;
		}

		void resolved(Supplier<PlayerSkin> skin) {
			resolved = skin;
		}
	}

	private static UUID offlineId(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
	}
}
