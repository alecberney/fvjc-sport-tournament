# UC-06 View Group Rankings — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `GET /api/tournaments/{id}/groups/rankings` endpoint with optional group-name filter, and a dedicated frontend rankings page with a single-select group dropdown.

**Architecture:** `RankingService` gains a `computeAllGroupRankings` method (delegates per-group work to an extracted private method). The controller binds the `?groups=` query param via a `@Value @Builder @Jacksonized` DTO using `@ModelAttribute`. The frontend adds a `LoadAllGroupRankings` action backed by a new API call; the rankings page reads from `ResultState.rankings` and dispatches on init and on filter change.

**Tech Stack:** Spring Boot 4.1 / Maven, Lombok, JUnit 5, Mockito · Angular standalone components, NGXS, Angular Material

## Global Constraints

- All local variables: `final var`
- Domain classes: `@Value @Builder @With`
- DTOs: `@Value @Builder @Jacksonized`
- Method parameters: always `final`
- Imports: `@app/` alias only — never relative paths in frontend
- Angular: standalone components, `inject()` only — no constructor injection
- NGXS: `patchState` only — never `setState`
- Pages dispatch actions; components are pure `@Input`/`@Output`

---

## File Map

**Backend — created:**
- `backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingSearchRequest.java`

**Backend — modified:**
- `backend/src/main/java/abe/fvjc/tournament/schedule/domain/RankingService.java`
- `backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingController.java`
- `backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java`

**Frontend — modified:**
- `frontend/src/app/api/result/result.api.dto.ts`
- `frontend/src/app/api/result/result.api.service.ts`
- `frontend/src/app/domain/result/result.actions.ts`
- `frontend/src/app/domain/result/result.state.ts`
- `frontend/src/app/modules/tournament.routes.ts`
- `frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html`

**Frontend — created:**
- `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.ts`
- `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.html`
- `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.scss`

---

## Task 1 — BE: `computeAllGroupRankings` in `RankingService` (TDD)

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/domain/RankingService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java`

**Interfaces:**
- Produces: `public List<GroupRanking> computeAllGroupRankings(UUID tournamentId, List<String> groupNameFilter)`

- [ ] **Step 1: Write three failing tests in `RankingServiceTest`**

Append to `RankingServiceTest.java` (after the existing tests):

```java
@Test
void computeAllGroupRankingsWhenNoFilterShouldReturnAllGroups() {
    final var tournamentId = UUID.randomUUID();
    final var groupIdA = GroupId.of(UUID.randomUUID());
    final var groupIdB = GroupId.of(UUID.randomUUID());
    final var org = OrganisationId.of(UUID.randomUUID());
    final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");
    final var groupB = GroupFakes.buildGroup(null).withId(groupIdB).withName("B");
    final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupIdA);
    final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupIdB);

    when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
    when(teamStore.findAllByGroupId(groupIdA.value())).thenReturn(List.of(t1));
    when(teamStore.findAllByGroupId(groupIdB.value())).thenReturn(List.of(t2));
    when(matchStore.findAllByGroupId(groupIdA.value())).thenReturn(List.of());
    when(matchStore.findAllByGroupId(groupIdB.value())).thenReturn(List.of());

    final var rankingsFound = rankingService.computeAllGroupRankings(tournamentId, List.of());

    verify(groupStore).findAllByTournamentId(tournamentId);
    verify(teamStore).findAllByGroupId(groupIdA.value());
    verify(teamStore).findAllByGroupId(groupIdB.value());

    assertEquals(2, rankingsFound.size());
}

@Test
void computeAllGroupRankingsWhenFilterAppliedShouldReturnMatchingGroups() {
    final var tournamentId = UUID.randomUUID();
    final var groupIdA = GroupId.of(UUID.randomUUID());
    final var groupIdB = GroupId.of(UUID.randomUUID());
    final var org = OrganisationId.of(UUID.randomUUID());
    final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");
    final var groupB = GroupFakes.buildGroup(null).withId(groupIdB).withName("B");
    final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupIdA);

    when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
    when(teamStore.findAllByGroupId(groupIdA.value())).thenReturn(List.of(t1));
    when(matchStore.findAllByGroupId(groupIdA.value())).thenReturn(List.of());

    final var rankingsFound = rankingService.computeAllGroupRankings(tournamentId, List.of("A"));

    verify(groupStore).findAllByTournamentId(tournamentId);

    assertEquals(1, rankingsFound.size());
    assertEquals("A", rankingsFound.get(0).getGroupName());
}

