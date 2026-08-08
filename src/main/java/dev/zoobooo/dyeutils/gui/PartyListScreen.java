package dev.zoobooo.dyeutils.gui;

import java.util.ArrayList;
import java.util.List;

import dev.zoobooo.dyeutils.party.PartyList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

public class PartyListScreen extends Screen {
	private static final int ROW_HEIGHT = 24;
	private static final int WIDGET_HEIGHT = 20;
	private static final int REMOVE_BUTTON_WIDTH = 20;
	private static final int GAP = 4;
	private static final int ROW_WIDTH = 260;

	private static final int HEADER_HEIGHT = 32;
	private static final int FOOTER_HEIGHT = 64;

	private final @Nullable Screen parent;

	private final List<String> working;

	private EntryList entries;

	public PartyListScreen(@Nullable Screen parent) {
		super(Component.translatable("dyeutils.partyList.title"));
		this.parent = parent;
		this.working = new ArrayList<>(PartyList.get());
	}

	@Override
	protected void init() {
		entries = new EntryList(minecraft, width, height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ROW_HEIGHT);
		addRenderableWidget(entries);
		rebuildRows();

		addRenderableWidget(Button.builder(Component.translatable("dyeutils.partyList.add"), button -> {
			syncFromRows();
			working.add("");
			rebuildRows();
			entries.focusLastRow();
		}).bounds(width / 2 - 154, height - 52, 150, WIDGET_HEIGHT).build());

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(width / 2 + GAP, height - 52, 150, WIDGET_HEIGHT).build());
	}

	private void rebuildRows() {
		entries.rebuild(working);
	}

	private void syncFromRows() {
		working.clear();
		for (Row row : entries.children()) {
			working.add(row.field.getValue());
		}
	}

	private void removeRow(Row target) {
		List<String> remaining = new ArrayList<>();
		for (Row row : entries.children()) {
			if (row != target) remaining.add(row.field.getValue());
		}

		working.clear();
		working.addAll(remaining);
		rebuildRows();
	}

	@Override
	public void onClose() {
		syncFromRows();

		// A blank row is an abandoned add. Malformed names are kept as typed and reported on invite.
		List<String> saved = new ArrayList<>();
		for (String name : working) {
			String trimmed = name.trim();
			if (!trimmed.isEmpty()) saved.add(trimmed);
		}

		PartyList.set(saved);
		minecraft.setScreen(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);

		if (working.isEmpty()) {
			graphics.centeredText(font,
					Component.translatable("dyeutils.partyList.empty").withStyle(ChatFormatting.GRAY),
					width / 2, height / 2 - 4, 0xFFFFFFFF);
		}
	}

	private class EntryList extends ContainerObjectSelectionList<Row> {
		EntryList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
		}

		void rebuild(List<String> values) {
			clearEntries();
			for (String value : values) {
				addEntry(new Row(value));
			}
		}

		void focusLastRow() {
			List<Row> rows = children();
			if (rows.isEmpty()) return;

			Row last = rows.getLast();
			scrollToEntry(last);
			setFocused(last);
			last.setFocused(last.field);
			last.field.setFocused(true);
		}

		@Override
		public int getRowWidth() {
			return ROW_WIDTH;
		}
	}

	private class Row extends ContainerObjectSelectionList.Entry<Row> {
		private final EditBox field;
		private final Button remove;

		Row(String value) {
			field = new EditBox(font, 0, 0, ROW_WIDTH - REMOVE_BUTTON_WIDTH - GAP, WIDGET_HEIGHT,
					Component.translatable("dyeutils.partyList.ign"));
			field.setMaxLength(16);
			field.setValue(value);

			remove = Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), button -> removeRow(this))
					.bounds(0, 0, REMOVE_BUTTON_WIDTH, WIDGET_HEIGHT)
					.tooltip(Tooltip.create(Component.translatable("dyeutils.partyList.remove")))
					.build();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int rowY = getContentY() + (getContentHeight() - WIDGET_HEIGHT) / 2;
			int fieldWidth = getContentWidth() - REMOVE_BUTTON_WIDTH - GAP;

			field.setWidth(fieldWidth);
			field.setX(getContentX());
			field.setY(rowY);

			remove.setX(getContentX() + fieldWidth + GAP);
			remove.setY(rowY);

			field.extractRenderState(graphics, mouseX, mouseY, partialTick);
			remove.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(field, remove);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(field, remove);
		}
	}
}
