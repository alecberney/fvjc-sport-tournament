# IDEA-02 — Code & architecture improvements

**Status:** in progress — F3, F6, F8, F9, B1, B2 (schedule), B3 and B4 done
**Scope:** frontend (main topic: view models) + backend
**Type:** technical debt / refactoring — no functional change for the user

---

## Summary

| # | Improvement | Layer | Effort | Value | Status |
|---|---|---|---|---|---|
| F1 | **View models** — one model per screen with every property ready to display | Frontend | M | High | To do |
| F2 | `TournamentDomainService` — stop duplicating `isDraft()` | Frontend | S | Medium | To do |
| F3 | Extract shared components (ranking table, score form) | Frontend | S | Medium | ✅ Done |
| F4 | Flatten nested `async` pipes into a single `vm$` | Frontend | S | Medium | To do |
| F5 | `OnPush` change detection everywhere | Frontend | S | Medium | To do |
| F6 | `takeUntilDestroyed()` instead of `destroy$` + `takeUntil` | Frontend | S | Low | ✅ Done |
| F7 | Route param via component input binding | Frontend | S | Low | To do |
| F8 | Score entry as a typed reactive form with validation | Frontend | S | Medium | ✅ Done |
| F9 | Data-driven `tournament-nav` | Frontend | XS | Low | ✅ Done |
| B1 | Move HTTP error handling out of `domain` | Backend | S | High (architecture) | ✅ Done |
| B2 | Extract the round-robin algorithm from `ScheduleService` | Backend | M | Medium | 🟡 Partial (bracket left) |
| B3 | Remove the N+1 queries in `GroupRankingService` | Backend | S | Medium | ✅ Done |
| B4 | Model the end of a tournament (`ENDED` status) | Both | M | Medium | ✅ Done |

### Done

- **F3** — `components/ranking-table/` (used by results + rankings pages) and
  `components/score-form/` (used by results + bracket pages).
- **F6** — the seven modals use `takeUntilDestroyed(this.destroyRef)`; no `destroy$` left.
- **F8** — `score-form` is a typed `FormGroup` with `required`, `min(0)`, `max(500)`.
- **F9** — `tournament-nav` renders a `tabs` array with `@for`.
- **B1** — `GlobalExceptionHandler`, `ValidationErrorResponse`, `ConflictErrorResponse`
  moved to `api.common.problem`; `ArchitectureTest.domainShouldNotDependOnSpringWeb`
  forbids `org.springframework.web..` / `org.springframework.http..` in `domain`.
- **B2 (schedule part)** — pairing algorithm extracted into `RoundRobinService` (+ `MatchPair`,
  `RoundRobinServiceTest`); `ScheduleService` dropped from 310 to ~200 lines.
  `BracketService` (seeding) is not extracted yet.
- **B3** — tournament-wide rankings load teams by `tournamentId` and matches with
  `matchStore.findAllByGroupIds(...)` once, then group in memory.
- **B4** — `ENDED` status (named `ENDED` rather than `FINISHED`), `POST /api/tournaments/{id}/end`
  allowed only when `IN_PROGRESS` and the final has a score (`TournamentService.end`).
  Frontend: `EndTournament` action, `BracketDomainService.findFinal` / `isFinalPlayed`
  (with spec), end confirmation modal on the bracket page, results locked once ended.
  Bracket matches now expose `nextMatchId` / `loserNextMatchId`. Podium display not done.

---

## Frontend

### F1 — View models: display properties ready "as is"

#### Problem

Templates take **domain models** and compute what they show on the fly. Display logic is
spread over the templates and the page classes, it is duplicated, and it cannot be tested
(the test rules only cover domain service static methods).

Examples from today's code:

| Where | Logic in the template / page |
|---|---|
| `tournament-bracket.page.html` | `match.team1?.name ?? 'À déterminer'`, `T{{ match.field }}`, `@if (match.result) { s1 - s2 } @else { — }`, `[class.playable]="match.team1 && match.team2"` |
| `tournament-results.page.html` | `{{ match.team1.name }} vs {{ match.team2.name }}`, `[class.has-result]="match.result !== null"`, `round.startTime \| date:'HH:mm'` |
| `tournament-results.page.ts` | `getSelectedMatch()` loops over every round on **each change detection cycle**; `getRankingForMatch()` looks up the ranking map |
| `tournament-rankings.page.ts` | `getRankingsToDisplay(groups, rankings)` filters in the page |
| 5 pages | `isDraft(tournament)` re-implemented in each page |

