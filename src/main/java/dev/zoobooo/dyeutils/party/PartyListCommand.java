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

public class PartyListCommand {
	public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
		return literal("playerlist")
				.executes(PartyListCommand::show)
				.then(literal("add")
						.then(argument("ign", StringArgumentType.word())
								.executes(context -> add(context, StringArgumentType.getString(context, "ign")))))
				.then(literal("remove")
						.then(argument("ign", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(PartyList.get(), builder))
								.executes(context -> remove(context, StringArgumentType.getString(context, "ign")))));
	}

	private static int show(CommandContext<FabricClientCommandSource> context) {
		List<String> members = PartyList.get();

		if (members.isEmpty()) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.listEmpty"));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.list", members.size(),
					Component.literal(String.join(", ", members)).withStyle(ChatFormatting.WHITE)));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int add(CommandContext<FabricClientCommandSource> context, String ign) {
		if (PartyList.add(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.added", ign));
		} else if (PartyList.contains(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.alreadyOnList", ign));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.invalidIgn", ign));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int remove(CommandContext<FabricClientCommandSource> context, String ign) {
		if (PartyList.remove(ign)) {
			DyeUtils.feedback(Component.translatable("dyeutils.message.removed", ign));
		} else {
			DyeUtils.feedback(Component.translatable("dyeutils.message.notOnList", ign));
		}

		return Command.SINGLE_SUCCESS;
	}
}
