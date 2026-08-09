package dev.zoobooo.dyeutils.party;

import java.util.ArrayList;
import java.util.List;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.config.DyeUtilsConfig;
import dev.zoobooo.dyeutils.util.MessageScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PartyInviter {
	// "You can only invite up to 5 people at once!"
	private static final int MAX_INVITES_PER_COMMAND = 5;
	private static final int MAX_COMMAND_LENGTH = 256;

	// 200ms is not enough for a run of party commands; Skyblocker's reparty uses 10 ticks too.
	private static final long INVITE_GAP_MS = 500L;

	private static final String DEFAULT_PREFIX = "/p invite ";
	private static final String WARP_COMMAND = "/pc !warp";

	public static void inviteAll() {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		List<String> configured = DyeUtilsConfig.get().partyMembers;
		List<String> members = DyeUtilsConfig.sanitise(configured);

		if (members.isEmpty()) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.emptyList"));
			return;
		}

		List<String> commands = buildCommands(normalisePrefix(DyeUtilsConfig.get().inviteCommandPrefix), members);

		for (String command : commands) {
			MessageScheduler.INSTANCE.queue(command, INVITE_GAP_MS);
		}

		DyeUtils.feedback(Component.translatable("dyeutils.message.invited", members.size(), commands.size()));

		int dropped = configured.size() - members.size();
		if (dropped > 0) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.droppedEntries", dropped));
		}
	}

	public static void warp() {
		if (Minecraft.getInstance().player == null) return;

		MessageScheduler.INSTANCE.queue(WARP_COMMAND);
		DyeUtils.feedback(Component.translatable("dyeutils.message.warping"));
	}

	static String normalisePrefix(String prefix) {
		String trimmed = prefix == null ? "" : prefix.trim();

		if (trimmed.isEmpty()) return DEFAULT_PREFIX;
		if (!trimmed.startsWith("/")) trimmed = "/" + trimmed;

		return trimmed + " ";
	}

	static List<String> buildCommands(String prefix, List<String> names) {
		List<String> commands = new ArrayList<>();
		StringBuilder current = new StringBuilder(prefix);
		int inCurrent = 0;

		for (String name : names) {
			int cost = inCurrent == 0 ? name.length() : name.length() + 1;
			boolean full = inCurrent >= MAX_INVITES_PER_COMMAND || current.length() + cost > MAX_COMMAND_LENGTH;

			if (inCurrent > 0 && full) {
				commands.add(current.toString());
				current = new StringBuilder(prefix);
				inCurrent = 0;
			}

			if (inCurrent > 0) current.append(' ');
			current.append(name);
			inCurrent++;
		}

		if (inCurrent > 0) commands.add(current.toString());

		return commands;
	}
}
