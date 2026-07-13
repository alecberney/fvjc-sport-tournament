# Delete a whole tournament — Design

**Date:** 2026-07-13
**Status:** Approved

## Goal

Let a user delete an entire tournament, with a confirmation modal. Deleting a
tournament must also remove all of its related data (cascade), and the delete
action must be reachable from **both** the tournament list cards and the
tournament detail header.

## Current state

The delete plumbing already exists end-to-end:

- **Backend:** `DELETE /api/tournaments/{id}` → `TournamentController.delete` →
  `TournamentService.delete(id)` → `tournamentStore.deleteById(id)`.
- **Frontend:** `DeleteTournament` action, `TournamentState.deleteTournament`
  handler (removes the tournament from the in-memory list), and
  `TournamentApiService.delete$(id)`.

Two gaps:

1. **No cascade.** `TournamentService.delete` removes only the tournament row.
   Teams, organisations, groups, schedule rounds/matches, and bracket
   rounds/matches all reference `tournamentId` with no DB foreign-key cascade,
   so they are left orphaned.
2. **No UI trigger and no confirmation modal.**

## Backend — cascade on delete

### Orchestration (Approach A: delegate to sibling services)

`TournamentService.delete(id)` deletes all related data before removing the
tournament, in dependency order:

```
bracket (matches → rounds)
  → schedule (matches → rounds)
  → groups
  → teams (+ organisations)
  → tournament
```

`TournamentService` gains dependencies on `BracketService`, `ScheduleService`,
`GroupService`, and `TeamService`. Each of those services exposes a new
`deleteAllByTournamentId(UUID tournamentId)` method. This reuses the
matches-before-rounds cleanup ordering that already lives inside
`ScheduleService.generate()` and `BracketService.generate()` — that inline
cleanup is extracted into the new methods and then called from `generate()`.

No dependency cycle is introduced: `BracketService`, `ScheduleService`,
`GroupService`, and `TeamService` depend on `TournamentStore`, not on
`TournamentService`.

Deletion runs in **any** tournament status (draft or in-progress) — there is no
status guard. The new `deleteAllByTournamentId` service methods perform pure
deletion with no `assertDraft` check.

### New service methods

- `BracketService.deleteAllByTournamentId(id)` — for each bracket round,
  `bracketMatchStore.deleteAllByRoundId(...)`, then
  `bracketRoundStore.deleteAllByTournamentId(id)`. (Extracted from
  `generate()`.)
- `ScheduleService.deleteAllByTournamentId(id)` — collect round ids via
  `roundStore.findAllByTournamentId(id)`, `matchStore.deleteAllByRoundIds(...)`,
  then `roundStore.deleteAllByTournamentId(id)`. (Extracted from `generate()`.)
- `GroupService.deleteAllByTournamentId(id)` — `groupStore.deleteAllByTournamentId(id)`.
- `TeamService.deleteAllByTournamentId(id)` — `teamStore.deleteAllByTournamentId(id)`,
  then `organisationStore.deleteAllByTournamentId(id)`.

### New store methods

Every other store already has the needed capability. Two must be added:

- `TeamStore.deleteAllByTournamentId(UUID)` + `TeamRepository.deleteByTournamentId`
  + `JpaTeamStore` impl (`@Transactional`).
- `OrganisationStore.deleteAllByTournamentId(UUID)` +
  `OrganisationRepository.deleteByTournamentId` + `JpaOrganisationStore` impl
  (`@Transactional`). `OrganisationEntity` already carries a `tournamentId`
  column.

Existing and reused as-is: `GroupStore.deleteAllByTournamentId`,
`RoundStore.deleteAllByTournamentId`, `MatchStore.deleteAllByRoundIds`,
`BracketRoundStore.deleteAllByTournamentId`,
`BracketMatchStore.deleteAllByRoundId`.

### Accepted limitation: non-atomic cascade

The cascade spans multiple stores. Per the project rule that `@Transactional`
lives only on `JpaXxxStore` methods (never on services), the whole cascade is
**not** a single transaction — a mid-cascade failure could leave partial data.
This follows the existing precedent set by `ScheduleService.generate()`, which
already performs multi-store mutations without a service-level transaction.

## Frontend

### Confirmation modal

New dumb component `TournamentDeleteConfirmModal` at
`display/tournament/components/tournament-delete-confirm/`, following the
existing `tournament-start-confirm.modal` pattern:

- Standalone; imports `MatDialogModule`, `MatButtonModule`, `MatIconModule`.
- Receives the tournament name via `MAT_DIALOG_DATA` (`{ name: string }`).
- Displays the tournament name, states the action is irreversible, and warns
  that all teams, groups, schedule, and brackets will be removed.
- `confirm()` closes with `true`; `cancel()` closes with `false`.
- The modal does **not** dispatch or navigate — the opening page owns that.

### List cards (`tournament-list.page`)

- Add a delete icon button to each `mat-card`.
- Its `(click)` calls `$event.stopPropagation()` first so it does not trigger
  the card's `routerLink` navigation.
- Opens `TournamentDeleteConfirmModal` with the card's tournament name; on a
  `true` result, dispatches `new DeleteTournament(tournament.id)`.
- Stays on the list — the state handler removes the card in place.

### Detail header (`tournament-detail.page`)

- `TournamentHeaderComponent` stays unchanged — it already exposes an
  `<ng-content>` action slot.
- The detail page projects a "Supprimer" button into that slot.
- On click, opens `TournamentDeleteConfirmModal` with the selected tournament's
  name; on a `true` result, dispatches `new DeleteTournament(tournamentId)` and,
  once the dispatch completes, navigates to the tournament list.
- The detail page injects `Router` for that navigation.

## Testing

Per the backend test rules (domain services only):

- `TournamentServiceTest` — `deleteWhenExistsShouldCascadeAndDeleteTournament`
  verifies each sibling service's `deleteAllByTournamentId` is invoked and the
  tournament is deleted; `deleteWhenNotFoundShouldThrowNotFoundException`.
- `deleteAllByTournamentId` tests for `TeamService`, `GroupService`,
  `ScheduleService`, `BracketService` verifying the correct store calls (and
  matches-before-rounds ordering for schedule/bracket).

No frontend unit tests: the change is confined to pages/components/modal, which
are out of scope per the frontend test rules (domain-service static methods
only). `TournamentDomainService` gains no new transformation logic.

## Out of scope

- DB-level foreign-key cascades / schema changes.
- A service-level transaction wrapping the cascade (see accepted limitation).
- Soft delete / archive / undo.
