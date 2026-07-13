# Delete a whole tournament — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user delete an entire tournament (from both the list cards and the detail header) behind a confirmation modal, cascading the delete to all related data.

**Architecture:** Backend — `TournamentService.delete` orchestrates the cascade by calling a new `deleteAllByTournamentId(UUID)` method on each sibling feature service (`BracketService`, `ScheduleService`, `GroupService`, `TeamService`) in dependency order, then deletes the tournament row. The bracket/schedule cleanup logic already living inside their `generate()` methods is extracted into these new methods and reused. Frontend — the delete plumbing (`DeleteTournament` action, state handler, API call) already exists; add a reusable confirmation modal plus two UI triggers.

**Tech Stack:** Java 21, Spring Boot, Lombok, JUnit 5 + Mockito (backend); Angular standalone components, NGXS, Angular Material (frontend).

## Global Constraints

- **Backend package layout:** feature-first — `abe.fvjc.tournament.<feature>.domain` / `.persistence` / `.api` (e.g. `abe.fvjc.tournament.team.domain`). Follow the imports in existing files exactly.
- **`final var`** for all local variables; **all method parameters `final`**.
- **`@Transactional` only on `JpaXxxStore` methods** — never on services (enforced by `ArchitectureTest`). The multi-store cascade is therefore intentionally **not atomic**; this follows the existing `ScheduleService.generate()` precedent. Do not add a service-level transaction.
- **Field order in classes:** stores (alphabetical) → services (alphabetical) → others; no blank line after the class opening brace.
- **Bean field names** = camelCase of the class name (`bracketService`, not `service`).
- **Backend tests:** domain services only. Mockito `@Mock private` + `@InjectMocks private` + `@ExtendWith(MockitoExtension.class)`. JUnit 5 assertions only. No `ArgumentCaptor`. `final var` everywhere. Test naming `methodWhenXxxShouldXxx`. No structural comments.
- **Frontend:** standalone components; `inject()` (no constructor DI); imports via `@app/` alias; modals dumb (return a result), pages own dispatch/navigation.
- **Frontend unit tests:** out of scope (domain-service static methods only per rules) — verify frontend tasks with `npm run build`.
- **Commands:** backend from `backend/` → `./mvnw test -Dtest=<Class>`; full build `./mvnw -q compile`. Frontend from `frontend/` → `npm run build`.
- **Commits:** the user works directly on `main` — no branches, no PRs.

---

## File Structure

**Backend — modify:**
- `backend/src/main/java/abe/fvjc/tournament/team/domain/TeamStore.java` — add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java` — add derived delete
- `backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java` — impl
- `backend/src/main/java/abe/fvjc/tournament/organisation/domain/OrganisationStore.java` — add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationRepository.java` — add derived delete
- `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/JpaOrganisationStore.java` — impl
- `backend/src/main/java/abe/fvjc/tournament/team/domain/TeamService.java` — add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/group/domain/GroupService.java` — add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/schedule/domain/ScheduleService.java` — extract + add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketService.java` — extract + add `deleteAllByTournamentId`
- `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentService.java` — cascade in `delete`

> NOTE: The persistence files live under `.../persistence/<feature>/` but declare package `abe.fvjc.tournament.<feature>.persistence` (see the existing `JpaTeamStore.java`). Use the package declaration from the neighbouring file, not the folder path.

**Backend — modify tests:**
- `backend/src/test/java/abe/fvjc/tournament/team/domain/TeamServiceTest.java`
- `backend/src/test/java/abe/fvjc/tournament/group/domain/GroupServiceTest.java`
- `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java`
- `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java`
- `backend/src/test/java/abe/fvjc/tournament/tournament/domain/TournamentServiceTest.java`

**Frontend — create:**
- `frontend/src/app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal.ts`
- `frontend/src/app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal.html`

**Frontend — modify:**
- `frontend/src/app/display/tournament/pages/tournament-list/tournament-list.page.ts` + `.html`
- `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts` + `.html`

---

## Task 1: Store-level `deleteAllByTournamentId` (team + organisation)

Persistence layer is not unit-tested (per rules) — deliverable is compiling code that the service tasks will mock.

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/team/domain/TeamStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/organisation/domain/OrganisationStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationRepository.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/JpaOrganisationStore.java`

