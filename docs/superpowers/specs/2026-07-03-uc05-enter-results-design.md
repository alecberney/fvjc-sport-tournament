# UC-05 — Start Tournament & Enter Results — Design

## Overview

The spec (`specs/tournament/UC-05-enter-results.md`) is authoritative on API contracts and validation rules. This doc covers implementation decisions.

UC-05 splits into three independent operations:
- **UC-05.1** — Start tournament (DRAFT → IN_PROGRESS)
- **UC-05.2** — Submit match result + get updated ranking inline
- **UC-05.4** — Get group ranking standalone

UC-05.3 (Get Round by number with navigation) is **dropped** in favour of reusing the existing schedule endpoint. The results page shows the full schedule list; clicking a match opens its result entry on the side.

---

## Backend

### Database migration (20260703120004)

Add two nullable columns to the `matches` table:

```
result_score1  INTEGER NULL
result_score2  INTEGER NULL
```

Both null until a result is submitted. Both non-null once submitted (a result is always two values).

### Domain layer (`schedule.domain`)

#### Modified value objects

**`Match`** — add `MatchResult result` (nullable, `@With`)

**`MatchOverview`** — add `MatchResult result` (nullable)

#### New value objects (all `@Value @Builder`)

- `MatchResult` — `int score1`, `int score2`
- `SubmitMatchResultRequest` — `Integer score1`, `Integer score2` (Integer for null validation in validator)
- `GroupRankingEntry` — `int rank`, `TeamRef team`, `int played`, `int wins`, `int draws`, `int defeats`, `int goalsFor`, `int goalsAgainst`, `int goalDifference`, `int points`
- `GroupRanking` — `GroupId groupId`, `String groupName`, `List<GroupRankingEntry> entries`

#### Modified `MatchStore`

Replace `existsResultByTournamentId(UUID tournamentId)` with `existsResultByRoundIds(List<UUID> roundIds)` — the `matches` table has no `tournament_id` column; the service already holds round IDs at the call site.

Add:
- `Optional<Match> findById(UUID matchId)`
- `Match save(Match match)` — updates a single match (used for result submission)
- `List<Match> findAllByGroupId(UUID groupId)` — used by `RankingService`

#### Modified `TeamStore`

Add `List<Team> findAllByGroupId(UUID groupId)` — used by `RankingService` to load the teams in a group without loading all tournament teams.

#### New validator — `ResultValidator` (`@UtilityClass`)

`validateSubmitMatchResultRequest(SubmitMatchResultRequest)` — validates score1/score2 not null, >= 0, <= 500. Collects all errors before throwing `ValidationException`.

#### New `RankingService`

```
computeGroupRanking(UUID tournamentId, UUID groupId) → GroupRanking
```

1. Load all teams for the group (`TeamStore.findAllByGroupId`)
2. Load all matches for the group (`MatchStore.findAllByGroupId`)
3. Compute per-team stats from matches that have a result (result != null)
4. Sort entries: points DESC → goalDifference DESC → goalsFor DESC
5. Assign ranks — tied teams share the same rank

All transformation logic in private static methods (testable in isolation).

#### New `ResultService`

Depends on: `GroupStore`, `MatchStore`, `TeamStore`, `TournamentStore`.

```
submitResult(UUID tournamentId, UUID matchId, SubmitMatchResultRequest) → MatchOverview
```

1. Load tournament, assert status == IN_PROGRESS (throw `ConflictException` if not)
2. Load match by ID (throw `NotFoundException` if not found)
3. Validate scores via `ResultValidator`
4. Save match with result via `MatchStore.save`
5. Load team refs and group name to assemble and return `MatchOverview`

Returns only `MatchOverview` — the controller calls `RankingService` separately and assembles the full response DTO.

#### Modified `TournamentService`

Add `start(UUID id) → Tournament`:
1. Load tournament (throw `NotFoundException` if not found)
2. Assert status == DRAFT (throw `ConflictException` if already IN_PROGRESS)
3. Assert at least one round exists: `RoundStore.countByTournamentId(id) > 0` (throw `ConflictException` if no schedule)
4. Save tournament with status IN_PROGRESS

