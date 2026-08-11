package dev.zoobooo.dyeutils.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

// A crash in a cosmetic client mod costs a run; a missing retexture does not.
public final class Failsafe {
	private static final Logger LOGGER = LogUtils.getLogger();

	/** Once per thing that broke; these run every tick or frame and would flood. */
	private static final Set<String> reported = ConcurrentHashMap.newKeySet();

	private Failsafe() {
	}

	public static boolean run(String what, Runnable action) {
		try {
			action.run();

			return true;
		} catch (Throwable t) {
			report(what, t);

			return false;
		}
	}

	public static void report(String what, Throwable t) {
		if (!reported.add(what)) return;

		LOGGER.error("[DyeUtils] {} failed and has been switched off for this session. "
				+ "Please report this with the stack trace below.", what, t);
	}
}
