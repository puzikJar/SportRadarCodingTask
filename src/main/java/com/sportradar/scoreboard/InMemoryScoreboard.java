package com.sportradar.scoreboard;

import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class InMemoryScoreboard implements Scoreboard {

    private static final Comparator<Match> HIGHEST_TOTAL_SCORE_FIRST =
            Comparator.comparingInt((Match match) -> match.homeScore() + match.awayScore()).reversed();
    private static final Comparator<Match> MOST_RECENTLY_STARTED_FIRST =
            Comparator.comparing(Match::startedAt).reversed();

    private final Clock clock;
    private final Map<MatchId, Match> matches = new LinkedHashMap<>();
    private final Set<Team> teamsPlaying = new HashSet<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    InMemoryScoreboard(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.clock = clock;
    }

    @Override
    public MatchId startMatch(String homeTeam, String awayTeam) {
        var home = new Team(homeTeam);
        var away = new Team(awayTeam);

        if (home.equals(away)) {
            throw new IllegalArgumentException(
                    "homeTeam and awayTeam must be different teams, was: '" + homeTeam + "' and '" + awayTeam + "'");
        }

        writeLock.lock();
        try {
            requireNotPlaying(home);
            requireNotPlaying(away);
            var id = new MatchId();
            matches.put(id, new Match(id,
                    home.name(),
                    away.name(),
                    0,
                    0,
                    clock.instant()));
            teamsPlaying.add(home);
            teamsPlaying.add(away);
            return id;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void updateScore(MatchId id, int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0) {
            throw new IllegalArgumentException(
                    "scores must not be negative, was: " + homeScore + " and " + awayScore);
        }

        writeLock.lock();
        try {
            Match match = matches.get(id);
            if (match == null) {
                throw new IllegalArgumentException("no match in progress with id: " + id);
            }
            matches.put(id,
                    new Match(id,
                            match.homeTeam(),
                            match.awayTeam(),
                            homeScore,
                            awayScore,
                            match.startedAt()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void finishMatch(MatchId id) {
        writeLock.lock();
        try {
            Match finished = matches.remove(id);
            if (finished == null) {
                throw new IllegalArgumentException("no match in progress with id: " + id);
            }
            teamsPlaying.remove(new Team(finished.homeTeam()));
            teamsPlaying.remove(new Team(finished.awayTeam()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Match> findMatchByTeam(String team) {
        Team searched = new Team(team);
        readLock.lock();
        try {
            return matches.values().stream()
                    .filter(match -> isPlayedBy(match, searched))
                    .findFirst();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Match> summary() {
        readLock.lock();
        try {
            return matches.values().stream()
                    .sorted(HIGHEST_TOTAL_SCORE_FIRST.thenComparing(MOST_RECENTLY_STARTED_FIRST))
                    .toList();
        } finally {
            readLock.unlock();
        }
    }

    private void requireNotPlaying(Team team) {
        if (teamsPlaying.contains(team)) {
            throw new TeamAlreadyPlayingException(team.name());
        }
    }

    private static boolean isPlayedBy(Match match, Team team) {
        return new Team(match.homeTeam()).equals(team) || new Team(match.awayTeam()).equals(team);
    }
}