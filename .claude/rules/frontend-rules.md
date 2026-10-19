# Frontend Architecture Rules

Framework: **Angular** (standalone components) — **TypeScript** strict — **NGXS** for state management.

Base path: `src/app/`

---

## Package Structure

Layer-first, then feature. Four top-level layers:

```
src/app/
├── api/
│   └── tournament/
│       ├── tournament.api.dto.ts
│       ├── tournament.api.service.ts
│       └── tournament.api.mapper.ts
├── domain/
│   └── tournament/
│       ├── tournament.model.ts
│       ├── tournament.actions.ts
│       ├── tournament.state.ts
│       ├── tournament.domain.service.ts   ← only if needed
│       ├── tournament.view.ts             ← only if the feature has view models
│       └── tournament.view.mapper.ts
├── display/
│   ├── shared/
│   │   └── pipes/
│   │       └── date-format.pipe.ts
│   └── tournament/
│       ├── pages/
│       │   └── tournament-list/
│       │       ├── tournament-list.page.ts
│       │       └── tournament-list.page.html
│       └── components/
│           └── tournament-card/
│               ├── tournament-card.component.ts
│               └── tournament-card.component.html
└── modules/
    └── tournament.routes.ts
```

Imports always use the `@app/` alias — never relative paths:

```typescript
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { TournamentApiService } from '@app/api/tournament/tournament.api.service';
```

---

## File Naming

Angular kebab-case convention:

| Type | Naming | Example |
|------|--------|---------|
| DTO interfaces | `*.dto.ts` | `tournament.dto.ts` |
| API service | `*.api.service.ts` | `tournament.api.service.ts` |
| API mapper | `*.api.mapper.ts` | `tournament.api.mapper.ts` |
| Domain model | `*.model.ts` | `tournament.model.ts` |
| NGXS actions | `*.actions.ts` | `tournament.actions.ts` |
| NGXS state | `*.state.ts` | `tournament.state.ts` |
| Domain service | `*.domain.service.ts` | `tournament.domain.service.ts` |
| View models | `*.view.ts` | `bracket.view.ts` |
| View mapper | `*.view.mapper.ts` | `bracket.view.mapper.ts` |
| Pipe | `*.pipe.ts` | `date-format.pipe.ts` |
| Page component | `*.page.ts` | `tournament-list.page.ts` |
| Page styles | `*.page.scss` | `tournament-list.page.scss` |
| Component | `*.component.ts` | `tournament-card.component.ts` |
| Component styles | `*.component.scss` | `tournament-card.component.scss` |
| Routes | `*.routes.ts` | `tournament.routes.ts` |

---

## Models

All models are **interfaces** — never classes or type aliases for object shapes.

```typescript
// tournament.dto.ts — matches BE DTOs exactly
export interface TournamentDto {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: string;       // ISO date string from BE
  status: TournamentStatus;
}

// tournament.model.ts — domain model
export interface Tournament {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: Date;         // mapped to Date object
  status: TournamentStatus;
}
```

- DTOs mirror the BE response **exactly** — no transformations, no computed fields.
- Domain models are what the rest of the app consumes — always mapped from DTOs via the mapper.
- Use `string` for raw BE dates in DTOs, `Date` in domain models.

---

## API Layer (`api/`)

### API Service

Calls the BE API using `HttpClient`. Returns **DTOs** only — no mapping here.

```typescript
// tournament.api.service.ts
@Injectable({ providedIn: 'root' })
export class TournamentApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  getAll$(): Observable<TournamentDto[]> {
    return this.http.get<TournamentDto[]>(this.baseUrl);
  }

  getById$(id: string): Observable<TournamentDto> {
    return this.http.get<TournamentDto>(`${this.baseUrl}/${id}`);
  }

  create$(request: TournamentCreateRequestDto): Observable<TournamentDto> {
    return this.http.post<TournamentDto>(this.baseUrl, request);
  }

  update$(id: string, request: TournamentUpdateRequestDto): Observable<TournamentDto> {
    return this.http.put<TournamentDto>(`${this.baseUrl}/${id}`, request);
  }

  delete$(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
```

- Method names end with `$` to signal they return an `Observable`.
- `inject()` is used — no constructor injection.
- No error handling here — let it propagate to the state.

### API Mapper

Always created — maps DTOs to domain models and domain models to request DTOs.

