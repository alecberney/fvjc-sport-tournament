# UC-07 — Generate Bracket from Group Stage Results — Design

## Overview

UC-07 adds a knockout bracket phase to the tournament. After the group stage is complete and rankings are known, the organizer generates a single-elimination bracket by specifying how many teams qualify per group and the schedule parameters. Teams are seeded into hats by finishing position and drawn randomly across hats.

---

## Backend

### New package: `abe.fvjc.tournament.bracket`

Three sub-packages: `domain`, `persistence`, `api`.

---

### Domain layer (`bracket.domain`)

#### Domain models

**`BracketRound`** — `@Value @Builder @With`
- `BracketRoundId id`
- `TournamentId tournamentId`
- `int number` — 1 = first bracket round (QF), 2 = second (SF), etc.
- `String name` — computed from match count: 8 teams → "Quarts de finale", 4 → "Demi-finales", 2 → "Finale"
- `LocalDateTime startTime`
- `List<BracketMatch> matches` — embedded for read/API use; populated by service, not stored directly

**`BracketMatch`** — `@Value @Builder @With`
- `BracketMatchId id`
- `BracketRoundId roundId`
- `int field`
- `TeamRef team1` — nullable; null = TBD (team not yet determined)
- `TeamRef team2` — nullable; null = TBD
- `MatchResult result` — nullable; null until submitted
- `BracketMatchId nextMatchId` — nullable; null for the final
- `int nextMatchTeamSlot` — `1` or `2`: which slot in the next match this winner fills

**`BracketRoundId`**, **`BracketMatchId`** — typed ID records with `.of(UUID)`, `.empty()`, `.isEmpty()`.

**`TieBreaker`** — enum
- `POINTS_SCORED` — selects teams with the most goals scored
- `POINTS_DIFF` — selects teams with the best goal difference (scored − conceded)
- `POINTS_TAKEN` — selects teams with the fewest goals conceded

**`BracketGenerateRequest`** — `@Value @Builder @With`
- `int totalQualifiedTeams` — total bracket size; must be a power of 2
- `TieBreaker tieBreaker` — criteria used to pick extra qualifiers when `totalQualifiedTeams % numGroups != 0`
- `String startTime` — HH:mm format
- `Integer matchDurationMinutes`
- `Integer breakDurationMinutes`

#### Store interfaces

**`BracketRoundStore`**
- `BracketRound save(BracketRound)`
- `Optional<BracketRound> findById(UUID)`
- `List<BracketRound> findAllByTournamentId(UUID)` — ordered by `number` asc
- `void deleteAllByTournamentId(UUID)`

**`BracketMatchStore`**
- `BracketMatch save(BracketMatch)`
- `Optional<BracketMatch> findById(UUID)`
- `List<BracketMatch> findAllByRoundId(UUID)`
- `void deleteAllByRoundId(UUID)`

#### `BracketService`

```java
@Service
@RequiredArgsConstructor
public class BracketService {
    private final BracketRoundStore bracketRoundStore;
    private final BracketMatchStore bracketMatchStore;
    private final GroupStore groupStore;
    private final TournamentStore tournamentStore;
    private final RankingService rankingService;

    public List<BracketRound> generate(UUID tournamentId, BracketGenerateRequest request);
    public List<BracketRound> findAll(UUID tournamentId);  // returns rounds with matches embedded
}
```

**Generation algorithm (`generate`):**

1. Load tournament to get `numberOfFields`
2. Load all groups for the tournament
3. Compute rankings for all groups (`RankingService.computeAllGroupRankings`)
4. Validate request (see `BracketValidator` below)
5. Compute `qualifiersPerGroup = totalQualifiedTeams / numGroups` and `extraQualifiers = totalQualifiedTeams % numGroups`
6. If `extraQualifiers > 0`: verify at least `extraQualifiers` groups have a rank-`qualifiersPerGroup+1` entry — throw `BusinessException` otherwise
7. Build seeded list:
   - For each rank position 1..`qualifiersPerGroup`: collect all teams at that rank across groups, shuffle, append to the list
   - If `extraQualifiers > 0`: collect all rank-`qualifiersPerGroup+1` teams across all groups, sort by `tieBreaker` criteria, take the top `extraQualifiers`, shuffle, append
