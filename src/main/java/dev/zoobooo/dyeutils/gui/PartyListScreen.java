package dev.zoobooo.dyeutils.gui;

import java.util.ArrayList;
import java.util.List;

import dev.zoobooo.dyeutils.party.Favourites;
import dev.zoobooo.dyeutils.party.PartyList;
import dev.zoobooo.dyeutils.util.Ign;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

public class PartyListScreen extends RosterScreen {
	private final List<String> working;

	private boolean reloadOnReturn;

	public PartyListScreen(@Nullable Screen parent) {
		super(Component.translatable("dyeutils.partyList.title"), parent);
		this.working = new ArrayList<>(PartyList.get());
	}

	// Screen only calls init the first time it is shown; coming back calls repositionElements.
	// added runs before init, so the rows are rebuilt here too.
	@Override
	public void added() {
		super.added();

		if (!reloadOnReturn) return;

		reloadOnReturn = false;

		working.clear();
		working.addAll(PartyList.get());
		rebuildRows();
	}

	@Override
	protected List<RosterRow> allRows() {
		List<RosterRow> rows = new ArrayList<>(working.size());

		for (int i = 0; i < working.size(); i++) {
			rows.add(new PartyRow(i));
		}

		return rows;
	}

	@Override
	protected Component emptyMessage() {
		return Component.translatable("dyeutils.partyList.empty");
	}

	@Override
	protected Component noMatchesMessage() {
		return Component.translatable("dyeutils.partyList.noMatches", filter);
	}

	@Override
	protected Component searchHint() {
		return Component.translatable("dyeutils.partyList.searchHint");
	}

	// Against the edit buffer, not the saved list: the buffer is what Done writes.
	@Override
	protected boolean alreadyListed(String name) {
		return Ign.contains(working, name);
	}

	@Override
	protected void addTyped(String name) {
		working.add(name);
		rebuildRows();
	}

	@Override
	protected void buildFooter(LinearLayout footer) {
		footer.addChild(Button.builder(Component.translatable("dyeutils.partyList.add"), button -> addRow())
				.width(ROW_WIDTH)
				.build());

		LinearLayout bottom = footer.addChild(LinearLayout.horizontal().spacing(GAP));
		int half = (ROW_WIDTH - GAP) / 2;

		bottom.addChild(Button.builder(Component.translatable("dyeutils.partyList.favourites"), button -> openFavourites())
				.width(half)
				.build());

		bottom.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.width(half)
				.build());
	}

	private void openFavourites() {
		commit();
		reloadOnReturn = true;
		minecraft.setScreen(new FavouritesScreen(this));
	}

	private void addRow() {
		// Otherwise the new row is hidden by the filter the moment it is created.
		clearFilter();

		working.add("");
		rebuildRows();
		entries.focusLast();
	}

	private void removeRow(int index) {
		working.remove(index);
		rebuildRows();
	}

	// Rows hidden by the filter are saved with the rest. Filtered out is not deleted.
	private void commit() {
		// A blank row is an abandoned add. Malformed names are kept as typed and reported on invite.
		List<String> saved = new ArrayList<>();

		for (String name : working) {
			String trimmed = name.trim();
			if (!trimmed.isEmpty()) saved.add(trimmed);
		}

		PartyList.set(saved);
	}

	@Override
	public void onClose() {
		commit();
		super.onClose();
	}

	private class PartyRow extends RosterRow {
		private final EditBox field;
		private final Button star;
		private final Button remove;

		PartyRow(int index) {
			field = new EditBox(font, ROW_WIDTH, WIDGET_HEIGHT, Component.translatable("dyeutils.partyList.ign"));
			field.setMaxLength(16);
			field.setValue(working.get(index));

			star = Button.builder(Component.translatable("dyeutils.partyList.star"), button -> toggleFavourite())
					.width(BUTTON_WIDTH)
					.build();

			remove = Button.builder(Component.translatable("dyeutils.partyList.remove"), button -> removeRow(index))
					.width(BUTTON_WIDTH)
					.build();

			// Every keystroke lands in the buffer before anything can rebuild the rows, which is what
			// stops a resize losing what is typed.
			field.setResponder(value -> {
				working.set(index, value);
				refreshStar();
			});

			refreshStar();
			showHeadFor(field.getValue());
		}

		private void refreshStar() {
			String name = field.getValue().trim();
			boolean valid = Ign.isValid(name);

			star.active = valid;
			star.setMessage(Component.translatable(valid && Favourites.contains(name)
					? "dyeutils.partyList.unstar"
					: "dyeutils.partyList.star"));
			star.setTooltip(valid ? null : Tooltip.create(Component.translatable("dyeutils.partyList.starHint")));
		}

		private void toggleFavourite() {
			Favourites.toggle(field.getValue().trim());
			refreshStar();
		}

		@Override
		public String name() {
			return field.getValue();
		}

		@Override
		protected void focusPrimary() {
			setFocused(field);
			field.setFocused(true);
		}

		@Override
		protected void arrange() {
			int y = widgetY();
			int buttons = (BUTTON_WIDTH + GAP) * 2;
			int fieldWidth = getContentRight() - contentLeft() - buttons;

			field.setWidth(fieldWidth);
			field.setX(contentLeft());
			field.setY(y);

			star.setX(getContentRight() - buttons + GAP);
			star.setY(y);

			remove.setX(getContentRight() - BUTTON_WIDTH);
			remove.setY(y);
		}

		@Override
		protected void extractWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			// Only once they stop typing, or it looks up every prefix of the name.
			if (!field.isFocused()) showHeadFor(field.getValue());

			field.extractRenderState(graphics, mouseX, mouseY, partialTick);
			star.extractRenderState(graphics, mouseX, mouseY, partialTick);
			remove.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(field, star, remove);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(field, star, remove);
		}
	}
}
