package dev.zoobooo.dyeutils.update;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModOrigin;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class AutoUpdater {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String DISABLE_PROPERTY = DyeUtils.NAMESPACE + ".noUpdate";

	private static final AtomicBoolean APPLIED = new AtomicBoolean();

	private AutoUpdater() {
	}

	public static void start() {
		if (Boolean.getBoolean(DISABLE_PROPERTY)) {
			LOGGER.info("[DyeUtils] Auto update disabled by -D{}.", DISABLE_PROPERTY);
			return;
		}

		FabricLoader loader = FabricLoader.getInstance();

		if (loader.isDevelopmentEnvironment()) {
			LOGGER.info("[DyeUtils] Auto update skipped in the development environment.");
			return;
		}

		ModContainer self = loader.getModContainer(DyeUtils.NAMESPACE).orElse(null);
		if (self == null) return;

		Path jar = ownJar(self);

		if (jar == null) {
			LOGGER.info("[DyeUtils] Not running from a plain jar, so auto update is off.");
			return;
		}

		Version current = self.getMetadata().getVersion();
		String minecraftVersion = loader.getModContainer("minecraft")
				.map(minecraft -> minecraft.getMetadata().getVersion().getFriendlyString())
				.orElse(null);

		if (minecraftVersion == null) return;

		UpdateInstaller installer = UpdateInstaller.inConfigDir();

		Thread thread = new Thread(() -> run(installer, jar, minecraftVersion, current), "DyeUtils Auto Updater");
		thread.setDaemon(true);
		thread.start();
	}

	private static void run(UpdateInstaller installer, Path jar, String minecraftVersion, Version current) {
		try {
			installer.discardStale(current);

			try (HttpClient http = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(5))
					.followRedirects(HttpClient.Redirect.NORMAL)
					.build()) {
				Optional<ReleaseLookup.Candidate> candidate = ReleaseLookup.findNewer(http, minecraftVersion, current);

				if (candidate.isPresent() && !alreadyStaged(installer, candidate.get())) {
					LOGGER.info("[DyeUtils] Update available: {} (running {}).",
							candidate.get().version().getFriendlyString(), current.getFriendlyString());

					installer.stage(http, candidate.get());
				}
			}

			if (installer.staged().isPresent()) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> apply(installer, jar), "DyeUtils Update Installer"));
			}
		} catch (Throwable t) {
			LOGGER.warn("[DyeUtils] Auto update failed: {}", t.toString());
		}
	}

	private static void apply(UpdateInstaller installer, Path jar) {
		if (APPLIED.compareAndSet(false, true)) installer.apply(jar);
	}

	private static boolean alreadyStaged(UpdateInstaller installer, ReleaseLookup.Candidate candidate) {
		return installer.staged()
				.map(UpdateInstaller::versionOf)
				.map(staged -> staged.compareTo(candidate.version()) >= 0)
				.orElse(false);
	}

	private static @Nullable Path ownJar(ModContainer self) {
		ModOrigin origin = self.getOrigin();
		if (origin.getKind() != ModOrigin.Kind.PATH) return null;

		Collection<Path> paths = origin.getPaths();
		if (paths.size() != 1) return null;

		Path path = paths.iterator().next();

		return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar") ? path : null;
	}
}
