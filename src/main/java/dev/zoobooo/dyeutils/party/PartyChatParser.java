package dev.zoobooo.dyeutils.party;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

final class PartyChatParser {
	// Anchored at both ends so somebody typing the same words in chat cannot trigger an invite.
	private static final Pattern JOINED = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w{1,16}) joined the party\\.$");

	// Matched on the middle only, so a reworded error still trips it.
	private static final Pattern INVITE_CAP = Pattern.compile("\\binvites pending\\b", Pattern.CASE_INSENSITIVE);

	// "X has left the party." is not one of these: that is somebody else going, and the run carries on.
	private static final Pattern PARTY_ENDED = Pattern.compile("^(?:"
			+ "You left the party\\.|"
			+ "(?:\\[[^\\]]+\\] )?\\w{1,16} has disbanded the party!|"
			// The tail names whoever did it and is not worth pinning down.
			+ "You have been kicked from the party by\\b.*|"
			+ "The party was disbanded because all invites expired and the party was empty\\.|"
			+ "You are not (?:currently )?in a party.*"
			+ ")$");

	private static final Pattern PARTY_CHAT = Pattern.compile("^Party > (?:\\[[^\\]]+\\] )?(\\w{1,16}): (.+)$");

	private PartyChatParser() {
	}

	static @Nullable String joinedPlayer(String message) {
		Matcher matcher = JOINED.matcher(message.trim());

		return matcher.matches() ? matcher.group(1) : null;
	}

	static boolean isInviteCapReached(String message) {
		return INVITE_CAP.matcher(message).find();
	}

	static boolean isPartyEnded(String message) {
		return PARTY_ENDED.matcher(message.trim()).matches();
	}

	/** Whoever said exactly this in party chat. Nothing but the command counts, so a sentence
	 * mentioning it is left alone. */
	static @Nullable String partyChatCommand(String message, String command) {
		Matcher matcher = PARTY_CHAT.matcher(message.trim());
		if (!matcher.matches()) return null;

		String spoken = matcher.group(2).trim();

		return spoken.toLowerCase(Locale.ROOT).equals(command.toLowerCase(Locale.ROOT)) ? matcher.group(1) : null;
	}
}
