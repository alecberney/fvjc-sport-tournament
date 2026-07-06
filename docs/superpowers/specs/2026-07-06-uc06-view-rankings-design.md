# UC-06 — View Group Rankings — Design

## Overview

The spec (`specs/tournament/UC-06-view-rankings.md`) is authoritative on API contracts and validation rules. This doc covers implementation decisions.

UC-06 adds a dedicated rankings page showing all group standings computed on the fly from match results. The page is accessible in any tournament status and supports filtering by group name via a single-select dropdown.

---

## Backend

### Domain layer (`schedule.domain`)

#### Modified `RankingService`

Add `computeAllGroupRankings(UUID tournamentId, List<String> groupNameFilter) → List<GroupRanking>`:

1. Load all groups via `groupStore.findAllByTournamentId(tournamentId)`
2. If `groupNameFilter` is non-null and non-empty, filter to groups whose `name` is in the list
3. For each remaining group, reuse existing per-group computation (teams + matches + stats)
4. Return `List<GroupRanking>`

The existing `computeGroupRanking(tournamentId, groupId)` is unchanged.

### API layer (`schedule.api`)

#### New DTO — `GroupRankingSearchRequest`

```java
@Value
@Builder
@Jacksonized
public class GroupRankingSearchRequest {
    List<String> groups; // null or empty = return all groups
}
```

Placed in `api/` alongside other controller-facing DTOs. Spring MVC binds query params via `@ModelAttribute` using the `@ConstructorProperties`-annotated all-args constructor that Lombok `@Value` generates.

#### Modified `RankingController`

Add endpoint:
```
GET /api/tournaments/{tournamentId}/groups/rankings
@ModelAttribute GroupRankingSearchRequest request
→ 200 List<GroupRankingDto>
```

Controller calls `rankingService.computeAllGroupRankings(tournamentId, request.getGroups())` and maps each result via `RankingApiMapper.toGroupRankingDto`.

`GroupRankingDto` already exists — no new response DTO needed.

### Unit tests (`schedule.domain`)

**Additions to `RankingServiceTest`:**
- `computeAllGroupRankingsWhenNoFilterShouldReturnAllGroups`
- `computeAllGroupRankingsWhenFilterAppliedShouldReturnMatchingGroups`
- `computeAllGroupRankingsWhenFilterMatchesNoneShouldReturnEmptyList`

---

## Frontend

### API layer (`api/result/`)

#### New DTO in `result.api.dto.ts`

```typescript
export interface GroupRankingSearchRequestDto {
  groups?: string[];
}
```

#### Modified `ResultApiService`

Add:
```typescript
loadAllGroupRankings$(tournamentId: string, request: GroupRankingSearchRequestDto): Observable<GroupRankingDto[]>
```

Builds query string from `request.groups` (e.g. `?groups=A,B`) and hits `GET /api/tournaments/{id}/groups/rankings`.

### Domain layer (`domain/result/`)

#### New action in `result.actions.ts`

```typescript
export class LoadAllGroupRankings {
  static readonly type = '[Result] Load All Group Rankings';
  constructor(
    public readonly tournamentId: string,
    public readonly request: GroupRankingSearchRequestDto,
  ) {}
}
```

#### Modified `ResultState`

New `@Action(LoadAllGroupRankings)` handler: calls `resultApiService.loadAllGroupRankings$`, maps each DTO via `ResultApiMapper.toGroupRankingDomain`, and replaces the `rankings` map with the new set (keyed by `groupId`).

### Display layer

#### New `TournamentRankingsPage` (`display/tournament/pages/tournament-rankings/`)

- On init: dispatches `LoadGroups(tournamentId)` (for filter options) and `LoadAllGroupRankings(tournamentId, {})` (all groups)
- Reads group names from `GroupState` to populate the filter dropdown
- Filter UI: `<mat-select>` single-select dropdown — "Tous les groupes" (no filter) + one entry per group name
- On selection change: dispatches `LoadAllGroupRankings(tournamentId, { groups: selected ? [selected] : [] })`
- Reads rankings from `ResultState.rankings`
- Displays each `GroupRanking` as a table: Rang / Équipe / J / V / N / D / BP / BC / Diff / Pts
- Component state: `selectedGroup: string | null = null`

#### Modified `TournamentNavComponent`

Add "Classements" tab linking to `/:id/rankings`.

### Routes (`tournament.routes.ts`)

Add:
```typescript
{ path: ':id/rankings', component: TournamentRankingsPage }
```

No new state to provide — `ResultState` and `GroupState` are already provided at the feature level.

---

## Out of scope for UC-06

- Real-time push updates (WebSocket/SSE) — rankings refresh on page init and on filter change
- Knockout stage rankings (UC-07)
- Persisting computed rankings — always computed on the fly