```typescript
// tournament.api.mapper.ts
export class TournamentApiMapper {

  static toDomain(dto: TournamentDto): Tournament {
    return {
      ...dto,
      date: new Date(dto.date),
    };
  }

  static toCreateRequest(tournament: Partial<Tournament>): TournamentCreateRequestDto {
    return {
      name: tournament.name!,
      sport: tournament.sport!,
      numberOfFields: tournament.numberOfFields!,
      minPlayersPerTeam: tournament.minPlayersPerTeam!,
      maxPlayersPerTeam: tournament.maxPlayersPerTeam!,
      date: tournament.date!.toISOString().split('T')[0],
    };
  }
}
```

- Static methods only — not an Angular service, no `@Injectable`.
- `toDomain(dto)` maps one DTO to one domain model.
- `toCreateRequest` / `toUpdateRequest` map domain data to request DTOs.

---

## Domain Layer (`domain/`)

### Domain Model

```typescript
// tournament.model.ts
export interface Tournament {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: Date;
  status: TournamentStatus;
}

export enum TournamentStatus {
  DRAFT = 'DRAFT',
  IN_PROGRESS = 'IN_PROGRESS',
}

export enum Sport {
  PETANQUE = 'PETANQUE',
  VOLLEY = 'VOLLEY',
  LUTTE = 'LUTTE',
  TIR_A_LA_CORDE = 'TIR_A_LA_CORDE',
  FOOTBALL = 'FOOTBALL',
}
```

### Actions

```typescript
// tournament.actions.ts
export class LoadTournaments {
  static readonly type = '[Tournament] Load Tournaments';
}

export class LoadTournamentById {
  static readonly type = '[Tournament] Load Tournament By Id';
  constructor(public readonly id: string) {}
}

export class CreateTournament {
  static readonly type = '[Tournament] Create Tournament';
  constructor(public readonly tournament: Partial<Tournament>) {}
}
```

- Action type format: `[Feature] Action Name`.
- Payload goes in the constructor with `public readonly`.

### Domain Service

Contains data transformation logic called by `@Selector()` methods. Transformation methods are **static** so selectors (which are static) can call them directly. This makes transformations unit-testable without any NGXS context.

```typescript
// tournament.domain.service.ts
@Injectable({ providedIn: 'root' })
export class TournamentDomainService {

  static toSelectOptions(tournaments: Tournament[]): SelectOption<string>[] {
    return tournaments.map(t => ({
      value: t.id,
      label: t.name,
      trackId: t.id,
    }));
  }

  static isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  static canStart(tournament: Tournament, hasSchedule: boolean): boolean {
    return TournamentDomainService.isDraft(tournament) && hasSchedule;
  }
}
```

- **Static methods** for all transformations called by `@Selector()` — pure functions, no side effects.
- Instance methods only when DI is genuinely needed (e.g. calling another service).
- Testable in isolation: `TournamentDomainService.toSelectOptions([...])` requires no Angular context.

### State

```typescript
// tournament.state.ts
export interface ITournamentState {
  tournaments: Tournament[];
  selected: Tournament | undefined;
}

@State<ITournamentState>({
  name: 'tournament',
  defaults: {
    tournaments: [],
    selected: undefined,
  },
})
@Injectable()
export class TournamentState {

  private readonly apiService = inject(TournamentApiService);

  @Selector()
  static getTournaments(state: ITournamentState): Tournament[] {
    return state.tournaments;
  }

  @Selector()
  static getSelected(state: ITournamentState): Tournament | undefined {
    return state.selected;
  }

  @Selector()
  static getTournamentOptions(state: ITournamentState): SelectOption<string>[] {
    return TournamentDomainService.toSelectOptions(state.tournaments);
  }

  @Action(LoadTournaments)
  loadTournaments(ctx: StateContext<ITournamentState>) {
    return this.apiService.getAll$().pipe(
      tap((dtos) => {
        ctx.patchState({ tournaments: dtos.map(TournamentApiMapper.toDomain) });
      })
    );
  }

  @Action(LoadTournamentById)
  loadTournamentById(ctx: StateContext<ITournamentState>, { id }: LoadTournamentById) {
    return this.apiService.getById$(id).pipe(
      tap((dto) => {
        ctx.patchState({ selected: TournamentApiMapper.toDomain(dto) });
      })
    );
  }

  @Action(CreateTournament)
  createTournament(ctx: StateContext<ITournamentState>, { tournament }: CreateTournament) {
    return this.apiService.create$(TournamentApiMapper.toCreateRequest(tournament)).pipe(
      tap((dto) => {
        const created = TournamentApiMapper.toDomain(dto);
        ctx.patchState({
          tournaments: [...ctx.getState().tournaments, created],
        });
      })
    );
  }
}
```

