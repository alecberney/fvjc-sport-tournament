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
- A blank line follows the opening brace and separates each method from the next — one blank line between methods, never packed together.

### Store Interface

Repository interfaces belong to the domain — no Spring or JPA imports.

Store methods take **typed ID records** — never raw `UUID`. Every ID parameter uses the typed ID that matches the field it identifies: the entity's own ID for `findById` / `deleteById`, and the foreign-key's typed ID for `findAllByXxxId` (e.g. `TournamentId tournamentId`, `GroupId groupId`). Collections of IDs use `List<XxxId>`, never `List<UUID>`. The `JpaXxxStore` unwraps `.value()` when delegating to the Spring Data repository; callers pass a typed ID (constructed via `XxxId.of(uuid)` when they only hold a raw `UUID`).

**One blank line separates each method** — store methods are never packed together.

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
    private final TeamSearchService teamSearchService;
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

    public Team update(final TeamUpdateRequest request, final TeamId id) {
        teamSearchService.findById(id);
        return teamStore.save(buildTeam(request, id));
    }

    public void delete(final TeamId id) {
        teamSearchService.findById(id);
        teamStore.deleteById(id);
    }
}
```

### Service Methods Take Typed IDs — Never Raw `UUID`

Every service method — **public and private alike** — that receives an entity identifier takes the matching **typed ID record** (`TournamentId`, `TeamId`, `MatchId`, …), never a raw `UUID`. This holds for the entity's own id (`findById`, `delete`, `start`) and for foreign-key ids (`registerTeams(TournamentId tournamentId, …)`, `buildTeam(TournamentId tournamentId, …)`, `toMatchOverview(Match match, TournamentId tournamentId)`). A raw `UUID` only appears where an id is *created* (`XxxId.of(UUID.randomUUID())`) or as an internal `Map`/`Set` key derived via `.value()`.

```java
// WRONG — service method receives a raw UUID
public List<TeamOverview> registerTeams(final UUID tournamentId, final TeamRegisterRequest request) { ... }
private static Team buildTeam(final UUID tournamentId, ...) { ... }

// CORRECT — typed ID everywhere
public List<TeamOverview> registerTeams(final TournamentId tournamentId, final TeamRegisterRequest request) { ... }
private static Team buildTeam(final TournamentId tournamentId, ...) { ... }
```

Because service signatures use typed IDs, the store call no longer wraps: `teamStore.findAllByTournamentId(tournamentId)`, not `teamStore.findAllByTournamentId(TournamentId.of(tournamentId))`.

**The controller performs the `UUID` → typed-ID conversion**, inline in the service call, since a `@PathVariable` binds as a raw `UUID`:

```java
@GetMapping("/{id}")
public TournamentDto getById(@PathVariable UUID id) {
    final var tournament = tournamentService.findById(TournamentId.of(id));
    return toTournamentDto(tournament);
}
```

`XxxId.of(id)` is treated as a trivial wrapper, not a data-fetch/transform step, so it stays inline as the argument rather than taking its own `final var`. A raw `UUID` held in a domain request object (e.g. `GroupSwapRequest.getTeamId1()`) is likewise wrapped inline at the call site: `teamSearchService.findById(TeamId.of(request.getTeamId1()))`.

### Builders in Services — `buildXxx` vs `toXxx`

A `Xxx.builder()...build()` call is **never left inline** inside a service method (including inside a stream, a lambda, or a `store.save(...)` argument). Every builder is extracted into its own method, named by how its result is used:

- **`buildXxx` — the object is persisted or created.** When the built object is passed to `store.save(...)` / `store.saveAll(...)` (directly or after `.withXxx(...)`), extract a **`private static buildXxx(...)`** method that returns the freshly built object. Generated IDs (`XxxId.of(UUID.randomUUID())`) live inside this method.
- **`toXxx` — the object is a mapping returned to the caller.** When the builder maps a domain object (or a set of them) into a view/overview model that the method returns, extract a mapping method named **`toXxx`** (target type in camelCase — `RoundOverview` → `toRoundOverview`, `TeamOverview` → `toTeamOverview`). Make it **`private static`** when it needs no stores (a pure mapping); keep it a **`private` instance** method when it must query a store to assemble the result.

A builder that is neither persisted nor a mapping of a domain object (e.g. a value object computed from primitives) may stay in its enclosing computation method — the rule targets persistence builders and mapping builders.

```java
// buildXxx — result is saved
private static Round buildRound(final UUID tournamentId, final RoundId roundId, final int number, final LocalDateTime startTime) {
    return Round.builder()
            .id(roundId)
            .tournamentId(TournamentId.of(tournamentId))
            .number(number)
            .startTime(startTime)
            .build();
}

