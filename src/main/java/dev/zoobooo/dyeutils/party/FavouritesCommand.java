package dev.zoobooo.dyeutils.party;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class FavouritesCommand {
	public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
		return literal("favourites")
				.executes(FavouritesCommand::show)
				.then(literal("add")
						.then(argument("ign", StringArgumentType.word())
								.executes(context -> add(StringArgumentType.getString(context, "ign")))))
				.then(literal("remove")
						.then(argument("ign", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(Favourites.get(), builder))
								.executes(context -> remove(StringArgumentType.getString(context, "ign")))))
				.then(literal("clear")
						.executes(FavouritesCommand::clear));
	}

	private static int show(CommandContext<FabricClientCommandSource> context) {
		List<String> names = Favourites.get();

		if (names.isEmpty()) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouritesEmpty"));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouritesList", names.size(),
					Component.literal(String.join(", ", names)).withStyle(ChatFormatting.WHITE)));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int add(String ign) {
		if (Favourites.add(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouriteAdded", ign));
		} else if (Favourites.contains(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.alreadyOnList", ign));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.invalidIgn", ign));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int remove(String ign) {
		if (Favourites.remove(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouriteRemoved", ign));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.notFavourite", ign));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int clear(CommandContext<FabricClientCommandSource> context) {
		int size = Favourites.get().size();

		if (size == 0) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouritesEmpty"));
		} else {
			Favourites.clear();
			DyeUtils.feedback(Component.translatable("dyeutils.message.favouritesCleared", size));
		}

		return Command.SINGLE_SUCCESS;
	}
}
