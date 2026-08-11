package dev.zoobooo.dyeutils.rift;

import java.util.Locale;

public final class BacteDefeat {
	private static final String ANNOUNCEMENT = "bacte down";

	private static final String CHAT_SEPARATOR = ": ";

	private BacteDefeat() {
	}

	public static boolean isAnnouncement(String message) {
		if (message == null) return false;

		int at = message.toLowerCase(Locale.ROOT).indexOf(ANNOUNCEMENT);
		if (at < 0) return false;

		// A timestamp in front is fine, a speaker is not. Only player chat has "name: ".
		return !message.substring(0, at).contains(CHAT_SEPARATOR);
	}
}