- State interface named `IXxxState`.
- `inject()` for dependencies — no constructor.
- Mapping from DTO to domain happens in `@Action` handlers via the mapper.
- `@Selector()` delegates computed/derived data to `TournamentDomainService` static methods.
- `@Selector()` returns domain models or view models — never DTOs.
- `patchState` only — never `setState` directly.
- State holds **business domain data only** — never view models, never UI state (selected row,
  open panel, typed form values). UI state lives in the page and is passed to view selectors
  as a parameter.

### View Models

A screen whose template would otherwise compute what it shows (fallback labels, formatted
scores, enabled/selected flags, conditions) gets a **view model**: an interface whose
properties are exactly what the template binds. The template never computes — it binds.

```typescript
// domain/bracket/bracket.view.ts
export interface BracketMatchView {
  id: string;
  fieldLabel: string;      // 'T3'
  team1Label: string;      // 'Les Aigles' | 'À déterminer'
  team2Label: string;
  scoreLabel: string;      // '3 - 1' | '—'
  isSelectable: boolean;
  isSelected: boolean;
}

export interface BracketRoundView {
  id: string;
  name: string;
  startTime: Date;         // dates stay Date — formatted by DateFormatPipe in the template
  matches: BracketMatchView[];
}
```

Views are built by a **view mapper** — a static class (not `@Injectable`) in `*.view.mapper.ts`,
next to the view. Each method is named `to` + view type: `toBracketPageView`, `toBracketRoundView`,
`toBracketMatchView`. The mapper only maps: business rules (playable, can end, …) stay in the
`XxxDomainService` static methods, which the mapper calls.

```typescript
// domain/bracket/bracket.view.mapper.ts
export class BracketViewMapper {

  static toBracketMatchView(match: BracketMatch, canEnterResults: boolean, selectedMatchId: string | null): BracketMatchView {
    return {
      id: match.id,
      fieldLabel: `T${match.field}`,
      team1Label: match.team1?.name ?? BracketViewMapper.TO_BE_DETERMINED,
      team2Label: match.team2?.name ?? BracketViewMapper.TO_BE_DETERMINED,
      scoreLabel: match.result ? `${match.result.score1} - ${match.result.score2}` : '—',
      isSelectable: canEnterResults && BracketDomainService.isPlayable(match),
      isSelected: match.id === selectedMatchId,
    };
  }
}
```

The view is **built by a selector**, never stored. The selector gathers every piece of state it
needs itself (from any state, via `createSelector`); page-only UI state is the selector's
parameter. One page view selector per page, named `getXxxPageView`:

```typescript
// bracket.state.ts
static getBracketPageView(selectedMatchId: string | null) {
  return createSelector(
    [TournamentState.getSelected, BracketState.getRounds, ScheduleState.hasAllResults],
    (tournament, rounds, hasAllResults) => tournament
      ? BracketViewMapper.toBracketPageView(tournament, rounds, hasAllResults, selectedMatchId)
      : null,
  );
}
```

```typescript
// tournament-bracket.page.ts — user selections are a stream feeding the view selector
private readonly matchSelection$ = new Subject<string | null>();

readonly vm$ = this.matchSelection$.pipe(
  startWith(null),
  switchMap((id) => this.store.select(BracketState.getBracketPageView(id))),
);

selectMatch(match: BracketMatchView): void {
  this.matchSelection$.next(match.id);
}
```

When the template needs the current UI value back (e.g. a `mat-select` `[ngModel]`), the
view carries it: the selector parameter is copied into the view (`vm.selectedGroupName`).

- View interfaces in `domain/<feature>/<feature>.view.ts`, mapper in `<feature>.view.mapper.ts`.
- Dates stay `Date` in views — formatting is the template's job, through `DateFormatPipe`.
- The page renders through a single `@if (vm$ | async; as vm)`.

### Date Formatting

Dates are formatted in templates with the shared `DateFormatPipe` (`display/shared/pipes/`) —
never Angular's `DatePipe` with an inline pattern. The pipe owns the patterns, so a format is
changed in one place.

```html
{{ tournament.date | dateFormat:'date' }}   <!-- 15 Aug 2027 -->
{{ round.startTime | dateFormat:'time' }}   <!-- 14:30 -->
```

---

## Display Layer (`display/`)

### Pages vs Components

- **Pages** — routed components, one per route. Dispatch actions and select state. Named `*.page.ts`.
- **Components** — reusable UI pieces used inside pages. Receive data via `@Input()`, emit events via `@Output()`. Never dispatch actions directly.