rounds.add(buildRound(tournamentId, roundId, r + 1, roundStart));
roundStore.saveAll(rounds);

// toXxx — result is a mapping returned to the caller (pure → static)
private static RoundOverview toRoundOverview(final Round round, final List<MatchOverview> matches) {
    return RoundOverview.builder()
            .id(round.getId())
            .number(round.getNumber())
            .startTime(round.getStartTime())
            .matches(matches)
            .build();
}

// toXxx — mapping that must query a store (instance, not static)
private GroupOverview toGroupOverview(final Group group) {
    final var teams = teamStore.findAllByGroupId(group.getId());
    return GroupOverview.builder()
            .id(group.getId())
            .name(group.getName())
            .tournamentId(group.getTournamentId())
            .teams(teams)
            .build();
}
```

### Search Service (find-by-id)

Looking up a single entity by its id — `store.findById(...).orElseThrow(() -> new NotFoundException("Xxx", id))` — is **never inlined** in a service. It lives in exactly one place: a dedicated `XxxSearchService` in the entity's domain package.

```java
@Service
@RequiredArgsConstructor
public class TeamSearchService {
    private final TeamStore teamStore;

    public Team findById(final TeamId teamId) {
        return teamStore.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team", teamId.value()));
    }
}
```

- One `XxxSearchService` per entity that needs a find-or-throw lookup — `@Service` + `@RequiredArgsConstructor`, in `domain/xxx/`.
- Its only dependency is that entity's store. It never depends on another service, so it can be injected anywhere without creating a circular dependency.
- The single public method is `findById(XxxId id)` — it takes the **typed ID**, returns the domain object, and throws `NotFoundException("Xxx", id.value())` when absent (the `.value()` keeps the message showing the raw UUID). Callers pass a typed id directly; a caller holding a raw `UUID` wraps it inline via `XxxId.of(uuid)`.
- Any service (including the entity's own `XxxService`) that needs the entity by id **injects the `XxxSearchService` and calls `findById`** instead of reaching into the store. Since the lookup is always consumed from another class, the method is always `public`.
- The not-found behaviour is unit-tested once, in `XxxSearchServiceTest`. Consuming-service tests mock the `XxxSearchService` and do **not** re-test the not-found path.

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

#### One named `final var` per step — never nest calls

Every call in a handler that **gets or transforms data** — mapping a `requestDto` to a domain request, invoking the service, or any intermediate step — is assigned to its own named `final var`. Never nest these calls inside one another. Only the terminal DTO mapping stays inline in the `return`; every input to it is already a named variable.

Variables are named after what they hold: the mapped request is `request`, the service result is named after the model (`teams`, `tournament`, `schedule`).

```java
// WRONG — nested calls
return toTeamDtos(teamService.registerTeams(tournamentId, toTeamRegisterRequest(requestDto)));

// CORRECT — one named var per step
final var request = toTeamRegisterRequest(requestDto);
final var teams = teamService.registerTeams(tournamentId, request);
return toTeamDtos(teams);
```

A handler that only calls the service (no request mapping) still names the service result before mapping it:

```java
// WRONG
return toTeamDto(teamService.findById(id));

