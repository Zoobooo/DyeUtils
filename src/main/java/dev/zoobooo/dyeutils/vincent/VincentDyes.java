package dev.zoobooo.dyeutils.vincent;

import java.util.List;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.mixin.ContainerScreenAccessor;
import dev.zoobooo.dyeutils.util.Failsafe;
import dev.zoobooo.dyeutils.vincent.HiddenDyes.Hidden;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// Vincent's menu shows this year's three boosted dyes outright, so opening it is the whole reveal. This
// paints over them until they are clicked.
//
// Nothing here touches the container. The real stacks stay exactly as Hypixel sent them, so the server,
// the tooltip the player eventually sees, and any other mod reading the menu all still see the truth --
// this only declines to draw it.
public final class VincentDyes {
	public static final VincentDyes INSTANCE = new VincentDyes();

	private static final String DYES_TITLE = "Dyes";

	private static final Identifier HIDDEN_DYE = DyeUtils.id("vincent/hidden_dye");

	// The interior of a vanilla slot, painted first so a placeholder with any transparency in it cannot
	// leak the silhouette of the dye underneath.
	private static final int SLOT_BACKGROUND = 0xFF8B8B8B;

	private static final int SLOT_SIZE = 16;
	private static final int CHEST_ROWS = 6;

	private @Nullable DyeRoulette roulette;

	private VincentDyes() {
	}

