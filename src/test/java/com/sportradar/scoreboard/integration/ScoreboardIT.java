package com.sportradar.scoreboard.integration;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.MatchId;
import com.sportradar.scoreboard.Scoreboard;
import com.sportradar.scoreboard.TeamAlreadyPlayingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("Live Football World Cup Scoreboard")
class ScoreboardIT {

    @Nested
    @DisplayName("Must start a new match at 0 – 0 between a home team and an away team")
    class StartMatch {

        @Test
        @DisplayName("The one where Mexico (home) vs Canada (away) is started and the board shows Mexico 0 – Canada 0")
        void startsMatchAtZeroZero() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));

            MatchId handle = board.startMatch("Mexico", "Canada");

            assertThat(handle).isNotNull();
            List<Match> summary = board.summary();
            assertThat(summary).hasSize(1);
            Match match = summary.get(0);
            assertThat(match.id()).isEqualTo(handle);
            assertThat(match.homeTeam()).isEqualTo("Mexico");
            assertThat(match.awayTeam()).isEqualTo("Canada");
            assertThat(match.homeScore()).isZero();
            assertThat(match.awayScore()).isZero();
        }

        @Test
        @DisplayName("The one where Spain is started against \" spain \" — the same team after trimming and ignoring case — and the start is rejected")
        void rejectsSameTeamOnBothSides() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));

            assertThatThrownBy(() -> board.startMatch("Spain", " spain "))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(board.summary()).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("The one where a team name is missing or blank and the start is rejected")
        void rejectsMissingOrBlankTeamName(String invalidName) {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));

            assertThatThrownBy(() -> board.startMatch(invalidName, "Canada"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(board.summary()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Must not let a team play in more than one match in progress")
    class OneMatchPerTeam {

        @Test
        @DisplayName("The one where Spain – Brazil is in progress, Spain – Italy is started, and the start is rejected naming Spain")
        void rejectsStartForTeamAlreadyPlaying() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            board.startMatch("Spain", "Brazil");

            assertThatExceptionOfType(TeamAlreadyPlayingException.class)
                    .isThrownBy(() -> board.startMatch("Spain", "Italy"))
                    .withMessageContaining("Spain")
                    .satisfies(e -> assertThat(e.team()).isEqualTo("Spain"));

            assertThat(board.summary()).hasSize(1);
        }

        @Test
        @DisplayName("The one where Spain – Brazil has been finished, Spain – Italy is started, and the start succeeds")
        void allowsStartForTeamWhoseMatchHasFinished() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId spainBrazil = board.startMatch("Spain", "Brazil");
            board.finishMatch(spainBrazil);

            board.startMatch("Spain", "Italy");

            assertThat(board.summary())
                    .extracting(Match::homeTeam, Match::awayTeam)
                    .containsExactly(tuple("Spain", "Italy"));
        }
    }

    @Nested
    @DisplayName("Must set a match in progress to the given absolute home and away score")
    class UpdateScore {

        @ParameterizedTest(name = "Spain {0} – Brazil {1} updated with ({2}, {3}) becomes Spain {2} – Brazil {3}")
        @CsvSource({
                "0, 0, 10, 2",
                "2, 1, 1, 1"
        })
        @DisplayName("The one where the score is set to the given absolute values, including a lower one (VAR correction)")
        void setsAbsoluteScore(int currentHome, int currentAway, int newHome, int newAway) {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId handle = board.startMatch("Spain", "Brazil");
            board.updateScore(handle, currentHome, currentAway);

            board.updateScore(handle, newHome, newAway);

            assertThat(board.summary()).hasSize(1);
            Match match = board.summary().get(0);
            assertThat(match.homeTeam()).isEqualTo("Spain");
            assertThat(match.awayTeam()).isEqualTo("Brazil");
            assertThat(match.homeScore()).isEqualTo(newHome);
            assertThat(match.awayScore()).isEqualTo(newAway);
        }

        @Test
        @DisplayName("The one where Spain 1 – Brazil 1 is updated with (-1, 1) and the update is rejected for a negative score")
        void rejectsNegativeScore() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId handle = board.startMatch("Spain", "Brazil");
            board.updateScore(handle, 1, 1);

            assertThatThrownBy(() -> board.updateScore(handle, -1, 1))
                    .isInstanceOf(IllegalArgumentException.class);

            Match match = board.summary().get(0);
            assertThat(match.homeScore()).isEqualTo(1);
            assertThat(match.awayScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("The one where the match handle refers to a match that has already finished and the update is rejected")
        void rejectsUpdateOfFinishedMatch() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId spainBrazil = board.startMatch("Spain", "Brazil");
            board.finishMatch(spainBrazil);

            assertThatThrownBy(() -> board.updateScore(spainBrazil, 1, 0))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(board.summary()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Must remove a finished match from the scoreboard")
    class FinishMatch {

        @Test
        @DisplayName("The one where Germany – France is finished and no longer appears in the summary")
        void removesFinishedMatchFromSummary() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId germanyFrance = board.startMatch("Germany", "France");
            board.startMatch("Uruguay", "Italy");

            board.finishMatch(germanyFrance);

            assertThat(board.summary())
                    .extracting(Match::homeTeam, Match::awayTeam)
                    .containsExactly(tuple("Uruguay", "Italy"));
        }

        @Test
        @DisplayName("The one where an already-finished match is finished again and the request is rejected")
        void rejectsFinishingAnAlreadyFinishedMatch() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId germanyFrance = board.startMatch("Germany", "France");
            board.finishMatch(germanyFrance);

            assertThatThrownBy(() -> board.finishMatch(germanyFrance))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Must order the summary by total score, highest first")
    class SummaryOrder {

        @Test
        @DisplayName("The one where five matches started in order are listed Uruguay 6 – Italy 6, Spain 10 – Brazil 2, "
                + "Mexico 0 – Canada 5, Argentina 3 – Australia 1, Germany 2 – France 2")
        void ordersMatchesByTotalScoreHighestFirst() {
            Scoreboard board = Scoreboard.inMemory(new SteppingClock());
            board.updateScore(board.startMatch("Mexico", "Canada"), 0, 5);
            board.updateScore(board.startMatch("Spain", "Brazil"), 10, 2);
            board.updateScore(board.startMatch("Germany", "France"), 2, 2);
            board.updateScore(board.startMatch("Uruguay", "Italy"), 6, 6);
            board.updateScore(board.startMatch("Argentina", "Australia"), 3, 1);

            assertThat(board.summary())
                    .extracting(Match::homeTeam, Match::homeScore, Match::awayTeam, Match::awayScore)
                    .containsExactly(
                            tuple("Uruguay", 6, "Italy", 6),
                            tuple("Spain", 10, "Brazil", 2),
                            tuple("Mexico", 0, "Canada", 5),
                            tuple("Argentina", 3, "Australia", 1),
                            tuple("Germany", 2, "France", 2));
        }

        @Test
        @DisplayName("The one where no matches are in progress and the summary is empty")
        void returnsEmptySummaryWhenNoMatchesInProgress() {
            Scoreboard board = Scoreboard.inMemory(new SteppingClock());

            assertThat(board.summary()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Must list the most recently started match first when total scores are tied")
    class TieBreak {

        @Test
        @DisplayName("The one where Spain 10 – Brazil 2 (started earlier) and Uruguay 6 – Italy 6 (started later) "
                + "both total 12 and Uruguay – Italy comes first")
        void listsMostRecentlyStartedFirstWhenTotalsAreTied() {
            Scoreboard board = Scoreboard.inMemory(new SteppingClock());
            board.updateScore(board.startMatch("Spain", "Brazil"), 10, 2);
            board.updateScore(board.startMatch("Uruguay", "Italy"), 6, 6);

            assertThat(board.summary())
                    .extracting(Match::homeTeam, Match::awayTeam)
                    .containsExactly(tuple("Uruguay", "Italy"), tuple("Spain", "Brazil"));
        }

        @Test
        @DisplayName("The one where two matches have the same total and started at the same instant "
                + "— they appear adjacent, in either order")
        void keepsMatchesWithSameTotalAndSameStartInstantAdjacent() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            board.updateScore(board.startMatch("Germany", "France"), 2, 2);
            board.updateScore(board.startMatch("Spain", "Brazil"), 3, 1);
            board.updateScore(board.startMatch("Mexico", "Canada"), 0, 1);

            List<Match> summary = board.summary();

            assertThat(summary.subList(0, 2))
                    .extracting(Match::homeTeam, Match::awayTeam)
                    .containsExactlyInAnyOrder(tuple("Germany", "France"), tuple("Spain", "Brazil"));
            assertThat(summary.get(2).homeTeam()).isEqualTo("Mexico");
        }
    }

    @Nested
    @DisplayName("Should find the match a team is currently playing in")
    class FindMatchByTeam {

        @Test
        @DisplayName("The one where Spain – Brazil is in progress and looking up \"brazil\" returns Spain – Brazil")
        void findsMatchByTeamIgnoringCase() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            MatchId spainBrazil = board.startMatch("Spain", "Brazil");
            board.startMatch("Germany", "France");

            assertThat(board.findMatchByTeam("brazil")).hasValueSatisfying(match -> {
                assertThat(match.id()).isEqualTo(spainBrazil);
                assertThat(match.homeTeam()).isEqualTo("Spain");
                assertThat(match.awayTeam()).isEqualTo("Brazil");
            });
        }

        @Test
        @DisplayName("The one where Spain – Brazil has been finished and looking up Spain returns nothing")
        void findsNothingForTeamWhoseMatchHasFinished() {
            Scoreboard board = Scoreboard.inMemory(Clock.fixed(Instant.parse("2026-06-11T18:00:00Z"), ZoneOffset.UTC));
            board.finishMatch(board.startMatch("Spain", "Brazil"));

            assertThat(board.findMatchByTeam("Spain")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Must be safe for concurrent use")
    class ConcurrentUse {

        @Test
        @Timeout(60)
        @DisplayName("The one where eight threads each start 500 matches at once and all 4000 matches are on the board")
        void keepsEveryMatchStartedConcurrently() throws Exception {
            int threads = 8;
            int matchesPerThread = 500;
            Scoreboard board = Scoreboard.inMemory(Clock.systemUTC());
            var nextThread = new AtomicInteger();

            runConcurrently(threads, () -> {
                int thread = nextThread.getAndIncrement();
                for (int m = 0; m < matchesPerThread; m++) {
                    board.startMatch("Home " + thread + "-" + m, "Away " + thread + "-" + m);
                }
                return null;
            });

            assertThat(board.summary()).hasSize(threads * matchesPerThread);
        }

        @RepeatedTest(100)
        @Timeout(60)
        @DisplayName("The one where eight threads start a match for Spain at once and exactly one start succeeds")
        void letsExactlyOneConcurrentStartWinForTheSameTeam() throws Exception {
            int threads = 8;
            Scoreboard board = Scoreboard.inMemory(Clock.systemUTC());
            var nextOpponent = new AtomicInteger();

            List<Boolean> started = runConcurrently(threads, () -> {
                try {
                    board.startMatch("Spain", "Opponent " + nextOpponent.getAndIncrement());
                    return true;
                } catch (TeamAlreadyPlayingException rejection) {
                    return false;
                }
            });

            assertThat(started).hasSize(threads).containsOnlyOnce(true);
            assertThat(board.summary()).hasSize(1);
        }

        @Test
        @Timeout(60)
        @DisplayName("The one where Spain's match is repeatedly replaced by a new one while summaries are read, "
                + "and no summary ever shows Spain in two matches")
        void neverShowsATeamInTwoMatchesWhileItsMatchIsReplaced() throws Exception {
            int summariesToRead = 300;
            Scoreboard board = Scoreboard.inMemory(Clock.systemUTC());
            for (int i = 0; i < 5000; i++) {
                board.startMatch("Filler Home " + i, "Filler Away " + i);
            }
            var nextRole = new AtomicInteger();
            var reading = new AtomicBoolean(true);
            var swapsCompleted = new AtomicInteger();
            var summariesWithSpainTwice = new AtomicInteger();
            var swapperRunning = new CountDownLatch(1);

            runConcurrently(2, () -> {
                if (nextRole.getAndIncrement() == 0) {
                    try {
                        swapperRunning.await();
                        for (int read = 0; read < summariesToRead; read++) {
                            long spainMatches = board.summary().stream()
                                    .filter(match -> match.homeTeam().equals("Spain")
                                            || match.awayTeam().equals("Spain"))
                                    .count();
                            if (spainMatches > 1) {
                                summariesWithSpainTwice.incrementAndGet();
                            }
                        }
                    } finally {
                        reading.set(false);
                    }
                } else {
                    MatchId current;
                    try {
                        current = board.startMatch("Spain", "Opponent 0");
                    } finally {
                        swapperRunning.countDown();
                    }
                    while (reading.get()) {
                        board.finishMatch(current);
                        current = board.startMatch("Spain", "Opponent " + swapsCompleted.incrementAndGet());
                    }
                }
                return null;
            });

            assertThat(swapsCompleted.get()).as("matches replaced while summaries were being read").isPositive();
            assertThat(summariesWithSpainTwice).hasValue(0);
        }

        private static <T> List<T> runConcurrently(int threads, Callable<T> task) throws Exception {
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            var start = new CountDownLatch(1);
            try {
                List<Future<T>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        return task.call();
                    }));
                }
                start.countDown();
                List<T> results = new ArrayList<>();
                for (Future<T> future : futures) {
                    results.add(future.get(5, TimeUnit.SECONDS));
                }
                return results;
            } finally {
                pool.shutdownNow();
            }
        }
    }

    private static final class SteppingClock extends Clock {

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
