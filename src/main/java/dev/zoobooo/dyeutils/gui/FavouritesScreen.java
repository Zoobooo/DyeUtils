package dev.zoobooo.dyeutils.gui;

import java.util.ArrayList;
import java.util.List;

import dev.zoobooo.dyeutils.party.Favourites;
import dev.zoobooo.dyeutils.party.PartyList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

public class FavouritesScreen extends RosterScreen {
	public FavouritesScreen(@Nullable Screen parent) {
		super(Component.translatable("dyeutils.favourites.title"), parent);
	}

	@Override
	protected List<RosterRow> allRows() {
		List<RosterRow> rows = new ArrayList<>();

		for (String name : Favourites.get()) {
			rows.add(new FavouriteRow(name));
		}

		return rows;
	}

	@Override
	protected Component emptyMessage() {
		return Component.translatable("dyeutils.favourites.empty");
	}

	@Override
	protected Component noMatchesMessage() {
		return Component.translatable("dyeutils.favourites.noMatches", filter);
	}

	@Override
	protected Component searchHint() {
		return Component.translatable("dyeutils.favourites.searchHint");
	}

	@Override
	protected boolean alreadyListed(String name) {
		return Favourites.contains(name);
	}

	@Override
	protected void addTyped(String name) {
		Favourites.add(name);
		rebuildRows();
	}

	@Override
	protected void buildFooter(LinearLayout footer) {
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.width(ROW_WIDTH)
				.build());
	}

	private void addToParty(String name) {
		if (!PartyList.add(name)) return;

		rebuildRows();
	}

	private class FavouriteRow extends RosterRow {
		private final String name;
		private final Button addToParty;
		private final Button remove;

		private int labelX;
		private int labelY;

		FavouriteRow(String name) {
			this.name = name;

			addToParty = Button.builder(Component.translatable("dyeutils.favourites.addToParty"),
							button -> addToParty(name))
					.width(BUTTON_WIDTH)
					.build();

			addToParty.active = !PartyList.contains(name);
			if (!addToParty.active) {
				addToParty.setTooltip(Tooltip.create(Component.translatable("dyeutils.favourites.alreadyOnParty")));
			}

			remove = Button.builder(Component.translatable("dyeutils.favourites.remove"), button -> {
				Favourites.remove(name);
				rebuildRows();
			}).width(BUTTON_WIDTH).build();

			showHeadFor(name);
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		protected void arrange() {
			int y = widgetY();
			int buttons = (BUTTON_WIDTH + GAP) * 2;

			// Drawn, not a widget: StringWidget cannot be left-aligned.
			labelX = contentLeft();
			labelY = getContentYMiddle() - font.lineHeight / 2;

			addToParty.setX(getContentRight() - buttons + GAP);
			addToParty.setY(y);

			remove.setX(getContentRight() - BUTTON_WIDTH);
			remove.setY(y);
		}

		@Override
		protected void extractWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			graphics.text(font, name, labelX, labelY, 0xFFFFFFFF);

			addToParty.extractRenderState(graphics, mouseX, mouseY, partialTick);
			remove.extractRenderState(graphics, mouseX, mouseY, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(addToParty, remove);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(addToParty, remove);
		}
	}
}
