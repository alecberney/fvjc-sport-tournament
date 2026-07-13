# Rest-Aware Schedule Packing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `ScheduleService.packRounds` distribute each team's matches with even rest — avoid back-to-back play and long idle stretches — while still filling every field in every round.

**Architecture:** A drop-in change to the match-selection ordering inside the existing greedy `packRounds`. We track the last round each team played, then order the remaining matches each round by (1) avoid back-to-back, (2) most-rested team first, (3) existing remaining-degree heuristic as tie-break. Field-filling and round count are unchanged.

**Tech Stack:** Java 21, Spring Boot, Lombok, JUnit 5 + Mockito, Maven (`./mvnw`).

## Global Constraints

- Backend architecture rules apply (see `.claude/rules/backend-rules.md`): `final var` for all locals; one operation per line; multi-line ternary formatting; `@UtilityClass`/mapper conventions unchanged here.
- Backend test rules apply (see `.claude/rules/backend-test-rules.md`): Mockito `@Mock`/`@InjectMocks`, `@ExtendWith(MockitoExtension.class)`, JUnit 5 assertions only (no AssertJ), no `ArgumentCaptor`, `final var` locals, Fakes via `ScheduleFakes`/`GroupFakes`/`TeamFakes`, static imports for Fakes and enums. **Test methods have no structural comments.**
- **Production code must include explanatory comments** on the rest-aware selection logic (user request): what each ordering criterion means and why. (This applies to `ScheduleService.java` only — NOT to the test file, which stays comment-free per test rules.)
- Hard constraint preserved: **fill fields always** — a field is never intentionally left empty; the round count stays minimal.
- Work directly on `main` — no feature branch (per user workflow).

---

### Task 1: Rest-aware packing in `ScheduleService`

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java` (replace `packRounds`; add two private helpers)
- Test: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java` (add one test method)

**Interfaces:**
- Consumes: `MatchPair` (existing private record: `MatchPair(GroupId groupId, TeamId team1Id, TeamId team2Id)`), `computeDegrees(List<MatchPair>) -> Map<TeamId,Integer>` (existing, unchanged).
- Produces: `packRounds(List<MatchPair> allMatches, int numFields) -> List<List<MatchPair>>` — signature unchanged; internal ordering becomes rest-aware. Two new private static helpers: `isBackToBack(MatchPair, Map<TeamId,Integer>, int) -> boolean` and `minRest(MatchPair, Map<TeamId,Integer>, int) -> int`.

> **Why this test scenario:** 1 field, group A (2 teams → 1 match) + group B (4 teams → 6 matches) = 7 matches over 7 rounds. The current degree-only ordering schedules group A's low-degree match late (round 5), so a1/a2 sit idle 4 rounds then play — exactly the "wait several rounds then play" pathology. The rest-aware ordering prioritizes the fully-rested A teams and schedules their match by round 3. This setup mirrors the existing `generateWhenSingleFieldShouldScheduleOneMatchPerRound` test, so the totals (7 rounds / 7 matches) are already known-correct.

- [ ] **Step 1: Write the failing test**

Add this method to `ScheduleServiceTest` (after `generateWhenSingleFieldShouldScheduleOneMatchPerRound`). It asserts that no team is left idle for a long stretch before playing: with one field, the small idle-prone group A must be scheduled within the first 3 rounds instead of being dumped near the end.

```java
    @Test
    void generateWhenSingleFieldShouldScheduleIdleGroupEarlyForRestFairness() {
        final var tournament = buildTournament().withNumberOfFields(1);
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        final var tA1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tA2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tB1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(tA1, tA2, tB1, tB2, tB3, tB4));

        final var scheduleGenerated = scheduleService.generate(tournament.getId().value(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        final var groupARoundNumber = scheduleGenerated.getRounds().stream()
                .filter(round -> round.getMatches().stream()
                        .anyMatch(match -> match.getGroupName().equals("A")))
                .map(RoundOverview::getNumber)
                .findFirst()
                .orElseThrow();

        assertEquals(7, scheduleGenerated.getTotalRounds());
        assertTrue(groupARoundNumber <= 3,
                "Group A should be scheduled early for rest fairness but was round " + groupARoundNumber);
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && ./mvnw test -Dtest=ScheduleServiceTest#generateWhenSingleFieldShouldScheduleIdleGroupEarlyForRestFairness -q`
Expected: FAIL — assertion error `Group A should be scheduled early for rest fairness but was round 5` (current degree-only ordering defers the low-degree group A match).

- [ ] **Step 3: Replace `packRounds` and add the two helpers**

In `ScheduleService.java`, replace the entire existing `packRounds` method with the version below, and add the two new helper methods directly after it. `computeDegrees` stays unchanged.

