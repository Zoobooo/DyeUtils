package dev.zoobooo.dyeutils.party;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.util.Failsafe;
import net.minecraft.network.chat.Component;

/** Lets someone take themselves off the party list when they are done, the way !warp works. */
public class PartyDropOut {
	static final String TRIGGER = "!partydt";

	public static final PartyDropOut INSTANCE = new PartyDropOut();

	private PartyDropOut() {
	}

	public void onGameMessage(String message) {
		Failsafe.run("Party drop-out command", () -> handle(message));
	}

	private void handle(String message) {
		String ign = PartyChatParser.partyChatCommand(message, TRIGGER);
		if (ign == null) return;

		boolean pulled = PartyInviteQueue.INSTANCE.drop(ign);
		boolean removed = PartyList.remove(ign);

		// Silent for anyone on neither, or every member trying it earns you a line.
		if (!pulled && !removed) return;

		DyeUtils.feedback(Component.translatable("dyeutils.message.droppedOut", ign));
	}
}
