package dev.zoobooo.dyeutils.gui;

import java.util.ArrayList;
import java.util.List;

import dev.zoobooo.dyeutils.skin.PlayerHeads;
import dev.zoobooo.dyeutils.util.Ign;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

public abstract class RosterScreen extends Screen {
	protected static final int ROW_WIDTH = 320;
	protected static final int ROW_HEIGHT = 24;
	protected static final int WIDGET_HEIGHT = 20;
	protected static final int GAP = 4;

	private static final int HEADER_HEIGHT = 24 + WIDGET_HEIGHT + GAP;
	private static final int FOOTER_HEIGHT = 64;

	private static final int ADD_BUTTON_WIDTH = 40;

	private final @Nullable Screen parent;

	protected String filter = "";

	protected HeaderAndFooterLayout layout;
	protected RosterList entries;

	private Button add;
	private int totalRows;

	protected RosterScreen(Component title, @Nullable Screen parent) {
		super(title);
		this.parent = parent;
	}

	protected abstract List<RosterRow> allRows();

	protected abstract Component emptyMessage();

	protected abstract Component noMatchesMessage();

	protected abstract Component searchHint();

	protected abstract void buildFooter(LinearLayout footer);

	protected abstract boolean alreadyListed(String name);

	protected abstract void addTyped(String name);

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);

		LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(GAP));
		header.addChild(new StringWidget(title, font));

		LinearLayout searchRow = header.addChild(LinearLayout.horizontal().spacing(GAP));
		searchRow.addChild(buildSearchBox());

		add = searchRow.addChild(Button.builder(Component.translatable("dyeutils.roster.add"), button -> addSearched())
				.width(ADD_BUTTON_WIDTH)
				.build());

		entries = layout.addToContents(
				new RosterList(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), ROW_HEIGHT, ROW_WIDTH));

		buildFooter(layout.addToFooter(LinearLayout.vertical().spacing(GAP)));

		layout.visitWidgets(this::addRenderableWidget);
		rebuildRows();
		repositionElements();
	}

	private void addSearched() {
		String name = filter.trim();
		if (!Ign.isValid(name) || alreadyListed(name)) return;

		addTyped(name);
		clearFilter();
		entries.focusLast();
	}

	private void refreshAddButton() {
		if (add == null) return;

		String name = filter.trim();
		boolean addable = Ign.isValid(name) && !alreadyListed(name);

		add.active = addable;
		add.setTooltip(addable ? null : Tooltip.create(Component.translatable("dyeutils.roster.addHint")));
	}

	private EditBox buildSearchBox() {
		EditBox search = new EditBox(font, ROW_WIDTH - ADD_BUTTON_WIDTH - GAP, WIDGET_HEIGHT,
				Component.translatable("dyeutils.roster.search"));
		search.setMaxLength(16);
		search.setHint(searchHint());

		// Before the responder: setValue would otherwise rebuild the rows from inside init.
		search.setValue(filter);
		search.setResponder(value -> {
			filter = value;
			rebuildRows();
		});

		return search;
	}

	protected void rebuildRows() {
		if (entries == null) return;

		List<RosterRow> all = allRows();
		List<RosterRow> visible = new ArrayList<>(all.size());
		List<String> names = new ArrayList<>(all.size());

		for (RosterRow row : all) {
			names.add(row.name());
			if (row.matches(filter)) visible.add(row);
		}

		totalRows = all.size();
		entries.rebuild(visible);
		refreshAddButton();

		PlayerHeads.prefetch(names);
	}

	protected void clearFilter() {
		if (filter.isEmpty()) return;

		filter = "";
		rebuildWidgets();
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
		entries.updateSize(width, layout);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		if (!entries.children().isEmpty()) return;

		// Two different situations, and one message for both reads as the list having been eaten.
		Component message = totalRows == 0 ? emptyMessage() : noMatchesMessage();

		graphics.centeredText(font, message.copy().withStyle(ChatFormatting.GRAY),
				width / 2, layout.getHeaderHeight() + layout.getContentHeight() / 2, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
