package dev.zoobooo.dyeutils;

import java.util.ArrayDeque;
import java.util.Queue;

import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;

import dev.zoobooo.dyeutils.config.DyeUtilsConfig;
import dev.zoobooo.dyeutils.config.DyeUtilsConfigScreen;
import dev.zoobooo.dyeutils.keybind.DyeUtilsKeys;
import dev.zoobooo.dyeutils.party.FavouritesCommand;
import dev.zoobooo.dyeutils.party.PartyDropOut;
import dev.zoobooo.dyeutils.party.PartyInviteQueue;
import dev.zoobooo.dyeutils.party.PartyInviter;
import dev.zoobooo.dyeutils.party.PartyListCommand;
import dev.zoobooo.dyeutils.rift.AutoDisband;
import dev.zoobooo.dyeutils.rift.BacteTracker;
import dev.zoobooo.dyeutils.rift.DyeSlimeRenderer;
import dev.zoobooo.dyeutils.util.MessageScheduler;
import dev.zoobooo.dyeutils.vincent.DyeHarvest;
import dev.zoobooo.dyeutils.vincent.VincentDyes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import org.slf4j.Logger;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class DyeUtils implements ClientModInitializer {
	public static final String NAMESPACE = "dyeutils";
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Component PREFIX = Component.literal("[DyeUtils] ").withStyle(ChatFormatting.AQUA);

	// A command callback runs while the chat screen is still closing, so setScreen waits a tick.
	private static final Queue<Screen> PENDING_SCREENS = new ArrayDeque<>();

	@Override
	public void onInitializeClient() {
		DyeUtilsConfig.load();
		DyeUtilsKeys.init();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				literal(NAMESPACE)
						.executes(context -> openScreenLater(DyeUtilsConfigScreen.create(null)))
						.then(PartyListCommand.build())
						.then(FavouritesCommand.build())));

		EntityRendererRegistry.register(EntityType.SLIME, DyeSlimeRenderer::new);

		VincentDyes.INSTANCE.init();

		ClientTickEvents.END_CLIENT_TICK.register(DyeUtils::onEndTick);

		// Overlay messages are the action bar, never party chatter.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (overlay) return;

			PartyInviteQueue.INSTANCE.onGameMessage(message);
			PartyDropOut.INSTANCE.onGameMessage(message.getString());
			AutoDisband.INSTANCE.onGameMessage(message.getString());
		});

		LOGGER.info("[DyeUtils] Initialised.");
	}

	private static void onEndTick(Minecraft client) {
		while (DyeUtilsKeys.INVITE_PARTY.consumeClick()) {
			PartyInviter.inviteAll();
		}

		while (DyeUtilsKeys.PARTY_WARP.consumeClick()) {
			PartyInviter.warp();
		}

		PartyInviteQueue.INSTANCE.tick();
		MessageScheduler.INSTANCE.tick();
		BacteTracker.INSTANCE.tick(client);
		DyeHarvest.INSTANCE.tick(client);

		Screen pending = PENDING_SCREENS.poll();
		if (pending != null) client.setScreen(pending);
	}

	public static int openScreenLater(Screen screen) {
		PENDING_SCREENS.add(screen);
		return Command.SINGLE_SUCCESS;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(NAMESPACE, path);
	}

	public static DyeUtilsConfig config() {
		return DyeUtilsConfig.get();
	}

	public static void feedback(Component message) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;

		client.player.sendSystemMessage(PREFIX.copy().append(message.copy().withStyle(ChatFormatting.GRAY)));
	}
}