**Interfaces:**
- Produces: `TeamStore.deleteAllByTournamentId(UUID)`, `OrganisationStore.deleteAllByTournamentId(UUID)` — consumed by Tasks 2.

- [ ] **Step 1: Add method to `TeamStore` interface**

In `TeamStore.java`, add below `void deleteById(UUID id);`:

```java
    void deleteAllByTournamentId(UUID tournamentId);
```

- [ ] **Step 2: Add derived query to `TeamRepository`**

In `TeamRepository.java`, add inside the interface body:

```java
    void deleteByTournamentId(UUID tournamentId);
```

- [ ] **Step 3: Implement in `JpaTeamStore`**

In `JpaTeamStore.java`, add after the existing `deleteById` method:

```java
    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        teamRepository.deleteByTournamentId(tournamentId);
    }
```

- [ ] **Step 4: Add method to `OrganisationStore` interface**

In `OrganisationStore.java`, add below `void deleteById(UUID id);`:

```java
    void deleteAllByTournamentId(UUID tournamentId);
```

- [ ] **Step 5: Add derived query to `OrganisationRepository`**

`OrganisationRepository.java` currently has an empty body. Add the import for `UUID` if missing and the method:

```java
package abe.fvjc.tournament.organisation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {
    void deleteByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 6: Implement in `JpaOrganisationStore`**

In `JpaOrganisationStore.java`, add after the existing `deleteById` method:

```java
    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        organisationRepository.deleteByTournamentId(tournamentId);
    }
```

- [ ] **Step 7: Verify it compiles**

Run (from `backend/`): `./mvnw -q compile`
Expected: BUILD SUCCESS, no errors.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/team backend/src/main/java/abe/fvjc/tournament/persistence/team backend/src/main/java/abe/fvjc/tournament/organisation backend/src/main/java/abe/fvjc/tournament/persistence/organisation
git commit -m "BE - Add deleteAllByTournamentId to team and organisation stores"
```

---

