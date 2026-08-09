package dev.zoobooo.dyeutils.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

final class UpdateInstaller {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String PART_SUFFIX = ".part";

	private final Path staging;

	UpdateInstaller(Path staging) {
		this.staging = staging;
	}

	static UpdateInstaller inConfigDir() {
		return new UpdateInstaller(FabricLoader.getInstance().getConfigDir()
				.resolve(DyeUtils.NAMESPACE)
				.resolve("update"));
	}

	Optional<Path> staged() {
		if (!Files.isDirectory(staging)) return Optional.empty();

		try (Stream<Path> entries = Files.list(staging)) {
			return entries.filter(path -> path.getFileName().toString().endsWith(".jar")).findFirst();
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	void discardStale(Version current) {
		if (!Files.isDirectory(staging)) return;

		try (Stream<Path> entries = Files.list(staging)) {
			for (Path path : entries.toList()) {
				String name = path.getFileName().toString();

				if (name.endsWith(PART_SUFFIX)) {
					delete(path);
					continue;
				}

				Version staged = versionOf(path);

				if (staged == null || staged.compareTo(current) <= 0) {
					LOGGER.info("[DyeUtils] Discarding staged update {}.", name);
					delete(path);
				}
			}
		} catch (IOException e) {
			LOGGER.warn("[DyeUtils] Could not tidy the update staging directory: {}", e.toString());
		}
	}

	void stage(HttpClient http, ReleaseLookup.Candidate candidate) {
		Path part = staging.resolve(candidate.fileName() + PART_SUFFIX);
		Path target = staging.resolve(candidate.fileName());

		try {
			Files.createDirectories(staging);
			Files.deleteIfExists(part);

			HttpRequest request = HttpRequest.newBuilder(candidate.download())
					.header("User-Agent", DyeUtils.NAMESPACE)
					.timeout(Duration.ofMinutes(2))
					.GET()
					.build();

			HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(part));

			if (response.statusCode() != 200) {
				LOGGER.warn("[DyeUtils] Download of {} got HTTP {}.", candidate.fileName(), response.statusCode());
				delete(part);
				return;
			}

			String problem = verify(part, candidate);

			if (problem != null) {
				LOGGER.warn("[DyeUtils] Rejected download of {}: {}", candidate.fileName(), problem);
				delete(part);
				return;
			}

			Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("[DyeUtils] Staged {}; it will be installed when the game closes.", candidate.fileName());
		} catch (Exception e) {
			LOGGER.warn("[DyeUtils] Could not stage {}: {}", candidate.fileName(), e.toString());
			delete(part);
		}
	}

	private static @Nullable String verify(Path file, ReleaseLookup.Candidate candidate) throws IOException {
		long size = Files.size(file);
		if (size != candidate.size()) return "expected " + candidate.size() + " bytes, got " + size;

		String expected = candidate.sha256();

		if (expected != null) {
			String actual = sha256(file);
			if (!expected.equalsIgnoreCase(actual)) return "sha256 " + actual + " does not match " + expected;
		}

		Version version = versionOf(file);
		if (version == null) return "not a readable dyeutils jar";

		if (version.compareTo(candidate.version()) != 0) {
			return "contains version " + version.getFriendlyString() + ", expected " + candidate.version().getFriendlyString();
		}

		return null;
	}

	void apply(Path runningJar) {
		Optional<Path> maybeStaged = staged();
		if (maybeStaged.isEmpty()) return;

		Path stagedJar = maybeStaged.get();

		try {
			byte[] replacement = Files.readAllBytes(stagedJar);

			if (versionOf(stagedJar) == null) {
				LOGGER.warn("[DyeUtils] Staged update is unreadable, leaving the installed jar alone.");
				return;
			}

			if (swapFile(runningJar, stagedJar) || overwriteInPlace(runningJar, replacement)) {
				delete(stagedJar);
				deleteIfEmpty(staging);
				deleteIfEmpty(staging.getParent());
			} else {
				LOGGER.warn("[DyeUtils] Could not install the update; it stays staged and will be retried.");
			}
		} catch (Throwable t) {
			LOGGER.warn("[DyeUtils] Update install failed: {}", t.toString());
		}
	}

	private static boolean swapFile(Path runningJar, Path stagedJar) {
		Path mods = runningJar.getParent();
		Path incoming = mods.resolve(stagedJar.getFileName().toString());

		if (incoming.equals(runningJar)) return false;

		Path pending = mods.resolve(incoming.getFileName() + ".tmp");

		try {
			Files.copy(stagedJar, pending, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			delete(pending);
			return false;
		}

		try {
			Files.delete(runningJar);
		} catch (IOException e) {
			delete(pending);
			return false;
		}

		try {
			Files.move(pending, incoming, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("[DyeUtils] Installed {}.", incoming.getFileName());

			return true;
		} catch (IOException e) {
			LOGGER.warn("[DyeUtils] Rename failed after removing the old jar: {}", e.toString());

			try {
				Files.move(pending, runningJar, StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("[DyeUtils] Installed the update under the previous file name instead.");

				return true;
			} catch (IOException recovery) {
				LOGGER.error("[DyeUtils] Could not restore a mod jar into {}.", mods, recovery);

				return false;
			}
		}
	}

	private static boolean overwriteInPlace(Path runningJar, byte[] replacement) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			try (FileChannel channel = FileChannel.open(runningJar, StandardOpenOption.WRITE)) {
				channel.truncate(0);

				ByteBuffer buffer = ByteBuffer.wrap(replacement);
				while (buffer.hasRemaining()) channel.write(buffer);

				channel.force(true);
			} catch (IOException e) {
				LOGGER.warn("[DyeUtils] In-place update write failed: {}", e.toString());
				continue;
			}

			try {
				if (Files.size(runningJar) == replacement.length && versionOf(runningJar) != null) {
					LOGGER.info("[DyeUtils] Installed the update into {}.", runningJar.getFileName());

					return true;
				}
			} catch (IOException e) {
				LOGGER.warn("[DyeUtils] Could not check the written jar: {}", e.toString());
			}

			LOGGER.warn("[DyeUtils] Written jar did not verify (attempt {} of 2).", attempt);
		}

		return false;
	}

	static @Nullable Version versionOf(Path jar) {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			ZipEntry entry = zip.getEntry("fabric.mod.json");
			if (entry == null) return null;

			try (InputStream in = zip.getInputStream(entry)) {
				JsonObject metadata = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
						.getAsJsonObject();

				if (!DyeUtils.NAMESPACE.equals(metadata.get("id").getAsString())) return null;

				return Version.parse(metadata.get("version").getAsString());
			}
		} catch (IOException | VersionParsingException | RuntimeException e) {
			return null;
		}
	}

	private static String sha256(Path file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			try (InputStream in = Files.newInputStream(file)) {
				byte[] buffer = new byte[8192];

				for (int read; (read = in.read(buffer)) != -1; ) {
					digest.update(buffer, 0, read);
				}
			}

			return HexFormat.of().formatHex(digest.digest());
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IOException(e);
		}
	}

	private static void delete(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
		}
	}

	private static void deleteIfEmpty(Path directory) {
		try (Stream<Path> entries = Files.list(directory)) {
			List<Path> remaining = entries.toList();
			if (remaining.isEmpty()) Files.delete(directory);
		} catch (IOException ignored) {
		}
	}
}
