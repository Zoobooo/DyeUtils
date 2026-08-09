package dev.zoobooo.dyeutils.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

final class ReleaseLookup {
	private static final URI RELEASES = URI.create("https://api.github.com/repos/Zoobooo/DyeUtils/releases?per_page=20");

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Pattern ASSET_NAME = Pattern.compile("^" + Pattern.quote(DyeUtils.NAMESPACE) + "-(.+)\\+([^+]+)\\.jar$");

	private static final String DIGEST_PREFIX = "sha256:";

	record Candidate(Version version, String fileName, URI download, long size, @Nullable String sha256) {}

	private ReleaseLookup() {
	}

	static Optional<Candidate> findNewer(HttpClient http, String minecraftVersion, Version current) {
		Candidate best = null;

		try {
			HttpRequest request = HttpRequest.newBuilder(RELEASES)
					.header("Accept", "application/vnd.github+json")
					.header("X-GitHub-Api-Version", "2022-11-28")
					.header("User-Agent", DyeUtils.NAMESPACE + "/" + current.getFriendlyString())
					.timeout(Duration.ofSeconds(10))
					.GET()
					.build();

			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				LOGGER.warn("[DyeUtils] Update check got HTTP {} from GitHub.", response.statusCode());
				return Optional.empty();
			}

			JsonArray releases = JsonParser.parseString(response.body()).getAsJsonArray();

			for (JsonElement element : releases) {
				JsonObject release = element.getAsJsonObject();
				if (bool(release, "draft")) continue;

				for (JsonElement assetElement : release.getAsJsonArray("assets")) {
					Candidate candidate = read(assetElement.getAsJsonObject(), minecraftVersion);

					if (candidate != null && (best == null || candidate.version().compareTo(best.version()) > 0)) {
						best = candidate;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.warn("[DyeUtils] Update check failed: {}", e.toString());
			return Optional.empty();
		}

		if (best == null) {
			LOGGER.info("[DyeUtils] No release found for Minecraft {}.", minecraftVersion);
			return Optional.empty();
		}

		return best.version().compareTo(current) > 0 ? Optional.of(best) : Optional.empty();
	}

	private static @Nullable Candidate read(JsonObject asset, String minecraftVersion) {
		String name = string(asset, "name");
		if (name == null) return null;

		Matcher matcher = ASSET_NAME.matcher(name);
		if (!matcher.matches()) return null;

		if (!matcher.group(2).equals(minecraftVersion)) return null;

		String url = string(asset, "browser_download_url");
		if (url == null) return null;

		try {
			Version version = Version.parse(matcher.group(1) + "+" + matcher.group(2));

			return new Candidate(version, name, URI.create(url), asset.get("size").getAsLong(), sha256(asset));
		} catch (VersionParsingException | RuntimeException e) {
			LOGGER.warn("[DyeUtils] Ignoring release asset {}: {}", name, e.toString());
			return null;
		}
	}

	private static @Nullable String sha256(JsonObject asset) {
		String digest = string(asset, "digest");

		return digest != null && digest.startsWith(DIGEST_PREFIX) ? digest.substring(DIGEST_PREFIX.length()) : null;
	}

	private static @Nullable String string(JsonObject object, String member) {
		JsonElement value = object.get(member);

		return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
	}

	private static boolean bool(JsonObject object, String member) {
		JsonElement value = object.get(member);

		return value != null && value.isJsonPrimitive() && value.getAsBoolean();
	}
}