```java
    private static List<List<MatchPair>> packRounds(final List<MatchPair> allMatches, final int numFields) {
        final var remaining = new ArrayList<>(allMatches);
        final var rounds = new ArrayList<List<MatchPair>>();
        // Round index in which each team last played. Absent = has not played yet.
        // Used to measure "rest" (rounds waited) and to detect back-to-back play.
        final var lastPlayedRound = new HashMap<TeamId, Integer>();
        var roundIndex = 0;
        while (!remaining.isEmpty()) {
            final var degrees = computeDegrees(remaining);
            final var currentRound = roundIndex;
            // Order remaining matches so the fairest one to play now sorts first:
            //   1. Matches with NO team that played the previous round come first,
            //      so we avoid back-to-back play whenever an alternative exists.
            //   2. Among equals, the match whose least-rested team has waited the
            //      longest comes first (min rest across the two teams, descending),
            //      so the most starved team is served first and idle waits stay short.
            //   3. Finally fall back to the remaining-degree heuristic (teams with
            //      the most matches left first) so the total round count stays minimal.
            remaining.sort((a, b) -> {
                final var backToBackA = isBackToBack(a, lastPlayedRound, currentRound);
                final var backToBackB = isBackToBack(b, lastPlayedRound, currentRound);
                if (backToBackA != backToBackB) {
                    return Boolean.compare(backToBackA, backToBackB);
                }
                final var restA = minRest(a, lastPlayedRound, currentRound);
                final var restB = minRest(b, lastPlayedRound, currentRound);
                if (restA != restB) {
                    return Integer.compare(restB, restA);
                }
                final var degreeA = degrees.get(a.team1Id()) + degrees.get(a.team2Id());
                final var degreeB = degrees.get(b.team1Id()) + degrees.get(b.team2Id());
                return Integer.compare(degreeB, degreeA);
            });
            final var round = new ArrayList<MatchPair>();
            final var usedTeams = new HashSet<TeamId>();
            final var iterator = remaining.iterator();
            while (iterator.hasNext() && round.size() < numFields) {
                final var pair = iterator.next();
                final var team1Free = !usedTeams.contains(pair.team1Id());
                final var team2Free = !usedTeams.contains(pair.team2Id());
                if (team1Free && team2Free) {
                    round.add(pair);
                    usedTeams.add(pair.team1Id());
                    usedTeams.add(pair.team2Id());
                    iterator.remove();
                }
            }
            // Every team that played is now marked as having played this round,
            // so later rounds can measure their rest and skip back-to-back matches.
            for (final var team : usedTeams) {
                lastPlayedRound.put(team, currentRound);
            }
            rounds.add(round);
            roundIndex++;
        }
        return rounds;
    }

    // A match is "back-to-back" when either of its teams played in the immediately
    // previous round. Such matches are only used to fill a field that would
    // otherwise sit empty. MIN_VALUE default keeps never-played teams from matching
    // the previous round (-1) in the very first round.
    private static boolean isBackToBack(final MatchPair pair, final Map<TeamId, Integer> lastPlayedRound,
                                        final int roundIndex) {
        final var previousRound = roundIndex - 1;
        return lastPlayedRound.getOrDefault(pair.team1Id(), Integer.MIN_VALUE) == previousRound
                || lastPlayedRound.getOrDefault(pair.team2Id(), Integer.MIN_VALUE) == previousRound;
    }

    // Rest = rounds since a team last played (a never-played team counts as fully
    // rested via the -1 default). A match's rest is the minimum across its two
    // teams, so the more starved team drives how urgently the match is scheduled.
    private static int minRest(final MatchPair pair, final Map<TeamId, Integer> lastPlayedRound,
                               final int roundIndex) {
        final var rest1 = roundIndex - lastPlayedRound.getOrDefault(pair.team1Id(), -1);
        final var rest2 = roundIndex - lastPlayedRound.getOrDefault(pair.team2Id(), -1);
        return Math.min(rest1, rest2);
    }
```

Note: the imports `java.util.ArrayList`, `java.util.HashMap`, `java.util.HashSet`, `java.util.Map` are already present in the file (from the current working-tree version) — no import changes needed.

- [ ] **Step 4: Run the new test to verify it passes**

Run: `cd backend && ./mvnw test -Dtest=ScheduleServiceTest#generateWhenSingleFieldShouldScheduleIdleGroupEarlyForRestFairness -q`
Expected: PASS (group A is now scheduled in round 3).

- [ ] **Step 5: Run the full `ScheduleServiceTest` to confirm no regressions**

Run: `cd backend && ./mvnw test -Dtest=ScheduleServiceTest -q`
Expected: PASS — all tests, including `generateWhenValidShouldReturnScheduleOverview`, `generateWhenSingleFieldShouldScheduleOneMatchPerRound`, `generateWhenGroupLargerThanFieldsShouldPackSameGroupMatchesInOneRound`, and `generateShouldFillAllFieldsAndNeverScheduleTeamTwiceInSameRound` (round count and field-fill assertions are unaffected — when every team plays every round, all matches are back-to-back and the field-fill logic is unchanged).

- [ ] **Step 6: Run the full backend test suite**

Run: `cd backend && ./mvnw test -q`
Expected: PASS — BUILD SUCCESS, no failures.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java \
        backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java
git commit -m "feat: rest-aware schedule packing to balance team rest

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- "Track last-played round per team" → `lastPlayedRound` map (Step 3). ✓
- "Score each remaining match by rest" / "minimum rest across two teams" → `minRest` helper + secondary sort key. ✓
- "Penalize back-to-back" → `isBackToBack` helper + primary sort key. ✓
- "Tie-break by remaining-degree" → tertiary sort key reusing `computeDegrees`. ✓
- "Greedy field fill unchanged" → inner `while` loop identical to current code. ✓
- "Update lastPlayedRound after each round" → `for (team : usedTeams)` block. ✓
- "Fill fields always" hard constraint → field-fill loop and round count logic unchanged; back-to-back matches are still used when nothing else is eligible. ✓
- "Comments requirement" (production code) → block comments on the map, the three sort keys, and both helpers. ✓
- "Testing: existing tests pass" → Step 5. "New test for fairness" → Step 1. ✓

**Placeholder scan:** No TBD/TODO/vague steps — all code and commands are concrete. ✓

**Type consistency:** `packRounds` signature unchanged; `isBackToBack`/`minRest` take `(MatchPair, Map<TeamId,Integer>, int)`; `computeDegrees` reused as-is; `RoundOverview::getNumber` and `MatchOverview.getGroupName()` used in the test match existing accessors (see `buildScheduleOverview`). ✓
