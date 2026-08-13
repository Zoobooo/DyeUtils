package dev.zoobooo.dyeutils.config;

import dev.zoobooo.dyeutils.DyeUtils;
import dev.zoobooo.dyeutils.gui.FavouritesScreen;
import dev.zoobooo.dyeutils.gui.PartyListScreen;
import dev.zoobooo.dyeutils.keybind.DyeUtilsKeys;
import net.azureaaron.dandelion.api.ButtonOption;
import net.azureaaron.dandelion.api.ConfigCategory;
import net.azureaaron.dandelion.api.ConfigType;
import net.azureaaron.dandelion.api.DandelionConfigScreen;
import net.azureaaron.dandelion.api.KeyMappingOption;
import net.azureaaron.dandelion.api.Option;
import net.azureaaron.dandelion.api.OptionGroup;
import net.azureaaron.dandelion.api.controllers.BooleanController;
import net.azureaaron.dandelion.api.controllers.IntegerController;
import net.azureaaron.dandelion.api.controllers.StringController;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

public class DyeUtilsConfigScreen {
	public static Screen create(@Nullable Screen parent) {
		return DandelionConfigScreen.create(DyeUtilsConfig.manager(), (defaults, config, builder) -> builder
				.title(Component.translatable("dyeutils.config.title"))
				.category(ConfigCategory.createBuilder()
						.id(DyeUtils.id("config/vincent"))
						.name(Component.translatable("dyeutils.config.category.vincent"))
						.group(vincent(defaults, config))
						.build())
				.category(ConfigCategory.createBuilder()
						.id(DyeUtils.id("config/celadon"))
						.name(Component.translatable("dyeutils.config.category.celadon"))
						.group(party(defaults, config))
						.group(hotkeys())
						.group(bacte(defaults, config))
						.group(advanced(defaults, config))
						.build()))
				.generateScreen(parent, ConfigType.MOUL_CONFIG);
	}

	private static OptionGroup party(DyeUtilsConfig defaults, DyeUtilsConfig config) {
		return OptionGroup.createBuilder()
				.id(DyeUtils.id("config/party"))
				.name(Component.translatable("dyeutils.config.group.party"))
				.description(Component.translatable("dyeutils.config.group.party.desc"))
				.option(ButtonOption.createBuilder()
						.id(DyeUtils.id("config/edit_party_members"))
						.name(Component.translatable("dyeutils.config.partyMembers.edit"))
						.description(Component.translatable("dyeutils.config.partyMembers.edit.desc"))
						.prompt(Component.translatable("dyeutils.config.partyMembers.editPrompt"))
						.action(screen -> Minecraft.getInstance().setScreen(new PartyListScreen(screen)))
						.build())
				.option(ButtonOption.createBuilder()
						.id(DyeUtils.id("config/edit_favourites"))
						.name(Component.translatable("dyeutils.config.favourites.edit"))
						.description(Component.translatable("dyeutils.config.favourites.edit.desc"))
						.prompt(Component.translatable("dyeutils.config.partyMembers.editPrompt"))
						.action(screen -> Minecraft.getInstance().setScreen(new FavouritesScreen(screen)))
						.build())
				.option(Option.<Boolean>createBuilder()
						.id(DyeUtils.id("config/auto_disband"))
						.name(Component.translatable("dyeutils.config.autoDisband"))
						.description(Component.translatable("dyeutils.config.autoDisband.desc"))
						.binding(defaults.autoDisband, () -> config.autoDisband, value -> config.autoDisband = value)
						.controller(BooleanController.createBuilder()
								.booleanStyle(BooleanController.BooleanStyle.ON_OFF)
								.coloured(true)
								.build())
						.build())
				.build();
	}

	private static OptionGroup hotkeys() {
		return OptionGroup.createBuilder()
				.id(DyeUtils.id("config/hotkeys"))
				.name(Component.translatable("dyeutils.config.group.hotkeys"))
				.description(Component.translatable("dyeutils.config.group.hotkeys.desc"))
				.option(hotkey(DyeUtilsKeys.INVITE_PARTY, "invite"))
				.option(hotkey(DyeUtilsKeys.PARTY_WARP, "warp"))
				.build();
	}

