# Backend Architecture Rules

Base package: `abe.fvjc.tournament`

## Package Structure

Layer-first, then feature. Three top-level layers, each containing one sub-package per feature:

```
abe.fvjc.tournament
├── api/
│   ├── tournament/
│   ├── team/
│   └── ...
├── domain/
│   ├── common/
│   │   └── problem/        ← shared domain exceptions
│   ├── tournament/
│   ├── team/
│   └── ...
└── persistence/
    ├── tournament/
    ├── team/
    └── ...
```

Shared domain exceptions live in `domain.common.problem` — never in a `shared` package.

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
| Domain validator | `TeamValidator` |
| Domain create request | `TeamCreateRequest` |
| Domain update request | `TeamUpdateRequest` |
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

---

## Style: Local Variables

Use `final var` for all local variables — type is inferred, reference is immutable.
Use `var` only when the variable is reassigned (rare; prefer `final`).
Method parameters and fields remain explicitly typed.

```java
// CORRECT
final var tournaments = tournamentStore.findAll();
final var entity = new TournamentEntity();

// WRONG
List<Tournament> tournaments = tournamentStore.findAll();
```

### One Operation Per Line

Never nest calls. Break nested operations into named `final var` steps.

```java
// WRONG
return TournamentDbMapper.toTournament(repository.save(TournamentDbMapper.toTournamentEntity(tournament)));

// CORRECT
final var entity = toTournamentEntity(tournament);
final var savedEntity = repository.save(entity);
return toTournament(savedEntity);
```

### Stream Chains

Stream chains: one method call per line, chained with indentation.

```java
return repository.findAll().stream()
        .map(TournamentDbMapper::toTournament)
        .toList();
```

### `.map` Is Pure — Never Throws

A `.map(...)` step (on a `Stream` or `Optional`) performs **simple mapping only** — a pure transformation from one value to another. It must never contain logic that can throw an exception: no validation, no `orElseThrow`, no `findById`, no lookups that can fail, no business rules. Mapping is total (defined for every input).

Anything that can throw — validation, "not found" checks, business invariants — belongs before or after the stream, not inside `.map`.

```java
// WRONG — .map hides a throwing lookup
return teamIds.stream()
        .map(id -> teamStore.findById(id).orElseThrow(() -> new NotFoundException("Team", id)))
        .toList();

// WRONG — .map validates
return requests.stream()
        .map(request -> {
            validateTeamCreateRequest(request);
            return buildTeam(request);
        })
        .toList();

// CORRECT — validate first, then map purely
requests.forEach(TeamValidator::validateTeamCreateRequest);
return requests.stream()
        .map(TeamService::buildTeam)
        .toList();
```

### Ternary Operator

Multi-line ternary: condition on first line, `?` and `:` each on their own line.

```java
final var id = tournament.getId().isEmpty()
        ? UUID.randomUUID()
        : tournament.getId().value();
```

---

## Style: Field Declarations

### No blank line after opening brace

No blank line between the class opening brace `{` and the first field.

```java
// CORRECT
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamStore teamStore;

// WRONG
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamStore teamStore;
```

### Field Order

Fields are declared in this order:
1. Stores — alphabetically by field name
2. Services — alphabetically by field name
3. Others — alphabetically by field name

```java
@RestController
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;
    private final OtherHelper otherHelper;
```

### Bean Naming

Every bean field must be named after its class (camelCase of the class name). Never abbreviate.

```java
// CORRECT
private final TournamentService tournamentService;
private final TournamentRepository tournamentRepository;

// WRONG
private final TournamentService service;
private final TournamentRepository repository;
```

---

## Style: Imports

### No Wildcard Imports

Wildcard imports (`.*`) are **forbidden** — regular and static alike. Every imported type, method, or field is listed explicitly, one per import. This applies to `src/main` and `src/test` equally.

```java
// WRONG
import jakarta.persistence.*;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static abe.fvjc.tournament.api.team.TeamApiMapper.*;

// CORRECT
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamDto;
```

### Static Imports — Public Static Methods

All `public static` methods from utility classes and mappers are used via static import.

```java
import static abe.fvjc.tournament.tournament.api.TournamentApiMapper.toTournamentDto;
import static abe.fvjc.tournament.tournament.api.TournamentApiMapper.toTournamentCreateRequest;
```

