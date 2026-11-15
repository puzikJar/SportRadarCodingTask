package com.sportradar.scoreboard;

import java.time.Instant;

/**
 * An immutable snapshot of a football match on the {@link Scoreboard}.
 *
 * <p>A snapshot does not change when the match is later updated; use {@link #id()} to act on the match. Instances are
 * immutable and therefore thread-safe. Snapshots produced by the scoreboard never have a {@code null} component.
 *
 * @param id        the identifier of the match
 * @param homeTeam  the home team name, trimmed
 * @param awayTeam  the away team name, trimmed
 * @param homeScore the home team's goals; never negative in snapshots produced by the scoreboard
 * @param awayScore the away team's goals; never negative in snapshots produced by the scoreboard
 * @param startedAt the instant the match was started, as read from the scoreboard's clock; it is unchanged by
 *                  score updates
 */
public record Match(MatchId id, String homeTeam, String awayTeam, int homeScore, int awayScore, Instant startedAt) {
}