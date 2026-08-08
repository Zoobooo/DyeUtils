package dev.zoobooo.dyeutils.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.zoobooo.dyeutils.config.DyeUtilsConfigScreen;

public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return DyeUtilsConfigScreen::create;
	}
}
