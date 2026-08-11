package dev.zoobooo.dyeutils.party;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitePacerTest {
	private final InvitePacer pacer = new InvitePacer();

	@Test
	void opensWithFourThenFourThenStops() {
		pacer.start(List.of("a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4", "c1"));

		assertEquals(List.of("a1", "a2", "a3", "a4"), pacer.pollBatch(0L));
		assertEquals(List.of("b1", "b2", "b3", "b4"), pacer.pollBatch(InvitePacer.INVITE_GAP_MS));

		assertEquals(List.of(), pacer.pollBatch(InvitePacer.INVITE_GAP_MS * 2));
		assertTrue(pacer.isWaiting(InvitePacer.INVITE_GAP_MS * 2));
	}

	@Test
	void oneJoinIsNotEnoughToReopenTheGate() {
		pacer.start(List.of("a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4", "c1"));
		pacer.pollBatch(0L);
		pacer.pollBatch(InvitePacer.INVITE_GAP_MS);

		pacer.onJoined("a1");

		assertEquals(List.of(), pacer.pollBatch(InvitePacer.INVITE_GAP_MS * 2),
				"seven outstanding is still at or over the cap");

		pacer.onJoined("a2");
		pacer.onJoined("a3");
		pacer.onJoined("a4");

		assertEquals(List.of("c1"), pacer.pollBatch(InvitePacer.INVITE_GAP_MS * 3));
	}

	@Test
	void lapsedInvitesFreeTheirSlots() {
		pacer.start(List.of("a1", "a2", "a3", "a4", "b1", "b2", "b3", "b4", "c1"));
		pacer.pollBatch(0L);
		pacer.pollBatch(InvitePacer.INVITE_GAP_MS);

		assertEquals(List.of("c1"), pacer.pollBatch(InvitePacer.INVITE_TTL_MS + 1));
	}

	@Test
	void everyoneInvitedWhoNeverJoinedIsReported() {
		pacer.start(List.of("a1", "a2"));
		pacer.pollBatch(0L);

		pacer.onJoined("a1");

		assertEquals(List.of("a2"), pacer.neverJoined());
	}

	@Test
	void aRefusedCommandPutsItsNamesBack() {
		pacer.start(List.of("a1", "a2", "a3", "a4"));
		assertEquals(List.of("a1", "a2", "a3", "a4"), pacer.pollBatch(0L));

		pacer.onRefused(1L);

		assertTrue(pacer.neverJoined().isEmpty(), "a refused command invited nobody");
		assertEquals(4, pacer.remainingCount());
	}

	@Test
	void startIsRefusedWhileARunIsGoing() {
		assertTrue(pacer.start(List.of("a1")));
		assertFalse(pacer.start(List.of("b1")));
	}
}
