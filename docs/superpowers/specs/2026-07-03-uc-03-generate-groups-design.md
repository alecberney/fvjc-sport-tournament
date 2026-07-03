# UC-03 — Generate Groups: Design

## Context

Implementation of `specs/tournament/UC-03-generate-groups.md` on branch `feature/uc-03-generate-groups`.

The feature adds group draw management to a DRAFT tournament. The admin provides a group size, the system runs a randomised draw that spreads same-organisation teams across groups, and produces named groups (A, B, C, …). The admin can then manually swap teams between groups or trigger a full reshuffle.

---

## Architecture

### Key decision: Team owns its groupId

Rather than a join table on the `Group` side, `Team` gets a nullable `groupId` field. Finding all teams in a group is a simple `findAllByGroupId` query. This avoids a join table and keeps persistence straightforward.

### Key decision: GroupView assembled by GroupService

The API embeds team data inside group responses. `GroupService` loads `Team` objects and assembles `GroupView` objects — consistent with how `TeamService` produces `TeamView`.

### Key decision: Separate groups page

Groups live at `/tournaments/:id/groups`, not on the tournament detail page.

### Key decision: Single-step generate modal

The `/distribution` endpoint is implemented on the backend (per spec) but the frontend skips the preview step — the modal has just a group size input and a Generate button.

### Key decision: Swap via button + modal

Each team row has a swap button. Clicking it opens a modal listing all teams in other groups; selecting one triggers the swap.

---

## Backend

### Domain — `group` feature

Package: `abe.fvjc.tournament.group.domain`

| Class | Type | Description |
|---|---|---|
| `GroupId` | record | `.of(UUID)`, `.empty()`, `.isEmpty()` |
| `Group` | `@Value @Builder @With` | `id`, `name`, `tournamentId` |
| `GroupView` | `@Value @Builder` | `id`, `name`, `tournamentId`, `teams: List<Team>` |
| `GroupDistribution` | `@Value @Builder` | `numberOfGroups`, `groupsOfBaseSize`, `groupsOfBaseSizePlusOne`, `baseSize`, `totalTeams` |
| `GroupStore` | interface | `save(Group)`, `saveAll(List<Group>)`, `findAllByTournamentId(UUID)`, `deleteAllByTournamentId(UUID)` |
| `GroupGenerateRequest` | `@Value @Builder` | `groupSize: Integer` |
| `GroupSwapRequest` | `@Value @Builder` | `teamId1: UUID`, `teamId2: UUID` |
| `GroupValidator` | `@UtilityClass` | `validateGroupGenerateRequest(request, totalTeams)` |
| `GroupService` | `@Service` | See below |

**GroupService methods:**

```
distribution(UUID tournamentId, GroupGenerateRequest) → GroupDistribution
  1. Load teams for tournament via teamStore.findAllByTournamentId
  2. Validate request (groupSize ≥ 2, totalTeams ≥ groupSize)
  3. Compute and return distribution — no persistence

generate(UUID tournamentId, GroupGenerateRequest) → List<GroupView>
  1. Load tournament → NotFoundException if missing
  2. Assert DRAFT → ConflictException if not
  3. Load teams for tournament
  4. Validate request
  5. Set groupId = empty on all teams (update via teamStore.save)
  6. Delete all existing groups (groupStore.deleteAllByTournamentId)
  7. Run distribution algorithm
  8. Run draw algorithm
  9. Save groups (groupStore.saveAll)
  10. Assign teams to groups (update each team's groupId via teamStore.save)
  11. Return GroupViews

findAllByTournamentId(UUID) → List<GroupView>
  1. Load all groups for tournament
  2. For each group, load teams via teamStore.findAllByGroupId
  3. Return assembled GroupViews

swap(UUID tournamentId, GroupSwapRequest) → List<GroupView>  (both affected groups)
  1. Load tournament → NotFoundException if missing
  2. Assert DRAFT → ConflictException if not
  3. Load all groups for tournament
  4. Find group containing teamId1, find group containing teamId2
  5. Validate both found (NotFoundException), validate different groups (ValidationException)
  6. Swap: team1.withGroupId(group2.id), team2.withGroupId(group1.id)
  7. Save both teams
  8. Return GroupViews for both affected groups
```

**Distribution algorithm** (given `totalTeams`, `groupSize`):
```
numberOfGroups = totalTeams / groupSize  (integer division)
remainder      = totalTeams % groupSize
→ remainder groups of (groupSize + 1)
→ (numberOfGroups - remainder) groups of groupSize
```

