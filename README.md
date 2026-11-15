# Live Football World Cup Scoreboard

A small Java 23 library that keeps a live scoreboard of football matches in progress. It is in-memory, thread-safe and
has no runtime dependencies.

## Usage

```java
Scoreboard board = Scoreboard.inMemory(Clock.systemUTC());

MatchId mexicoCanada = board.startMatch("Mexico", "Canada");   // Mexico 0 – Canada 0
board.updateScore(mexicoCanada, 0, 5);                         // absolute score, not an increment

board.findMatchByTeam("canada");                               // Optional<Match>, case-insensitive
List<Match> summary = board.summary();                         // highest total first, newest first on ties

board.finishMatch(mexicoCanada);                               // removes it from the scoreboard
```

Public API: `Scoreboard`, `Match`, `MatchId`, `TeamAlreadyPlayingException`. Everything else is package-private.

## Assumptions

- **One live match per team.** Starting a match for a team that is already playing is rejected with
  `TeamAlreadyPlayingException`, which names the team. Once the match is finished the team can play again.
- **Team names** are trimmed and compared case-insensitively, and stored with the caller's casing, trimmed. A null or
  blank name is invalid. A team cannot play itself (`"Spain"` against `" spain "` is rejected).
- **Match identity.** Starting a match returns an opaque `MatchId`. Update and finish take that handle.
- **Scores are absolute.** `updateScore(id, home, away)` sets the score. Lower and identical values are accepted. Negative scores are rejected. Zero is valid.
- **Ordering.** Summary is ordered by total score, highest first. Ties go to the most recently started match, using the
  start instant from an injectable `java.time.Clock`. When the start instant is also equal, the order is unspecified.
- **Finishing twice or updating a finished match** is an error, because a finished match is no longer in progress.
- **Out of scope:** history of finished matches, match cancellation, match clock or minute, persistence.

## Reasoning

- **Small public API.** `InMemoryScoreboard` and `Team` are package-private, and callers only see the `Scoreboard`
  interface.
- **Immutable snapshots.** `Match` is a record. `summary()` and `findMatchByTeam()` return snapshots that do not change
  when the match is updated later, so callers never see a half-updated match.
- **Read/Write ratio** considering the Lib is going to be used in heavy read system, where reads are dominant, so ReentrantReadWriteLock is used for threadsafety.
- **Don't make the caller do the work.** Trimming, case-insensitive comparison, the one-match-per-team check and the
  ordering all happen inside the library.
- **Exceptions.** Invalid arguments, including `null`, throw `IllegalArgumentException`. The one custom exception,
  `TeamAlreadyPlayingException`, is unchecked and carries the offending team, because it describes a state conflict
  rather than a bad argument.
- **Injectable clock.** Tests control the start instant, so the tie-break is deterministic and doesn't depend on
  sleeping or on real time.

## Custom feature
As per consideration that Lib is meant to be used in heavy read system. So, implemented an ability to look up for the live
match a specified team is currently playing in. `findMatchByTeam()` implements this behaviour. Returns Optional and protected by readLock.  

## Trade-offs

| Decision | Chosen | Alternative | Why                                                                                                                                                                                                       |
|---|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Concurrency | One `ReentrantReadWriteLock` around a `LinkedHashMap` and a set of playing teams | Concurrent collections or lock-free structures | Checking that a team is free and inserting the match must be one atomic step. Used a write lock for simplicity. Considering write throughput is not a concern for a scoreboard.                           |
| Summary ordering | Sort on every `summary()` call, O(n log n) | Keep a sorted structure updated on writes | A World Cup has a handful of live matches, so sorting is cheap. A sorted structure would add write-path complexity for no visible gain.                                                                   |
| Match identity | UUID-backed `MatchId` | Identify a match by its team names | Names are ambiguous across rematches and casing. The cost is that the caller has to keep the handle, softened by `findMatchByTeam`.                                                                       |
| Score update | Replace the immutable `Match` in the map | Mutate a match object | Keeps snapshots safe to hand out without copying.                                                                                                                                                         |
| Score validation | In `updateScore`, not in the `Match` record | A compact constructor on `Match` | The library never accepts a `Match` as input, so the record only ever holds values the scoreboard produced. One owner of the rule, no duplication.                                                        |
| Extensibility | `Scoreboard` is an open interface; the factory is named `inMemory(Clock)` | Sealed interface; a generic `create(...)` | Users can stub or decorate the interface in their own tests. A named factory lets other backends be added later without breaking callers. The invariants live in `InMemoryScoreboard`, not the interface. |
| Dependencies | None at runtime, no logging | Logging facade | Nothing in the library needs logging. No dependencies means no version conflicts for users.                                                                                                               |

Thread safety is checked by stress tests in `ScoreboardIT`: concurrent starts, a race for the same team where exactly one
start must win, and summaries read while a team's match is replaced.

## Build and test

Requires JDK 23.

```bash
mvn test      # unit tests (*Test)
mvn verify    # unit tests and integration tests (*IT, run by failsafe)
mvn package   # build the jar
```

Make sure `JAVA_HOME` points at a JDK 23 install. With an older JDK the build fails with "invalid target release: 23".
