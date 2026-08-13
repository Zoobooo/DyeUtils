package dev.zoobooo.dyeutils.vincent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import org.jspecify.annotations.Nullable;

// Dye icons are player heads, so a dye can be rebuilt from the texture blob the menu was already
// carrying. Same reasoning as PlayerHeads: SkinManager reads the textures property off the profile
// locally, so a profile assembled here runs the whole vanilla download-and-cache path unchanged, and
// the roulette needs no network of its own.
final class DyeIcons {
	// Keyed by texture rather than name, so a dye Hypixel has re-drawn does not keep its old icon.
	private static final Map<String, ItemStack> STACKS = new HashMap<>();

	private DyeIcons() {
	}

	record Dye(String name, ItemStack stack, int colour) {
	}

	static List<String> lore(ItemStack stack) {
		List<Component> lines = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines();
		List<String> plain = new ArrayList<>(lines.size());

		for (Component line : lines) {
			plain.add(line.getString());
		}

		return plain;
	}

	static String name(ItemStack stack) {
		return DyeLore.strip(stack.getHoverName().getString()).trim();
	}

	static @Nullable String texture(ItemStack stack) {
		ResolvableProfile profile = stack.get(DataComponents.PROFILE);
		if (profile == null) return null;

		for (Property property : profile.partialProfile().properties().get("textures")) {
			if (property.value() != null && !property.value().isBlank()) return property.value();
		}

		return null;
	}

	// A dye is a player head, with a texture, named like a dye. Deliberately not "and carries a hex" as
	// well: animated dyes name two colours instead of one and have no Hex line at all. The name check
	// already excludes Vincent's bucket accessory.
	static @Nullable Dye read(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.PLAYER_HEAD)) return null;

		List<String> lore = lore(stack);
		String name = name(stack);

		if (!DyeLore.isDyeName(name)) return null;
		if (texture(stack) == null) return null;

		return new Dye(name, stack, DyeLore.colour(lore));
	}

	static ItemStack head(String name, String texture) {
		return STACKS.computeIfAbsent(texture, key -> {
			ItemStack stack = new ItemStack(Items.PLAYER_HEAD);

			Multimap<String, Property> properties = ArrayListMultimap.create();
			properties.put("textures", new Property("textures", key));

			// Derived from the name so vanilla's skin cache keys the same way every session.
			UUID id = UUID.nameUUIDFromBytes(("dyeutils:dye:" + name).getBytes(StandardCharsets.UTF_8));

			stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(
					new GameProfile(id, name, new PropertyMap(properties))));
			stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));

			return stack;
		});
	}

	// Everything learned so far, for the roulette to scroll past. Empty on a fresh install, which the
	// caller covers by falling back to the dyes in the open menu.
	static List<Dye> pool() {
		List<Dye> dyes = new ArrayList<>();

		VincentState.get().icons().forEach((name, icon) ->
				dyes.add(new Dye(name, head(name, icon.texture()), icon.colour())));

		return dyes;
	}
}
