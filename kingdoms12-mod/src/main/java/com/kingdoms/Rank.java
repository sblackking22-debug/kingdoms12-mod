package com.kingdoms;

public enum Rank {
	RECRUIT,
	SWORDSMAN,
	AXEMAN,
	ARCHER;

	public static Rank byOrdinal(int i) {
		Rank[] v = values();
		return v[Math.floorMod(i, v.length)];
	}

	public static Rank byName(String s) {
		for (Rank r : values()) {
			if (r.name().equalsIgnoreCase(s)) return r;
		}
		return null;
	}
}
