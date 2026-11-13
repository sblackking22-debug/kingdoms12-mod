package com.kingdoms;

public enum Faction {
	VILLAGERS("villager"),
	PILLAGERS("pillager");

	public final String id;

	Faction(String id) {
		this.id = id;
	}

	public Faction enemy() {
		return this == VILLAGERS ? PILLAGERS : VILLAGERS;
	}

	public static Faction byOrdinal(int i) {
		return i == 1 ? PILLAGERS : VILLAGERS;
	}

	public static Faction byName(String s) {
		if (s == null) return null;
		for (Faction f : values()) {
			if (f.id.equalsIgnoreCase(s) || f.name().equalsIgnoreCase(s)) return f;
		}
		return null;
	}
}