	private static KeyMappingOption hotkey(KeyMapping mapping, String name) {
		return KeyMappingOption.createBuilder()
				.id(DyeUtils.id("config/hotkey_" + name))
				.name(Component.translatable("dyeutils.config.hotkey." + name))
				.description(Component.translatable("dyeutils.config.hotkey." + name + ".desc"))
				.keyMapping(mapping)
				.build();
	}

	private static OptionGroup bacte(DyeUtilsConfig defaults, DyeUtilsConfig config) {
		return OptionGroup.createBuilder()
				.id(DyeUtils.id("config/bacte"))
				.name(Component.translatable("dyeutils.config.group.bacte"))
				.description(Component.translatable("dyeutils.config.group.bacte.desc"))
				.option(Option.<Boolean>createBuilder()
						.id(DyeUtils.id("config/bacte_skin"))
						.name(Component.translatable("dyeutils.config.bacteSkin"))
						.description(Component.translatable("dyeutils.config.bacteSkin.desc"))
						.binding(defaults.bacteSkin, () -> config.bacteSkin, value -> config.bacteSkin = value)
						.controller(BooleanController.createBuilder()
								.booleanStyle(BooleanController.BooleanStyle.ON_OFF)
								.coloured(true)
								.build())
						.build())
				.build();
	}

	private static OptionGroup vincent(DyeUtilsConfig defaults, DyeUtilsConfig config) {
		return OptionGroup.createBuilder()
				.id(DyeUtils.id("config/vincent_dyes"))
				.name(Component.translatable("dyeutils.config.group.vincent"))
				.description(Component.translatable("dyeutils.config.group.vincent.desc"))
				.option(Option.<Boolean>createBuilder()
						.id(DyeUtils.id("config/vincent_reveal"))
						.name(Component.translatable("dyeutils.config.vincentReveal"))
						.description(Component.translatable("dyeutils.config.vincentReveal.desc"))
						.binding(defaults.vincentReveal, () -> config.vincentReveal, value -> config.vincentReveal = value)
						.controller(BooleanController.createBuilder()
								.booleanStyle(BooleanController.BooleanStyle.ON_OFF)
								.coloured(true)
								.build())
						.build())
				.option(Option.<Integer>createBuilder()
						.id(DyeUtils.id("config/vincent_reveal_seconds"))
						.name(Component.translatable("dyeutils.config.vincentRevealSeconds"))
						.description(Component.translatable("dyeutils.config.vincentRevealSeconds.desc"))
						.binding(defaults.vincentRevealSeconds, () -> config.vincentRevealSeconds,
								value -> config.vincentRevealSeconds = value)
						.controller(IntegerController.createBuilder()
								.range(1, 10)
								.slider(1)
								.build())
						.build())
				.build();
	}

	private static OptionGroup advanced(DyeUtilsConfig defaults, DyeUtilsConfig config) {
		return OptionGroup.createBuilder()
				.id(DyeUtils.id("config/advanced"))
				.name(Component.translatable("dyeutils.config.group.advanced"))
				.collapsed(true)
				.option(Option.<String>createBuilder()
						.id(DyeUtils.id("config/invite_command_prefix"))
						.name(Component.translatable("dyeutils.config.inviteCommandPrefix"))
						.description(Component.translatable("dyeutils.config.inviteCommandPrefix.desc"))
						.binding(defaults.inviteCommandPrefix, () -> config.inviteCommandPrefix, value -> config.inviteCommandPrefix = value)
						.controller(StringController.createBuilder().build())
						.build())
				.option(Option.<Boolean>createBuilder()
						.id(DyeUtils.id("config/hide_commands"))
						.name(Component.translatable("dyeutils.config.hideCommandsFromChat"))
						.description(Component.translatable("dyeutils.config.hideCommandsFromChat.desc"))
						.binding(defaults.hideCommandsFromChat, () -> config.hideCommandsFromChat, value -> config.hideCommandsFromChat = value)
						.controller(BooleanController.createBuilder()
								.booleanStyle(BooleanController.BooleanStyle.ON_OFF)
								.coloured(true)
								.build())
						.build())
				.build();
	}
}
