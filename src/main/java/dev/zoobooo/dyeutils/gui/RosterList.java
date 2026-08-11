package dev.zoobooo.dyeutils.gui;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public class RosterList extends ContainerObjectSelectionList<RosterRow> {
	private final int rowWidth;

	public RosterList(Minecraft minecraft, int width, int height, int y, int itemHeight, int rowWidth) {
		super(minecraft, width, height, y, itemHeight);
		this.rowWidth = rowWidth;
	}

	public void rebuild(List<RosterRow> rows) {
		clearEntries();

		for (RosterRow row : rows) {
			addEntry(row);
		}
	}

	public void focusLast() {
		List<RosterRow> rows = children();
		if (rows.isEmpty()) return;

		RosterRow last = rows.getLast();
		scrollToEntry(last);
		setFocused(last);
		last.focusPrimary();
	}

	@Override
	public int getRowWidth() {
		return rowWidth;
	}

	// Left off, as in vanilla. Switching it on boxes the whole row, and every row already holds a
	// widget that draws its own focus border.
}