## Task 2: `TeamService.deleteAllByTournamentId`

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/team/domain/TeamService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/team/domain/TeamServiceTest.java`

**Interfaces:**
- Consumes: `TeamStore.deleteAllByTournamentId(UUID)`, `OrganisationStore.deleteAllByTournamentId(UUID)` (Task 1).
- Produces: `TeamService.deleteAllByTournamentId(UUID)` — consumed by Task 6.

- [ ] **Step 1: Write the failing test**

`TeamServiceTest` already declares `@Mock private OrganisationStore organisationStore;`, `@Mock private TeamStore teamStore;`, and `@InjectMocks private TeamService teamService;`. Add this test method (and `import java.util.UUID;` if not present):

```java
    @Test
    void deleteAllByTournamentIdShouldDeleteTeamsThenOrganisations() {
        final var tournamentId = UUID.randomUUID();

        teamService.deleteAllByTournamentId(tournamentId);

        verify(teamStore).deleteAllByTournamentId(tournamentId);
        verify(organisationStore).deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run (from `backend/`): `./mvnw test -Dtest=TeamServiceTest#deleteAllByTournamentIdShouldDeleteTeamsThenOrganisations`
Expected: FAIL — compilation error, `deleteAllByTournamentId` not defined on `TeamService`.

- [ ] **Step 3: Implement the method**

In `TeamService.java`, add a public method (place it near `deleteTeam`):

```java
    public void deleteAllByTournamentId(final UUID tournamentId) {
        teamStore.deleteAllByTournamentId(tournamentId);
        organisationStore.deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=TeamServiceTest#deleteAllByTournamentIdShouldDeleteTeamsThenOrganisations`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/team/domain/TeamService.java backend/src/test/java/abe/fvjc/tournament/team/domain/TeamServiceTest.java
git commit -m "BE - Add TeamService.deleteAllByTournamentId"
```

---

## Task 3: `GroupService.deleteAllByTournamentId`

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/group/domain/GroupService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/group/domain/GroupServiceTest.java`

**Interfaces:**
- Consumes: `GroupStore.deleteAllByTournamentId(UUID)` (already exists).
- Produces: `GroupService.deleteAllByTournamentId(UUID)` — consumed by Task 6.

- [ ] **Step 1: Write the failing test**

`GroupServiceTest` already declares `@Mock private GroupStore groupStore;` and `@InjectMocks private GroupService groupService;`, and imports `java.util.UUID`. Add:

```java
    @Test
    void deleteAllByTournamentIdShouldDeleteGroups() {
        final var tournamentId = UUID.randomUUID();

        groupService.deleteAllByTournamentId(tournamentId);

        verify(groupStore).deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=GroupServiceTest#deleteAllByTournamentIdShouldDeleteGroups`
Expected: FAIL — `deleteAllByTournamentId` not defined on `GroupService`.

- [ ] **Step 3: Implement the method**

In `GroupService.java`, add a public method:

```java
    public void deleteAllByTournamentId(final UUID tournamentId) {
        groupStore.deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=GroupServiceTest#deleteAllByTournamentIdShouldDeleteGroups`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/group/domain/GroupService.java backend/src/test/java/abe/fvjc/tournament/group/domain/GroupServiceTest.java
git commit -m "BE - Add GroupService.deleteAllByTournamentId"
```

---

## Task 4: `ScheduleService.deleteAllByTournamentId` (extract cleanup)

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/domain/ScheduleService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java`

**Interfaces:**
- Consumes: `RoundStore.findAllByTournamentId(UUID)`, `MatchStore.deleteAllByRoundIds(List<UUID>)`, `RoundStore.deleteAllByTournamentId(UUID)` (all exist).
- Produces: `ScheduleService.deleteAllByTournamentId(UUID)` — consumed by Task 6.

- [ ] **Step 1: Write the failing tests**

`ScheduleServiceTest` mocks `roundStore`, `matchStore`, and injects `scheduleService`. Add these imports if missing: `import static org.mockito.ArgumentMatchers.anyList;` and `import abe.fvjc.tournament.tournament.domain.TournamentId;`. Add:

```java
    @Test
    void deleteAllByTournamentIdWhenRoundsExistShouldDeleteMatchesThenRounds() {
        final var tournamentId = UUID.randomUUID();
        final var round = ScheduleFakes.buildRound(TournamentId.of(tournamentId));

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(round));

        scheduleService.deleteAllByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore).deleteAllByRoundIds(List.of(round.getId().value()));
        verify(roundStore).deleteAllByTournamentId(tournamentId);
    }

    @Test
    void deleteAllByTournamentIdWhenNoRoundsShouldNotDeleteAnything() {
        final var tournamentId = UUID.randomUUID();

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        scheduleService.deleteAllByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore, never()).deleteAllByRoundIds(anyList());
        verify(roundStore, never()).deleteAllByTournamentId(tournamentId);
    }
```

> If `List`, `UUID`, `when`, `verify`, `never` are not already imported, add them — check the existing imports in the file first (`ScheduleServiceTest` already uses Mockito and `java.util.List`/`UUID`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=ScheduleServiceTest#deleteAllByTournamentIdWhenRoundsExistShouldDeleteMatchesThenRounds+deleteAllByTournamentIdWhenNoRoundsShouldNotDeleteAnything`
Expected: FAIL — `deleteAllByTournamentId` not defined on `ScheduleService`.

- [ ] **Step 3: Implement the method**

In `ScheduleService.java`, add a public method:

```java
    public void deleteAllByTournamentId(final UUID tournamentId) {
        final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
        if (existingRounds.isEmpty()) {
            return;
        }
        final var existingRoundIds = existingRounds.stream()
                .map(r -> r.getId().value())
                .toList();
        matchStore.deleteAllByRoundIds(existingRoundIds);
        roundStore.deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 4: Reuse the method inside `generate()`**

In `generate()`, replace the existing cleanup block:

```java
            matchStore.deleteAllByRoundIds(existingRoundIds);
            roundStore.deleteAllByTournamentId(tournamentId);
```

with a call to the new method (the `existsResultByRoundIds` guard directly above it stays unchanged):

```java
            deleteAllByTournamentId(tournamentId);
```

- [ ] **Step 5: Run the full `ScheduleServiceTest` to verify nothing regressed**

Run: `./mvnw test -Dtest=ScheduleServiceTest`
Expected: PASS — the two new tests plus all existing `generate` tests.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/schedule/domain/ScheduleService.java backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java
git commit -m "BE - Add ScheduleService.deleteAllByTournamentId and reuse in generate"
```

---

## Task 5: `BracketService.deleteAllByTournamentId` (extract cleanup)

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java`

**Interfaces:**
- Consumes: `BracketRoundStore.findAllByTournamentId(UUID)`, `BracketMatchStore.deleteAllByRoundId(UUID)`, `BracketRoundStore.deleteAllByTournamentId(UUID)` (all exist).
- Produces: `BracketService.deleteAllByTournamentId(UUID)` — consumed by Task 6.

- [ ] **Step 1: Write the failing test**

`BracketServiceTest` mocks `bracketRoundStore` and `bracketMatchStore`, and injects `bracketService`. Add `import abe.fvjc.tournament.bracket.domain.BracketRoundId;` if needed (it is in the same package, so no import is required — `BracketRoundId` is directly usable). Add:

```java
    @Test
    void deleteAllByTournamentIdShouldDeleteBracketMatchesThenRounds() {
        final var tournamentId = UUID.randomUUID();
        final var round = BracketRound.builder()
                .id(BracketRoundId.of(UUID.randomUUID()))
                .build();

        when(bracketRoundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(round));

        bracketService.deleteAllByTournamentId(tournamentId);

        verify(bracketRoundStore).findAllByTournamentId(tournamentId);
        verify(bracketMatchStore).deleteAllByRoundId(round.getId().value());
        verify(bracketRoundStore).deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=BracketServiceTest#deleteAllByTournamentIdShouldDeleteBracketMatchesThenRounds`
Expected: FAIL — `deleteAllByTournamentId` not defined on `BracketService`.

- [ ] **Step 3: Implement the method**

In `BracketService.java`, add a public method:

```java
    public void deleteAllByTournamentId(final UUID tournamentId) {
        final var existingRounds = bracketRoundStore.findAllByTournamentId(tournamentId);
        existingRounds.forEach(r -> bracketMatchStore.deleteAllByRoundId(r.getId().value()));
        bracketRoundStore.deleteAllByTournamentId(tournamentId);
    }
```

- [ ] **Step 4: Reuse the method inside `generate()`**

In `generate()`, replace the existing cleanup block:

```java
        final var existingRounds = bracketRoundStore.findAllByTournamentId(tournamentId);
        existingRounds.forEach(r -> bracketMatchStore.deleteAllByRoundId(r.getId().value()));
        bracketRoundStore.deleteAllByTournamentId(tournamentId);
```

with:

```java
        deleteAllByTournamentId(tournamentId);
```

- [ ] **Step 5: Run the full `BracketServiceTest` to verify nothing regressed**

Run: `./mvnw test -Dtest=BracketServiceTest`
Expected: PASS — the new test plus all existing `generate` tests.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketService.java backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java
git commit -m "BE - Add BracketService.deleteAllByTournamentId and reuse in generate"
```

---

## Task 6: `TournamentService.delete` cascade orchestration

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/tournament/domain/TournamentServiceTest.java`

**Interfaces:**
- Consumes: `BracketService.deleteAllByTournamentId`, `ScheduleService.deleteAllByTournamentId`, `GroupService.deleteAllByTournamentId`, `TeamService.deleteAllByTournamentId` (Tasks 2–5).

- [ ] **Step 1: Write the failing tests**

In `TournamentServiceTest`, add four mocks. The class currently has `@Mock private RoundStore roundStore;` and `@Mock private TournamentStore tournamentStore;`. Add (with matching imports for the service classes):

```java
    @Mock
    private BracketService bracketService;

    @Mock
    private GroupService groupService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private TeamService teamService;
```

Imports to add:

```java
import abe.fvjc.tournament.bracket.domain.BracketService;
import abe.fvjc.tournament.group.domain.GroupService;
import abe.fvjc.tournament.schedule.domain.ScheduleService;
import abe.fvjc.tournament.team.domain.TeamService;
```

Add the two test methods:

```java
    @Test
    void deleteWhenNotFoundShouldThrowNotFoundException() {
        final var id = tournamentId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentService.delete(id));

        verify(tournamentStore).findById(id);

        assertEquals("Tournament not found with id: " + id, exception.getMessage());
    }

    @Test
    void deleteWhenExistsShouldCascadeAndDeleteTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.of(tournament));

        tournamentService.delete(id);

        verify(bracketService).deleteAllByTournamentId(id);
        verify(scheduleService).deleteAllByTournamentId(id);
        verify(groupService).deleteAllByTournamentId(id);
        verify(teamService).deleteAllByTournamentId(id);
        verify(tournamentStore).deleteById(id);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./mvnw test -Dtest=TournamentServiceTest#deleteWhenExistsShouldCascadeAndDeleteTournament+deleteWhenNotFoundShouldThrowNotFoundException`
Expected: FAIL — compilation error: `TournamentService` has no `bracketService`/`groupService`/`scheduleService`/`teamService` fields for `@InjectMocks` to inject, and the cascade verifies fail.

- [ ] **Step 3: Add the service dependencies and cascade logic**

In `TournamentService.java`:

1. Add imports:

```java
import abe.fvjc.tournament.bracket.domain.BracketService;
import abe.fvjc.tournament.group.domain.GroupService;
import abe.fvjc.tournament.schedule.domain.ScheduleService;
import abe.fvjc.tournament.team.domain.TeamService;
```

2. Add fields — respecting field order (stores alphabetical, then services alphabetical). The final field block becomes:

```java
    private final RoundStore roundStore;
    private final TournamentStore tournamentStore;
    private final BracketService bracketService;
    private final GroupService groupService;
    private final ScheduleService scheduleService;
    private final TeamService teamService;
```

3. Replace the existing `delete` method:

```java
    public void delete(final UUID id) {
        findById(id);
        tournamentStore.deleteById(id);
    }
```

with:

```java
    public void delete(final UUID id) {
        findById(id);
        bracketService.deleteAllByTournamentId(id);
        scheduleService.deleteAllByTournamentId(id);
        groupService.deleteAllByTournamentId(id);
        teamService.deleteAllByTournamentId(id);
        tournamentStore.deleteById(id);
    }
```

- [ ] **Step 4: Run `TournamentServiceTest` to verify it passes**

Run: `./mvnw test -Dtest=TournamentServiceTest`
Expected: PASS — new delete tests plus all existing create/find/start tests.

- [ ] **Step 5: Run the whole backend test suite + architecture test**

Run (from `backend/`): `./mvnw test`
Expected: BUILD SUCCESS — all tests pass, including `ArchitectureTest` (no cyclic-dependency or layer violation, `@Transactional` still off services).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentService.java backend/src/test/java/abe/fvjc/tournament/tournament/domain/TournamentServiceTest.java
git commit -m "BE - Cascade delete tournament related data on delete"
```

---

## Task 7: Confirmation modal component

**Files:**
- Create: `frontend/src/app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal.ts`
- Create: `frontend/src/app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal.html`

**Interfaces:**
- Produces: `TournamentDeleteConfirmModal` (dumb dialog, closes with `true`/`false`) and `TournamentDeleteConfirmData` (`{ name: string }`) — consumed by Tasks 8 & 9.

- [ ] **Step 1: Create the modal component**

Create `tournament-delete-confirm.modal.ts`:

```typescript
import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface TournamentDeleteConfirmData {
  name: string;
}

@Component({
  selector: 'app-tournament-delete-confirm-modal',
  templateUrl: './tournament-delete-confirm.modal.html',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
})
export class TournamentDeleteConfirmModal {

  private readonly dialogRef = inject(MatDialogRef<TournamentDeleteConfirmModal>);

  readonly data = inject<TournamentDeleteConfirmData>(MAT_DIALOG_DATA);

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
```

- [ ] **Step 2: Create the modal template**

Create `tournament-delete-confirm.modal.html`:

```html
<h2 mat-dialog-title>Supprimer le tournoi</h2>

<mat-dialog-content>
  <p>Êtes-vous sûr de vouloir supprimer le tournoi <strong>{{ data.name }}</strong> ?</p>
  <p>Toutes les données associées (équipes, groupes, calendrier et tableau final) seront définitivement supprimées.</p>
  <p><strong>Cette action est irréversible.</strong></p>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
  <button mat-raised-button color="warn" (click)="confirm()">
    <mat-icon>delete</mat-icon>
    Supprimer
  </button>
</mat-dialog-actions>
```

- [ ] **Step 3: Verify the build compiles**

Run (from `frontend/`): `npm run build`
Expected: build succeeds (the component is not yet referenced, but must compile).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/display/tournament/components/tournament-delete-confirm
git commit -m "FE - Add tournament delete confirmation modal"
```

---

## Task 8: Delete from the tournament list cards

**Files:**
- Modify: `frontend/src/app/display/tournament/pages/tournament-list/tournament-list.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-list/tournament-list.page.html`

**Interfaces:**
- Consumes: `TournamentDeleteConfirmModal`, `TournamentDeleteConfirmData` (Task 7); `DeleteTournament` action (already exists in `tournament.actions.ts`).

- [ ] **Step 1: Wire the delete handler in the page**

In `tournament-list.page.ts`, add imports:

```typescript
import { DeleteTournament, LoadTournaments } from '@app/domain/tournament/tournament.actions';
import { TournamentDeleteConfirmModal } from '@app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal';
```

(The existing `LoadTournaments` import line is replaced by the combined import above.)

Add this method to the `TournamentListPage` class (after `openCreateModal`):

```typescript
  openDeleteModal(tournament: Tournament, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.dialog
      .open(TournamentDeleteConfirmModal, { data: { name: tournament.name } })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.store.dispatch(new DeleteTournament(tournament.id));
        }
      });
  }
```

- [ ] **Step 2: Add the delete button to each card**

In `tournament-list.page.html`, replace the `<mat-card-actions>` block:

```html
          <mat-card-actions>
            <mat-chip-set>
              <mat-chip [class.chip-active]="tournament.status !== 'DRAFT'">
                {{ tournament.status === 'DRAFT' ? 'Brouillon' : 'En cours' }}
              </mat-chip>
            </mat-chip-set>
          </mat-card-actions>
```

with (adds a right-aligned delete icon button whose click is stopped from triggering the card `routerLink`):

```html
          <mat-card-actions class="card-actions">
            <mat-chip-set>
              <mat-chip [class.chip-active]="tournament.status !== 'DRAFT'">
                {{ tournament.status === 'DRAFT' ? 'Brouillon' : 'En cours' }}
              </mat-chip>
            </mat-chip-set>
            <button
              mat-icon-button
              color="warn"
              aria-label="Supprimer le tournoi"
              (click)="openDeleteModal(tournament, $event)"
            >
              <mat-icon>delete</mat-icon>
            </button>
          </mat-card-actions>
```

- [ ] **Step 3: Add a small layout rule for the actions row**

In `tournament-list.page.scss`, append:

```scss
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
```

- [ ] **Step 4: Verify the build compiles**

Run (from `frontend/`): `npm run build`
Expected: build succeeds. (`MatButtonModule` and `MatIconModule` are already imported in the list page.)

- [ ] **Step 5: Manually verify behavior**

Run (from `frontend/`): `npm start`, open the tournament list, click the delete (trash) icon on a card. Confirm the modal opens (card does NOT navigate), the tournament name shows, cancelling keeps the card, confirming removes it from the list.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/display/tournament/pages/tournament-list
git commit -m "FE - Add delete action to tournament list cards"
```

---

## Task 9: Delete from the tournament detail header

**Files:**
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html`

**Interfaces:**
- Consumes: `TournamentDeleteConfirmModal` (Task 7); `DeleteTournament` action; Angular `Router`.

- [ ] **Step 1: Wire the delete handler + navigation in the page**

In `tournament-detail.page.ts`:

1. Add `Router` to the router import and inject it:

```typescript
import { ActivatedRoute, Router } from '@angular/router';
```

```typescript
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
```

2. Add imports for the action and the modal:

```typescript
import { DeleteTournament, LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { TournamentDeleteConfirmModal } from '@app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal';
```

(The existing `LoadTournamentById` import line is replaced by the combined import above.)

3. Add this method to the class:

```typescript
  openDeleteModal(tournament: Tournament): void {
    this.dialog
      .open(TournamentDeleteConfirmModal, { data: { name: tournament.name } })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.store
            .dispatch(new DeleteTournament(this.tournamentId))
            .subscribe(() => this.router.navigate(['..'], { relativeTo: this.route }));
        }
      });
  }
```

- [ ] **Step 2: Add the delete button to the header slot**

In `tournament-detail.page.html`, update the `<app-tournament-header>` block to project a "Supprimer" button (it renders in the header actions slot alongside the existing register button):

```html
  <app-tournament-header [tournament]="tournament">
    @if (isDraft(tournament)) {
      <button mat-raised-button color="primary" (click)="openRegisterModal()">
        <mat-icon>group_add</mat-icon>
        Inscrire des équipes
      </button>
    }
    <button mat-stroked-button color="warn" (click)="openDeleteModal(tournament)">
      <mat-icon>delete</mat-icon>
      Supprimer
    </button>
  </app-tournament-header>
```

- [ ] **Step 3: Verify the build compiles**

Run (from `frontend/`): `npm run build`
Expected: build succeeds. (`MatButtonModule` and `MatIconModule` are already imported in the detail page.)

- [ ] **Step 4: Manually verify behavior**

Run (from `frontend/`): `npm start`, open a tournament detail page, click "Supprimer". Confirm the modal shows the tournament name; cancelling stays on the page; confirming deletes and navigates back to the tournament list (where the tournament is gone).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/display/tournament/pages/tournament-detail
git commit -m "FE - Add delete action to tournament detail header"
```

---

## Self-Review

- **Spec coverage:** cascade orchestration (Task 6) + per-service `deleteAllByTournamentId` (Tasks 2–5) + new store methods (Task 1) cover the backend cascade section; confirm modal (Task 7), list-card trigger (Task 8), detail-header trigger (Task 9) cover the frontend section. Non-atomic cascade limitation is honoured (no `@Transactional` on services). Testing section: `TournamentServiceTest` + per-service delete tests present; no frontend unit tests (out of scope). All spec sections map to a task.
- **Type consistency:** `deleteAllByTournamentId(UUID)` used uniformly across all stores and services; `TournamentDeleteConfirmData { name: string }` defined in Task 7 and consumed identically in Tasks 8 & 9; `DeleteTournament(id: string)` matches the existing action constructor.
- **No placeholders:** every code and test step shows the full content; commands include expected output.
