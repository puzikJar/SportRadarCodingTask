package com.sportradar.scoreboard;

import java.util.UUID;

/**
 * An opaque identifier of a match on a {@link Scoreboard}, returned when the match is started.
 *
 * <p>Ids are created only by the library. Two ids are equal if they identify the same match, and an id is never
 * reused, so the id of a finished match cannot address a later match. Instances are immutable and thread-safe.
 */
public final class MatchId {

    private final UUID value;

    MatchId() {
        this.value = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MatchId that && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "MatchId[" + value + "]";
    }
}