package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * A live scoreboard of football matches in progress.
 *
 * <p>Implementations returned by {@link #inMemory(Clock)} are thread-safe.
 */
public interface Scoreboard {

    /**
     * Creates an empty scoreboard that keeps its matches in memory only; nothing is persisted.
     *
     * @param clock the clock used to timestamp match starts, never {@code null}
     * @return a new, empty, thread-safe scoreboard
     * @throws IllegalArgumentException if {@code clock} is {@code null}
     */
    static Scoreboard inMemory(Clock clock) {
        return new InMemoryScoreboard(clock);
    }

    /**
     * Starts a new match at 0 – 0.
     *
     * @param homeTeam the home team name, never {@code null} or blank
     * @param awayTeam the away team name, never {@code null} or blank
     * @return the identifier of the started match
     * @throws IllegalArgumentException if a name is {@code null} or blank, or both names denote the same team
     * @throws TeamAlreadyPlayingException if either team is already playing in a match in progress
     */
    MatchId startMatch(String homeTeam, String awayTeam);

    /**
     * Sets the score of a match in progress to the given absolute values. Lower and identical scores are accepted,
     * so corrections are possible.
     *
     * @param id        the identifier returned by {@link #startMatch(String, String)}, never {@code null}
     * @param homeScore the home team's goals, never negative
     * @param awayScore the away team's goals, never negative
     * @throws IllegalArgumentException if a score is negative or no match in progress has the given id
     */
    void updateScore(MatchId id, int homeScore, int awayScore);

    /**
     * Finishes a match in progress, removing it from the scoreboard.
     *
     * @param id the identifier returned by {@link #startMatch(String, String)}, never {@code null}
     * @throws IllegalArgumentException if no match in progress has the given id, for instance because it was
     *                                  already finished
     */
    void finishMatch(MatchId id);

    /**
     * Finds the match in progress that the given team is playing in. The team name is trimmed and compared ignoring
     * case.
     *
     * @param team the team name, never {@code null} or blank
     * @return the current snapshot of the team's match, or an empty optional if the team is not playing
     * @throws IllegalArgumentException if the team name is {@code null} or blank
     */
    Optional<Match> findMatchByTeam(String team);

    /**
     * Returns the matches in progress, ordered by total score (home plus away), highest first. Matches with the same
     * total are ordered by start instant, most recently started first; the order between matches that also started at
     * the same instant is unspecified.
     *
     * <p>The result is a consistent point-in-time snapshot: it is not affected by later operations on the scoreboard.
     *
     * @return an immutable, ordered snapshot, never {@code null}; empty if no match is in progress
     */
    List<Match> summary();
}
