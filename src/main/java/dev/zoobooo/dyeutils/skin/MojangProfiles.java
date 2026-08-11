package dev.zoobooo.dyeutils.skin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

// mowojang is a public read-only mirror of Mojang's profile API. Mojang rate limits profile
// lookups to roughly one per player per minute, which a screenful of names hits at once.
final class MojangProfiles {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String BASE = "https://mowojang.matdoes.dev/";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	private static final Gson GSON = new Gson();

	private static HttpClient client;

	record Textures(String value, @Nullable String signature) {}

	private MojangProfiles() {
	}

	static Map<String, UUID> ids(List<String> names) {
		if (names.isEmpty()) return Map.of();

		return parseIds(post(BASE, GSON.toJson(names)));
	}

	// Takes an id only; a username is an error, which is why a head costs two requests.
	static Optional<Textures> textures(UUID id) {
		return parseTextures(get(BASE + "session/minecraft/profile/" + undashed(id) + "?unsigned=false"));
	}

	// Names that do not exist are missing from the response rather than reported, so match by
	// name and never by position.
	static Map<String, UUID> parseIds(@Nullable String json) {
		Map<String, UUID> resolved = new HashMap<>();

		JsonElement body = parse(json);
		if (body == null || !body.isJsonArray()) return resolved;

		for (JsonElement element : body.getAsJsonArray()) {
			if (!element.isJsonObject()) continue;

			JsonObject entry = element.getAsJsonObject();

			String name = string(entry, "name");
			String rawId = string(entry, "id");
			if (name == null || rawId == null) continue;

			UUID id = parseUuid(rawId);
			if (id == null) continue;

			resolved.put(name.toLowerCase(Locale.ROOT), id);
		}

		return resolved;
	}

	static Optional<Textures> parseTextures(@Nullable String json) {
		JsonElement body = parse(json);
		if (body == null || !body.isJsonObject()) return Optional.empty();

		JsonObject profile = body.getAsJsonObject();
		if (!profile.has("properties") || !profile.get("properties").isJsonArray()) return Optional.empty();

		for (JsonElement element : profile.getAsJsonArray("properties")) {
			if (!element.isJsonObject()) continue;

			JsonObject property = element.getAsJsonObject();
			if (!"textures".equals(string(property, "name"))) continue;

			String value = string(property, "value");
			if (value == null) continue;

			return Optional.of(new Textures(value, string(property, "signature")));
		}

		return Optional.empty();
	}

	/** Mojang writes ids without dashes; UUID.fromString insists on them. */
	static @Nullable UUID parseUuid(String raw) {
		String value = raw.trim();

		try {
			if (value.length() == 32) {
				value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16)
						+ "-" + value.substring(16, 20) + "-" + value.substring(20);
			}

			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static @Nullable JsonElement parse(@Nullable String json) {
		if (json == null || json.isBlank()) return null;

		try {
			return JsonParser.parseString(json);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static @Nullable String string(JsonObject object, String key) {
		return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : null;
	}

	private static @Nullable String get(String url) {
		return send(HttpRequest.newBuilder(URI.create(url)).GET());
	}

	private static @Nullable String post(String url, String json) {
		return send(HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json)));
	}

	private static @Nullable String send(HttpRequest.Builder request) {
		try {
			HttpResponse<String> response = client().send(
					request.timeout(REQUEST_TIMEOUT).header("User-Agent", userAgent()).build(),
					HttpResponse.BodyHandlers.ofString());

			// 404 is an ordinary answer: nobody owns that name.
			if (response.statusCode() != 200) return null;

			return response.body();
		} catch (Exception e) {
			LOGGER.warn("[DyeUtils] Player head lookup failed: {}", e.toString());

			return null;
		}
	}

	private static synchronized HttpClient client() {
		if (client == null) {
			client = HttpClient.newBuilder()
					.connectTimeout(CONNECT_TIMEOUT)
					.followRedirects(HttpClient.Redirect.NORMAL)
					.build();
		}

		return client;
	}

	/** So whoever runs the service can see what the traffic is. */
	private static String userAgent() {
		String version = FabricLoader.getInstance().getModContainer("dyeutils")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");

		return "dyeutils/" + version;
	}

	private static String undashed(UUID id) {
		return id.toString().replace("-", "");
	}
}