Needs `RoundStore` injected.

#### Modified `ScheduleService`

Replace `matchStore.existsResultByTournamentId(tournamentId)` with:

```java
final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
final var roundIds = existingRounds.stream().map(r -> r.getId().value()).toList();
if (!roundIds.isEmpty() && matchStore.existsResultByRoundIds(roundIds)) { ... }
```

No new public methods on `ScheduleService`.

#### Modified `RoundStore`

Add `int countByTournamentId(UUID tournamentId)` — used by `TournamentService.start`.

### Persistence layer (`schedule.persistence`)

**`MatchEntity`** — add `Integer resultScore1`, `Integer resultScore2` (nullable)

**`MatchDbMapper`** — map result columns in both directions:
- `toMatch`: set `result` to `new MatchResult(entity.getResultScore1(), entity.getResultScore2())` when both columns non-null, null otherwise
- `toMatchEntity`: set result columns from `match.getResult()` when non-null, leave null otherwise

**`MatchRepository`** — add:
- `List<MatchEntity> findByGroupId(UUID groupId)`
- `boolean existsByRoundIdInAndResultScore1IsNotNull(List<UUID> roundIds)`
- `Optional<MatchEntity> findById(UUID id)` — already provided by `JpaRepository`, no declaration needed

**`JpaMatchStore`** — implement new store methods:
- `findById` → `matchRepository.findById(matchId).map(MatchDbMapper::toMatch)`
- `save(Match)` → map to entity, call `matchRepository.save`, map result back
- `findAllByGroupId` → `matchRepository.findByGroupId(groupId).stream().map(...).toList()`
- `existsResultByRoundIds` → `matchRepository.existsByRoundIdInAndResultScore1IsNotNull(roundIds)`

**`RoundRepository`** — add `int countByTournamentId(UUID tournamentId)`

**`JpaRoundStore`** — implement `countByTournamentId`

### API layer

#### New controllers

**`TournamentController`** — add endpoint:
```
POST /api/tournaments/{id}/start → 200 TournamentDto
```

**`ResultController`** (`@RequestMapping("/api/tournaments/{tournamentId}")`):
```
PUT /matches/{matchId}/result → 200 MatchResultResponseDto
```
Controller calls `resultService.submitResult` → gets `MatchOverview`, then calls `rankingService.computeGroupRanking(tournamentId, matchOverview.getGroupId().value())` → assembles DTO via mapper.

**`RankingController`** (`@RequestMapping("/api/tournaments/{tournamentId}")`):
```
GET /groups/{groupId}/ranking → 200 GroupRankingDto
```

#### New/modified DTOs

- `SubmitMatchResultRequestDto` — `@NotNull @Min(0) @Max(500) Integer score1`, same for `score2`
- `MatchResultDto` — `int score1`, `int score2`
- `MatchDto` — add `MatchResultDto result` (nullable) — existing schedule endpoint returns null for unplayed matches, no breaking change
- `MatchResultResponseDto` — `MatchDto match`, `GroupRankingDto ranking`
- `GroupRankingDto` — `UUID groupId`, `String groupName`, `List<GroupRankingEntryDto> entries`
- `GroupRankingEntryDto` — `int rank`, `MatchTeamDto team`, `int played`, `int wins`, `int draws`, `int defeats`, `int goalsFor`, `int goalsAgainst`, `int goalDifference`, `int points`

#### New mappers

- `ResultApiMapper` (`@UtilityClass`) — `toSubmitMatchResultRequest(dto)`, `toMatchResultResponseDto(MatchOverview, GroupRanking)`
- `RankingApiMapper` (`@UtilityClass`) — `toGroupRankingDto(GroupRanking)`

### Unit tests (`schedule.domain`)

**`ResultValidatorTest`** — score null, negative, above 500, valid