@Test
void computeAllGroupRankingsWhenFilterMatchesNoneShouldReturnEmptyList() {
    final var tournamentId = UUID.randomUUID();
    final var groupIdA = GroupId.of(UUID.randomUUID());
    final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");

    when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA));

    final var rankingsFound = rankingService.computeAllGroupRankings(tournamentId, List.of("Z"));

    verify(groupStore).findAllByTournamentId(tournamentId);

    assertEquals(0, rankingsFound.size());
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -Dtest=RankingServiceTest -q 2>&1 | tail -20
```

Expected: compilation error — `computeAllGroupRankings` does not exist yet.

- [ ] **Step 3: Refactor `RankingService` to extract `computeRankingForGroup` and add `computeAllGroupRankings`**

Replace the full content of `RankingService.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.team.domain.TeamStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;

    public GroupRanking computeGroupRanking(final UUID tournamentId, final UUID groupId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var group = groups.stream()
                .filter(g -> g.getId().value().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Group", groupId));
        return computeRankingForGroup(group);
    }

    public List<GroupRanking> computeAllGroupRankings(final UUID tournamentId, final List<String> groupNameFilter) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var filtered = (groupNameFilter == null || groupNameFilter.isEmpty())
                ? groups
                : groups.stream()
                        .filter(g -> groupNameFilter.contains(g.getName()))
                        .toList();
        return filtered.stream()
                .map(this::computeRankingForGroup)
                .toList();
    }

    private GroupRanking computeRankingForGroup(final Group group) {
        final var teams = teamStore.findAllByGroupId(group.getId().value());
        final var matches = matchStore.findAllByGroupId(group.getId().value());
        final var statsMap = buildStatsMap(teams);
        accumulateStats(matches, statsMap);
        final var entries = buildRankedEntries(teams, statsMap);
        return GroupRanking.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .entries(entries)
                .build();
    }

    private static Map<UUID, int[]> buildStatsMap(final List<Team> teams) {
        final var map = new HashMap<UUID, int[]>();
        for (final var team : teams) {
            map.put(team.getId().value(), new int[6]);
        }
        return map;
    }

    private static void accumulateStats(final List<Match> matches, final Map<UUID, int[]> statsMap) {
        for (final var match : matches) {
            if (match.getResult() == null) continue;
            final var r = match.getResult();
            final var s1 = statsMap.get(match.getTeam1Id().value());
            final var s2 = statsMap.get(match.getTeam2Id().value());
            if (s1 == null || s2 == null) continue;
            s1[0]++;
            s2[0]++;
            s1[4] += r.getScore1();
            s1[5] += r.getScore2();
            s2[4] += r.getScore2();
            s2[5] += r.getScore1();
            if (r.getScore1() > r.getScore2()) {
                s1[1]++;
                s2[3]++;
            } else if (r.getScore1() < r.getScore2()) {
                s2[1]++;
                s1[3]++;
            } else {
                s1[2]++;
                s2[2]++;
            }
        }
    }

    private static List<GroupRankingEntry> buildRankedEntries(
            final List<Team> teams, final Map<UUID, int[]> statsMap) {
        final var sorted = teams.stream()
                .sorted(Comparator
                        .comparingInt((Team t) -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[1] * 2 + s[2]);
                        })
                        .thenComparingInt(t -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[4] - s[5]);
                        })
                        .thenComparingInt(t -> -statsMap.get(t.getId().value())[4]))
                .toList();
        final var entries = new ArrayList<GroupRankingEntry>();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            final var team = sorted.get(i);
            final var s = statsMap.get(team.getId().value());
            final var points = s[1] * 2 + s[2];
            final var goalDiff = s[4] - s[5];
            if (i > 0) {
                final var prev = sorted.get(i - 1);
                final var ps = statsMap.get(prev.getId().value());
                final var prevPoints = ps[1] * 2 + ps[2];
                final var prevGoalDiff = ps[4] - ps[5];
                if (points != prevPoints || goalDiff != prevGoalDiff || s[4] != ps[4]) {
                    rank = i + 1;
                }
            }
            entries.add(GroupRankingEntry.builder()
                    .rank(rank)
                    .team(TeamRef.builder()
                            .id(TeamId.of(team.getId().value()))
                            .name(team.getName())
                            .build())
                    .played(s[0])
                    .wins(s[1])
                    .draws(s[2])
                    .defeats(s[3])
                    .goalsFor(s[4])
                    .goalsAgainst(s[5])
                    .goalDifference(goalDiff)
                    .points(points)
                    .build());
        }
        return entries;
    }
}
```

- [ ] **Step 4: Run tests to confirm all pass**

```bash
cd backend && ./mvnw test -Dtest=RankingServiceTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 8 tests pass (5 existing + 3 new).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/schedule/domain/RankingService.java \
        backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java
