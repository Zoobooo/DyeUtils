package dev.zoobooo.dyeutils.rift;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;

public final class BacteTracker {
	private static final int SCAN_INTERVAL_TICKS = 10;

	private static final double ASSOCIATION_RADIUS = 8.0;

	public static final BacteTracker INSTANCE = new BacteTracker();

	private final Set<Integer> bacteIds = new HashSet<>();

	private int ticks;

	private BacteTracker() {
	}

	public void tick(Minecraft client) {
		if (client.level == null) {
			bacteIds.clear();
			ticks = 0;

			return;
		}

		if (++ticks < SCAN_INTERVAL_TICKS) return;

		ticks = 0;
		scan(client.level);
	}

	public boolean isBacte(Slime slime) {
		return named(slime) || bacteIds.contains(slime.getId());
	}

	private void scan(ClientLevel level) {
		List<Slime> slimes = new ArrayList<>();
		List<Entity> tags = new ArrayList<>();

		for (Entity entity : level.entitiesForRendering()) {
			if (entity instanceof Slime slime) slimes.add(slime);
			if (!(entity instanceof Slime) && named(entity)) tags.add(entity);
		}

		bacteIds.clear();

		for (Entity tag : tags) {
			Slime nearest = nearestSlime(slimes, tag);
			if (nearest != null) bacteIds.add(nearest.getId());
		}
	}

	private static Slime nearestSlime(List<Slime> slimes, Entity tag) {
		Slime nearest = null;
		double best = ASSOCIATION_RADIUS * ASSOCIATION_RADIUS;

		for (Slime slime : slimes) {
			double distance = slime.distanceToSqr(tag);

			if (distance < best) {
				best = distance;
				nearest = slime;
			}
		}

		return nearest;
	}

	private static boolean named(Entity entity) {
		Component name = entity.getCustomName();

		return name != null && BacteNames.isBacte(name.getString());
	}
}
