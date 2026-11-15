package com.sportradar.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("InMemoryScoreboard")
class InMemoryScoreboardTest {

    @Test
    @DisplayName("rejects a null clock when created")
    void rejectsNullClock() {
        assertThatThrownBy(() -> Scoreboard.inMemory(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("stores team names trimmed of surrounding whitespace")
    void storesTeamNamesTrimmed() {
        Scoreboard board = new InMemoryScoreboard(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));

        board.startMatch("  Mexico ", " Canada  ");

        Match match = board.summary().get(0);

        assertThat(match.homeTeam()).isEqualTo("Mexico");
        assertThat(match.awayTeam()).isEqualTo("Canada");
    }

    @ParameterizedTest(name = "rejects a negative score of ({0}, {1}) and leaves the score unchanged")
    @CsvSource({
            "-1, 1",
            "1, -1",
            "-1, -1"
    })
    @DisplayName("rejects a negative home or away score")
    void rejectsNegativeScoreOnEitherSide(int homeScore, int awayScore) {
        Scoreboard board = new InMemoryScoreboard(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
        MatchId id = board.startMatch("Spain", "Brazil");

        assertThatThrownBy(() -> board.updateScore(id, homeScore, awayScore))
                .isInstanceOf(IllegalArgumentException.class);

        Match match = board.summary().get(0);
        assertThat(match.homeScore()).isZero();
        assertThat(match.awayScore()).isZero();
    }

    @Test
    @DisplayName("records the clock's instant at the moment a match is started")
    void recordsClockInstantAtStart() {
        Instant kickOff = Instant.parse("2026-06-11T18:00:00Z");
        Scoreboard board = new InMemoryScoreboard(Clock.fixed(kickOff, ZoneOffset.UTC));

        board.startMatch("Spain", "Brazil");

        assertThat(board.summary().get(0).startedAt()).isEqualTo(kickOff);
    }

    @Test
    @DisplayName("keeps the start instant unchanged when the score is updated")
    void keepsStartInstantWhenScoreIsUpdated() {
        Scoreboard board = new InMemoryScoreboard(new TickingClock());
        MatchId id = board.startMatch("Spain", "Brazil");
        Instant startedAt = board.summary().get(0).startedAt();

        board.updateScore(id, 2, 1);

        assertThat(board.summary().get(0).startedAt()).isEqualTo(startedAt);
    }

    /** A clock that moves one second forward on every read, so a second reading always differs from the first. */
    private static final class TickingClock extends Clock {

        private Instant now = Instant.parse("2026-06-11T18:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            now = now.plusSeconds(1);
            return now;
        }
    }
}