**Draw algorithm:**
1. Group teams by `organisationId`, sort organisations by team count descending
2. Assign each org's teams round-robin across groups (largest org first)
3. Shuffle remaining unassigned teams randomly
4. Fill remaining slots in each group

**Group naming** (0-based index `i`):
```
i < 26  → single letter: 'A' + i
i ≥ 26  → groupName(i / 26 - 1) + ('A' + i % 26)
```

### Domain changes — `team` feature

- `Team` gets a new field: `groupId: GroupId` (nullable via `GroupId.empty()`)
- `TeamStore` gets: `findAllByGroupId(UUID groupId)`

---

### Persistence — `group` feature

Package: `abe.fvjc.tournament.group.persistence`

- `GroupEntity` — `@Getter @Setter @NoArgsConstructor`, fields: `id UUID`, `name String`, `tournamentId UUID`
- `GroupRepository` — `findAllByTournamentId(UUID)`, `deleteAllByTournamentId(UUID)`
- `JpaGroupStore implements GroupStore` — `@Transactional` per method, `readOnly = true` on reads
- `GroupDbMapper` — `@UtilityClass`, `toGroup(entity)` / `toGroupEntity(group)`

### Persistence changes — `team` feature

- `TeamEntity` gets: `groupId UUID` (nullable column)
- `TeamDbMapper` updated to map `groupId` (null → `GroupId.empty()`)
- `TeamRepository` gets: `findAllByGroupId(UUID groupId)`

---

### API — `group` feature

Package: `abe.fvjc.tournament.group.api`

**Controller:** `GroupController` at `/api/tournaments/{tournamentId}/groups`

| Method | Endpoint | Handler | Status |
|---|---|---|---|
| POST | `/distribution` | `distribution` | 200 |
| POST | `/generate` | `generate` | 201 |
| GET | `/` | `getAll` | 200 |
| POST | `/swap` | `swap` | 200 |

**DTOs:**

- `GroupDto`: `id: UUID`, `name: String`, `teams: List<GroupTeamDto>`
- `GroupTeamDto`: `id: UUID`, `name: String`, `organisationId: UUID`
- `GroupDistributionDto`: `numberOfGroups`, `groupsOfBaseSize`, `groupsOfBaseSizePlusOne`, `baseSize`, `totalTeams` (all `int`)
- `GroupGenerateRequestDto`: `@NotNull Integer groupSize`
- `GroupSwapRequestDto`: `@NotNull UUID teamId1`, `@NotNull UUID teamId2`

**`GroupApiMapper`** (`@UtilityClass`): `toGroupDto`, `toGroupTeamDto`, `toGroupDistributionDto`, `toGroupGenerateRequest`, `toGroupSwapRequest`

---

### Database (Liquibase)

Two new changelogs added to `master.xml`:

**Alter `teams` table** — add nullable `group_id`:
- `group_id TEXT NULL`

**New `groups` table:**
- `id TEXT PK`
- `name TEXT NOT NULL`
- `tournament_id TEXT NOT NULL`

---

### Tests

`GroupServiceTest` with:
- `@Mock GroupStore groupStore`
- `@Mock TeamStore teamStore`
- `@Mock TournamentStore tournamentStore`
- `@InjectMocks GroupService groupService`

`GroupFakes` + `IdGenerator` in `src/test/java/.../group/domain/`

Test cases:
- `generate` — happy path (draw correct, groups saved), not DRAFT → 409, groupSize < 2 → 400, not enough teams → 400, existing groups replaced on regenerate
- `distribution` — happy path with correct counts, groupSize < 2 → 400
- `findAllByTournamentId` — returns assembled GroupViews with teams
- `swap` — happy path (groupIds swapped), team not found → 404, same group → 400, not DRAFT → 409

---

## Frontend

### New route

`tournament.routes.ts` gets a new child:
```typescript
{ path: ':id/groups', component: TournamentGroupsPage }
```
`GroupState` provided at the route group level alongside `TournamentState` and `TeamState`.

---

### Group feature files

**`api/group/`:**
- `group.api.dto.ts` — `GroupDto`, `GroupTeamDto`, `GroupDistributionDto`, `GroupGenerateRequestDto`, `GroupSwapRequestDto`
- `group.api.service.ts` — `generate$(tournamentId, request)`, `getAll$(tournamentId)`, `swap$(tournamentId, request)`
- `group.api.mapper.ts` — `toDomain(dto): Group`. When mapping `GroupTeamDto` to `Team`, set `paid: false`, `responsibleFirstName: ''`, `responsibleLastName: ''` — these fields are not returned by the group API and are not displayed in the groups UI.

