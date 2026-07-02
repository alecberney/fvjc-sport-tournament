# Tournament Base Architecture — Skeleton Design

**Date:** 2026-07-01  
**Scope:** Full skeleton (option 2) — all files defined by the architecture rules, no business logic implemented.

---

## Goal

Establish the complete file and package structure for the `tournament` feature on both backend and frontend. Every layer is present and wired; all methods are stubbed. No real CRUD logic is implemented at this stage.

---

## Backend

### Dependencies to add

- `spring-boot-starter-validation` added to `pom.xml` (required for `@Valid` on request bodies).

### Package structure

`abe.fvjc.tournament.tournament` with 3 sub-packages:

```
tournament/
├── domain/
│   ├── Tournament.java
│   ├── TournamentId.java
│   ├── TournamentStatus.java
│   ├── Sport.java
│   ├── TournamentStore.java
│   └── TournamentService.java
├── persistence/
│   ├── TournamentEntity.java
│   ├── TournamentRepository.java
│   ├── JpaTournamentStore.java
│   └── TournamentDbMapper.java
└── api/
    ├── TournamentDto.java
    ├── TournamentCreateRequestDto.java
    ├── TournamentController.java
    └── TournamentApiMapper.java
```

### Domain layer

**`Tournament.java`** — `@Value @Builder` immutable aggregate:
- `TournamentId id`
- `String name`
- `Sport sport`
- `int numberOfFields`
- `int minPlayersPerTeam`
- `int maxPlayersPerTeam`
- `LocalDate date`
- `TournamentStatus status`

**`TournamentId.java`** — `record` with `.of(UUID)`, `.empty()`, `.isEmpty()`.

**`TournamentStatus.java`** — `enum`: `DRAFT`, `IN_PROGRESS`.

**`Sport.java`** — `enum`: `PETANQUE`, `VOLLEY`, `LUTTE`, `TIR_A_LA_CORDE`, `FOOTBALL`.

**`TournamentStore.java`** — interface (no Spring/JPA imports):
- `Tournament save(Tournament)`
- `Optional<Tournament> findById(UUID)`
- `List<Tournament> findAll()`
- `void deleteById(UUID)`

**`TournamentService.java`** — `@Service`, injects `TournamentStore`. Stub methods: `create`, `findById`, `findAll`, `delete` — all throw `UnsupportedOperationException`.

### Persistence layer (all package-private)

**`TournamentEntity.java`** — `@Entity @Table(name="tournaments")`, `@Getter @Setter @NoArgsConstructor`, columns matching domain fields + UUID primary key.

**`TournamentRepository.java`** — `interface` extends `JpaRepository<TournamentEntity, UUID>`.

**`JpaTournamentStore.java`** — `@Repository @Transactional`, implements `TournamentStore`. All methods throw `UnsupportedOperationException`.

**`TournamentDbMapper.java`** — `@UtilityClass` package-private. Methods: `static Tournament toTournament(TournamentEntity)`, `static TournamentEntity toTournamentEntity(Tournament)` — both throw `UnsupportedOperationException`.

### API layer

**`TournamentDto.java`** — `@Value @Builder` (public): `UUID id`, all tournament fields, `TournamentStatus status`.

**`TournamentCreateRequestDto.java`** — `@Value @Builder` (public): all fields except `id` and `status`. Validation annotations present (`@NotBlank`, `@NotNull`, `@Min`, `@Max`) as per UC-01 spec.

**`TournamentController.java`** — `@RestController @RequestMapping("/api/tournaments")`:
- `GET /` → `findAll()` stub returning `null`
- `GET /{id}` → `findById(UUID)` stub returning `null`
- `POST /` → `create(@RequestBody @Valid)` stub returning `null` with `@ResponseStatus(CREATED)`
- `DELETE /{id}` → `delete(UUID)` stub with `@ResponseStatus(NO_CONTENT)`

**`TournamentApiMapper.java`** — `@UtilityClass` public. Methods: `static TournamentDto toTournamentDto(Tournament)`, `static Tournament toTournament(TournamentCreateRequestDto)` — both throw `UnsupportedOperationException`.

### Liquibase migration

New changeset in `db/changelog/` (e.g. `20260701120000_create_tournaments_table.xml`), using Liquibase XML `<createTable>` instruction:

```xml
<changeSet id="20260701120000" author="aberney">
    <createTable tableName="tournaments">
        <column name="id" type="TEXT">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="name" type="TEXT">
            <constraints nullable="false"/>
        </column>
        <column name="sport" type="TEXT">
            <constraints nullable="false"/>
        </column>
        <column name="number_of_fields" type="INTEGER">
            <constraints nullable="false"/>
        </column>
        <column name="min_players_per_team" type="INTEGER">
            <constraints nullable="false"/>
        </column>
        <column name="max_players_per_team" type="INTEGER">
            <constraints nullable="false"/>
        </column>
        <column name="date" type="TEXT">
            <constraints nullable="false"/>
        </column>
        <column name="status" type="TEXT">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

Registered in `db/master.xml`.

---

## Frontend

### Dependencies to add

- `@ngxs/store` installed via npm and wired into `app.config.ts` with `provideStore([])`.

### File structure

```
src/app/tournament/
├── api/
│   ├── tournament.api.dto.ts
│   ├── tournament.api.service.ts
│   └── tournament.api.mapper.ts
├── domain/
│   ├── tournament.model.ts
│   ├── tournament.actions.ts
│   └── tournament.state.ts
├── display/
│   └── pages/
│       └── tournament-list/
│           ├── tournament-list.page.ts
│           └── tournament-list.page.html
└── modules/
    └── tournament.routes.ts
```

### API layer

**`tournament.api.dto.ts`** — `TournamentDto` interface (all fields, `date` as `string`, `status` as `TournamentStatus`) + `TournamentCreateRequestDto` interface.

**`tournament.api.service.ts`** — `@Injectable({ providedIn: 'root' })`, injects `HttpClient`. Methods: `getAll$()`, `getById$(id)`, `create$(dto)`, `delete$(id)` — all return `EMPTY`.

**`tournament.api.mapper.ts`** — static class (not `@Injectable`). `static toDomain(dto)` and `static toCreateRequest(tournament)` — both throw `Error('Not implemented')`.

### Domain layer

**`tournament.model.ts`** — `Tournament` interface + `TournamentStatus` enum + `Sport` enum.

**`tournament.actions.ts`** — classes: `LoadTournaments`, `LoadTournamentById(id)`, `CreateTournament(tournament)`, `DeleteTournament(id)`.

**`tournament.state.ts`** — `ITournamentState` interface (`tournaments: Tournament[], selected: Tournament | undefined`). `TournamentState` class with `@Selector()` stubs for `getTournaments` and `getSelected`, and empty `@Action()` handlers for all 4 actions.

### Display layer

**`tournament-list.page.ts`** — standalone, dispatches `LoadTournaments` on `ngOnInit`, selects `tournaments$` from store.

**`tournament-list.page.html`** — placeholder: `<p>Tournament list</p>`.

### Routing

**`tournament.routes.ts`** — `provideStates([TournamentState])` + route `{ path: '', component: TournamentListPage }`.

**`app.routes.ts`** — lazy-load `TOURNAMENT_ROUTES` at path `''`.

---

## Out of scope

- Business logic in any method
- Form UI components
- Error handling
- Unit tests (follow-up task)