```typescript
// pages/tournament-list/tournament-list.page.ts
@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  standalone: true,
  imports: [...],
})
export class TournamentListPage implements OnInit {

  private readonly store = inject(Store);

  tournaments$ = this.store.select(TournamentState.getTournaments);

  ngOnInit(): void {
    this.store.dispatch(new LoadTournaments());
  }
}
```

```typescript
// components/tournament-card/tournament-card.component.ts
@Component({
  selector: 'app-tournament-card',
  templateUrl: './tournament-card.component.html',
  standalone: true,
  imports: [...],
})
export class TournamentCardComponent {
  @Input({ required: true }) tournament!: Tournament;
  @Output() selected = new EventEmitter<Tournament>();
}
```

- All components are **standalone**.
- All components use `changeDetection: ChangeDetectionStrategy.OnPush` — pages, components and modals alike.
- `inject()` for dependencies — no constructor.
- Styles go in a `.scss` file referenced via `styleUrl` — never inline `styles`.
- Pages use `store.select()` and `store.dispatch()`.
- Components are pure — they only receive inputs and emit outputs.

### Change Detection and UI State

Every component is `OnPush`: Angular re-checks it only when an `@Input` gets a new reference,
an event fires in its template, or an `async` pipe emits. So the template must never depend on
a plain mutable field.

**No signals, no mutable UI fields** — everything the template shows changes through an
observable read with `| async`:

- Component fields are `readonly`. Never `this.field = value` to change what is displayed.
- Store data reaches the template through `vm$ | async` (see View Models).
- **User-driven UI state** (selected row, filter) is a `Subject` of user events (`next()` in the
  handler), piped with `startWith(initial)` into the view selector — the stream is the state,
  nothing is stored.
- **Action-driven UI state** (error message, "saving…") is derived from the NGXS `Actions`
  stream (`ofActionDispatched` / `ofActionCompleted` / `ofActionSuccessful`) — never set in a
  `subscribe` callback.
- Reactive forms are the exception: the `FormGroup` is the form's own mutable state, created once
  as a `readonly` field.

```typescript
// WRONG — plain field set in an async callback: OnPush never re-renders it
errorMessage: string | null = null;
error: (err) => { this.errorMessage = 'Erreur lors de la génération.'; }

// CORRECT — derived from the action stream: cleared on dispatch, set on failure
protected readonly errorMessage$: Observable<string | null> = merge(
  this.actions$.pipe(ofActionDispatched(GenerateBracket), map(() => null)),
  this.actions$.pipe(
    ofActionCompleted(GenerateBracket),
    map(({ result }) => result.error ? BracketGenerateModal.toErrorMessage(result.error) : null),
  ),
);
```

```html
@if (errorMessage$ | async; as message) { <p class="error-text">{{ message }}</p> }
```

### Typed Forms

Reactive forms are **typed** (`FormGroup<...>`). Access controls through the typed `form.controls` / `form.value` properties directly — **never** the stringly-typed `form.get('...')` accessor. `get('...')` returns `AbstractControl | null` and loses the value type; the typed property preserves it and is checked by the compiler.

```typescript
// WRONG — stringly-typed, loses the type, returns AbstractControl | null
const duration = this.form.get('matchDurationMinutes')?.value;
this.form.get('matchDurationMinutes')?.setValue(30);
this.form.get('matchDurationMinutes')?.invalid;
```

```typescript
// CORRECT — typed property access on the form
const duration = this.form.controls.matchDurationMinutes.value;
this.form.controls.matchDurationMinutes.setValue(30);
this.form.controls.matchDurationMinutes.invalid;

// value shortcut when reading only
const duration = this.form.value.matchDurationMinutes;
```

- Applies everywhere a control is accessed — in components, pages, and templates.
- In templates, bind through the typed control too: `form.controls.matchDurationMinutes` — never `form.get('matchDurationMinutes')`.

### Template Conditions

A `@if` / `@else if` condition made of **multiple parts combined with `||` or `&&`** must not live in the template — extract it into a method (or getter) on the `.ts` returning `boolean`, and call that from the template. A single, atomic expression (one comparison, one method call, one truthy check) stays inline.

```html
<!-- WRONG — compound logic in the template -->
@if (form.controls.startTime.hasError('required') || form.controls.startTime.hasError('pattern')) { ... }
@if (isDraft(tournament) && (schedule$ | async)?.totalRounds! > 0) { ... }
```

