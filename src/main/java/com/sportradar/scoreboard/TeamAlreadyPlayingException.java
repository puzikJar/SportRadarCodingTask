package com.sportradar.scoreboard;

/**
 * Thrown when a match is started for a team that is already playing in a match in progress.
 *
 * <p>This is an unchecked exception. It is immutable and therefore thread-safe.
 */
public final class TeamAlreadyPlayingException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final String team;

    TeamAlreadyPlayingException(String team) {
        super("Team is already playing in a match in progress: " + team);
        this.team = team;
    }

    /**
     * Returns the team that is already playing.
     *
     * @return the team name, trimmed, never {@code null}
     */
    public String team() {
        return team;
    }
}
