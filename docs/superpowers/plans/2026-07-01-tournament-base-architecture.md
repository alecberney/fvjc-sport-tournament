# Tournament Base Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold every file in the Tournament feature for both backend and frontend — all layers present, all methods stubbed — with zero business logic.

**Architecture:** Feature-first DDD on the backend (domain → persistence → api), NGXS feature module on the frontend (api → domain → display → modules). Both tracks compile and wire independently; they connect only at runtime over HTTP.

**Tech Stack:** Java 21 · Spring Boot 3.5.3 · Lombok · SQLite · Liquibase · Angular 19 (standalone) · TypeScript 5.7 · NGXS

## Global Constraints

- Package root: `abe.fvjc.tournament`
- Base path (frontend): `src/app/`
- All domain objects are immutable: `@Value @Builder`
- JPA entities are package-private (`class`, not `public class`)
- Mappers are `@UtilityClass` with explicit `static` on every method
- DI via `inject()` everywhere — no constructor injection in Angular
- NGXS state via `inject()` — no constructor injection in state classes
- Liquibase changelogs use XML instructions (`<createTable>` etc.) — never raw SQL

---

### Task 1: Add spring-boot-starter-validation

**Files:**
- Modify: `backend/pom.xml`

**Interfaces:**
- Produces: `@Valid`, `@NotBlank`, `@NotNull`, `@Min`, `@Max` available in classpath (used in Task 5)

- [ ] **Step 1: Add the dependency**

In `backend/pom.xml`, inside the `<dependencies>` block, add after the `spring-boot-starter-web` dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- [ ] **Step 2: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "BE - Add spring-boot-starter-validation dependency"
```

---

### Task 2: Backend domain layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/Tournament.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentId.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentStatus.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/Sport.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/domain/TournamentService.java`

**Interfaces:**
- Produces: `Tournament`, `TournamentId`, `TournamentStatus`, `Sport`, `TournamentStore`, `TournamentService` — consumed by Tasks 3, 4, and 5.

- [ ] **Step 1: Create `TournamentStatus.java`**

```java
package abe.fvjc.tournament.tournament.domain;

public enum TournamentStatus {
    DRAFT,
    IN_PROGRESS
}
```

- [ ] **Step 2: Create `Sport.java`**

```java
package abe.fvjc.tournament.tournament.domain;

public enum Sport {
    PETANQUE,
    VOLLEY,
    LUTTE,
    TIR_A_LA_CORDE,
    FOOTBALL
}
```

- [ ] **Step 3: Create `TournamentId.java`**