```html
<!-- CORRECT — the template reads a single named intent -->
@if (isStartTimeInvalid()) { ... }
@if (canStartTournament(tournament, schedule$ | async)) { ... }
```

```typescript
protected isStartTimeInvalid(): boolean {
  return this.form.controls.startTime.hasError('required')
    || this.form.controls.startTime.hasError('pattern');
}

protected canStartTournament(tournament: Tournament, schedule: Schedule | null | undefined): boolean {
  return this.isDraft(tournament) && !!schedule && schedule.totalRounds > 0;
}
```

- Applies to `@if`, `@else if`, and any structural condition — the moment two or more conditions are joined by `||` / `&&`.
- The method is named after the **intent** it expresses (`isStartTimeInvalid`, `canStartTournament`) — never a mechanical `checkCondition1`.
- Values only available in the template (an `async` pipe result, a `@for` item) are passed in as method arguments — the method stays a pure function of its inputs.
- A single inline expression (`@if (isDraft(tournament))`, `@if (schedule.totalRounds === 0)`) is left as-is — this rule targets compound conditions only.

---

## Shared Styles

Style rules that are **identical across two or more components/pages** are not duplicated — they live once as a reusable class in the global stylesheet and are referenced by class name from any template.

```
src/
├── styles.scss              ← global entry; @use-s the partials below
└── styles/
    └── _global.scss         ← reusable class definitions
```

```scss
// styles.scss — @use must come before any other rule
@use 'styles/global';

// styles/_global.scss
.empty-state { /* ... */ }
.responsible-row { /* ... */ }
.modal-form { /* ... */ }
```

- Global styles are **not** view-encapsulated, so a class defined here (and its descendant selectors like `.empty-state mat-icon`) applies inside any component template that uses the class name.
- Only extract rules that are **genuinely the same**. Styles that merely look similar but differ in spacing, colour, or children stay local to their component — never normalise differing styles into a shared class without intent.
- The shared class is named after its **purpose** (`.empty-state`, `.modal-form`), not the component it came from (`.edit-form`).
- A component keeps its own `.scss` only for styles unique to it. When extraction leaves a component with no unique styles, delete the `.scss` file and drop its `styleUrl`.

---

## Modules Layer (`modules/`)

Registers routes and provides the NGXS state for the feature.

```typescript
// modules/tournament.routes.ts
export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
    ],
  },
];
```

---

## Rules Summary

| Rule | Detail |
|------|--------|
| Models | Always `interface` — never `class` or `type` for objects |
| DTOs | Mirror BE exactly — no transformations |
| Mapper | Always created — static class, not `@Injectable` |
| Mapping | DTOs → domain in `@Action` handlers via mapper |
| API service | Returns DTOs only, methods end with `$` |
| State interface | Named `IXxxState` |
| Selectors | Return domain models or view models — never DTOs |
| `patchState` | Always used — never `setState` directly |
| Selectors | Delegate computed data to `XxxDomainService` static methods — never compute inline |
| Domain service | Transformation methods are `static` — testable without Angular context |
| State content | Business domain data only — never view models or UI state |
| View models | `*.view.ts` interfaces, built by a static `XxxViewMapper` (`*.view.mapper.ts`) with `toXxxView` methods; business rules stay in the domain service |
| View selectors | One `getXxxPageView(uiState)` per page built with `createSelector` — gathers all state itself, UI state is the parameter; the view is never stored |
| Date formatting | `DateFormatPipe` (`dateFormat:'date'` / `'time'`) in templates — never `DatePipe` with an inline pattern; dates stay `Date` in views |
| Pages | Dispatch actions, select state — one per route |
| Components | Pure — `@Input` / `@Output` only, no store access |
| Standalone | All components are standalone |
| Change detection | Every component uses `ChangeDetectionStrategy.OnPush` |
| UI state | No signals, no mutable fields — fields are `readonly`; store data via `vm$ \| async`; user-driven UI state is a `Subject` + `startWith` feeding the view selector; action-driven UI state derived from the NGXS `Actions` stream |
| Typed forms | Access controls via typed `form.controls.xxx` / `form.value.xxx` — never `form.get('xxx')` |
| Template conditions | Compound `@if` conditions (`||` / `&&`) extracted into an intent-named `.ts` method returning `boolean` — single expressions stay inline |
| Styles | Always use `styleUrl` with a `.scss` file — never inline `styles` |
| Shared styles | Identical rules across components extracted to `styles/_global.scss` as a purpose-named class — never duplicate; leave differing styles local |
| DI | `inject()` everywhere — no constructor injection |
| Action type | `[Feature] Action Name` format |
