package dev.zoobooo.dyeutils.party;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyChatParserTest {
	@Test
	void everyWayThePartyEndsIsRecognised() {
		assertTrue(PartyChatParser.isPartyEnded("You left the party."));
		assertTrue(PartyChatParser.isPartyEnded("[MVP+] Zoobo has disbanded the party!"));
		assertTrue(PartyChatParser.isPartyEnded("Zoobo has disbanded the party!"));
		assertTrue(PartyChatParser.isPartyEnded("You have been kicked from the party by [MVP++] Zoobo"));
		assertTrue(PartyChatParser.isPartyEnded(
				"The party was disbanded because all invites expired and the party was empty."));
		assertTrue(PartyChatParser.isPartyEnded("You are not currently in a party."));
		assertTrue(PartyChatParser.isPartyEnded("You are not in a party."));
	}

	@Test
	void oneMemberLeavingDoesNotEndTheParty() {
		assertFalse(PartyChatParser.isPartyEnded("[MVP+] Zoobo has left the party."));
		assertFalse(PartyChatParser.isPartyEnded("Zoobo has left the party."));
	}

	@Test
	void aPlayerQuotingTheWordsDoesNotEndTheParty() {
		assertFalse(PartyChatParser.isPartyEnded("Party > [MVP+] Zoobo: You left the party."));
		assertFalse(PartyChatParser.isPartyEnded("Zoobo: You left the party."));
		assertFalse(PartyChatParser.isPartyEnded("Party > Zoobo: Zoobo has disbanded the party!"));
	}

	@Test
	void aPartyCommandResolvesToItsSpeaker() {
		assertEquals("Zoobo", command("Party > [MVP+] Zoobo: !partydt"));
		assertEquals("Zoobo", command("Party > Zoobo: !partydt"));
		assertEquals("Zoobo", command("Party > [MVP++] Zoobo: !PARTYDT"));
	}

	@Test
	void onlyPartyChatCounts() {
		assertNull(command("Guild > [MVP+] Zoobo: !partydt"));
		assertNull(command("Zoobo: !partydt"));
		assertNull(command("From [MVP+] Zoobo: !partydt"));
		assertNull(command("Co-op > Zoobo: !partydt"));
	}

	@Test
	void theWholeMessageHasToBeTheCommand() {
		assertNull(command("Party > Zoobo: !partydt bye"));
		assertNull(command("Party > Zoobo: use !partydt to leave"));
		assertNull(command("Party > Zoobo: !partydtt"));
	}

	@Test
	void joinsStillParse() {
		assertEquals("Zoobo", PartyChatParser.joinedPlayer("[MVP+] Zoobo joined the party."));
		assertEquals("Zoobo", PartyChatParser.joinedPlayer("Zoobo joined the party."));
		assertNull(PartyChatParser.joinedPlayer("Party > Zoobo: Zoobo joined the party."));
	}

	private static String command(String message) {
		return PartyChatParser.partyChatCommand(message, PartyDropOut.TRIGGER);
	}
}