git commit -m "UC-06: BE - Add computeAllGroupRankings to RankingService"
```

---

## Task 2 — BE: `GroupRankingSearchRequest` DTO + controller endpoint

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingSearchRequest.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingController.java`

**Interfaces:**
- Consumes: `RankingService.computeAllGroupRankings(UUID, List<String>)` from Task 1
- Produces: `GET /api/tournaments/{tournamentId}/groups/rankings?groups=A` → `List<GroupRankingDto>`

- [ ] **Step 1: Create `GroupRankingSearchRequest`**

Create `backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingSearchRequest.java`:

```java
package abe.fvjc.tournament.schedule.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class GroupRankingSearchRequest {
    List<String> groups;
}
```

- [ ] **Step 2: Add the endpoint to `RankingController`**

Replace the full content of `RankingController.java`:

```java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class RankingController {
    private final RankingService rankingService;

    @GetMapping("/groups/{groupId}/ranking")
    public GroupRankingDto getGroupRanking(
            @PathVariable UUID tournamentId,
            @PathVariable UUID groupId) {
        final var ranking = rankingService.computeGroupRanking(tournamentId, groupId);
        return toGroupRankingDto(ranking);
    }

    @GetMapping("/groups/rankings")
    public List<GroupRankingDto> getAllGroupRankings(
            @PathVariable UUID tournamentId,
            @ModelAttribute GroupRankingSearchRequest request) {
        return rankingService.computeAllGroupRankings(tournamentId, request.getGroups()).stream()
                .map(RankingApiMapper::toGroupRankingDto)
                .toList();
    }
}
```

- [ ] **Step 3: Build to verify no compilation errors**

```bash
cd backend && ./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Run full backend test suite**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingSearchRequest.java \
        backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingController.java
git commit -m "UC-06: BE - Add GET /groups/rankings endpoint with GroupRankingSearchRequest filter"
```

---

## Task 3 — FE: API layer (`GroupRankingSearchRequestDto` + `loadAllGroupRankings$`)

**Files:**
- Modify: `frontend/src/app/api/result/result.api.dto.ts`
- Modify: `frontend/src/app/api/result/result.api.service.ts`

**Interfaces:**
- Produces:
  - `GroupRankingSearchRequestDto { groups?: string[] }`
  - `ResultApiService.loadAllGroupRankings$(tournamentId: string, request: GroupRankingSearchRequestDto): Observable<GroupRankingDto[]>`

- [ ] **Step 1: Add `GroupRankingSearchRequestDto` to `result.api.dto.ts`**

Append to the end of `frontend/src/app/api/result/result.api.dto.ts`:

```typescript
export interface GroupRankingSearchRequestDto {
  groups?: string[];
}
```

- [ ] **Step 2: Add `loadAllGroupRankings$` to `ResultApiService`**

Replace the full content of `frontend/src/app/api/result/result.api.service.ts`:

```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TournamentDto } from '@app/api/tournament/tournament.api.dto';
import { GroupRankingDto, GroupRankingSearchRequestDto, MatchResultResponseDto, SubmitMatchResultRequestDto } from '@app/api/result/result.api.dto';

@Injectable({ providedIn: 'root' })
export class ResultApiService {

  private readonly http = inject(HttpClient);

  startTournament$(tournamentId: string): Observable<TournamentDto> {
    return this.http.post<TournamentDto>(`/api/tournaments/${tournamentId}/start`, {});
  }

  submitResult$(tournamentId: string, matchId: string, request: SubmitMatchResultRequestDto): Observable<MatchResultResponseDto> {
    return this.http.put<MatchResultResponseDto>(`/api/tournaments/${tournamentId}/matches/${matchId}/result`, request);
  }

  loadGroupRanking$(tournamentId: string, groupId: string): Observable<GroupRankingDto> {
    return this.http.get<GroupRankingDto>(`/api/tournaments/${tournamentId}/groups/${groupId}/ranking`);
  }

  loadAllGroupRankings$(tournamentId: string, request: GroupRankingSearchRequestDto): Observable<GroupRankingDto[]> {
    let params = new HttpParams();
    if (request.groups?.length) {
      params = params.set('groups', request.groups[0]);
    }
    return this.http.get<GroupRankingDto[]>(`/api/tournaments/${tournamentId}/groups/rankings`, { params });
  }
}
```