```java
package abe.fvjc.tournament.tournament.domain;

import java.util.UUID;

public record TournamentId(UUID value) {

    public static TournamentId of(UUID value) {
        return new TournamentId(value);
    }

    public static TournamentId empty() {
        return new TournamentId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 4: Create `Tournament.java`**

```java
package abe.fvjc.tournament.tournament.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Tournament {
    TournamentId id;
    String name;
    Sport sport;
    int numberOfFields;
    int minPlayersPerTeam;
    int maxPlayersPerTeam;
    LocalDate date;
    TournamentStatus status;
}
```

- [ ] **Step 5: Create `TournamentStore.java`**

```java
package abe.fvjc.tournament.tournament.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentStore {
    Tournament save(Tournament tournament);
    Optional<Tournament> findById(UUID id);
    List<Tournament> findAll();
    void deleteById(UUID id);
}
```

- [ ] **Step 6: Create `TournamentService.java`**

```java
package abe.fvjc.tournament.tournament.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TournamentService {

    private final TournamentStore tournamentStore;

    public TournamentService(TournamentStore tournamentStore) {
        this.tournamentStore = tournamentStore;
    }

    public Tournament create(Tournament tournament) {
        throw new UnsupportedOperationException();
    }

    public Tournament findById(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<Tournament> findAll() {
        throw new UnsupportedOperationException();
    }

    public void delete(UUID id) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 7: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/tournament/domain/
git commit -m "BE - Add Tournament domain layer skeleton"
```

---

### Task 3: Liquibase migration — tournaments table

**Files:**
- Create: `backend/src/main/resources/db/changelog/20260701120000_create_tournaments_table.xml`
- Modify: `backend/src/main/resources/db/master.xml`

**Interfaces:**
- Produces: `tournaments` table in the database — required at runtime by Task 4's JPA entity.

- [ ] **Step 1: Create the changeset file**

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

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

</databaseChangeLog>
```

- [ ] **Step 2: Register in `master.xml`**

Add a new `<include>` line after the existing one in `backend/src/main/resources/db/master.xml`:

```xml
<include relativeToChangelogFile="true" file="changelog/20260701120000_create_tournaments_table.xml"/>
```

The full `master.xml` after edit:

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<!--@formatter:off-->
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <property name="now" value="now()" dbms="h2"/>
    <property name="now" value="date()" dbms="sqlite"/>
    <property name="createdByDefaultValue" value="system"/>

    <include relativeToChangelogFile="true" file="changelog/20250710130200_example.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260701120000_create_tournaments_table.xml"/>
</databaseChangeLog>
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "BE - Add Liquibase migration for tournaments table"
```

---

### Task 4: Backend persistence layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/persistence/TournamentEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/persistence/TournamentRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/persistence/JpaTournamentStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/persistence/TournamentDbMapper.java`

**Interfaces:**
- Consumes: `Tournament`, `TournamentId`, `TournamentStatus`, `Sport`, `TournamentStore` from Task 2.
- Produces: `JpaTournamentStore` implementing `TournamentStore` — Spring will inject it into `TournamentService`.

- [ ] **Step 1: Create `TournamentEntity.java`**

```java
package abe.fvjc.tournament.tournament.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
class TournamentEntity {

    @Id
    private UUID id;
    private String name;
    private String sport;
    private int numberOfFields;
    private int minPlayersPerTeam;
    private int maxPlayersPerTeam;
    private String date;
    private String status;
}
```

- [ ] **Step 2: Create `TournamentRepository.java`**

```java
package abe.fvjc.tournament.tournament.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TournamentRepository extends JpaRepository<TournamentEntity, UUID> {}
```

- [ ] **Step 3: Create `TournamentDbMapper.java`**

```java
package abe.fvjc.tournament.tournament.persistence;

import abe.fvjc.tournament.tournament.domain.Tournament;
import lombok.experimental.UtilityClass;

@UtilityClass
class TournamentDbMapper {

    static Tournament toTournament(TournamentEntity entity) {
        throw new UnsupportedOperationException();
    }

    static TournamentEntity toTournamentEntity(Tournament tournament) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 4: Create `JpaTournamentStore.java`**

```java
package abe.fvjc.tournament.tournament.persistence;

import abe.fvjc.tournament.tournament.domain.Tournament;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
class JpaTournamentStore implements TournamentStore {

    private final TournamentRepository repository;

    JpaTournamentStore(TournamentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Tournament save(Tournament tournament) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Tournament> findById(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tournament> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteById(UUID id) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 5: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/tournament/persistence/
git commit -m "BE - Add Tournament persistence layer skeleton"
```

---

### Task 5: Backend API layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/api/TournamentDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/api/TournamentCreateRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/api/TournamentApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/tournament/api/TournamentController.java`

**Interfaces:**
- Consumes: `Tournament`, `TournamentStatus`, `Sport` from Task 2; `TournamentService` from Task 2; validation annotations from Task 1.
- Produces: `GET /api/tournaments`, `GET /api/tournaments/{id}`, `POST /api/tournaments`, `DELETE /api/tournaments/{id}` (all return stubs).

- [ ] **Step 1: Create `TournamentDto.java`**

```java
package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Sport;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class TournamentDto {
    UUID id;
    String name;
    Sport sport;
    int numberOfFields;
    int minPlayersPerTeam;
    int maxPlayersPerTeam;
    LocalDate date;
    TournamentStatus status;
}
```

- [ ] **Step 2: Create `TournamentCreateRequestDto.java`**

```java
package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Sport;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class TournamentCreateRequestDto {

    @NotBlank
    String name;

    @NotNull
    Sport sport;

    @NotNull
    @Min(1)
    @Max(500)
    Integer numberOfFields;

    @NotNull
    @Min(1)
    Integer minPlayersPerTeam;

    @NotNull
    Integer maxPlayersPerTeam;

    @NotNull
    LocalDate date;
}
```

- [ ] **Step 3: Create `TournamentApiMapper.java`**

```java
package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Tournament;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TournamentApiMapper {

    public static TournamentDto toTournamentDto(Tournament tournament) {
        throw new UnsupportedOperationException();
    }

    public static Tournament toTournament(TournamentCreateRequestDto dto) {
        throw new UnsupportedOperationException();
    }
}
```

- [ ] **Step 4: Create `TournamentController.java`**

```java
package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.TournamentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
class TournamentController {

    private final TournamentService tournamentService;

    TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    List<TournamentDto> findAll() {
        return null;
    }

    @GetMapping("/{id}")
    TournamentDto findById(@PathVariable UUID id) {
        return null;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TournamentDto create(@RequestBody @Valid TournamentCreateRequestDto request) {
        return null;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
    }
}
```

- [ ] **Step 5: Verify it compiles**

```bash
cd backend && ./mvnw compile -q
```

Expected: `BUILD SUCCESS` with no errors.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/tournament/api/
git commit -m "BE - Add Tournament API layer skeleton"
```

---

### Task 6: Install NGXS and wire into app config

**Files:**
- Modify: `frontend/package.json` (via npm install)
- Modify: `frontend/src/app/app.config.ts`

**Interfaces:**
- Produces: `provideStore`, `provideStates`, `Store`, `State`, `Action`, `Selector`, `StateContext` available from `@ngxs/store` — consumed by Tasks 7, 8, and 9.

- [ ] **Step 1: Install NGXS**

```bash
cd frontend && npm install @ngxs/store
```

Expected: package added to `node_modules/` and `package.json` updated.

- [ ] **Step 2: Wire into `app.config.ts`**

Replace the contents of `frontend/src/app/app.config.ts` with:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideStore } from '@ngxs/store';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideStore([]),
  ],
};
```

- [ ] **Step 3: Verify it builds**

```bash
cd frontend && npm run build 2>&1 | tail -5
```

Expected: `Application bundle generation complete.`

- [ ] **Step 4: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/app/app.config.ts
git commit -m "FE - Install NGXS and wire into app config"
```

---

### Task 7: Frontend API layer

**Files:**
- Create: `frontend/src/app/tournament/api/tournament.api.dto.ts`
- Create: `frontend/src/app/tournament/api/tournament.api.service.ts`
- Create: `frontend/src/app/tournament/api/tournament.api.mapper.ts`

**Interfaces:**
- Consumes: `Sport`, `TournamentStatus`, `Tournament` from Task 8's `tournament.model.ts` — **write Task 8's model file first, then come back and create these files** (circular dependency — the DTO imports enums defined in the model).
- Produces: `TournamentDto`, `TournamentCreateRequestDto`, `TournamentApiService`, `TournamentApiMapper` — consumed by Task 8 (state) and Task 9 (page).

> **Note:** The DTO imports enums from `tournament.model.ts`. Create Task 8's `tournament.model.ts` first, then return here to create the API layer.

- [ ] **Step 1: Create `tournament.api.dto.ts`**

```typescript
import { Sport, TournamentStatus } from '../domain/tournament.model';

export interface TournamentDto {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: string;
  status: TournamentStatus;
}

export interface TournamentCreateRequestDto {
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: string;
}
```

- [ ] **Step 2: Create `tournament.api.service.ts`**

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { TournamentDto, TournamentCreateRequestDto } from './tournament.api.dto';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  getAll$(): Observable<TournamentDto[]> {
    return EMPTY;
  }

  getById$(id: string): Observable<TournamentDto> {
    return EMPTY;
  }

  create$(request: TournamentCreateRequestDto): Observable<TournamentDto> {
    return EMPTY;
  }

  delete$(id: string): Observable<void> {
    return EMPTY;
  }
}
```

- [ ] **Step 3: Create `tournament.api.mapper.ts`**

```typescript
import { TournamentDto, TournamentCreateRequestDto } from './tournament.api.dto';
import { Tournament } from '../domain/tournament.model';

export class TournamentApiMapper {

  static toDomain(dto: TournamentDto): Tournament {
    throw new Error('Not implemented');
  }

  static toCreateRequest(tournament: Partial<Tournament>): TournamentCreateRequestDto {
    throw new Error('Not implemented');
  }
}
```

- [ ] **Step 4: Verify it builds**

```bash
cd frontend && npm run build 2>&1 | tail -5
```

Expected: `Application bundle generation complete.`

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/tournament/api/
git commit -m "FE - Add Tournament API layer skeleton"
```

---

### Task 8: Frontend domain layer

**Files:**
- Create: `frontend/src/app/tournament/domain/tournament.model.ts`
- Create: `frontend/src/app/tournament/domain/tournament.actions.ts`
- Create: `frontend/src/app/tournament/domain/tournament.state.ts`

**Interfaces:**
- Produces: `Tournament`, `TournamentStatus`, `Sport` (model); `LoadTournaments`, `LoadTournamentById`, `CreateTournament`, `DeleteTournament` (actions); `TournamentState`, `ITournamentState` (state) — consumed by Task 7 (API layer) and Task 9 (page + routes).

> **Order:** Create `tournament.model.ts` first (Task 7 depends on it). Then create `tournament.actions.ts` and `tournament.state.ts`.

- [ ] **Step 1: Create `tournament.model.ts`**

```typescript
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
```

- [ ] **Step 2: Create `tournament.actions.ts`**

```typescript
import { Tournament } from './tournament.model';

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

export class DeleteTournament {
  static readonly type = '[Tournament] Delete Tournament';
  constructor(public readonly id: string) {}
}
```

- [ ] **Step 3: Create `tournament.state.ts`**

```typescript
import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { Tournament } from './tournament.model';
import {
  CreateTournament,
  DeleteTournament,
  LoadTournamentById,
  LoadTournaments,
} from './tournament.actions';
import { TournamentApiService } from '../api/tournament.api.service';

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

  @Action(LoadTournaments)
  loadTournaments(_ctx: StateContext<ITournamentState>): void {}

  @Action(LoadTournamentById)
  loadTournamentById(_ctx: StateContext<ITournamentState>, _action: LoadTournamentById): void {}

  @Action(CreateTournament)
  createTournament(_ctx: StateContext<ITournamentState>, _action: CreateTournament): void {}

  @Action(DeleteTournament)
  deleteTournament(_ctx: StateContext<ITournamentState>, _action: DeleteTournament): void {}
}
```

- [ ] **Step 4: Verify it builds**

```bash
cd frontend && npm run build 2>&1 | tail -5
```

Expected: `Application bundle generation complete.`

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/tournament/domain/
git commit -m "FE - Add Tournament domain layer skeleton"
```

---

### Task 9: Frontend display layer and routing

**Files:**
- Create: `frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.ts`
- Create: `frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.html`
- Create: `frontend/src/app/tournament/modules/tournament.routes.ts`
- Modify: `frontend/src/app/app.routes.ts`

**Interfaces:**
- Consumes: `TournamentState.getTournaments` selector from Task 8; `LoadTournaments` action from Task 8; `TournamentState` from Task 8.
- Produces: App routes to `TournamentListPage` at `/`.

- [ ] **Step 1: Create `tournament-list.page.ts`**

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '../../domain/tournament.model';
import { TournamentState } from '../../domain/tournament.state';
import { LoadTournaments } from '../../domain/tournament.actions';

@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  standalone: true,
  imports: [AsyncPipe],
})
export class TournamentListPage implements OnInit {

  private readonly store = inject(Store);

  readonly tournaments$: Observable<Tournament[]> = this.store.select(TournamentState.getTournaments);

  ngOnInit(): void {
    this.store.dispatch(new LoadTournaments());
  }
}
```

- [ ] **Step 2: Create `tournament-list.page.html`**

```html
<p>Tournament list</p>
```

- [ ] **Step 3: Create `tournament.routes.ts`**

```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament.state';
import { TournamentListPage } from '../display/pages/tournament-list/tournament-list.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState])],
    children: [
      { path: '', component: TournamentListPage },
    ],
  },
];
```

- [ ] **Step 4: Update `app.routes.ts`**

Replace the contents of `frontend/src/app/app.routes.ts` with:

```typescript
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./tournament/modules/tournament.routes').then(m => m.TOURNAMENT_ROUTES),
  },
];
```

- [ ] **Step 5: Verify it builds**

```bash
cd frontend && npm run build 2>&1 | tail -5
```

Expected: `Application bundle generation complete.`

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/tournament/display/ frontend/src/app/tournament/modules/ frontend/src/app/app.routes.ts
git commit -m "FE - Add Tournament display layer and routing skeleton"
```

---

## Execution order

Tasks 1–5 are backend-only and can run sequentially without any frontend dependency.  
Tasks 6–9 are frontend-only and can run after Task 6 (NGXS installation).  
Tasks 7 and 8 have a shared dependency on `tournament.model.ts` — create that file first (Task 8 Step 1), then proceed with Task 7.