8. Draw round-1 pairs: seed 1 vs seed N, seed 2 vs seed N-1, … (best seeds face worst seeds)
9. Compute round count: `log2(totalTeams)`
10. Compute round names from teams-in-round count: 8 → "Quarts de finale", 4 → "Demi-finales", 2 → "Finale"
11. Compute start times: round 1 = `request.startTime`; each subsequent round starts `matchDurationMinutes + breakDurationMinutes` after the previous
12. Create `BracketRound` and save for each round
13. Create `BracketMatch` records for round 1 with drawn `team1`/`team2`; subsequent rounds get `team1 = null`, `team2 = null`
14. Wire `nextMatchId` across rounds: matches 1 and 2 from round N → match 1 of round N+1 (slots 1 and 2 respectively), matches 3 and 4 → match 2, etc.
15. Assign fields round-robin: `field = (matchIndex % numberOfFields) + 1`
16. Save all matches

**TieBreaker comparator:**
- `POINTS_SCORED` → sort by `goalsFor` descending
- `POINTS_DIFF` → sort by `goalDifference` descending
- `POINTS_TAKEN` → sort by `goalsAgainst` ascending (fewer is better)

**Validation (`BracketValidator`):**

`@UtilityClass` in `bracket.domain`. Method: `validateBracketGenerateRequest(BracketGenerateRequest, int numGroups)`.

- `totalQualifiedTeams >= 2`
- `totalQualifiedTeams` must be a power of 2
- `matchDurationMinutes >= 1`
- `breakDurationMinutes >= 0`

#### Unit tests (`BracketServiceTest`)

- `generateWhenValidRequestShouldCreateRoundsAndMatches`
- `generateWhenTotalTeamsNotPowerOfTwoShouldThrowValidationException`
- `generateWhenFourTeamsQualifiedShouldCreateTwoRounds`
- `generateWhenExtraQualifiersShouldSelectByTieBreakerAndCreateBracket`

---

### Persistence layer (`bracket.persistence`)

**`BracketRoundEntity`** — `@Entity @Table(name="bracket_rounds")`, package-private, `@Getter @Setter @NoArgsConstructor`
- `UUID id`, `UUID tournamentId`, `int number`, `String name`, `LocalDateTime startTime`

**`BracketMatchEntity`** — `@Entity @Table(name="bracket_matches")`, package-private
- `UUID id`, `UUID roundId`, `int field`
- `UUID team1Id` (nullable), `String team1Name` (nullable)
- `UUID team2Id` (nullable), `String team2Name` (nullable)
- `Integer score1` (nullable), `Integer score2` (nullable)
- `UUID nextMatchId` (nullable), `int nextMatchTeamSlot`

**`BracketRoundRepository`** — `interface extends JpaRepository<BracketRoundEntity, UUID>`
- `List<BracketRoundEntity> findAllByTournamentIdOrderByNumberAsc(UUID tournamentId)`
- `void deleteAllByTournamentId(UUID tournamentId)`

**`BracketMatchRepository`** — `interface extends JpaRepository<BracketMatchEntity, UUID>`
- `List<BracketMatchEntity> findAllByRoundId(UUID roundId)`
- `void deleteAllByRoundId(UUID roundId)`

**`JpaBracketRoundStore`** and **`JpaBracketMatchStore`** — `@Repository @RequiredArgsConstructor`, implement stores.

**`BracketDbMapper`** — `@UtilityClass` package-private.

---

### API layer (`bracket.api`)

#### DTOs

**`BracketGenerateRequestDto`** — `@Value @Builder @Jacksonized`
- `@Min(2) int totalQualifiedTeams`
- `@NotNull TieBreaker tieBreaker`
- `@NotNull String startTime` — HH:mm format
- `@NotNull @Min(1) Integer matchDurationMinutes`
- `@NotNull @Min(0) Integer breakDurationMinutes`

**`BracketRoundDto`** — `@Value @Builder @Jacksonized`
- `UUID id`, `int number`, `String name`, `LocalDateTime startTime`, `List<BracketMatchDto> matches`

**`BracketMatchDto`** — `@Value @Builder @Jacksonized`
- `UUID id`, `int field`
- `TeamRefDto team1` (nullable), `TeamRefDto team2` (nullable)
- `MatchResultDto result` (nullable)

#### Controller

```
POST /api/tournaments/{tournamentId}/bracket/generate  → 201 List<BracketRoundDto>
GET  /api/tournaments/{tournamentId}/bracket           → 200 List<BracketRoundDto>
```

**`BracketController`** — `@RestController @RequestMapping("/api/tournaments/{tournamentId}/bracket")`

