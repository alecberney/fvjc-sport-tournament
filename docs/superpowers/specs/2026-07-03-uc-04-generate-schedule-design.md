# UC-04 — Generate Group Stage Schedule — Design

## Overview

Generate the group stage schedule: round-robin matches per group, assigned to fields, organised into global rounds with computed start times. The spec (`specs/tournament/UC-04-generate-schedule.md`) is authoritative on the algorithm and API contract. This doc covers implementation decisions.

---

## Rename: View → Overview

Before implementing UC-04, rename existing assembled read objects across the codebase:
- `GroupView` → `GroupOverview` (domain + api + tests)
- `TeamView` → `TeamOverview` (domain + api + tests)

Rationale: "View" collides with the DB view concept. All assembled read-only projections use the `Overview` suffix going forward.

---

## Backend

### Package conventions

Same layer-first directory, feature-first package name pattern as existing code:
- Directory: `api/schedule/`, `domain/schedule/`, `persistence/schedule/`
- Packages: `schedule.api`, `schedule.domain`, `schedule.persistence`

### Domain layer (`schedule.domain`)

**Value objects:**
- `Round` — `@Value @Builder @With`: `RoundId id`, `TournamentId tournamentId`, `int number`, `LocalDateTime startTime`
- `RoundId` — typed ID record with `.of(UUID)`, `.empty()`, `.isEmpty()`
- `Match` — `@Value @Builder @With`: `MatchId id`, `RoundId roundId`, `int field`, `GroupId groupId`, `TeamId team1Id`, `TeamId team2Id`
  - No `result` field — added in UC-05
- `MatchId` — typed ID record with `.of(UUID)`, `.empty()`, `.isEmpty()`
- `TeamRef` — `@Value @Builder`: `TeamId id`, `String name` — lightweight team reference used in overviews
- `MatchOverview` — assembled: `MatchId id`, `int field`, `GroupId groupId`, `String groupName`, `TeamRef team1`, `TeamRef team2`
- `RoundOverview` — assembled: `RoundId id`, `int number`, `LocalDateTime startTime`, `List<MatchOverview> matches`
- `ScheduleOverview` — `int totalRounds`, `int totalMatches`, `List<RoundOverview> rounds`

**Request:**
- `ScheduleGenerateRequest` — `@Value @Builder`: `String startTime` (format "HH:mm"), `Integer matchDurationMinutes`, `Integer breakDurationMinutes`

**Interfaces:**
- `RoundStore` — `saveAll(List<Round>)`, `findAllByTournamentId(UUID)`, `deleteAllByTournamentId(UUID)`
- `MatchStore` — `saveAll(List<Match>)`, `findAllByRoundIds(List<UUID>)`, `deleteAllByRoundIds(List<UUID>)`, `existsResultByTournamentId(UUID)`
  - `existsResultByTournamentId` always returns `false` in UC-04 since there are no results yet; kept for the regeneration guard and UC-05 compatibility

**Service — `ScheduleService`:**
- `generate(UUID tournamentId, ScheduleGenerateRequest request) -> ScheduleOverview`
  1. Load and assert tournament is DRAFT
  2. Load groups; assert at least one exists
  3. Check `matchStore.existsResultByTournamentId()` → throw BusinessException if true
  4. Delete existing rounds + matches if any
  5. Run algorithm (see spec Step 1–5)
  6. Persist all rounds then all matches
  7. Assemble and return ScheduleOverview
- `findByTournamentId(UUID tournamentId) -> ScheduleOverview`
  - Empty ScheduleOverview (`totalRounds=0`, `totalMatches=0`, `rounds=[]`) when no rounds exist

**Validator — `ScheduleValidator`:**
- Validates `startTime` is non-null and parseable as `LocalTime` (format "HH:mm")
- Validates `matchDurationMinutes` non-null, >= 1
- Validates `breakDurationMinutes` non-null, >= 0
- Collects all errors before throwing `ValidationException`

**Algorithm helpers (private static methods in ScheduleService):**
- `generateGroupMatches(List<TeamId> teams) -> List<List<TeamId[]>>` — circle method round-robin; each inner list is one sub-round's pairs; teams are flat-iterated in order
- `buildFieldQueues(List<GroupOverview> groups, int numFields) -> Map<Integer, List<TeamId[]>>` — assign groups to fields mod numFields, interleave matches
- `buildRoundsAndMatches(Map<Integer, List<TeamId[]>> fieldQueues, Map<GroupId, String> groupNames, LocalDateTime firstRoundStart, int matchDuration, int breakDuration) -> result pairs`

### Persistence layer (`schedule.persistence`)

**Entities (package-private):**
- `RoundEntity` — `@Getter @Setter @NoArgsConstructor`, table `rounds`
- `MatchEntity` — `@Getter @Setter @NoArgsConstructor`, table `matches`
  - Carries both `roundId` and `tournamentId` for efficient bulk delete/exists queries

**Repositories (package-private):**
- `RoundRepository extends JpaRepository<RoundEntity, UUID>` — `findByTournamentId(UUID)`, `deleteByTournamentId(UUID)`
- `MatchRepository extends JpaRepository<MatchEntity, UUID>` — `findByRoundIdIn(List<UUID>)`, `deleteByRoundIdIn(List<UUID>)`, `existsByTournamentId(UUID)` (always false until UC-05 adds result columns)

**Mappers (package-private `@UtilityClass`):** `RoundDbMapper`, `MatchDbMapper`