	public void init() {
		// Fires again on every resize, and Fabric clears the per-screen listeners first, so registering
		// each time is both correct and required. Nothing durable may live in the closure.
		ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
			if (!isDyesMenu(screen)) return;

			ScreenMouseEvents.allowMouseClick(screen).register(this::allowMouseClick);
			ScreenKeyboardEvents.allowKeyPress(screen).register(this::allowKeyPress);

			// The reveal is drawn here rather than in the pre-tooltip window on purpose: afterExtract is
			// above everything, tooltips included, which is what a modal overlay wants.
			ScreenEvents.afterExtract(screen).register((extracted, graphics, mouseX, mouseY, tick) ->
					Failsafe.run("Vincent reveal", () -> drawRoulette(extracted, graphics)));

			// Covers the server closing the container, another mod swapping the screen, and quitting.
			ScreenEvents.remove(screen).register(removed -> cancel());
		});
	}

	static boolean isDyesMenu(Screen screen) {
		return screen instanceof ContainerScreen chest
				&& chest.getMenu().getRowCount() == CHEST_ROWS
				&& DYES_TITLE.equals(DyeLore.strip(screen.getTitle().getString()).trim());
	}

	// Called from ScreenTooltipMixin, after the slots are drawn and before the tooltip is flushed.
	public void beforeTooltips(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!DyeUtils.config().vincentReveal) return;
		if (!(screen instanceof ContainerScreen chest) || !isDyesMenu(chest)) return;

		Failsafe.run("Vincent dye placeholders", () -> cover(chest, graphics, mouseX, mouseY));
	}

	// Whether a reveal is on screen at all. The HUD is drawn before any of this and has no idea a screen
	// is open, so it only needs the yes or no.
	public boolean isRevealing() {
		return DyeUtils.config().vincentReveal && roulette != null;
	}

	// The reveal replaces the menu rather than sitting over it, so the menu's own render is skipped for
	// those frames. Read by ContainerScreenRenderMixin.
	public boolean hidesMenu(Screen screen) {
		return DyeUtils.config().vincentReveal && roulette != null && roulette.isFor(screen);
	}

	private void cover(ContainerScreen chest, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		// Nothing to cover while the reveal is playing: the menu is not being drawn at all.
		if (hidesMenu(chest)) return;

		HiddenDyes dyes = HiddenDyes.of(chest);
		if (dyes.isEmpty()) return;

		VincentState state = VincentState.get();

		// The year comes off the dyes themselves, so this is also how a new year re-hides all three.
		if (state.syncYear(dyes.year())) state.save();

		for (Hidden dye : dyes.entries()) {
			if (state.isRevealed(dye.name())) continue;

			int x = slotX(chest, dye);
			int y = slotY(chest, dye);

			graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HIDDEN_DYE, x, y, SLOT_SIZE, SLOT_SIZE);

			if (((ContainerScreenAccessor) chest).dyeutils$hoveredSlot() == dye.slot()) {
				replaceTooltip(graphics, mouseX, mouseY);
			}
		}
	}

	private static void replaceTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<FormattedCharSequence> lines = List.of(
				Component.translatable("dyeutils.vincent.hidden")
						.withStyle(ChatFormatting.LIGHT_PURPLE).getVisualOrderText(),
				Component.translatable("dyeutils.vincent.hiddenHint")
						.withStyle(ChatFormatting.GRAY).getVisualOrderText());

		// Vanilla has already claimed this frame's tooltip with the real dye's, hence override = true.
		// An empty list would be ignored rather than clearing it, which is why this replaces instead.
		graphics.setTooltipForNextFrame(Minecraft.getInstance().font, lines,
				DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, true);
	}

	private boolean allowMouseClick(Screen screen, MouseButtonEvent event) {
		try {
			return !swallow(screen, event);
		} catch (Throwable t) {
			// A cosmetic mod that eats the player's clicks is worse than one that does nothing.
			Failsafe.report("Vincent dye click", t);

			return true;
		}
	}

	private boolean swallow(Screen screen, MouseButtonEvent event) {
		if (!DyeUtils.config().vincentReveal) return false;
		if (!(screen instanceof ContainerScreen chest) || !isDyesMenu(chest)) return false;

		// While the reveal is playing, nothing reaches the menu underneath it.
		if (roulette != null && roulette.isFor(screen)) return true;

		Hidden hit = under(chest, event.x(), event.y());
		if (hit == null) return false;

		start(chest, hit);

		// Swallowed whole, so no click reaches slotClicked and nothing goes to the server. Right-click
		// counts too: on a real dye it opens a preview, which would give the answer away outright.
		return true;
	}

	private @Nullable Hidden under(ContainerScreen chest, double mouseX, double mouseY) {
		HiddenDyes dyes = HiddenDyes.of(chest);
		VincentState state = VincentState.get();

		for (Hidden dye : dyes.entries()) {
			if (state.isRevealed(dye.name())) continue;

			// The same rectangle the cover was painted in, so a click can only ever be swallowed where
			// the player can actually see a placeholder.
			if (over(slotX(chest, dye), slotY(chest, dye), mouseX, mouseY)) return dye;
		}

		return null;
	}

	private boolean allowKeyPress(Screen screen, KeyEvent event) {
		try {
			if (roulette == null || !roulette.isFor(screen)) return true;

			// Escape gets out of the reveal without closing the menu. A second press closes it as usual,
			// and the dye stays hidden so it can be clicked again.
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) cancel();

			return false;
		} catch (Throwable t) {
			Failsafe.report("Vincent reveal keys", t);

			return true;
		}
	}

	private void start(ContainerScreen chest, Hidden dye) {
		ItemStack stack = dye.slot().getItem();
		DyeIcons.Dye winner = DyeIcons.read(stack);

		// Unreachable while the snapshot exists, since it only exists because this same read succeeded.
		if (winner == null) return;

		long duration = Math.clamp(DyeUtils.config().vincentRevealSeconds, 1, 10) * 1000L;

		roulette = DyeRoulette.start(chest, winner, dye.multiplier(), duration);
	}

	private void drawRoulette(Screen screen, GuiGraphicsExtractor graphics) {
		DyeRoulette active = roulette;

		if (active == null || !active.isFor(screen)) return;

		if (!DyeUtils.config().vincentReveal) {
			cancel();

			return;
		}

		if (active.draw(graphics)) return;

		// Marked revealed only once the reveal has played out, so a spin cut short by Escape or by the
		// menu closing leaves the dye hidden and clickable again.
		VincentState state = VincentState.get();

		if (state.reveal(active.winnerName())) state.save();

		roulette = null;
	}

	private void cancel() {
		// Dropped rather than paused: this is the only strong reference to the screen, its menu and the
		// fifty stacks on the strip.
		roulette = null;
	}

	private static int slotX(ContainerScreen chest, Hidden dye) {
		return ((ContainerScreenAccessor) chest).dyeutils$leftPos() + dye.slot().x;
	}

	private static int slotY(ContainerScreen chest, Hidden dye) {
		return ((ContainerScreenAccessor) chest).dyeutils$topPos() + dye.slot().y;
	}

	// Vanilla's slot box, margin included: isHovering tests mouseX >= x - 1, so the hoverable area is
	// 18x18 around a 16x16 slot. Matching it means a click on the border ring is swallowed exactly where
	// the tooltip is replaced.
	private static boolean over(int x, int y, double mouseX, double mouseY) {
		return mouseX >= x - 1 && mouseX < x + SLOT_SIZE + 1
				&& mouseY >= y - 1 && mouseY < y + SLOT_SIZE + 1;
	}
}
