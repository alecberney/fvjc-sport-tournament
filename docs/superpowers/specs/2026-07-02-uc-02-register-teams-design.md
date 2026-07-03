# UC-02 — Register Teams: Design

## Context

Implementation of `specs/tournament/UC-02-register-teams.md` on branch `feature/uc-02-register-teams`.

The feature adds team registration to a DRAFT tournament. It introduces two new aggregates (`Organisation` and `Team`) and a tournament detail page where the admin manages teams.

---

## Architecture

### Key decision: Organisation lifecycle owned by TeamService

`Organisation` has no standalone API. Its lifecycle is entirely driven by team operations:
- Created once per batch registration
- Updated when a team's responsible person changes
- Deleted automatically when its last team is removed

Therefore `TeamService` owns both aggregates via `OrganisationStore` and `TeamStore`. No separate `OrganisationService`.

### Key decision: ConflictException for 409

A new `ConflictException` (alongside the existing `NotFoundException`) maps to `409 Conflict` with body `{ "error": "..." }`. A `ConflictErrorResponse` DTO and handler are added to `GlobalExceptionHandler`.

---

## Backend

### Shared exception additions

**`ConflictException`** in `shared.exception`:
```java
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
```

**`ConflictErrorResponse`** DTO in `shared.exception` (or api layer):
```json
{ "error": "..." }
```

Handler added to `GlobalExceptionHandler`: `ConflictException` → `409 Conflict`.

---

### Domain — `organisation` feature

Package: `abe.fvjc.tournament.organisation.domain`

| Class | Type | Description |
|---|---|---|
| `OrganisationId` | record | `.of(UUID)`, `.empty()`, `.isEmpty()` |
| `Organisation` | `@Value @Builder @With` | `id`, `responsibleFirstName`, `responsibleLastName`, `tournamentId` |
| `OrganisationStore` | interface | `save`, `findById`, `deleteById` |

---

### Domain — `team` feature

Package: `abe.fvjc.tournament.team.domain`

| Class | Type | Description |
|---|---|---|
| `TeamId` | record | `.of(UUID)`, `.empty()`, `.isEmpty()` |
| `Team` | `@Value @Builder @With` | `id`, `name`, `paid`, `organisationId`, `tournamentId` |
| `TeamStore` | interface | `save`, `findById`, `findAllByTournamentId`, `deleteById`, `countByOrganisationId` |
| `TeamRegisterRequest` | `@Value @Builder` | `name`, `responsibleFirstName`, `responsibleLastName`, `count`, `List<Boolean> paid`, `UUID tournamentId` |
| `TeamUpdateRequest` | `@Value @Builder` | `name`, `responsibleFirstName`, `responsibleLastName`, `paid` |
| `TeamPaidRequest` | `@Value @Builder` | `paid` |
| `TeamValidator` | `@UtilityClass` | `validateTeamRegisterRequest`, `validateTeamUpdateRequest` |
| `TeamService` | `@Service` | See below |

**TeamService methods:**

```
registerTeams(TeamRegisterRequest, UUID tournamentId) → List<Team>
  1. Load tournament — throw NotFoundException if missing
  2. Assert DRAFT — throw ConflictException if not
  3. Validate request (TeamValidator)
  4. Create one Organisation
  5. Create N Teams (name = "base" if count=1, "base N" if count>1)
  6. Return created teams

findAllByTournamentId(UUID tournamentId) → List<Team>

updateTeam(UUID teamId, UUID tournamentId, TeamUpdateRequest) → Team
  1. Load tournament — throw NotFoundException if missing
  2. Assert DRAFT — throw ConflictException if not
  3. Load team — throw NotFoundException if missing
  4. Validate request
  5. Update Organisation (responsible person fields)
  6. Save updated Team
  7. Return updated team

deleteTeam(UUID teamId, UUID tournamentId) → void
  1. Load tournament — throw NotFoundException if missing
  2. Assert DRAFT — throw ConflictException if not
  3. Load team — throw NotFoundException if missing
  4. Delete team
  5. If countByOrganisationId == 0 → delete organisation

markPaid(UUID teamId, UUID tournamentId, boolean paid) → Team
  1. Load team — throw NotFoundException if missing
  2. Save team with updated paid flag
  3. Return updated team (no DRAFT check — allowed anytime)
```

---

### Persistence — `organisation` feature

Package: `abe.fvjc.tournament.organisation.persistence`

