package dev.zoobooo.dyeutils.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;

import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Running a post chain needs the pool the renderer allocates its intermediate targets from, and that
// field is private.
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
	@Accessor("resourcePool")
	CrossFrameResourcePool dyeutils$resourcePool();
}
