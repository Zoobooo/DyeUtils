package dev.zoobooo.dyeutils.gui;

import java.util.Locale;
import java.util.function.Supplier;

import dev.zoobooo.dyeutils.skin.PlayerHeads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.world.entity.player.PlayerSkin;

import org.jspecify.annotations.Nullable;

public abstract class RosterRow extends ContainerObjectSelectionList.Entry<RosterRow> {
	static final int WIDGET_HEIGHT = 20;
	static final int GAP = 4;

	static final int BUTTON_WIDTH = 56;

	private static final int HOVER_FILL = 0x30FFFFFF;

	static final int HEAD_SIZE = 16;

	private String headName = "";
	private @Nullable Supplier<PlayerSkin> head;

	public abstract String name();

	protected abstract void arrange();

	protected abstract void extractWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);

	protected void focusPrimary() {
	}

	public boolean matches(String filter) {
		if (filter.isEmpty()) return true;

		return name().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
	}

	protected int widgetY() {
		return getContentY() + (getContentHeight() - WIDGET_HEIGHT) / 2;
	}

	protected int contentLeft() {
		return getContentX() + HEAD_SIZE + GAP;
	}

	// Not driven from name(): a row being typed into would ask about every prefix.
	protected void showHeadFor(String name) {
		String trimmed = name.trim();
		if (trimmed.equalsIgnoreCase(headName)) return;

		headName = trimmed;
		head = trimmed.isEmpty() ? null : PlayerHeads.lookup(trimmed);
	}

	@Override
	public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
		if (hovered) {
			graphics.fill(getContentX(), getContentY(), getContentRight(), getContentBottom(), HOVER_FILL);
		}

		if (head != null) {
			PlayerFaceExtractor.extractRenderState(graphics, head.get(),
					getContentX(), getContentY() + (getContentHeight() - HEAD_SIZE) / 2, HEAD_SIZE);
		}

		extractWidgets(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		arrange();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		arrange();
	}

	@Override
	public void setWidth(int width) {
		super.setWidth(width);
		arrange();
	}

	@Override
	public void setHeight(int height) {
		super.setHeight(height);
		arrange();
	}
}
