package com.ashraf.seed;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/** The three service windows a seeded restaurant can serve. */
public enum MealTime {
    BREAKFAST('B', LocalTime.of(7, 0), LocalTime.of(11, 0)),
    LUNCH('L', LocalTime.of(12, 0), LocalTime.of(15, 30)),
    DINNER('D', LocalTime.of(19, 0), LocalTime.of(23, 0));

    private final char code;
    private final LocalTime defaultOpen;
    private final LocalTime defaultClose;

    MealTime(char code, LocalTime defaultOpen, LocalTime defaultClose) {
        this.code = code;
        this.defaultOpen = defaultOpen;
        this.defaultClose = defaultClose;
    }

    public char code() { return code; }
    public LocalTime defaultOpen() { return defaultOpen; }
    public LocalTime defaultClose() { return defaultClose; }

    /** "BL" -> {BREAKFAST, LUNCH}. */
    public static Set<MealTime> parse(String codes) {
        Set<MealTime> result = EnumSet.noneOf(MealTime.class);
        for (char c : codes.trim().toUpperCase().toCharArray()) {
            boolean found = false;
            for (MealTime m : values()) {
                if (m.code == c) { result.add(m); found = true; }
            }
            if (!found) throw new IllegalArgumentException("Unknown meal code '" + c + "' in '" + codes + "'");
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty meal codes");
        return result;
    }
}
