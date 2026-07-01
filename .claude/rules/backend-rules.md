# Backend Architecture Rules

Base package: `abe.fvjc.tournament`

## Package Structure

Feature-first, then layer. Each feature has exactly 3 sub-packages:

```
abe.fvjc.tournament
├── tournament/
│   ├── api/
│   ├── domain/
│   └── persistence/
├── team/
│   ├── api/
│   ├── domain/
│   └── persistence/
├── group/
│   ├── api/
│   ├── domain/
│   └── persistence/
├── match/
│   ├── api/
│   ├── domain/
│   └── persistence/
├── schedule/
│   ├── api/
│   ├── domain/
│   └── persistence/
└── shared/
    └── exception/
```

## Dependency Direction

```
api → domain ← persistence
```

- `domain` never imports from `api` or `persistence`
- `api` never imports from `persistence`
- `persistence` implements domain interfaces

## Naming Conventions

| Concept | Example |
|---|---|
| Domain class | `Team` |
| Domain ID record | `TeamId` |
| Domain store interface | `TeamStore` |
| JPA entity | `TeamEntity` |
| Spring Data repository | `TeamRepository` |
| Store implementation | `JpaTeamStore` |
| Service | `TeamService` |
| Controller | `TeamController` |
| Base response DTO | `TeamDto` |
| Create request DTO | `TeamCreateRequestDto` |
| Update request DTO | `TeamUpdateRequestDto` |
| Delete request DTO | `TeamDeleteRequestDto` |
| API mapper | `TeamApiMapper` |
| Persistence mapper | `TeamDbMapper` |

## Domain Layer (`domain/`)

### Domain Classes

Use Lombok `@Value` + `@Builder` — all domain objects are immutable.

```java
@Value
@Builder
public class Team {
    TeamId id;
    String name;
    String sport;
    TournamentId tournamentId;
}
```

### Typed ID Records

Every aggregate root and entity has its own typed ID record. Never use raw `UUID` in domain objects.

```java
public record TeamId(UUID value) {

    public static TeamId of(UUID value) {
        return new TeamId(value);
    }

    public static TeamId empty() {
        return new TeamId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- `.of(UUID)` — normal constructor
- `.empty()` — signals a not-yet-persisted entity
- `.isEmpty()` — checks if the ID has no value

### Store Interface

Repository interfaces belong to the domain — no Spring or JPA imports.

```java
public interface TeamStore {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findAll();
    void deleteById(UUID id);
}
```

### Service

Services live in `domain/`. They orchestrate store calls but contain no mapping or HTTP logic. Use `@RequiredArgsConstructor` — no explicit constructor.

```java
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamStore teamStore;

    public Team create(Team team) {
        return teamStore.save(team);
    }

    public Team findById(UUID id) {
        return teamStore.findById(id)
            .orElseThrow(() -> new NotFoundException("Team", id));
    }

    public Team update(Team team) {
        findById(team.getId().value());
        return teamStore.save(team);
    }

    public void delete(UUID id) {
        findById(id);
        teamStore.deleteById(id);
    }
}
```

### Value Objects

Value objects use Lombok `@Value` + `@Builder`.

```java
@Value
@Builder
public class Score {
    int home;
    int away;

    public boolean isHomeWin() { return home > away; }
    public boolean isDraw()    { return home == away; }
}
```

## Persistence Layer (`persistence/`)

### JPA Entity

JPA entities use `@Getter` + `@Setter` + `@NoArgsConstructor` (JPA requires mutability). They are **package-private** — they must not leak outside the `persistence` package.

```java
@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
class TeamEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;
    private String sport;
    private UUID tournamentId;
}
```

### Spring Data Repository

Package-private — only used by `JpaXxxStore`.

```java
interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
    List<TeamEntity> findByTournamentId(UUID tournamentId);
}
```

### Store Implementation

`@Transactional` lives here, not on services or controllers. Apply it **per method**, not on the class — use `readOnly = true` on read methods. Use `@RequiredArgsConstructor` — no explicit constructor.

```java
@Repository
@RequiredArgsConstructor
class JpaTeamStore implements TeamStore {

    private final TeamRepository repository;