Usage in controller:
```java
return toTournamentDto(tournamentService.create(toTournamentCreateRequest(request)));
```

### Static Imports — Enum Values

Enum values are always referenced via static import, not via the enum class name.

```java
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.tournament.domain.Sport.PETANQUE;
```

Usage: `DRAFT`, not `TournamentStatus.DRAFT`.

---

## Domain Layer (`domain/`)

### Domain Classes

Use Lombok `@Value` + `@With` + `@Builder`, **in that order** — all domain objects are immutable. `@With` generates `withFieldName(value)` methods returning a new instance with that field changed — used in tests to override specific fields (e.g., `buildTournament().withId(TournamentId.empty())`). Avoid `@Builder(toBuilder = true)` — build fresh instances instead.

```java
@Value
@With
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

Store methods take **typed ID records** — never raw `UUID`. Every ID parameter uses the typed ID that matches the field it identifies: the entity's own ID for `findById` / `deleteById`, and the foreign-key's typed ID for `findAllByXxxId` (e.g. `TournamentId tournamentId`, `GroupId groupId`). Collections of IDs use `List<XxxId>`, never `List<UUID>`. The `JpaXxxStore` unwraps `.value()` when delegating to the Spring Data repository; callers pass a typed ID (constructed via `XxxId.of(uuid)` when they only hold a raw `UUID`).

```java
public interface TeamStore {
    Team save(Team team);
    Optional<Team> findById(TeamId id);
    List<Team> findAllByTournamentId(TournamentId tournamentId);
    void deleteById(TeamId id);
}
```

### CreateRequest / UpdateRequest

Service methods that accept input use a `XxxCreateRequest` or `XxxUpdateRequest` domain object:
- Lives in `domain/`
- Mirrors the corresponding DTO exactly, but without validation annotations
- Named `XxxCreateRequest` or `XxxUpdateRequest`
- Validated by the domain validator before use

```java
// team/domain/TeamCreateRequest.java
@Value
@Builder
public class TeamCreateRequest {
    String name;
    String sport;
    UUID tournamentId;
}
```

### Validator

Each feature has a `XxxValidator` `@UtilityClass` in the domain package. It handles all domain business rules (cross-field constraints, business invariants). Validators collect all errors before throwing to surface every problem at once.

Method name = `validate` + model + request type: `validateTeamCreateRequest`, `validateTeamUpdateRequest`. Always statically imported at call sites.

```java
@UtilityClass
public class TeamValidator {