#### Proposed solution

Introduce a **view model** (`XxxView`) per screen or per displayed item: an interface whose
properties are exactly what the template renders — already formatted strings, booleans
for every condition, CSS state flags. The template only binds; it never computes.

```typescript
// domain/bracket/bracket.view.ts
export interface BracketMatchView {
  id: string;
  fieldLabel: string;      // 'T3'
  team1Label: string;      // 'Les Aigles' | 'À déterminer'
  team2Label: string;
  scoreLabel: string;      // '3 - 1' | '—'
  isPlayable: boolean;     // both teams known
  isPlayed: boolean;
}

export interface BracketRoundView {
  id: string;
  name: string;
  startTimeLabel: string;  // '14:30'
  matches: BracketMatchView[];
}
```

The mapping is a **static, pure** method of the domain service — therefore unit-testable
with plain Jest under the existing test rules:

```typescript
// domain/bracket/bracket.domain.service.ts
static toMatchView(match: BracketMatch): BracketMatchView {
  return {
    id: match.id,
    fieldLabel: `T${match.field}`,
    team1Label: match.team1?.name ?? 'À déterminer',
    team2Label: match.team2?.name ?? 'À déterminer',
    scoreLabel: match.result ? `${match.result.score1} - ${match.result.score2}` : '—',
    isPlayable: !!match.team1 && !!match.team2,
    isPlayed: !!match.result,
  };
}
```

The selector exposes the view; the template becomes a pure binding:

```typescript
@Selector()
static getRoundViews(state: IBracketState): BracketRoundView[] {
  return state.rounds.map(BracketDomainService.toRoundView);
}
```

```html
<tr [class.playable]="match.isPlayable" (click)="selectMatch(match)">
  <td>{{ match.fieldLabel }}</td>
  <td>{{ match.team1Label }}</td>
  <td class="vs">vs</td>
  <td>{{ match.team2Label }}</td>
  <td>{{ match.scoreLabel }}</td>
</tr>
```

Views combining several states (e.g. the results page: schedule + selected match + group
ranking) use a selector built with `createSelector` / `createPropertySelectors`, so the
selected match lookup is memoised instead of being re-run on every change detection.

#### Benefits

- Templates contain only bindings → easier to read, impossible to get logic wrong there.
- All display rules (labels, placeholders, enabled/disabled, CSS states) are **unit-tested**.
- Removes the per-cycle method calls from templates (performance).
- A single place to change a label or a format (`'À déterminer'`, `'T3'`, `'HH:mm'`).
- Naturally solves the "compound `@if` condition" rule: the condition is a view boolean.

#### Rules to amend

- `frontend-rules.md` — *"`@Selector()` returns domain models — never DTOs"* becomes
  *"returns domain models or view models"*. Add a `*.view.ts` row to the file naming table
  and a "View models" section (interfaces only, built by static `toXxxView` methods).
- Decide where `*.view.ts` lives: `domain/<feature>/` (next to the domain service that
  builds it — recommended, keeps the test rules unchanged) or `display/<feature>/views/`.

#### Migration

Page by page, starting with the screens with the most template logic: **bracket** →
**results** → **rankings** → **schedule** → **detail**.

---

### F2 — `TournamentDomainService`

`isDraft(tournament)` is copy-pasted in `tournament-detail`, `tournament-groups`,
`tournament-schedule`, `tournament-results` pages and `tournament-actions` component;
`isInProgress` in two places. There is no `tournament.domain.service.ts`.
Create it with static `isDraft` / `isInProgress` / `canStart`, with a spec and a
`TournamentFakes`. With F1 these become flags on a `TournamentView` (`isDraft`,
`canGenerateSchedule`, `canStart`…).

### F3 — Extract shared components

- The **10-column ranking table** is duplicated verbatim in `tournament-results.page.html`
  and `tournament-rankings.page.html` → `components/ranking-table/` with
  `@Input() ranking`.
- The **score entry form** (two `mat-form-field` + "Enregistrer") is duplicated in the
  results and bracket pages → `components/score-form/` with `@Input()` team labels /
  initial scores and `@Output() submitted`.

