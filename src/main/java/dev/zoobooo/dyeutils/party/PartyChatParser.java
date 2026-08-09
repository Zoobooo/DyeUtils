package dev.zoobooo.dyeutils.party;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

final class PartyChatParser {
	// Anchored at both ends so somebody typing the same words in chat cannot trigger an invite.
	private static final Pattern JOINED = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w{1,16}) joined the party\\.$");

	// Matched on the middle only, so a reworded error still trips it.
	private static final Pattern INVITE_CAP = Pattern.compile("\\binvites pending\\b", Pattern.CASE_INSENSITIVE);

	private PartyChatParser() {
	}

	static @Nullable String joinedPlayer(String message) {
		Matcher matcher = JOINED.matcher(message.trim());

		return matcher.matches() ? matcher.group(1) : null;
	}

	static boolean isInviteCapReached(String message) {
		return INVITE_CAP.matcher(message).find();
	}
}