- [ ] **Step 3: Build frontend to verify no compilation errors**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: build succeeds with no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/api/result/result.api.dto.ts \
        frontend/src/app/api/result/result.api.service.ts
git commit -m "UC-06: FE - Add GroupRankingSearchRequestDto and loadAllGroupRankings$ to ResultApiService"
```

---

## Task 4 — FE: Domain layer (`LoadAllGroupRankings` action + `ResultState` handler)

**Files:**
- Modify: `frontend/src/app/domain/result/result.actions.ts`
- Modify: `frontend/src/app/domain/result/result.state.ts`

**Interfaces:**
- Consumes: `ResultApiService.loadAllGroupRankings$` from Task 3
- Produces: `LoadAllGroupRankings(tournamentId: string, request: GroupRankingSearchRequestDto)` action; `ResultState.rankings` updated with all returned groups

- [ ] **Step 1: Add `LoadAllGroupRankings` action to `result.actions.ts`**

Replace the full content of `frontend/src/app/domain/result/result.actions.ts`:

```typescript
import { GroupRankingSearchRequestDto } from '@app/api/result/result.api.dto';

export class StartTournament {
  static readonly type = '[Result] Start Tournament';
  constructor(public readonly tournamentId: string) {}
}

export class SubmitResult {
  static readonly type = '[Result] Submit Result';
  constructor(
    public readonly tournamentId: string,
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}

export class LoadGroupRanking {
  static readonly type = '[Result] Load Group Ranking';
  constructor(
    public readonly tournamentId: string,
    public readonly groupId: string,
  ) {}
}

export class LoadAllGroupRankings {
  static readonly type = '[Result] Load All Group Rankings';
  constructor(
    public readonly tournamentId: string,
    public readonly request: GroupRankingSearchRequestDto,
  ) {}
}

export class UpdateMatchResult {
  static readonly type = '[Result] Update Match Result';
  constructor(
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}
```

- [ ] **Step 2: Add `@Action(LoadAllGroupRankings)` handler to `ResultState`**

Replace the full content of `frontend/src/app/domain/result/result.state.ts`:

```typescript
import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { GroupRanking } from '@app/domain/result/result.model';
import { LoadAllGroupRankings, LoadGroupRanking, StartTournament, SubmitResult, UpdateMatchResult } from '@app/domain/result/result.actions';
import { ResultApiService } from '@app/api/result/result.api.service';
import { ResultApiMapper } from '@app/api/result/result.api.mapper';
import { PatchSelectedStatus } from '@app/domain/tournament/tournament.actions';

export interface IResultState {
  rankings: { [groupId: string]: GroupRanking };
}

@State<IResultState>({
  name: 'result',
  defaults: { rankings: {} },
})
@Injectable()
export class ResultState {

  private readonly resultApiService = inject(ResultApiService);

  @Selector()
  static getRankings(state: IResultState): { [groupId: string]: GroupRanking } {
    return state.rankings;
  }

  @Selector()
  static getRankingForGroup(state: IResultState): (groupId: string) => GroupRanking | undefined {
    return (groupId: string) => state.rankings[groupId];
  }

  @Action(StartTournament)
  startTournament(ctx: StateContext<IResultState>, { tournamentId }: StartTournament) {
    return this.resultApiService.startTournament$(tournamentId).pipe(
      tap((dto) => {
        ctx.dispatch(new PatchSelectedStatus(dto.status));
      })
    );
  }