// CORRECT
final var team = teamService.findById(id);
return toTeamDto(team);
```

```java
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;

    @GetMapping
    public List<TeamDto> getAll() {
        final var teams = teamService.findAll();
        return toTeamDtos(teams);
    }

    @GetMapping("/{id}")
    public TeamDto getById(@PathVariable UUID id) {
        final var team = teamService.findById(id);
        return toTeamDto(team);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamDto create(@RequestBody @Valid TeamCreateRequestDto requestDto) {
        final var request = toTeamCreateRequest(requestDto);
        final var team = teamService.create(request);
        return toTeamDto(team);
    }

    @PutMapping("/{id}")
    public TeamDto updateName(@PathVariable UUID id, @RequestBody @Valid TeamUpdateRequestDto requestDto) {
        final var request = toTeamUpdateRequest(requestDto);
        final var team = teamService.update(request, id);
        return toTeamDto(team);
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

**One validation annotation per line.** Each validation annotation goes on its own line above the field it constrains — never inline with the field, never several annotations sharing a line. A blank line separates each field from the next. This applies to every request DTO field that carries at least one validation annotation.

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
    @NotBlank
    String name;

    @NotBlank
    String sport;

    @NotNull
    UUID tournamentId;
}
```

```java
// WRONG — annotations inline / sharing a line, no blank line between fields
public class SubmitMatchResultRequestDto {
    @NotNull @Min(0) @Max(500) Integer score1;
    @NotNull @Min(0) @Max(500) Integer score2;
}

// CORRECT — one annotation per line, blank line between fields
public class SubmitMatchResultRequestDto {
    @NotNull
    @Min(0)
    @Max(500)
    Integer score1;

    @NotNull
    @Min(0)
    @Max(500)
    Integer score2;
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

public Tournament findById(final TournamentId id) {
    return tournamentSearchService.findById(id);
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
| DTO field formatting | One validation annotation per line, above the field; a blank line separates each field — never inline or several annotations on one line |
| Domain validator | `XxxValidator` `@UtilityClass` in `domain/` — throws `ValidationException` |
| Validator method naming | `validateXxxCreateRequest` / `validateXxxUpdateRequest` — statically imported at call sites |
| CreateRequest | `XxxCreateRequest` in `domain/` — mirrors DTO fields, no validation annotations |
| `buildXxx` in service | A builder whose result is persisted (`store.save`/`saveAll`) is extracted into a `private static buildXxx(...)` method — never left inline |
| `toXxx` in service | A builder that maps a domain object into a returned view/overview model is extracted into a `toXxx` mapping method — `private static` when pure, `private` instance when it queries a store |
| JPA entities | `@Getter` + `@Setter` + `@NoArgsConstructor`, package-private |
| Typed IDs | Every entity has a typed ID record with `.of()` and `.empty()` |
| Typed ID spacing | Blank line after the opening brace and one blank line between each method — never packed together |
| Store ID parameters | Store methods take typed ID records — never raw `UUID`; collections use `List<XxxId>`. `JpaXxxStore` unwraps `.value()`; callers wrap raw UUIDs via `XxxId.of(uuid)` |
| Store method spacing | One blank line between each method in the store interface — never packed together |
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
| Controller named steps | Every get/transform call gets its own named `final var` (`request`, then the model result) — never nest calls; only the terminal DTO mapping stays inline in the `return` |
| No leaking | `TeamEntity` and `TeamRepository` are package-private |
| No mapping in DTOs | No `from()` or `toDomain()` on DTOs — mappers own all conversions |
| Services in domain | `TeamService` lives in `domain/`, not in `api/` |
| Search service | Find-by-id (`findById(XxxId)` + `NotFoundException`) lives only in a dedicated `XxxSearchService` (one dependency: the store); takes the typed ID and throws `NotFoundException("Xxx", id.value())`; services inject it instead of inlining `store.findById(...).orElseThrow(...)`; not-found tested once in `XxxSearchServiceTest` |
| Service ID parameters | Every service method (public and private) receiving an entity id takes the matching typed ID record — never raw `UUID`; store calls no longer wrap (`teamStore.findAllByTournamentId(tournamentId)`). Raw `UUID` appears only at id creation (`XxxId.of(UUID.randomUUID())`) or internal `Map`/`Set` keys |
| Controller ID conversion | The controller wraps `@PathVariable UUID` into the typed ID inline in the service call (`tournamentService.findById(TournamentId.of(id))`) — `XxxId.of(id)` is a trivial wrapper, not a data step, so it stays inline |
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
