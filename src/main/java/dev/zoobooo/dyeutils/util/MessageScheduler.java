package dev.zoobooo.dyeutils.util;

import java.util.ArrayDeque;
import java.util.Queue;

import dev.zoobooo.dyeutils.DyeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtil;

public class MessageScheduler {
	private static final long MIN_DELAY_MS = 200L;

	public static final MessageScheduler INSTANCE = new MessageScheduler();

	private record Pending(String message, long gapMs) {}

	private final Queue<Pending> queue = new ArrayDeque<>();
	private long lastMessage = 0L;

	private MessageScheduler() {
	}

	public void queue(String message) {
		queue(message, MIN_DELAY_MS);
	}

	public void queue(String message, long gapMs) {
		queue.add(new Pending(message, Math.max(MIN_DELAY_MS, gapMs)));
	}

	public void tick() {
		Pending next = queue.peek();
		if (next == null) return;

		Minecraft client = Minecraft.getInstance();

		if (client.player == null || client.player.connection == null) {
			queue.clear();
			return;
		}

		if (lastMessage + next.gapMs() > System.currentTimeMillis()) return;

		queue.poll();
		send(client, next.message());
		lastMessage = System.currentTimeMillis();
	}

	private void send(Minecraft client, String message) {
		String trimmed = StringUtil.trimChatMessage(message.trim().replaceAll("\\s+", " "));

		if (trimmed.isEmpty()) return;

		if (!DyeUtils.config().hideCommandsFromChat) {
			client.gui.getChat().addRecentChat(trimmed);
		}

		if (trimmed.startsWith("/")) {
			client.player.connection.sendCommand(trimmed.substring(1));
		} else {
			client.player.connection.sendChat(trimmed);
		}
	}
}