  @Action(SubmitResult)
  submitResult(ctx: StateContext<IResultState>, { tournamentId, matchId, score1, score2 }: SubmitResult) {
    return this.resultApiService.submitResult$(tournamentId, matchId, { score1, score2 }).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto.ranking);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
        if (dto.match.result) {
          ctx.dispatch(new UpdateMatchResult(dto.match.id, dto.match.result.score1, dto.match.result.score2));
        }
      })
    );
  }

  @Action(LoadGroupRanking)
  loadGroupRanking(ctx: StateContext<IResultState>, { tournamentId, groupId }: LoadGroupRanking) {
    return this.resultApiService.loadGroupRanking$(tournamentId, groupId).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }

  @Action(LoadAllGroupRankings)
  loadAllGroupRankings(ctx: StateContext<IResultState>, { tournamentId, request }: LoadAllGroupRankings) {
    return this.resultApiService.loadAllGroupRankings$(tournamentId, request).pipe(
      tap((dtos) => {
        const newRankings = { ...ctx.getState().rankings };
        dtos.forEach(dto => {
          const ranking = ResultApiMapper.toGroupRankingDomain(dto);
          newRankings[ranking.groupId] = ranking;
        });
        ctx.patchState({ rankings: newRankings });
      })
    );
  }
}
```

- [ ] **Step 3: Build to verify no compilation errors**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/domain/result/result.actions.ts \
        frontend/src/app/domain/result/result.state.ts
git commit -m "UC-06: FE - Add LoadAllGroupRankings action and ResultState handler"
```

---

## Task 5 — FE: Rankings page + nav tab + route

**Files:**
- Create: `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.ts`
- Create: `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.scss`
- Modify: `frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html`
- Modify: `frontend/src/app/modules/tournament.routes.ts`

**Interfaces:**
- Consumes: `LoadAllGroupRankings` from Task 4, `LoadGroups` from `GroupState`, `LoadTournamentById` from `TournamentState`

- [ ] **Step 1: Create `tournament-rankings.page.ts`**

Create `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.ts`:

```typescript
import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { Group } from '@app/domain/group/group.model';
import { GroupRanking } from '@app/domain/result/result.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { GroupState } from '@app/domain/group/group.state';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { LoadAllGroupRankings } from '@app/domain/result/result.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';

@Component({
  selector: 'app-tournament-rankings-page',
  templateUrl: './tournament-rankings.page.html',
  styleUrl: './tournament-rankings.page.scss',
  standalone: true,
  imports: [AsyncPipe, FormsModule, MatFormFieldModule, MatSelectModule, MatIconModule, TournamentNavComponent],
})
export class TournamentRankingsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<Group[]> = this.store.select(GroupState.getGroups);
  readonly rankings$: Observable<{ [groupId: string]: GroupRanking }> = this.store.select(ResultState.getRankings);

  selectedGroup: string | null = null;
  protected tournamentId!: string;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadGroups(this.tournamentId),
      new LoadAllGroupRankings(this.tournamentId, {}),
    ]);
  }

  onGroupFilterChange(): void {
    this.store.dispatch(new LoadAllGroupRankings(
      this.tournamentId,
      { groups: this.selectedGroup ? [this.selectedGroup] : [] },
    ));
  }

  getRankingsToDisplay(groups: Group[], rankings: { [groupId: string]: GroupRanking }): GroupRanking[] {
    if (this.selectedGroup) {
      const group = groups.find(g => g.name === this.selectedGroup);
      if (!group) return [];
      const ranking = rankings[group.id];
      return ranking ? [ranking] : [];
    }
    return groups
      .map(g => rankings[g.id])
      .filter((r): r is GroupRanking => r !== undefined);
  }
}
```

- [ ] **Step 2: Create `tournament-rankings.page.html`**

Create `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.html`:

```html
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <h1>{{ tournament.name }}</h1>
  </div>
  <app-tournament-nav [tournamentId]="tournamentId" />

  <div class="rankings-layout">
    @if (groups$ | async; as groups) {
      <div class="rankings-filter">
        <mat-form-field appearance="outline">
          <mat-label>Groupe</mat-label>
          <mat-select [(ngModel)]="selectedGroup" (ngModelChange)="onGroupFilterChange()">
            <mat-option [value]="null">Tous les groupes</mat-option>
            @for (group of groups; track group.id) {
              <mat-option [value]="group.name">Groupe {{ group.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (rankings$ | async; as rankings) {
        @for (ranking of getRankingsToDisplay(groups, rankings); track ranking.groupId) {
          <div class="group-ranking">
            <h2>Groupe {{ ranking.groupName }}</h2>
            <table class="ranking-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Équipe</th>
                  <th>J</th>
                  <th>V</th>
                  <th>N</th>
                  <th>D</th>
                  <th>BP</th>
                  <th>BC</th>
                  <th>Diff</th>
                  <th>Pts</th>
                </tr>
              </thead>
              <tbody>
                @for (entry of ranking.entries; track entry.team.id) {
                  <tr>
                    <td>{{ entry.rank }}</td>
                    <td>{{ entry.team.name }}</td>
                    <td>{{ entry.played }}</td>
                    <td>{{ entry.wins }}</td>
                    <td>{{ entry.draws }}</td>
                    <td>{{ entry.defeats }}</td>
                    <td>{{ entry.goalsFor }}</td>
                    <td>{{ entry.goalsAgainst }}</td>
                    <td>{{ entry.goalDifference }}</td>
                    <td><strong>{{ entry.points }}</strong></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      }
    }
  </div>
}
```

