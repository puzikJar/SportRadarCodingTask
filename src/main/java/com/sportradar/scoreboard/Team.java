package com.sportradar.scoreboard;

import java.util.Locale;

/**
 * A team name: trimmed, never blank, and compared ignoring case. The caller's casing is kept for display.
 */
record Team(String name) {

    Team {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("team name must not be null or blank, was: " + name);
        }
        name = name.trim();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Team that && key().equals(that.key());
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }

    private String key() {
        return name.toLowerCase(Locale.ROOT);
    }
}