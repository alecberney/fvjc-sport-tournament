# Backend Test Rules

## Scope

Unit tests are written for the **`domain` package only** — services and domain logic.  
Infrastructure (`persistence`) and API (`api`) layers are not unit-tested at this stage.

---

## Test Structure

Test methods have no structural comments — code flow speaks for itself. A test has five natural sections:

1. Test data setup (fakes)
2. Mock stubs (`when(...).thenReturn(...)`)
3. Method call under test — result named `<model><Action>` (e.g., `tournamentCreated`, `tournamentFound`)
4. Verify block
5. *(blank line)*
6. Assert block

```java
@Test
void createWhenValidRequestShouldSetDraftStatus() {
    final var request = TournamentFakes.buildCreateRequest();

    when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

    final var tournamentCreated = tournamentService.create(request);

    verify(tournamentStore).save(any(Tournament.class));

    assertEquals(DRAFT, tournamentCreated.getStatus());
}
```

---

## Test Method Naming

```
methodTestedNameWhenXxxxShouldXxx
```

Examples:
- `createWhenValidRequestShouldSetDraftStatus`
- `findByIdWhenNotFoundShouldThrowNotFoundException`
- `findByIdWhenExistsShouldReturnTournament`
- `deleteWhenExistsShouldCallStore`

---

## Mockito

Use Mockito to mock all dependencies injected into the class under test.

```java
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {
    @Mock
    private TeamStore teamStore;

    @InjectMocks
    private TeamService teamService;
}
```

- Use `@ExtendWith(MockitoExtension.class)` — no Spring context.
- `@Mock` fields are `private`.
- `@InjectMocks` field is `private`.
- One `@Mock` per dependency.
- `@InjectMocks` on the class under test.
- `when(...).thenReturn(...)` for happy path stubs that return a specific value.
- `when(...).then(returnsFirstArg())` for store `save` stubs — returns the argument passed in, enabling assertions on the result without a captor.
- `when(...).thenThrow(...)` for error path stubs.
- At least one `verify(mock).method(...)` per `when` stub.
- **`ArgumentCaptor` is banned** — use `returnsFirstArg()` and assert on the return value instead.

### Save stub pattern

```java
when(teamStore.save(any(Team.class))).then(returnsFirstArg());

final var teamCreated = teamService.create(request);

verify(teamStore).save(any(Team.class));

assertEquals("Les Aigles", teamCreated.getName());
assertEquals(FOOTBALL, teamCreated.getSport());
```

---

## Assertions

Use **JUnit 5 assertions only** — never AssertJ.

```java
import static org.junit.jupiter.api.Assertions.*;

assertEquals(expected, actual);
assertNotNull(value);
assertTrue(condition);
assertFalse(condition);
assertThrows(ExceptionClass.class, () -> methodUnderTest());
```

`assertThrows` is **never** used bare — always capture its return into `final var exception` and assert on it. A test that only checks the thrown type is incomplete. `verify(...)` comes between the capture and the final assertion.

```java
@Test
void findByIdWhenNotFoundShouldThrowNotFoundException() {
    final var id = UUID.randomUUID();

    when(tournamentStore.findById(id)).thenReturn(Optional.empty());

    final var exception = assertThrows(NotFoundException.class, () -> tournamentService.findById(id));

    verify(tournamentStore).findById(id);

    assertEquals("Tournament not found with id: " + id, exception.getMessage());
}
```

What to assert on the captured exception, by type:

- `NotFoundException` — `assertTrue(exception.getMessage().contains("<Entity>"))`.
- `ConflictException` / `BusinessException` — `assertEquals("<exact message>", exception.getMessage())`.
- `ValidationException` — assert the offending field: `assertEquals("<field>", exception.getErrors().getFirst().field())`.

---

## Fakes

Fakes are `@UtilityClass` classes that build domain objects with sensible defaults.  
All Fakes live together in a single **`domain.fakes`** package (`src/test/java/abe/fvjc/tournament/domain/fakes/`) — one `XxxFakes` class per feature, never colocated with the tests that use them. Fakes classes are `public` so any test can import them.

```
src/test/java/abe/fvjc/tournament/domain/
├── fakes/
│   ├── IdGenerator.java      ← the single, shared IdGenerator
│   ├── BracketFakes.java
│   ├── GroupFakes.java
│   ├── OrganisationFakes.java
│   ├── PersonFakes.java
│   ├── ScheduleFakes.java
│   ├── TeamFakes.java
│   └── TournamentFakes.java
├── team/
│   └── TeamServiceTest.java  ← tests import fakes from domain.fakes
└── ...
```

### Naming

Fakes methods are prefixed with `build`:

```java
TeamFakes.buildTeam()
TeamFakes.buildCreateRequest()
```

### IdGenerator

There is exactly **one** `IdGenerator` for the whole test suite, a package-private `@UtilityClass` living in the `domain.fakes` package alongside the Fakes. It generates typed ID instances for use by the Fakes only, with one package-protected (package-private) `static` method per typed ID. Each method is named `random` + the ID type: `randomTeamId()`, `randomTournamentId()`, `randomGroupId()`, …