    public static void validateTeamCreateRequest(final TeamCreateRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getMaxPlayersPerTeam() < request.getMinPlayersPerTeam()) {
            errors.add(new ValidationException.FieldError("maxPlayersPerTeam",
                "Le nombre maximum de joueurs doit être supérieur ou égal au minimum"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

Static import in service:
```java
import static abe.fvjc.tournament.team.domain.TeamValidator.validateTeamCreateRequest;

validateTeamCreateRequest(request);
```

- `@UtilityClass` — final class, private constructor, all methods static
- Throws `ValidationException` (returns 400 via `GlobalExceptionHandler`)
- Do not call from the API layer — only from service methods

### Service

Services live in `domain/`. They orchestrate store calls but contain no mapping or HTTP logic. Use `@RequiredArgsConstructor` — no explicit constructor. All method parameters are `final`.

```java
@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamStore teamStore;

    public Team create(final TeamCreateRequest request) {
        validateTeamCreateRequest(request);
        return teamStore.save(buildTeam(request));
    }

    private static Team buildTeam(final TeamCreateRequest request) {
        return Team.builder()
            .id(TeamId.of(UUID.randomUUID()))
            .name(request.getName())
            .sport(request.getSport())
            .tournamentId(TournamentId.of(request.getTournamentId()))
            .build();
    }

    public Team findById(final UUID id) {
        return teamStore.findById(TeamId.of(id))
            .orElseThrow(() -> new NotFoundException("Team", id));
    }

    public Team update(final TeamUpdateRequest request, final UUID id) {
        findById(id);
        return teamStore.save(Team.builder()
            .id(TeamId.of(id))
            .name(request.getName())
            .sport(request.getSport())
            .tournamentId(TournamentId.of(request.getTournamentId()))
            .build());
    }

    public void delete(final UUID id) {
        findById(id);
        teamStore.deleteById(TeamId.of(id));
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

---

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
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public Team save(Team team) {
        final var entity = toTeamEntity(team);
        final var savedEntity = teamRepository.save(entity);
        return toTeam(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(TeamId id) {
        return teamRepository.findById(id.value())
                .map(TeamDbMapper::toTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAllByTournamentId(TournamentId tournamentId) {
        return teamRepository.findByTournamentId(tournamentId.value()).stream()
                .map(TeamDbMapper::toTeam)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(TeamId id) {
        teamRepository.deleteById(id.value());
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
        final var entity = new TeamEntity();
        final var id = team.getId().isEmpty()
                ? UUID.randomUUID()
                : team.getId().value();
        entity.setId(id);
        entity.setName(team.getName());
        entity.setSport(team.getSport());
        entity.setTournamentId(team.getTournamentId().value());
        return entity;
    }
}
```

- Method name = `to` + target type: `toTeam(TeamEntity)`, `toTeamEntity(Team)`
- Always use explicit `static` keyword on methods

---

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

A `@RequestBody` parameter of type `XxxRequestDto` (or `XxxCreateRequestDto` / `XxxUpdateRequestDto`) is always named `requestDto` — never `request`. The name `request` is reserved for the mapped domain request object. Endpoints returning a list delegate to the mapper's plural `toXxxDtos(...)` method — never stream inline.

```java
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;

    @GetMapping
    public List<TeamDto> getAll() {
        return toTeamDtos(teamService.findAll());
    }

    @GetMapping("/{id}")
    public TeamDto getById(@PathVariable UUID id) {
        return toTeamDto(teamService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@RequestBody @Valid TeamCreateRequestDto requestDto) {
        return toTeamDto(teamService.create(toTeamCreateRequest(requestDto)));
    }

    @PutMapping("/{id}")
    public TeamDto updateName(@PathVariable UUID id, @RequestBody @Valid TeamUpdateRequestDto requestDto) {
        return toTeamDto(teamService.update(toTeamUpdateRequest(requestDto), id));
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

DTOs carry **structural validation only** (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`). Business rules (cross-field, domain invariants) belong in the domain validator.

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

    static List<TeamDto> toTeamDtos(final List<Team> teams) {
        return emptyIfNull(teams).stream()
            .map(TeamApiMapper::toTeamDto)
            .toList();
    }

    static TeamCreateRequest toTeamCreateRequest(TeamCreateRequestDto dto) {
        return TeamCreateRequest.builder()
            .name(dto.getName())
            .sport(dto.getSport())
            .tournamentId(dto.getTournamentId())
            .build();
    }
}
```

- Method name = `to` + target type: `toTeamDto(Team)`, `toTeamCreateRequest(TeamCreateRequestDto)`
- Always use explicit `static` keyword on methods
- Visibility ladder: `private` → package-private → `protected` → `public` — use the lowest that satisfies callers

#### Collection methods

Every `to` + type mapping that a caller needs for a list has a matching **plural** method named `toXxxDtos`, taking `List<Domain>` and returning `List<Dto>`. It always wraps the source with `emptyIfNull` (from `org.apache.commons.collections4.CollectionUtils`, static import) before streaming, so a `null` collection maps to an empty list instead of throwing.

```java
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

static List<TeamDto> toTeamDtos(final List<Team> teams) {
    return emptyIfNull(teams).stream()
        .map(TeamApiMapper::toTeamDto)
        .toList();
}
```

- **Every** collection streamed inside a mapper — top-level lists and nested lists built inside a `toXxxDto` — goes through `emptyIfNull(...)`. Never call `.stream()` directly on a collection getter.
- A nested list is mapped via its own plural method: `.matches(toBracketMatchDtos(round.getMatches()))`, not an inline stream.
- The plural method takes the same visibility as its singular counterpart (package-private by default; `private` when used only internally).

---

## Common Domain Problems (`domain.common.problem`)

```java
// domain/common/problem/NotFoundException.java
public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
}

// domain/common/problem/BusinessException.java
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

// domain/common/problem/ValidationException.java
public class ValidationException extends RuntimeException {
    private final List<FieldError> errors;

