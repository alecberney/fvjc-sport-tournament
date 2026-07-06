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
│       └── tournament.domain.service.ts   ← only if needed
├── display/
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
- `@Selector()` returns domain models — never DTOs.
- `patchState` only — never `setState` directly.

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
- `inject()` for dependencies — no constructor.
- Styles go in a `.scss` file referenced via `styleUrl` — never inline `styles`.
- Pages use `store.select()` and `store.dispatch()`.
- Components are pure — they only receive inputs and emit outputs.

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
| Selectors | Return domain models — never DTOs |
| `patchState` | Always used — never `setState` directly |
| Selectors | Delegate computed data to `XxxDomainService` static methods — never compute inline |
| Domain service | Transformation methods are `static` — testable without Angular context |
| Pages | Dispatch actions, select state — one per route |
| Components | Pure — `@Input` / `@Output` only, no store access |
| Standalone | All components are standalone |
| Styles | Always use `styleUrl` with a `.scss` file — never inline `styles` |
| DI | `inject()` everywhere — no constructor injection |
| Action type | `[Feature] Action Name` format |