- `OrganisationEntity` (package-private, `@Getter @Setter @NoArgsConstructor`)
- `OrganisationRepository` (package-private)
- `JpaOrganisationStore implements OrganisationStore` (`@Repository`, `@Transactional` per method)
- `OrganisationDbMapper` (`@UtilityClass`, package-private)

---

### Persistence — `team` feature

Package: `abe.fvjc.tournament.team.persistence`

- `TeamEntity` (package-private)
- `TeamRepository` (package-private) — needs `findAllByTournamentId`, `countByOrganisationId`
- `JpaTeamStore implements TeamStore` (`@Repository`, `@Transactional` per method)
- `TeamDbMapper` (`@UtilityClass`, package-private)

---

### API — `team` feature

Package: `abe.fvjc.tournament.team.api`

**Controller:** `TeamController` at `/api/tournaments/{tournamentId}/teams`

| Method | Endpoint | Handler | Status |
|---|---|---|---|
| POST | `/` | `register` | 201 |
| GET | `/` | `getAll` | 200 |
| PUT | `/{teamId}` | `update` | 200 |
| DELETE | `/{teamId}` | `delete` | 204 |
| PATCH | `/{teamId}/paid` | `updatePaid` | 200 |

**DTOs:**

`TeamDto`: `id`, `name`, `paid`, `organisationId`, `responsibleFirstName`, `responsibleLastName`

`TeamRegisterRequestDto`: `@NotBlank name`, `@NotBlank responsibleFirstName`, `@NotBlank responsibleLastName`, `@NotNull count`, `@NotNull paid` (structural validation only — domain validator handles business rules)

`TeamUpdateRequestDto`: `@NotBlank name`, `@NotBlank responsibleFirstName`, `@NotBlank responsibleLastName`, `@NotNull paid`

`TeamPaidRequestDto`: `@NotNull paid`

**`TeamApiMapper`** (`@UtilityClass`): `toTeamDto`, `toTeamRegisterRequest`, `toTeamUpdateRequest`, `toTeamPaidRequest`

---

### Database (Liquibase)

Two new changelogs added to `master.xml`:

**`organisations` table:**
- `id` TEXT PK
- `responsible_first_name` TEXT NOT NULL
- `responsible_last_name` TEXT NOT NULL
- `tournament_id` TEXT NOT NULL

**`teams` table:**
- `id` TEXT PK
- `name` TEXT NOT NULL
- `paid` BOOLEAN NOT NULL DEFAULT FALSE
- `organisation_id` TEXT NOT NULL (FK → organisations.id)
- `tournament_id` TEXT NOT NULL

---

### Tests

`TeamServiceTest` with:
- `@Mock OrganisationStore organisationStore`
- `@Mock TeamStore teamStore`
- `@Mock TournamentStore tournamentStore`
- `@InjectMocks TeamService teamService`

`TeamFakes` (`@UtilityClass`) + `IdGenerator` in `src/test/java/.../team/domain/`.

Test cases:
- `registerTeams` — happy path (count=1, count>1), not DRAFT → 409, validation failure → 400
- `findAllByTournamentId` — returns list
- `updateTeam` — happy path, not DRAFT → 409, team not found → 404
- `deleteTeam` — happy path (org deleted when last team), org kept when other teams remain, not DRAFT → 409
- `markPaid` — happy path, team not found → 404

---

## Frontend

### New route

`tournament.routes.ts` gets a second child route:
```typescript
{ path: ':id', component: TournamentDetailPage }
```

`TournamentState` and `TeamState` both provided at the route group level.

---

### Team feature files

**`api/team/`:**
- `team.api.dto.ts` — mirrors backend DTOs exactly (`responsibleFirstName/LastName` flat on `TeamDto`)
- `team.api.service.ts` — `getAll$(tournamentId)`, `register$(tournamentId, request)`, `update$(tournamentId, teamId, request)`, `delete$(tournamentId, teamId)`, `markPaid$(tournamentId, teamId, paid)`
- `team.api.mapper.ts` — `toDomain(dto): Team`

**`domain/team/`:**
- `team.model.ts` — `Team` interface: `id`, `name`, `paid`, `organisationId`, `responsibleFirstName`, `responsibleLastName`
- `team.actions.ts` — `LoadTeams`, `RegisterTeams`, `UpdateTeam`, `DeleteTeam`, `MarkTeamPaid`
- `team.state.ts` — `ITeamState { teams: Team[] }`, actions handled, selectors: `getTeams`, `getTeamsGroupedByOrganisation`
- `team.domain.service.ts` — static `groupByOrganisation(teams: Team[]): { organisationId: string; responsibleName: string; teams: Team[] }[]`