**Store implementations:** `JpaRoundStore`, `JpaMatchStore` — `@Repository @RequiredArgsConstructor`, `@Transactional` per method

### API layer (`schedule.api`)

**Controller — `ScheduleController`:**
- `POST /api/tournaments/{tournamentId}/schedule/generate` → 201 Created → `ScheduleDto`
- `GET /api/tournaments/{tournamentId}/schedule` → 200 OK → `ScheduleDto`

**DTOs:**
- `ScheduleGenerateRequestDto` — `@NotNull String startTime`, `@NotNull @Min(1) Integer matchDurationMinutes`, `@NotNull @Min(0) Integer breakDurationMinutes`
- `ScheduleDto` — `int totalRounds`, `int totalMatches`, `List<RoundDto> rounds`
- `RoundDto` — `UUID id`, `int number`, `String startTime` (ISO datetime), `List<MatchDto> matches`
- `MatchDto` — `UUID id`, `int field`, `UUID groupId`, `String groupName`, `MatchTeamDto team1`, `MatchTeamDto team2`
  - No `result` field — added with proper type in UC-05
- `MatchTeamDto` — `UUID id`, `String name`

**Mapper — `ScheduleApiMapper`:** `@UtilityClass`, maps `ScheduleGenerateRequestDto → ScheduleGenerateRequest` and `ScheduleOverview → ScheduleDto`

### Database migrations (Liquibase)

Two new changesets:

```
rounds table:
  id          TEXT NOT NULL PRIMARY KEY
  tournament_id TEXT NOT NULL
  number      INT NOT NULL
  start_time  TEXT NOT NULL

matches table:
  id            TEXT NOT NULL PRIMARY KEY
  round_id      TEXT NOT NULL
  tournament_id TEXT NOT NULL
  field         INT NOT NULL
  group_id      TEXT NOT NULL
  team1_id      TEXT NOT NULL
  team2_id      TEXT NOT NULL
```

`result_score1` and `result_score2` columns added by UC-05 migration.

### Unit tests (`schedule.domain`)

**`IdGenerator`** (package-private `@UtilityClass`): `roundId()`, `matchId()`

**`ScheduleFakes`** (`@UtilityClass`):
- `buildGenerateRequest()` — valid request with realistic defaults
- `buildRound(TournamentId tournamentId)` — round with generated ID
- `buildMatch(RoundId roundId)` — match with generated IDs

**`ScheduleServiceTest`** — key scenarios:
- `generateWhenTournamentNotFoundShouldThrowNotFoundException`
- `generateWhenTournamentNotDraftShouldThrowConflictException`
- `generateWhenNoGroupsShouldThrowValidationException`
- `generateWhenResultsExistShouldThrowBusinessException`
- `generateWhenValidShouldPersistRoundsAndMatches`
- `generateWhenExistingScheduleShouldDeleteBeforePersisting`
- `findByTournamentIdWhenNoRoundsShouldReturnEmptyOverview`
- `findByTournamentIdWhenRoundsExistShouldReturnPopulatedOverview`

---

## Frontend

### Naming

Domain models are interfaces (no `Overview` suffix on FE — that's a backend naming concern).

### API layer (`api/schedule/`)

- `schedule.api.dto.ts` — mirrors backend response DTOs exactly
- `schedule.api.service.ts` — `generate$(tournamentId, request)`, `getSchedule$(tournamentId)` — both return `Observable<ScheduleDto>`
- `schedule.api.mapper.ts` — static class; `toDomain(dto)` maps `ScheduleDto` to `Schedule` (parses `startTime` strings to `Date`)

### Domain layer (`domain/schedule/`)

**Models (`schedule.model.ts`):**
```
Schedule { totalRounds, totalMatches, rounds: Round[] }
Round    { id, number, startTime: Date, matches: Match[] }
Match    { id, field, groupId, groupName, team1: MatchTeam, team2: MatchTeam }
MatchTeam { id, name }
```

**Actions (`schedule.actions.ts`):**
- `GenerateSchedule(tournamentId: string, request: ScheduleGenerateRequestDto)`
- `LoadSchedule(tournamentId: string)`

**State (`schedule.state.ts`):**
- `IScheduleState { schedule: Schedule | undefined }`
- Selector: `getSchedule`
- Actions: `@Action(GenerateSchedule)`, `@Action(LoadSchedule)`

### Display layer

**`tournament-schedule.page.ts/.html`:**
- Dispatches `LoadSchedule` + `LoadTournamentById` on init
- Shows "Générer le calendrier" button when tournament is DRAFT
- Empty state when `schedule.totalRounds === 0`
- Round list: each round shows its time and matches (field, group name, team names)

**`schedule-generate.modal.ts/.html`:**
- Form fields: `startTime` (text, pattern "HH:mm"), `matchDurationMinutes` (number, min 1), `breakDurationMinutes` (number, min 0)
- Closes on `GenerateSchedule` action success

### Routes & navigation

- `ScheduleState` added to `provideStates` in `tournament.routes.ts`
- New route: `{ path: ':id/schedule', component: TournamentSchedulePage }`
- `tournament-detail.page.html`: add "Calendrier" button next to existing "Groupes" button (both visible in DRAFT)

---

## Out of scope for UC-04

- Match results (`result` field) — UC-05
- Tournament start (DRAFT → IN_PROGRESS) — UC-05
- Rankings — UC-06
