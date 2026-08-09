package dev.zoobooo.dyeutils.party;

import java.util.List;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.config.DyeUtilsConfig;
import dev.zoobooo.dyeutils.util.MessageScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PartyInviteQueue {
	public static final PartyInviteQueue INSTANCE = new PartyInviteQueue();

	private final InvitePacer pacer = new InvitePacer();

	private boolean announcedWait;

	private PartyInviteQueue() {
	}

	public boolean start(List<String> members) {
		if (!pacer.start(members)) return false;

		announcedWait = false;
		DyeUtils.feedback(Component.translatable("dyeutils.message.invited", members.size()));

		return true;
	}

	public void tick() {
		if (!pacer.isActive()) return;

		Minecraft client = Minecraft.getInstance();

		// Disconnected mid-run: drop everything rather than resuming against the next server.
		if (client.player == null || client.player.connection == null) {
			pacer.reset();
			return;
		}

		long now = System.currentTimeMillis();

		List<String> batch = pacer.pollBatch(now);

		if (!batch.isEmpty()) {
			send(batch);
			return;
		}

		if (pacer.isFinished(now)) {
			finish();
			return;
		}

		if (pacer.isWaiting(now)) announceWait();
	}

	private void send(List<String> batch) {
		String prefix = PartyInviter.normalisePrefix(DyeUtilsConfig.get().inviteCommandPrefix);

		for (String command : PartyInviter.buildCommands(prefix, batch, InvitePacer.BATCH)) {
			MessageScheduler.INSTANCE.queue(command, InvitePacer.INVITE_GAP_MS);
		}
	}

	public void onGameMessage(Component message) {
		String plain = message.getString();

		String joinedIgn = PartyChatParser.joinedPlayer(plain);

		if (joinedIgn != null) {
			pacer.onJoined(joinedIgn);
			return;
		}

		if (pacer.isActive() && PartyChatParser.isInviteCapReached(plain)) {
			pacer.onRefused(System.currentTimeMillis());
			announceWait();
		}
	}

	/** Said once per run; on a long list the gate closes repeatedly. */
	private void announceWait() {
		if (announcedWait) return;

		announcedWait = true;
		DyeUtils.feedback(Component.translatable("dyeutils.message.invitesPending", pacer.remainingCount()));
	}

	private void finish() {
		List<String> missing = pacer.neverJoined();

		if (missing.isEmpty()) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.allJoined", pacer.invitedCount()));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.neverJoined", missing.size(),
					Component.literal(String.join(", ", missing)).withStyle(ChatFormatting.WHITE)));
		}

		pacer.reset();
	}
}
