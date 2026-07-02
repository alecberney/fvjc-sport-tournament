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

For exception tests, assign `assertThrows` to a `var` and assert on the exception — `verify(...)` comes after:

```java
@Test
void findByIdWhenNotFoundShouldThrowNotFoundException() {
    final var id = IdGenerator.tournamentId().value();

    when(tournamentStore.findById(id)).thenReturn(Optional.empty());

    final var exception = assertThrows(NotFoundException.class, () -> tournamentService.findById(id));

    verify(tournamentStore).findById(id);

    assertEquals("Tournament not found with id: " + id, exception.getMessage());
}
```

---

## Fakes

Fakes are `@UtilityClass` classes that build domain objects with sensible defaults.  
One Fakes class per feature, located in the test source tree under the same package as the domain class.

```
src/test/java/abe/fvjc/tournament/
└── team/
    └── domain/
        ├── IdGenerator.java
        ├── TeamFakes.java
        └── TeamServiceTest.java
```

### Naming

Fakes methods are prefixed with `build`:

```java
TeamFakes.buildTeam()
TeamFakes.buildCreateRequest()
```

### IdGenerator

Each feature has a package-private `IdGenerator` `@UtilityClass` in its test domain package. It generates typed ID instances for use in fakes only. All methods are package-private.

```java
// team/domain/IdGenerator.java
@UtilityClass
class IdGenerator {
    static TeamId teamId() {
        return TeamId.of(UUID.randomUUID());
    }
}
```

- Package-private — only Fakes and tests in the same package can use it.
- Generates a new random UUID-based ID on every call.
- Never pass a UUID from outside — IDs are always generated internally.

### Fakes Structure

All `buildXxx()` methods for domain objects include a non-empty ID generated via `IdGenerator`. There is no separate `buildXxxWithId()` variant — the base method always has an ID.

To get a domain object with an empty ID in a test, use Lombok `@With`:

```java
TournamentFakes.buildTournament().withId(TournamentId.empty())
```

```java
// team/domain/TeamFakes.java
@UtilityClass
public class TeamFakes {

    public static Team buildTeam() {
        return Team.builder()
            .id(IdGenerator.teamId())
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
| Mocking | Mockito with `@Mock` + `@InjectMocks` + `@ExtendWith(MockitoExtension.class)` |
| `@Mock` visibility | `@Mock private` — all mock fields are `private` |
| `@InjectMocks` visibility | `private` — the class under test field is `private` |
| No `ArgumentCaptor` | Banned — use `returnsFirstArg()` and assert on the return value |
| Save stub | `when(store.save(any(X.class))).then(returnsFirstArg())` |
| Verify | At least one `verify` per `when` stub |
| Verify / assert separation | Blank line between the verify block and the assert block |
| Result variable naming | Named `<model><Action>`: `tournamentCreated`, `tournamentFound` |
| Fake variable naming | Named after the model: `tournament`, `request` — not `saved`, `tournamentWithId` |
| Fakes | `@UtilityClass` per feature in `src/test/`, named `XxxFakes` |
| Fakes method naming | Prefix `build`: `buildTeam()`, `buildCreateRequest()` |
| Fakes always have ID | `buildXxx()` always uses `IdGenerator.xxxId()` — no `buildXxxWithId()` variant |
| Empty ID in tests | Use Lombok `@With`: `TournamentFakes.buildTournament().withId(TournamentId.empty())` |
| IdGenerator | Package-private `@UtilityClass` per feature — generates typed IDs, used only by Fakes/tests |
| No `toBuilder` | Use Lombok `@With` for field overrides — never `toBuilder()` |
| Static imports | Enum values, Fakes methods, and `IdGenerator` methods always statically imported in tests |
| `final var` | All local variables use `final var` |