**`RankingServiceTest`** (key scenarios):
- `computeGroupRankingWhenNoResultsShouldReturnZeroStats`
- `computeGroupRankingWhenWinShouldGive2Points`
- `computeGroupRankingWhenDrawShouldGive1PointEach`
- `computeGroupRankingWhenMultipleResultsShouldOrderByPointsThenGoalDiffThenGoalsFor`
- `computeGroupRankingWhenFullyTiedShouldShareRank`

**`ResultServiceTest`** (key scenarios):
- `submitResultWhenTournamentNotFoundShouldThrowNotFoundException`
- `submitResultWhenTournamentNotInProgressShouldThrowConflictException`
- `submitResultWhenMatchNotFoundShouldThrowNotFoundException`
- `submitResultWhenValidShouldSaveAndReturnMatchOverview`

**`TournamentServiceTest`** additions:
- `startWhenNotFoundShouldThrowNotFoundException`
- `startWhenAlreadyInProgressShouldThrowConflictException`
- `startWhenNoRoundsShouldThrowConflictException`
- `startWhenValidShouldTransitionToInProgress`

---

## Frontend

### API layer (`api/result/`)

- `result.api.dto.ts` — `SubmitMatchResultRequestDto`, `MatchResultDto`, `MatchResultResponseDto`, `GroupRankingDto`, `GroupRankingEntryDto`; also update `schedule.api.dto.ts` to add `result: MatchResultDto | null` to `MatchDto`
- `result.api.service.ts` — `startTournament$(tournamentId)`, `submitResult$(tournamentId, matchId, request)`, `loadGroupRanking$(tournamentId, groupId)`
- `result.api.mapper.ts` — static class; maps DTOs to domain models

### Domain layer (`domain/result/`)

- `result.model.ts` — `MatchResult { score1, score2 }`, `GroupRankingEntry`, `GroupRanking { groupId, groupName, entries }`
- Update `schedule.model.ts` — add `result: MatchResult | null` to `Match`
- `result.actions.ts` — `StartTournament(tournamentId)`, `SubmitResult(tournamentId, matchId, score1, score2)`, `LoadGroupRanking(tournamentId, groupId)`
- `result.state.ts` — `IResultState { rankings: { [groupId: string]: GroupRanking } }`
  - `StartTournament` — calls `resultApiService.startTournament$`, patches `TournamentState.selected` status to IN_PROGRESS on success
  - `SubmitResult` — calls `resultApiService.submitResult$`, stores ranking by groupId in `ResultState`; `ScheduleState` also listens to `SubmitResult` with its own `@Action` handler and updates the relevant match's result inline
  - `LoadGroupRanking` — calls `resultApiService.loadGroupRanking$`, stores ranking by groupId

### Display layer

**New `TournamentResultsPage`** (`display/tournament/pages/tournament-results/`):
- Side-by-side layout: left panel shows the full schedule list (reads from `ScheduleState`); right panel is context-dependent
- `selectedMatchId` held in component state (not NGXS)
- Clicking a match sets `selectedMatchId` → right panel shows score entry form for that match (pre-filled if result already exists)
- On form submit, dispatches `SubmitResult`
- After `SubmitResult` success, right panel switches to show the `GroupRanking` for that match's group (read from `ResultState.rankings`)
- On init: dispatches `LoadSchedule(tournamentId)` and `LoadTournamentById(tournamentId)`; also dispatches `LoadGroupRanking` for each group visible in the schedule

**Update `TournamentSchedulePage`**:
- Add "Démarrer le tournoi" button, visible when `tournament.status === DRAFT` and `schedule.totalRounds > 0`
- Button opens a confirm dialog; on confirm, dispatches `StartTournament(tournamentId)` then navigates to `/:id/results`

### Routes (`tournament.routes.ts`)

- Add `{ path: ':id/results', component: TournamentResultsPage }`
- Add `ResultState` to `provideStates`

---

## Out of scope for UC-05

- UC-05.3 (Get Round by number with navigation flags) — dropped; results page reuses existing schedule data
- Knockout stage (UC-07)
- Persisted rankings — always computed on the fly
