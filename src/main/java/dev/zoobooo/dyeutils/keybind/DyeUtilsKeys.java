package dev.zoobooo.dyeutils.keybind;

import com.mojang.blaze3d.platform.InputConstants;

import dev.zoobooo.dyeutils.DyeUtils;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class DyeUtilsKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(DyeUtils.NAMESPACE, "main"));

	public static final KeyMapping INVITE_PARTY = register("invite_party");
	public static final KeyMapping PARTY_WARP = register("party_warp");

	private static KeyMapping register(String name) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key." + DyeUtils.NAMESPACE + "." + name,
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				CATEGORY));
	}

	// Touching the class runs the static initialisers above.
	public static void init() {
	}
}
