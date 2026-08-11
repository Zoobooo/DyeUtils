package dev.zoobooo.dyeutils.rift;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.util.Failsafe;
import dev.zoobooo.dyeutils.util.MessageScheduler;
import net.minecraft.network.chat.Component;

public class AutoDisband {
	private static final String DISBAND_COMMAND = "/p disband";

	/** Hypixel sends the announcement more than once, and chat mods merge repeats. */
	private static final long DEBOUNCE_MS = 10_000L;

	public static final AutoDisband INSTANCE = new AutoDisband();

	private long lastDisband;

	private AutoDisband() {
	}

	public void onGameMessage(String message) {
		Failsafe.run("Bacte defeat handling", () -> handle(message));
	}

	private void handle(String message) {
		if (!BacteDefeat.isAnnouncement(message)) return;
		if (!DyeUtils.config().autoDisband) return;

		long now = System.currentTimeMillis();

		if (now - lastDisband < DEBOUNCE_MS) return;

		lastDisband = now;

		MessageScheduler.INSTANCE.queue(DISBAND_COMMAND);
		DyeUtils.feedback(Component.translatable("dyeutils.message.disbanding"));
	}

	public void reset() {
		lastDisband = 0L;
	}
}