```java
@PostMapping("/generate")
@ResponseStatus(HttpStatus.CREATED)
public List<BracketRoundDto> generate(@PathVariable final UUID tournamentId,
                                      @RequestBody @Valid final BracketGenerateRequestDto request)

@GetMapping
public List<BracketRoundDto> getAll(@PathVariable final UUID tournamentId)
```

**`BracketApiMapper`** — `@UtilityClass` public. Methods: `toBracketRoundDto(BracketRound)`, `toBracketMatchDto(BracketMatch)`, `toBracketGenerateRequest(BracketGenerateRequestDto)`.

---

## Frontend

### API layer (`api/bracket/`)

**`bracket.api.dto.ts`**
```typescript
export interface BracketGenerateRequestDto {
  totalQualifiedTeams: number;
  tieBreaker: string; // TieBreaker enum value
  startTime: string;  // HH:mm
  matchDurationMinutes: number;
  breakDurationMinutes: number;
}

export interface BracketRoundDto {
  id: string;
  number: number;
  name: string;
  startTime: string;
  matches: BracketMatchDto[];
}

export interface BracketMatchDto {
  id: string;
  field: number;
  team1: TeamRefDto | null;
  team2: TeamRefDto | null;
  result: MatchResultDto | null;
}
```

(`TeamRefDto` and `MatchResultDto` already exist in `result.api.dto.ts` — re-export or import from there.)

**`bracket.api.service.ts`**
- `generateBracket$(tournamentId, request): Observable<BracketRoundDto[]>`
- `loadBracket$(tournamentId): Observable<BracketRoundDto[]>`

**`bracket.api.mapper.ts`** — static `toDomain(dto: BracketRoundDto): BracketRound`

---

### Domain layer (`domain/bracket/`)

**`bracket.model.ts`**
```typescript
export interface BracketRound {
  id: string;
  number: number;
  name: string;
  startTime: Date;
  matches: BracketMatch[];
}

export interface BracketMatch {
  id: string;
  field: number;
  team1: TeamRef | null;
  team2: TeamRef | null;
  result: MatchResult | null;
}
```

**`bracket.actions.ts`**
```typescript
export class LoadBracket {
  static readonly type = '[Bracket] Load Bracket';
  constructor(public readonly tournamentId: string) {}
}

export class GenerateBracket {
  static readonly type = '[Bracket] Generate Bracket';
  constructor(
    public readonly tournamentId: string,
    public readonly request: BracketGenerateRequestDto,
  ) {}
}
```

**`bracket.state.ts`** — `IBracketState { rounds: BracketRound[] }`
- `@Selector() static getRounds`: returns `rounds`
- `@Selector() static hasBracket`: returns `rounds.length > 0`
- `@Action(LoadBracket)`: calls `loadBracket$`, maps DTOs, patchState
- `@Action(GenerateBracket)`: calls `generateBracket$`, maps DTOs, patchState

---

### Display layer

#### `BracketGenerateModal` (`display/tournament/components/bracket-generate-modal/`)

Form fields:
- `totalQualifiedTeams` — number input (min 2), hint: must be a power of 2
- `tieBreaker` — select with options: `POINTS_DIFF` (default), `POINTS_SCORED`, `POINTS_TAKEN`
- `startTime` — text input HH:mm
- `matchDurationMinutes` — number input (min 1)
- `breakDurationMinutes` — number input (min 0)

On submit: emits form values; parent page dispatches `GenerateBracket`.

#### `TournamentBracketPage` (`display/tournament/pages/tournament-bracket/`)

- On init: dispatches `LoadTournamentById(tournamentId)` and `LoadBracket(tournamentId)`
- If no bracket (`hasBracket$ = false`): shows "Générer le bracket" button
- On button click: opens `BracketGenerateModal`; on modal close with result: dispatches `GenerateBracket`
- If bracket exists: displays each round as a section

Round display:
```
[Round name] — [startTime | date:'HH:mm']
  Field | Team 1        | vs | Team 2        | Score
  T1    | Les Aigles    | vs | Les Renards   | —
  T2    | À déterminer  | vs | À déterminer  | —
```

#### Nav update (`TournamentNavComponent`)

Add "Bracket" tab with `account_tree` icon, route `/:id/bracket`, before the disabled tabs.

#### Routes (`tournament.routes.ts`)

```typescript
{ path: ':id/bracket', component: TournamentBracketPage }
```

`BracketState` provided at feature level alongside existing states.

---

## Out of scope for UC-07

- Entering bracket match results (UC-08)
- Winner advancement logic (UC-08)
- Regenerating the bracket after results have been entered
- Avoiding same-group matchups in the draw