```java
// domain/fakes/IdGenerator.java
@UtilityClass
class IdGenerator {

    static TeamId randomTeamId() {
        return TeamId.of(UUID.randomUUID());
    }

    static TournamentId randomTournamentId() {
        return TournamentId.of(UUID.randomUUID());
    }
    // ... one method per typed ID (randomGroupId, randomMatchId, randomBracketRoundId, ...)
}
```

- Method naming: `random` + typed ID name — `randomTeamId()`, `randomMatchId()`, `randomBracketRoundId()`.
- Package-private class with package-protected methods — usable only by the Fakes in the same `domain.fakes` package, never by tests directly.
- One shared generator for all features — never a per-feature `IdGenerator`.
- Generates a new random UUID-based ID on every call.
- Never pass a UUID from outside — IDs are always generated internally.

### Fakes Structure

All `buildXxx()` methods for domain objects include a non-empty ID generated via `IdGenerator`. There is no separate `buildXxxWithId()` variant — the base method always has an ID.

To get a domain object with an empty ID in a test, use Lombok `@With`:

```java
TournamentFakes.buildTournament().withId(TournamentId.empty())
```

```java
// domain/fakes/TeamFakes.java
@UtilityClass
public class TeamFakes {

    public static Team buildTeam() {
        return Team.builder()
            .id(randomTeamId())
            .name("Les Aigles")
            .sport(FOOTBALL)
            .tournamentId(TournamentId.of(UUID.randomUUID()))
            .build();
    }

    public static TeamCreateRequest buildCreateRequest() {
        return TeamCreateRequest.builder()
            .name("Les Aigles")
            .sport(FOOTBALL)
            .build();
    }
}
```

- `buildXxx()` — base factory with hardcoded realistic defaults and a generated ID.
- `buildCreateRequest()` — builds the domain request object for service tests.
- `buildXxxWith[Field](value)` — named variant for a specific test scenario.
- Enum values are always statically imported.
- Never use `toBuilder()` — use Lombok `@With` (`obj.withField(value)`) for field overrides.
- Fakes are only in `src/test/` — never in `src/main/`.

### Variable Naming

Variables built from fakes are named after the model, not the role they play:

```java
// CORRECT
final var tournament = TournamentFakes.buildTournament();
final var request = TournamentFakes.buildCreateRequest();

// WRONG
final var saved = TournamentFakes.buildTournament();
final var tournamentWithId = TournamentFakes.buildTournament();
```

The result of the method under test is named `<model><Action>`:

```java
final var tournamentCreated = tournamentService.create(request);
final var tournamentFound   = tournamentService.findById(id);
```

---

## Rules Summary

| Rule | Detail |
|------|--------|
| Test scope | `domain` package only — services |
| Test naming | `methodTestedNameWhenXxxxShouldXxx` |
| No comments | No `// setup`, `// when`, `// call`, `// verify`, `// assert` comments |
| Assertions | JUnit 5 only — never AssertJ |
| `assertThrows` | Never bare — capture into `final var exception` and assert on it (message for `NotFoundException`/`ConflictException`/`BusinessException`, `getErrors().getFirst().field()` for `ValidationException`) |
| Mocking | Mockito with `@Mock` + `@InjectMocks` + `@ExtendWith(MockitoExtension.class)` |
| `@Mock` visibility | `@Mock private` — all mock fields are `private` |
| `@InjectMocks` visibility | `private` — the class under test field is `private` |
| No `ArgumentCaptor` | Banned — use `returnsFirstArg()` and assert on the return value |
| Save stub | `when(store.save(any(X.class))).then(returnsFirstArg())` |
| Verify | At least one `verify` per `when` stub |
| Verify / assert separation | Blank line between the verify block and the assert block |
| Result variable naming | Named `<model><Action>`: `tournamentCreated`, `tournamentFound` |
| Fake variable naming | Named after the model: `tournament`, `request` — not `saved`, `tournamentWithId` |
| Fakes location | All `XxxFakes` in the single `domain.fakes` package (`src/test/`), never colocated with tests; `public @UtilityClass` |
| Fakes method naming | Prefix `build`: `buildTeam()`, `buildCreateRequest()` |
| Fakes always have ID | `buildXxx()` always uses `IdGenerator.randomXxxId()` — no `buildXxxWithId()` variant |
| Empty ID in tests | Use Lombok `@With`: `TournamentFakes.buildTournament().withId(TournamentId.empty())` |
| IdGenerator | Exactly one package-private `@UtilityClass` in `domain.fakes` with package-protected `random<TypedId>()` methods (e.g. `randomTeamId()`) — never per-feature, used only by Fakes |
| No `toBuilder` | Use Lombok `@With` for field overrides — never `toBuilder()` |
| Static imports | Enum values and Fakes methods statically imported in tests; `IdGenerator` methods statically imported within the Fakes |
| `final var` | All local variables use `final var` |