**`modules/tournament.routes.ts`** — provides `[TournamentState, TeamState]`

---

### Display components

**`TournamentDetailPage`** (page):
- `LoadTournamentById` on init
- `LoadTeams` on init
- Selects `TournamentState.getSelected` and `TeamState.getTeamsGroupedByOrganisation`
- Opens `TeamRegisterModal` and `TeamEditModal` via `MatDialog`
- Dispatches `DeleteTeam` and `MarkTeamPaid` on events from child component

**`TeamListComponent`** (component, `@Input() groups`, `@Output()` for edit/delete/paid):
- Displays teams grouped: organisation header (responsible name), then team rows
- Each row: team name, paid badge/toggle, edit icon, delete icon

**`TeamRegisterModal`** (modal/page-like dialog):
- Fields: base name, responsible first name, responsible last name, count (number input, min 1)
- Dynamic paid checkboxes: renders N checkboxes as count changes
- On submit: dispatches `RegisterTeams`

**`TeamEditModal`** (modal):
- Pre-filled with team data
- Fields: name, responsible first name, responsible last name, paid
- On submit: dispatches `UpdateTeam`

---

## Error handling

| Scenario | Backend | Frontend |
|---|---|---|
| Validation failure | 400 `{ errors: [...] }` | Show field errors in form |
| Not found | 404 | Navigate back or show error |
| Tournament not DRAFT | 409 `{ error: "..." }` | Show snackbar/toast message |

---

## Files to create/modify

### Backend (new files)
```
domain/common/problem/ConflictErrorResponse.java
domain/common/problem/ConflictException.java       (shared.exception package)
domain/common/problem/GlobalExceptionHandler.java  (add ConflictException handler)
domain/organisation/OrganisationId.java
domain/organisation/Organisation.java
domain/organisation/OrganisationStore.java
domain/team/TeamId.java
domain/team/Team.java
domain/team/TeamStore.java
domain/team/TeamRegisterRequest.java
domain/team/TeamUpdateRequest.java
domain/team/TeamPaidRequest.java
domain/team/TeamValidator.java
domain/team/TeamService.java
persistence/organisation/OrganisationEntity.java
persistence/organisation/OrganisationRepository.java
persistence/organisation/JpaOrganisationStore.java
persistence/organisation/OrganisationDbMapper.java
persistence/team/TeamEntity.java
persistence/team/TeamRepository.java
persistence/team/JpaTeamStore.java
persistence/team/TeamDbMapper.java
api/team/TeamController.java
api/team/TeamDto.java
api/team/TeamRegisterRequestDto.java
api/team/TeamUpdateRequestDto.java
api/team/TeamPaidRequestDto.java
api/team/TeamApiMapper.java
resources/db/changelog/20260702120000_create_organisations_table.xml
resources/db/changelog/20260702120001_create_teams_table.xml
```

### Backend (test files)
```
test/.../team/domain/IdGenerator.java
test/.../team/domain/TeamFakes.java
test/.../team/domain/TeamServiceTest.java
```

### Frontend (new files)
```
api/team/team.api.dto.ts
api/team/team.api.service.ts
api/team/team.api.mapper.ts
domain/team/team.model.ts
domain/team/team.actions.ts
domain/team/team.state.ts
domain/team/team.domain.service.ts
display/tournament/pages/tournament-detail/tournament-detail.page.ts
display/tournament/pages/tournament-detail/tournament-detail.page.html
display/tournament/pages/tournament-detail/tournament-detail.page.scss
display/tournament/pages/team-register/team-register.modal.ts
display/tournament/pages/team-register/team-register.modal.html
display/tournament/pages/team-register/team-register.modal.scss
display/tournament/pages/team-edit/team-edit.modal.ts
display/tournament/pages/team-edit/team-edit.modal.html
display/tournament/pages/team-edit/team-edit.modal.scss
display/tournament/components/team-list/team-list.component.ts
display/tournament/components/team-list/team-list.component.html
display/tournament/components/team-list/team-list.component.scss
```

### Frontend (modified files)
```
modules/tournament.routes.ts              (add :id route, provide TeamState)
display/tournament/pages/tournament-list/tournament-list.page.ts   (add "Voir" button)
display/tournament/pages/tournament-list/tournament-list.page.html (add "Voir" button)
```
