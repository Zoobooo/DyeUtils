package dev.zoobooo.dyeutils.party;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.zoobooo.dyeutils.util.Ign;

final class NameList {
	private final Supplier<List<String>> reader;
	private final Consumer<List<String>> writer;

	NameList(Supplier<List<String>> reader, Consumer<List<String>> writer) {
		this.reader = reader;
		this.writer = writer;
	}

	List<String> get() {
		return List.copyOf(reader.get());
	}

	void set(List<String> names) {
		writer.accept(names);
	}

	boolean contains(String ign) {
		return Ign.contains(get(), ign);
	}

	boolean add(String ign) {
		String trimmed = ign.trim();
		if (!Ign.isValid(trimmed)) return false;

		List<String> names = new ArrayList<>(get());
		if (Ign.contains(names, trimmed)) return false;

		names.add(trimmed);
		set(names);

		return true;
	}

	boolean remove(String ign) {
		List<String> names = new ArrayList<>(get());
		int index = Ign.indexOf(names, ign);
		if (index < 0) return false;

		names.remove(index);
		set(names);

		return true;
	}

	void clear() {
		set(List.of());
	}

	boolean toggle(String ign) {
		if (remove(ign)) return false;

		return add(ign);
	}
}