### F4 — One `vm$` per page instead of nested `async`

`tournament-results.page.html` subscribes to `schedule$` **twice** and nests
`schedule$ | async` → `rankings$ | async` → `getSelectedMatch(...)`. Combine into one
selector (or `combineLatest`) and render with a single `@if (vm$ | async; as vm)`.
Fits naturally with F1.

### F5 — `ChangeDetectionStrategy.OnPush`

No component sets `OnPush`. Everything is driven by the store and inputs, so `OnPush` is
safe and cheap once F1/F4 remove the mutable page fields (`selectedMatchId`, `score1`…)
or move them to signals.

### F6 — `takeUntilDestroyed()`

Seven modals repeat `destroy$ = new Subject()` + `takeUntil(this.destroy$)` +
`ngOnDestroy`. Replace with `takeUntilDestroyed(this.destroyRef)` from
`@angular/core/rxjs-interop`. Some subscriptions (e.g. `afterClosed()` in
`tournament-schedule.page.ts`) are not protected at all today.

### F7 — Route param through component input binding

Every page does `this.route.snapshot.paramMap.get('id')!`. Enable
`withComponentInputBinding()` in `app.config.ts` and declare `@Input() id!: string`
(or `input.required<string>()`). Removes `ActivatedRoute` from all pages and the
non-null assertion.

### F8 — Typed reactive form for scores

Score entry uses template-driven `[(ngModel)]` with `min` / `max` attributes that are
never enforced: negative values or `600` can be submitted and only the backend rejects
them. Use a typed `FormGroup<{ score1, score2 }>` with `Validators.required`, `min(0)`,
`max(500)` — consistent with the other forms and the typed-forms rule. Goes into the
`score-form` component from F3.

### F9 — Data-driven `tournament-nav`

The six tab links are copy-pasted blocks with their own template reference. Declare a
`tabs = [{ path, label, icon, exact }]` array and render with `@for`.

---

## Backend

### B1 — HTTP error handling lives in `domain`

`GlobalExceptionHandler`, `ValidationErrorResponse` and `ConflictErrorResponse` are in
`domain.common.problem`. They are Spring MVC / HTTP concerns (`@RestControllerAdvice`,
status codes, response bodies) — the domain should only own the exceptions.
Move them to `api.common.problem` (exceptions stay in `domain.common.problem`) and add an
`ArchitectureTest` rule: *`domain` must not depend on `org.springframework.web..`*.

### B2 — Extract the round-robin algorithm from `ScheduleService`

`ScheduleService` (310 lines) mixes orchestration (stores, validation) with the pairing
algorithm (circle method, field assignment, team-rest balancing). Extract a pure
`RoundRobinScheduler` (`@UtilityClass`, no store) taking teams / fields and returning the
round plan. It becomes testable with plain inputs, and `ScheduleService` keeps only
"load → plan → save". Same reasoning applies to `BracketService` (289 lines) and the
seeding logic.

### B3 — N+1 queries in `GroupRankingService`

Rankings for all groups call `teamStore.findAllByGroupId` and `matchStore.findAllByGroupId`
**once per group**. Load teams and matches once by `tournamentId` and group them in
memory (as `ScheduleService.getSchedule` already does with `teamById` / `groupById` maps).

### B4 — Tournament end state

`TournamentStatus` has only `DRAFT` and `IN_PROGRESS`; a tournament never ends.
Add `FINISHED` (set when the bracket final has a result) so the UI can lock results entry
and show a podium. Needs a backend transition + a frontend `isFinished` flag (via F1/F2).

---

## Suggested order

Remaining work:

1. **F2 + F7** — quick wins, no rule change.
2. **F1 + F4 + F5** — amend `frontend-rules.md`, then migrate page by page (bracket first).
3. **B2** — extract the seeding / bracket-building logic from `BracketService`.
4. **B4 follow-up** — podium display for an `ENDED` tournament.

---

## Open questions

- Where do view models live: `domain/<feature>/*.view.ts` or `display/<feature>/views/`?
- Should formatting (dates, `'HH:mm'`) move into views as strings, or stay as Angular
  pipes for locale handling?
- Page-local UI state (selected match, typed scores): keep in the page as signals, or
  move into the NGXS state so a selector can build the full screen view?
