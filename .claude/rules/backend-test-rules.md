# Backend Test Rules

## Scope

Unit tests are written for the **`domain` package only** — services and domain logic.  
Infrastructure (`persistence`) and API (`api`) layers are not unit-tested at this stage.

---

## Test Structure

Every unit test follows this exact 5-part structure, each part separated by a blank line and marked with a comment:

```java
@Test
void should_returnTeam_when_teamExists() {
    // setup
    Team team = TeamFakes.aTeam();
    UUID id = team.getId().value();

    // when
    when(teamStore.findById(id)).thenReturn(Optional.of(team));

    // call
    Team result = teamService.findById(id);

    // verify
    verify(teamStore).findById(id);

    // assert
    assertThat(result).isEqualTo(team);
}
```

| Part | Purpose |
|------|---------|
| `// setup` | Build test data using Fakes, prepare inputs |
| `// when` | Stub Mockito mocks with `when(...).thenReturn(...)` |
| `// call` | Invoke the method under test — exactly one call |
| `// verify` | `verify(mock)` for each `when` stub — at minimum one verify per stub |
| `// assert` | Assert on the value returned by the method under test |

---

## Mockito

Use Mockito to mock all dependencies injected into the class under test.

```java
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    TeamStore teamStore;

    @InjectMocks
    TeamService teamService;
}
```

- Use `@ExtendWith(MockitoExtension.class)` — no Spring context.
- One `@Mock` per dependency.
- `@InjectMocks` on the class under test.
- `when(...).thenReturn(...)` for happy path stubs.
- `when(...).thenThrow(...)` for error path stubs.
- At least one `verify(mock).method(...)` per `when` stub.

---

## Fakes

Fakes are `@UtilityClass` classes that build domain objects with sensible defaults.  
One Fakes class per feature, located in the test source tree under the same package as the domain class.

```
src/test/java/abe/fvjc/tournament/
└── team/
    └── domain/
        └── TeamFakes.java
```

```java
// team/domain/TeamFakes.java
@UtilityClass
public class TeamFakes {

    public static Team aTeam() {
        return Team.builder()
            .id(TeamId.of(UUID.randomUUID()))
            .name("Les Aigles")
            .sport(Sport.FOOTBALL)
            .tournamentId(TournamentId.of(UUID.randomUUID()))
            .build();
    }

    public static Team aTeamWithId(UUID id) {
        return aTeam().toBuilder()
            .id(TeamId.of(id))
            .build();
    }

    public static Team aTeamWithTournament(UUID tournamentId) {
        return aTeam().toBuilder()
            .tournamentId(TournamentId.of(tournamentId))
            .build();
    }
}
```

- Default values are realistic but arbitrary — they must compile and be valid domain objects.
- Override specific fields with named variants: `aTeamWithId(...)`, `aTeamWithTournament(...)`.
- Use `toBuilder()` for overrides — never mutate the base object.
- Fakes are only in `src/test/` — never in `src/main/`.

---

## Full Example

```java
@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    TeamStore teamStore;

    @InjectMocks
    TeamService teamService;

    @Test
    void should_returnSavedTeam_when_teamIsCreated() {
        // setup
        Team team = TeamFakes.aTeam();
        Team savedTeam = team.toBuilder().id(TeamId.of(UUID.randomUUID())).build();

        // when
        when(teamStore.save(team)).thenReturn(savedTeam);

        // call
        Team result = teamService.create(team);

        // verify
        verify(teamStore).save(team);

        // assert
        assertThat(result).isEqualTo(savedTeam);
        assertThat(result.getId().isEmpty()).isFalse();
    }

    @Test
    void should_throwNotFoundException_when_teamDoesNotExist() {
        // setup
        UUID id = UUID.randomUUID();

        // when
        when(teamStore.findById(id)).thenReturn(Optional.empty());

        // call + assert (exception case — call and assert are combined)
        assertThatThrownBy(() -> teamService.findById(id))
            .isInstanceOf(NotFoundException.class);

        // verify
        verify(teamStore).findById(id);
    }
}
```

> **Exception tests:** when the method under test throws, `// call` and `// assert` are combined into a single `assertThatThrownBy` block. `// verify` still follows.

---

## Test Method Naming

```
should_[expected outcome]_when_[condition]
```

Examples:
- `should_returnTeam_when_teamExists`
- `should_throwNotFoundException_when_teamDoesNotExist`
- `should_saveTeam_when_teamIsValid`

---

## Rules Summary

| Rule | Detail |
|------|--------|
| Test scope | `domain` package only — services |
| Structure | `setup` → `when` → `call` → `verify` → `assert` (in order, with comments) |
| Mocking | Mockito with `@Mock` + `@InjectMocks` + `@ExtendWith(MockitoExtension.class)` |
| Verify | At least one `verify` per `when` stub |
| Fakes | `@UtilityClass` per feature in `src/test/`, named `XxxFakes` |
| Fake overrides | Use `toBuilder()` named variants — never mutate the base object |
| Assertions | AssertJ (`assertThat`) |
| Test naming | `should_[outcome]_when_[condition]` |
| Exception tests | `assertThatThrownBy` combines call + assert; `verify` still comes after |
