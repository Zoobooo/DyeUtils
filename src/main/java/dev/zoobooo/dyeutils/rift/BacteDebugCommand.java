package dev.zoobooo.dyeutils.rift;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class BacteDebugCommand {
	private static final double RADIUS = 32.0;
	private static final int MAX_LINES = 20;

	public static LiteralArgumentBuilder<FabricClientCommandSource> build() {
		return literal("debug").then(literal("slimes").executes(BacteDebugCommand::dump));
	}

	private static int dump(CommandContext<FabricClientCommandSource> context) {
		Minecraft client = Minecraft.getInstance();

		if (client.level == null || client.player == null) return Command.SINGLE_SUCCESS;

		List<String> lines = new ArrayList<>();

		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity.distanceTo(client.player) > RADIUS) continue;

			Component name = entity.getCustomName();
			boolean interesting = entity instanceof Slime || name != null;
			if (!interesting) continue;

			StringBuilder line = new StringBuilder(entity.getType().toShortString());

			if (entity instanceof Slime slime) {
				line.append(" size=").append(slime.getSize());
				line.append(" bacte=").append(BacteTracker.INSTANCE.isBacte(slime));
			}

			line.append(" dist=").append(Math.round(entity.distanceTo(client.player)));

			if (name != null) {
				line.append(" name=\"").append(name.getString()).append('"');
				line.append(" matches=").append(BacteNames.isBacte(name.getString()));
			} else {
				line.append(" unnamed");
			}

			lines.add(line.toString());
		}

		if (lines.isEmpty()) {
			DyeUtils.feedback(Component.literal("No slimes or named entities within " + (int) RADIUS + " blocks."));

			return Command.SINGLE_SUCCESS;
		}

		DyeUtils.feedback(Component.literal(lines.size() + " nearby entity(s):"));

		for (String line : lines.subList(0, Math.min(lines.size(), MAX_LINES))) {
			DyeUtils.feedback(Component.literal(line).withStyle(ChatFormatting.WHITE));
		}

		if (lines.size() > MAX_LINES) {
			DyeUtils.feedback(Component.literal("...and " + (lines.size() - MAX_LINES) + " more."));
		}

		return Command.SINGLE_SUCCESS;
	}
}
