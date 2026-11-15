
---

# How AI tools were used

## Summary

The whole library was built with **Claude Code** (Anthropic's CLI agent) in two sessions. 
The model was switched during the work with `/model`.

The process follows the one in `CLAUDE.md`, and every step is a skill kept in `.claude/skills/`:

1. **`/discover`** turned the assignment into an Example Mapping spec: rules, examples, counter-examples and open
   questions. I answered the open questions, and the result was saved to `doc/specs/`.
2. **`/accept`** wrote one failing integration test for the next spec rule (`ScoreboardIT`, one `@Nested` class per
   rule).
3. **`/tdd`** ran one inner cycle at a time: RED, GREEN, REFACTOR, then a proposed edge case. It stopped after each
   cycle and waited for me.
4. **`/review`** produced a read-only architecture and quality report. I picked which findings to act on.
5. **README.md** and this file were written by the AI from the spec, the code and the decisions in the conversation.

A hook in `.claude/settings.json` runs `mvn verify -q` after every file write or edit, so the full test suite ran after
every AI change.

## Prompt history

Slash commands are shown with their arguments. The text a skill expands to is in `.claude/skills/*/SKILL.md`, so I
have not repeated it. The wording of my own prompts is kept as I typed it.

### Session 1: 2026-11-14 / 15 (discovery, rules 1 to 7, thread safety)

| #  | Prompt                                                                                                                                                                                                                                                                   |
|----|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1  | `/discover` with the assignment (full text below)                                                                                                                                                                                                                        |
| 2  | "Repeat Q5" (asked the AI to restate an open question)                                                                                                                                                                                                                   |
| 3  | "Let's go with Clock timestamp. If both matches started at the same time they can just follow each other one by one, that is ok."                                                                                                                                        |
| 4  | `/accept Must start a new match at 0 – 0`                                                                                                                                                                                                                                |
| 5  | "Use IllegalArgumentException for parameter validation."                                                                                                                                                                                                                 |
| 6  | `/tdd StartMatch`                                                                                                                                                                                                                                                        |
| 7  | `/tdd reject null, empty or blank team name`                                                                                                                                                                                                                             |
| 8  | `/tdd reject same team on both sides`                                                                                                                                                                                                                                    |
| 9  | `/tdd trim stored team names`                                                                                                                                                                                                                                            |
| 10 | `/tdd create TeamAlreadyPlayingException`                                                                                                                                                                                                                                |
| 11 | "Go with the TeamName refactor"                                                                                                                                                                                                                                          |
| 12 | `/accept rule 3 set the score`                                                                                                                                                                                                                                           |
| 13 | `/tdd create updateScore`                                                                                                                                                                                                                                                |
| 14 | "Make public API return MatchId object instead of whole Match object."                                                                                                                                                                                                   |
| 15 | `/tdd reject negative score`                                                                                                                                                                                                                                             |
| 16 | "Away score negative: (1, -1). The guard covers it, but only the home side is pinned. - cover away side as well."                                                                                                                                                        |
| 17 | `/accept rule 4 finish a match`                                                                                                                                                                                                                                          |
| 18 | `/tdd create finishMatch`                                                                                                                                                                                                                                                |
| 19 | `/tdd reject finishing an already finished match`                                                                                                                                                                                                                        |
| 20 | `/accept rule 2 team release counter-example`                                                                                                                                                                                                                            |
| 21 | `/tdd make finishMatch release both teams`                                                                                                                                                                                                                               |
| 22 | `/accept rule 5 order summary by total score`                                                                                                                                                                                                                            |
| 23 | `/tdd sort summary by total score and start time`                                                                                                                                                                                                                        |
| 24 | "Include startedAt property into Match and remove LiveMatch."                                                                                                                                                                                                            |
| 25 | `/accept rule 6 most recently started first on ties`                                                                                                                                                                                                                     |
| 26 | `/accept rule 7 find the match a team is playing in`                                                                                                                                                                                                                     |
| 27 | `/tdd create findMatchByTeam` plus the next edge case: a null or blank lookup throws `IllegalArgumentException`                                                                                                                                                          |
| 28 | "Rename TeamName to Team and move all private methods at the bottom of InMemoryScoreboard.java"                                                                                                                                                                          |
| 29 | `/tdd make the scoreboard thread-safe`                                                                                                                                                                                                                                   |
| 30 | `/tdd same team race, exactly one wins`                                                                                                                                                                                                                                  |
| 31 | "Use ReentrantReadWriteLock to protect check-then-act scenarios in write API. Leave LinkedHashMap as read/write operations will be protected by locks. Delete synchronize locks in findMatchByTeam and summary." |                                                       |
| 32 | "Refactor Concurrent tests. Extract private helper method" (I pasted the `runConcurrently` helper to use)                                                                                                                                                                |
| 33 | "I asssume read methods would be used really frequiently, so I want to use REentrantReadWrite lock to increase throughput."                                                                                                                                              |
| 34 | `/review`                                                                                                                                                                                                                                                                |
| 35 | "Add the missing update-after-finish test with /tdd"                                                                                                                                                                                                                     |
| 36 | `/tdd Match rejects null components and negative scores`                                                                                                                                                                                                                 |
| 37 | "Fix the summary() Javadoc ordering"                                                                                                                                                                                                                                     |
| 38 | "Fix Scoreboard.create(null) next"                                                                                                                                                                                                                                       |
| 39 | "Change create to inMemory"                                                                                                                                                                                                                                              |

## Artifacts that guided the implementation

| Artifact | What it did |
|---|---|
| The assignment text above | The requirements, with open questions left in on purpose. |
| `CLAUDE.md` | Project rules: the development process (discover, accept, tdd, review), library-design rules (small API, immutability, JLBP, JPMS, Javadoc), Java 23 conventions, testing standards. |
| `doc/specs/live-football-world-cup-scoreboard.md` | The Example Mapping spec: 7 rules with examples, counter-examples and the resolved decisions. Every integration test traces back to it. |
| `.claude/skills/discover/` (`SKILL.md`, `references/example-mapping.md`, `templates/spec-template.md`) | How the spec was produced and what shape it has. |
| `.claude/skills/accept/SKILL.md` | One failing integration test per spec rule. |
| `.claude/skills/tdd/SKILL.md` | One RED, GREEN, REFACTOR, CHALLENGE cycle per invocation. |
| `.claude/skills/review/` (`SKILL.md`, `templates/report-template.md`) | The review checklist and the report structure. |
| `.claude/rules/test-rules.md` | Rules for test classes: `@DisplayName` everywhere, `@Nested`, parameterized tests, `*IT` versus `*Test`, never change a test to make it pass. Loaded when files in `src/test/java` are touched. |
| `.claude/settings.json` | A hook that runs `mvn verify -q` after every write or edit. |