- [ ] **Step 3: Create `tournament-rankings.page.scss`**

Create `frontend/src/app/display/tournament/pages/tournament-rankings/tournament-rankings.page.scss`:

```scss
.rankings-layout {
  padding: 16px;
}

.rankings-filter {
  margin-bottom: 24px;
}

.group-ranking {
  margin-bottom: 32px;

  h2 {
    margin-bottom: 8px;
  }
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 8px 12px;
    text-align: left;
    border-bottom: 1px solid #e0e0e0;
  }

  th {
    font-weight: 600;
    background-color: #f5f5f5;
  }

  tbody tr:hover {
    background-color: #fafafa;
  }
}
```

- [ ] **Step 4: Add "Classements" tab to `tournament-nav.component.html`**

Replace the full content of `frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html`:

```html
<nav mat-tab-nav-bar [tabPanel]="tabPanel">
  <a mat-tab-link
     [routerLink]="['/', tournamentId]"
     routerLinkActive
     #equipesLink="routerLinkActive"
     [active]="equipesLink.isActive"
     [routerLinkActiveOptions]="{ exact: true }">
    <mat-icon>groups</mat-icon>
    Équipes
  </a>
  <a mat-tab-link
     [routerLink]="['/', tournamentId, 'schedule']"
     routerLinkActive
     #horaireLink="routerLinkActive"
     [active]="horaireLink.isActive">
    <mat-icon>calendar_month</mat-icon>
    Horaire
  </a>
  <a mat-tab-link
     [routerLink]="['/', tournamentId, 'results']"
     routerLinkActive
     #resultsLink="routerLinkActive"
     [active]="resultsLink.isActive">
    <mat-icon>emoji_events</mat-icon>
    Résultats
  </a>
  <a mat-tab-link
     [routerLink]="['/', tournamentId, 'rankings']"
     routerLinkActive
     #rankingsLink="routerLinkActive"
     [active]="rankingsLink.isActive">
    <mat-icon>leaderboard</mat-icon>
    Classements
  </a>
  <a mat-tab-link disabled>
    <mat-icon>grid_view</mat-icon>
    Groupes
  </a>
  <a mat-tab-link disabled>
    <mat-icon>account_tree</mat-icon>
    Arbre
  </a>
</nav>
<mat-tab-nav-panel #tabPanel></mat-tab-nav-panel>
```

- [ ] **Step 5: Register the route in `tournament.routes.ts`**

Replace the full content of `frontend/src/app/modules/tournament.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { ScheduleState } from '../domain/schedule/schedule.state';
import { ResultState } from '../domain/result/result.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';
import { TournamentSchedulePage } from '../display/tournament/pages/tournament-schedule/tournament-schedule.page';
import { TournamentResultsPage } from '../display/tournament/pages/tournament-results/tournament-results.page';
import { TournamentRankingsPage } from '../display/tournament/pages/tournament-rankings/tournament-rankings.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState, ResultState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
      { path: ':id/schedule', component: TournamentSchedulePage },
      { path: ':id/results', component: TournamentResultsPage },
      { path: ':id/rankings', component: TournamentRankingsPage },
    ],
  },
];
```

- [ ] **Step 6: Build frontend to verify no compilation errors**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: build succeeds with no errors.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/display/tournament/pages/tournament-rankings/ \
        frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html \
        frontend/src/app/modules/tournament.routes.ts
git commit -m "UC-06: FE - Add TournamentRankingsPage with group filter dropdown and nav tab"
```