**`domain/group/`:**
- `group.model.ts` — `Group { id: string, name: string, teams: Team[] }` (reuses existing `Team` type)
- `group.actions.ts` — `LoadGroups`, `GenerateGroups`, `SwapTeams`
- `group.state.ts` — `IGroupState { groups: Group[] }`, selectors: `getGroups`

---

### Display components

**`TournamentGroupsPage`** (page, `display/tournament/pages/tournament-groups/`):
- `LoadGroups` on init
- Selects `GroupState.getGroups` and `TournamentState.getSelected`
- Opens `GroupGenerateModal` and `TeamSwapModal` via `MatDialog`

**`GroupListComponent`** (component, `display/tournament/components/group-list/`):
- `@Input({ required: true }) groups: Group[]`
- `@Input({ required: true }) isDraft: boolean`
- `@Output() swapRequested` emitting `{ team: Team, currentGroupName: string }`
- Displays each group with its name and team list; swap button per team (only when isDraft)

**`GroupGenerateModal`** (modal, `display/tournament/pages/group-generate/`):
- Single field: group size (number input)
- On submit: dispatches `GenerateGroups`

**`TeamSwapModal`** (modal, `display/tournament/pages/team-swap/`):
- Receives: `{ team: Team, currentGroupName: string, groups: Group[] }` via dialog data
- Lists all teams in other groups, grouped by group name
- On selection: dispatches `SwapTeams`

---

### Modified files

- `tournament-detail.page.html` / `.ts` — add "Groupes" navigation button linking to groups page
- `modules/tournament.routes.ts` — add groups child route, provide `GroupState`

---

## Error handling

| Scenario | Backend | Frontend |
|---|---|---|
| groupSize < 2 | 400 `{ errors: [...] }` | Show field error in modal |
| Not enough teams | 400 | Show field error in modal |
| Tournament not DRAFT | 409 `{ error: "..." }` | Show snackbar |
| Team not found (swap) | 404 | Show snackbar |
| Same group (swap) | 400 | Show snackbar |

---

## Files to create/modify

### Backend (new files)
```
domain/group/GroupId.java
domain/group/Group.java
domain/group/GroupView.java
domain/group/GroupDistribution.java
domain/group/GroupStore.java
domain/group/GroupGenerateRequest.java
domain/group/GroupSwapRequest.java
domain/group/GroupValidator.java
domain/group/GroupService.java
persistence/group/GroupEntity.java
persistence/group/GroupRepository.java
persistence/group/JpaGroupStore.java
persistence/group/GroupDbMapper.java
api/group/GroupController.java
api/group/GroupDto.java
api/group/GroupTeamDto.java
api/group/GroupDistributionDto.java
api/group/GroupGenerateRequestDto.java
api/group/GroupSwapRequestDto.java
api/group/GroupApiMapper.java
resources/db/changelog/20260703120000_alter_teams_add_group_id.xml
resources/db/changelog/20260703120001_create_groups_table.xml
```

### Backend (modified files)
```
domain/team/Team.java                      (add groupId: GroupId)
domain/team/TeamStore.java                 (add findAllByGroupId)
persistence/team/TeamEntity.java           (add groupId column)
persistence/team/TeamDbMapper.java         (map groupId)
persistence/team/TeamRepository.java       (add findAllByGroupId)
resources/db/changelog/master.xml          (add new changelogs)
```

### Backend (test files)
```
test/.../group/domain/IdGenerator.java
test/.../group/domain/GroupFakes.java
test/.../group/domain/GroupServiceTest.java
```

### Frontend (new files)
```
api/group/group.api.dto.ts
api/group/group.api.service.ts
api/group/group.api.mapper.ts
domain/group/group.model.ts
domain/group/group.actions.ts
domain/group/group.state.ts
display/tournament/pages/tournament-groups/tournament-groups.page.ts
display/tournament/pages/tournament-groups/tournament-groups.page.html
display/tournament/pages/tournament-groups/tournament-groups.page.scss
display/tournament/pages/group-generate/group-generate.modal.ts
display/tournament/pages/group-generate/group-generate.modal.html
display/tournament/pages/group-generate/group-generate.modal.scss
display/tournament/pages/team-swap/team-swap.modal.ts
display/tournament/pages/team-swap/team-swap.modal.html
display/tournament/pages/team-swap/team-swap.modal.scss
display/tournament/components/group-list/group-list.component.ts
display/tournament/components/group-list/group-list.component.html
display/tournament/components/group-list/group-list.component.scss
```

### Frontend (modified files)
```
modules/tournament.routes.ts
display/tournament/pages/tournament-detail/tournament-detail.page.ts
display/tournament/pages/tournament-detail/tournament-detail.page.html
```