    @Override
    @Transactional
    public Team save(Team team) {
        return TeamDbMapper.toTeam(repository.save(TeamDbMapper.toTeamEntity(team)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(UUID id) {
        return repository.findById(id).map(TeamDbMapper::toTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAll() {
        return repository.findAll().stream().map(TeamDbMapper::toTeam).toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
```

### Persistence Mapper (`TeamDbMapper`)

`@UtilityClass` — `final` class, `private` constructor, all methods `static`. Package-private. Use the **minimum necessary visibility** on each method: start `private`, then package-private, then `protected`, then `public` only if callers outside the package genuinely need it.

```java
@UtilityClass
class TeamDbMapper {

    static Team toTeam(TeamEntity entity) {
        return Team.builder()
            .id(TeamId.of(entity.getId()))
            .name(entity.getName())
            .sport(entity.getSport())
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static TeamEntity toTeamEntity(Team team) {
        TeamEntity entity = new TeamEntity();
        entity.setId(team.getId().isEmpty() ? UUID.randomUUID() : team.getId().value());
        entity.setName(team.getName());
        entity.setSport(team.getSport());
        entity.setTournamentId(team.getTournamentId().value());
        return entity;
    }
}
```

- Method name = `to` + target type: `toTeam(TeamEntity)`, `toTeamEntity(Team)`
- Always use explicit `static` keyword on methods

## API Layer (`api/`)

### Controller

Controllers are thin — one service call per endpoint, no business logic. Use `@RequiredArgsConstructor`. All handler methods are `public`. Method names follow the HTTP verb convention:

| HTTP method | Method name pattern | Example |
|---|---|---|
| `GET` (collection) | `getAll` | `getAll()` |
| `GET` (single) | `getById` / `getByXxx` | `getById(UUID id)` |
| `POST` | `create` | `create(...)` |
| `PUT` / `PATCH` | `updateXxx` | `updateStatus(...)` |
| `DELETE` | `delete` | `delete(UUID id)` |

```java
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<TeamDto> getAll() {
        return teamService.findAll().stream().map(TeamApiMapper::toTeamDto).toList();
    }

    @GetMapping("/{id}")
    public TeamDto getById(@PathVariable UUID id) {
        return TeamApiMapper.toTeamDto(teamService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@RequestBody @Valid TeamCreateRequestDto request) {
        return TeamApiMapper.toTeamDto(teamService.create(TeamApiMapper.toTeam(request)));
    }

    @PutMapping("/{id}")
    public TeamDto updateName(@PathVariable UUID id, @RequestBody @Valid TeamUpdateRequestDto request) {
        return TeamApiMapper.toTeamDto(teamService.update(TeamApiMapper.toTeam(request, id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        teamService.delete(id);
    }
}
```

### DTOs

Use Lombok `@Value` + `@Builder` + `@Jacksonized`. `@Jacksonized` enables Jackson deserialization with `@Builder` — always required. No mapping logic inside DTOs — that belongs in the mapper.

```java
@Value
@Builder
@Jacksonized
public class TeamDto {
    UUID id;
    String name;
    String sport;
}

@Value
@Builder
@Jacksonized
public class TeamCreateRequestDto {
    @NotBlank String name;
    @NotBlank String sport;
    @NotNull UUID tournamentId;
}
```

### API Mapper (`TeamApiMapper`)

`@UtilityClass` — public class since it may be called from other packages. All methods explicitly `static`. Use **minimum necessary visibility** on each method: default to package-private (no modifier), escalate to `public` only when callers outside the `api` package require it.

```java
@UtilityClass
public class TeamApiMapper {

    static TeamDto toTeamDto(Team team) {
        return TeamDto.builder()
            .id(team.getId().value())
            .name(team.getName())
            .sport(team.getSport())
            .build();
    }

    static Team toTeam(TeamCreateRequestDto dto) {
        return Team.builder()
            .id(TeamId.empty())
            .name(dto.getName())
            .sport(dto.getSport())
            .tournamentId(TournamentId.of(dto.getTournamentId()))
            .build();
    }

    static Team toTeam(TeamUpdateRequestDto dto, UUID id) {
        return Team.builder()
            .id(TeamId.of(id))
            .name(dto.getName())
            .sport(dto.getSport())
            .build();
    }
}
```

- Method name = `to` + target type: `toTeamDto(Team)`, `toTeam(TeamCreateRequestDto)`
- Always use explicit `static` keyword on methods
- Visibility ladder: `private` → package-private → `protected` → `public` — use the lowest that satisfies callers

## Shared Layer (`shared/`)

```java
// shared/exception/NotFoundException.java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
}

// shared/exception/BusinessException.java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

## Method Parameters

Method parameters are treated as immutable — never reassign or mutate a parameter inside a method. If a modified version is needed, produce a new instance and return it.

```java
// WRONG — mutating the parameter
public Team assignId(Team team) {
    team.setId(UUID.randomUUID()); // forbidden
    return team;
}

// CORRECT — produce a new instance and return it
public Team assignId(Team team) {
    return team.toBuilder()
        .id(TeamId.of(UUID.randomUUID()))
        .build();
}
```

This applies to all layers — services, mappers, stores.

---

## Rules Summary

| Rule | Detail |
|---|------|
| Immutability | Domain classes, value objects, and DTOs use `@Value` + `@Builder` |
| DTOs | Always add `@Jacksonized` alongside `@Value` + `@Builder` |
| JPA entities | `@Getter` + `@Setter` + `@NoArgsConstructor`, package-private |
| Typed IDs | Every entity has a typed ID record with `.of()` and `.empty()` |
| `@RequiredArgsConstructor` | Used on services, controllers, and store implementations — no explicit constructors |
| `@Transactional` | Per method on `JpaXxxStore` only — never on the class, never on services or controllers; use `readOnly = true` on reads |
| Mappers | `@UtilityClass` with explicit `static` methods, named `to` + target type |
| Mapper visibility | Minimum necessary: `private` → package-private → `protected` → `public` |
| API mapper | `TeamApiMapper` in `api/`, public class, methods package-private by default |
| Persistence mapper | `TeamDbMapper` in `persistence/`, package-private class and methods |
| Controller methods | Always `public`; named by HTTP verb: `getAll`, `getById`, `create`, `updateXxx`, `delete` |
| No leaking | `TeamEntity` and `TeamRepository` are package-private |
| No mapping in DTOs | No `from()` or `toDomain()` on DTOs — mappers own all conversions |
| Services in domain | `TeamService` lives in `domain/`, not in `api/` |
| Exceptions | Only `NotFoundException` and `BusinessException` thrown from services |
| Method parameters | Never mutate a parameter — produce and return a new instance via `toBuilder()` |