    public ValidationException(List<FieldError> errors) {
        super("Validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public record FieldError(String field, String message) {}
}
```

`ValidationException` is thrown by domain validators and mapped to `400 Bad Request` by `GlobalExceptionHandler`.

---

## Method Parameters

All method parameters are declared `final`. Never reassign or mutate a parameter inside a method. If a modified version is needed, produce a new instance and return it.

```java
public Tournament create(final TournamentCreateRequest request) {
    validateTournamentCreateRequest(request);
    return tournamentStore.save(buildTournament(request));
}

public Tournament findById(final UUID id) {
    return tournamentStore.findById(TournamentId.of(id))
        .orElseThrow(() -> new NotFoundException("Tournament", id));
}
```

---

## Rules Summary

| Rule | Detail |
|---|------|
| Immutability | Domain classes, value objects, and DTOs use `@Value` + `@Builder` |
| No `toBuilder` | Build fresh instances with the full builder — never use `toBuilder()` |
| DTOs | Always add `@Jacksonized` alongside `@Value` + `@Builder` |
| DTO validation | Structural only (`@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Size`) — no business rules |
| Domain validator | `XxxValidator` `@UtilityClass` in `domain/` — throws `ValidationException` |
| Validator method naming | `validateXxxCreateRequest` / `validateXxxUpdateRequest` — statically imported at call sites |
| CreateRequest | `XxxCreateRequest` in `domain/` — mirrors DTO fields, no validation annotations |
| `buildXxx` in service | Domain object construction extracted into a `private static buildXxx(XxxCreateRequest)` method |
| JPA entities | `@Getter` + `@Setter` + `@NoArgsConstructor`, package-private |
| Typed IDs | Every entity has a typed ID record with `.of()` and `.empty()` |
| Store ID parameters | Store methods take typed ID records — never raw `UUID`; collections use `List<XxxId>`. `JpaXxxStore` unwraps `.value()`; callers wrap raw UUIDs via `XxxId.of(uuid)` |
| `@RequiredArgsConstructor` | Used on services, controllers, and store implementations — no explicit constructors |
| `@Transactional` | Per method on `JpaXxxStore` only — never on the class, never on services or controllers; use `readOnly = true` on reads |
| Mappers | `@UtilityClass` with explicit `static` methods, named `to` + target type |
| Mapper visibility | Minimum necessary: `private` → package-private → `protected` → `public` |
| API mapper | `TeamApiMapper` in `api/`, public class, methods package-private by default |
| Mapper collection methods | Every list mapping has a plural `toXxxDtos(List<Domain>)` method; every streamed collection (top-level and nested) is wrapped in `emptyIfNull(...)` — never `.stream()` on a raw getter |
| Persistence mapper | `TeamDbMapper` in `persistence/`, package-private class and methods |
| Controller methods | Always `public`; named by HTTP verb: `getAll`, `getById`, `create`, `updateXxx`, `delete` |
| `@RequestBody` naming | A `XxxRequestDto` body parameter is named `requestDto` — never `request` (reserved for the mapped domain request) |
| Controller list returns | Delegate to the mapper's plural `toXxxDtos(...)` — never stream inline in the controller |
| No leaking | `TeamEntity` and `TeamRepository` are package-private |
| No mapping in DTOs | No `from()` or `toDomain()` on DTOs — mappers own all conversions |
| Services in domain | `TeamService` lives in `domain/`, not in `api/` |
| Exceptions | `NotFoundException`, `BusinessException`, `ValidationException` thrown from services/validators |
| Method parameters | Always `final` — never mutate a parameter, produce and return a new instance |
| `@With` on domain classes | `@Value @With @Builder` (in that order) — enables `obj.withField(value)` for test overrides without `toBuilder` |
| `final var` | All local variables use `final var`; use `var` only when reassigned |
| One operation per line | Never nest calls — break into `final var` steps |
| Stream chains | One method per line, chained with indentation |
| `.map` is pure | `.map(...)` does simple mapping only — never throws (no validation, `orElseThrow`, failing lookups, or business rules); do those before/after the stream |
| Ternary | Condition on line 1; `?` and `:` each on their own line |
| No blank line after `{` | First field immediately follows the class opening brace |
| Field order | Stores → Services → Others, each group alphabetical |
| Bean naming | Field name = camelCase of class name (no abbreviation) |
| Static imports | `public static` methods and enum values always imported statically |
| No wildcard imports | `.*` imports forbidden (regular and static) — list every type/method explicitly, in `src/main` and `src/test` |
