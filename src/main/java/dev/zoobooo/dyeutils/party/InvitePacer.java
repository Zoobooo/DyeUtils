package dev.zoobooo.dyeutils.party;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Hypixel checks the outstanding invite count when the command is issued and refuses it at 5
// or more, but never checks the total afterwards. So 4 then 4 is accepted and leaves 8 outstanding,
// while 4 then 4 then 1 is refused. Verified in game.
final class InvitePacer {
	/** Four, not five: a five-name command lands exactly on the threshold and blocks the next. */
	static final int BATCH = 4;

	/** Hypixel refuses a command issued while this many invites are outstanding. */
	static final int CAP = 5;

	/** How long Hypixel gives a player to accept, after which the slot frees itself. */
	static final long INVITE_TTL_MS = 60_000L;

	// 200ms is not enough for a run of party commands; Skyblocker's reparty uses 10 ticks too.
	static final long INVITE_GAP_MS = 500L;

	private record Outstanding(String ign, long sentAt) {}

	private final Deque<String> remaining = new ArrayDeque<>();
	private final List<Outstanding> outstanding = new ArrayList<>();

	private final Set<String> invited = new LinkedHashSet<>();
	private final Set<String> joined = new HashSet<>();

	private List<String> lastBatch = List.of();

	private long lastSend = -INVITE_GAP_MS;

	private long blockedUntil;

	private boolean active;

	boolean start(List<String> members) {
		if (active || members.isEmpty()) return false;

		reset();
		remaining.addAll(members);
		active = true;

		return true;
	}

	boolean isActive() {
		return active;
	}

	int remainingCount() {
		return remaining.size();
	}

	int invitedCount() {
		return invited.size();
	}

	List<String> pollBatch(long now) {
		if (!active) return List.of();

		expire(now);

		if (remaining.isEmpty() || outstanding.size() >= CAP) return List.of();
		if (now < blockedUntil || now - lastSend < INVITE_GAP_MS) return List.of();

		List<String> batch = new ArrayList<>(BATCH);
		while (batch.size() < BATCH && !remaining.isEmpty()) batch.add(remaining.poll());

		for (String name : batch) {
			outstanding.add(new Outstanding(name, now));
			invited.add(name);
		}

		lastBatch = List.copyOf(batch);
		lastSend = now;

		return batch;
	}

	void onJoined(String ign) {
		if (!active) return;

		joined.add(ign.toLowerCase(Locale.ROOT));

		// Does not on its own open the gate: that needs the count to fall below CAP.
		outstanding.removeIf(entry -> entry.ign().equalsIgnoreCase(ign));

		remaining.removeIf(name -> name.equalsIgnoreCase(ign));

		blockedUntil = 0L;
	}

	/** They asked to be left out, so they are not reported either. An invite already sent has to lapse. */
	boolean drop(String ign) {
		if (!active) return false;

		boolean queued = remaining.removeIf(name -> name.equalsIgnoreCase(ign));
		boolean sent = invited.removeIf(name -> name.equalsIgnoreCase(ign));

		return queued || sent;
	}

	// Slots we did not know about were taken, most likely by invites sent before the run began.
	void onRefused(long now) {
		if (!active) return;

		for (int i = lastBatch.size() - 1; i >= 0; i--) {
			String name = lastBatch.get(i);

			outstanding.removeIf(entry -> entry.ign().equalsIgnoreCase(name));
			invited.remove(name);
			remaining.addFirst(name);
		}

		lastBatch = List.of();
		blockedUntil = now + INVITE_TTL_MS;
	}

	boolean isWaiting(long now) {
		if (!active) return false;

		expire(now);

		return !remaining.isEmpty() && (outstanding.size() >= CAP || now < blockedUntil);
	}

	boolean isFinished(long now) {
		if (!active) return false;

		expire(now);

		return remaining.isEmpty() && outstanding.isEmpty();
	}

	List<String> neverJoined() {
		return invited.stream()
				.filter(name -> !joined.contains(name.toLowerCase(Locale.ROOT)))
				.toList();
	}

	void reset() {
		remaining.clear();
		outstanding.clear();
		invited.clear();
		joined.clear();
		lastBatch = List.of();
		lastSend = -INVITE_GAP_MS;
		blockedUntil = 0L;
		active = false;
	}

	/** A lapsed invite frees its slot with no message to tell us so. */
	private void expire(long now) {
		outstanding.removeIf(entry -> now - entry.sentAt() >= INVITE_TTL_MS);
	}
}
