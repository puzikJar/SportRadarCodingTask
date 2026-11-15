# Live Football World Cup Scoreboard

**As a sports data provider, I want a live scoreboard of World Cup matches in progress so that consumers always see current scores in a meaningful order.**

## Rules and Examples

### Rule: Must start a new match at 0 – 0 between a home team and an away team

Starting a match returns a handle that identifies the match for every later operation.

- **Example:** The one where Mexico (home) vs Canada (away) is started and the board shows Mexico 0 – Canada 0.
- **Counter-example:** The one where Spain is started against " spain " — the same team after trimming and ignoring case — and the start is rejected.
- **Counter-example:** The one where a team name is missing or blank and the start is rejected.

---

### Rule: Must not let a team play in more than one match in progress

Team names are compared trimmed and case-insensitively; the stored name keeps the caller's casing, trimmed.

- **Example:** The one where Spain – Brazil is in progress, Spain – Italy is started, and the start is rejected naming Spain.
- **Counter-example:** The one where Spain – Brazil has been finished, Spain – Italy is started, and the start succeeds.

---

### Rule: Must set a match in progress to the given absolute home and away score

Corrections are allowed: any non-negative score is accepted, including a lower or identical one.

| Current score | Update (home, away) | Result |
|---|---|---|
| Spain 0 – Brazil 0 | (10, 2) | Spain 10 – Brazil 2 |
| Spain 2 – Brazil 1 | (1, 1) | Spain 1 – Brazil 1 (VAR correction) |
| Spain 1 – Brazil 1 | (-1, 1) | Rejected — negative score |

The negative row is the boundary: zero is valid, below zero is not.

- **Counter-example:** The one where the match handle refers to a match that has already finished and the update is rejected.

---

### Rule: Must remove a finished match from the scoreboard

- **Example:** The one where Germany – France is finished and no longer appears in the summary.
- **Counter-example:** The one where an already-finished match is finished again and the request is rejected.

---

### Rule: Must order the summary by total score, highest first

Using the matches from the story, started in this order and updated to these scores:

| Start order | Match | Total | Summary position |
|---|---|---|---|
| 1 | Mexico 0 – Canada 5 | 5 | 3 |
| 2 | Spain 10 – Brazil 2 | 12 | 2 |
| 3 | Germany 2 – France 2 | 4 | 5 |
| 4 | Uruguay 6 – Italy 6 | 12 | 1 |
| 5 | Argentina 3 – Australia 1 | 4 | 4 |

- **Counter-example:** The one where no matches are in progress and the summary is empty.

---

### Rule: Must list the most recently started match first when total scores are tied

"Most recently started" is the start instant taken from the scoreboard's clock (injectable for tests).

- **Example:** The one where Spain 10 – Brazil 2 (started earlier) and Uruguay 6 – Italy 6 (started later) both total 12 and Uruguay – Italy comes first.
- **Counter-example:** The one where two matches have the same total and started at the same instant — they appear adjacent, in either order.

---

### Rule: Should find the match a team is currently playing in

The additional operation. Lookup uses the same trimmed, case-insensitive team matching.

- **Example:** The one where Spain – Brazil is in progress and looking up "brazil" returns Spain – Brazil.
- **Counter-example:** The one where Spain – Brazil has been finished and looking up Spain returns nothing.

---

## Resolved decisions (for implementation)

- **Match identity:** start returns a match handle; update and finish take that handle.
- **One live match per team:** enforced on start; rejection names the offending team.
- **Score update:** absolute (home, away) pair; decreases and no-op updates allowed; negatives rejected.
- **Tie-break:** start instant from an injectable `java.time.Clock`; equal instants → order between them unspecified.
- **Additional operation:** find the in-progress match by team name (empty result when none).
- **Thread safety:** all operations safe for concurrent use; summary is an immutable, consistent snapshot.
- **Team names:** trimmed, compared case-insensitively, stored with caller's casing.
- **Out of scope:** history of finished matches, match cancellation, match clock/minute, persistence.
