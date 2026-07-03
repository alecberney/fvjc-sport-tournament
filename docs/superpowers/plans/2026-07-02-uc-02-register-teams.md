# UC-02 Register Teams — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add team registration, listing, editing, deletion, and paid-marking to a DRAFT tournament, behind a new tournament detail page.

**Architecture:** `Organisation` and `Team` are two new aggregates managed by `TeamService`; `Organisation` has no standalone API — its lifecycle is driven entirely by team operations. A `TeamView` record joins `Team` + `Organisation` data for API responses, avoiding denormalization on the domain object. The Angular frontend adds a tournament detail page with `TeamState` (NGXS) and a suite of dialogs.

**Tech Stack:** Java 21, Spring Boot 4.1, Lombok, Liquibase, SQLite/H2, JUnit 5, Mockito — Angular 21, NGXS, Angular Material, Reactive Forms.

## Global Constraints

- All local Java variables: `final var`. Method parameters: `final`.
- No nested calls — break into `final var` steps.
- Domain packages: `abe.fvjc.tournament.<feature>.<layer>` (e.g. `abe.fvjc.tournament.team.domain`).
- Directory structure layer-first: `api/team/`, `domain/team/`, `persistence/team/`.
- Shared exceptions: package `abe.fvjc.tournament.shared.exception`.
- JPA entities and Spring Data repos are package-private.
- `@Transactional` per method on `JpaXxxStore` only.
- Request DTOs: `@Value @Builder @JsonDeserialize(builder = ...)` + inner `@JsonPOJOBuilder(withPrefix = "")` class.
- Response DTOs: `@Value @Builder @Jacksonized`.
- Test assertions: JUnit 5 only. No `ArgumentCaptor`. No AssertJ.
- Frontend: `inject()` everywhere. All components standalone. `patchState` only.
- Frontend imports: `@app/` alias always.
- Validation error messages: verbatim from spec.

---

### Task 1: ConflictException and 409 handler

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/common/problem/ConflictException.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/common/problem/ConflictErrorResponse.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/common/problem/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `ConflictException(String message)` in package `abe.fvjc.tournament.shared.exception`

- [ ] **Step 1: Create ConflictException**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/common/problem/ConflictException.java
package abe.fvjc.tournament.shared.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(final String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create ConflictErrorResponse**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/common/problem/ConflictErrorResponse.java
package abe.fvjc.tournament.shared.exception;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictErrorResponse {
    String error;
}
```

- [ ] **Step 3: Add handler to GlobalExceptionHandler**

Add this method to `GlobalExceptionHandler` (after the existing `handleNotFound` method):

```java
@ExceptionHandler(ConflictException.class)
public ResponseEntity<ConflictErrorResponse> handleConflict(ConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ConflictErrorResponse.builder().error(ex.getMessage()).build());
}
```

Also add this import at the top:
```java
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.ConflictErrorResponse;
```

- [ ] **Step 4: Run backend tests to confirm no regressions**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/common/problem/
git commit -m "BE - Add ConflictException and 409 handler"
```

---

### Task 2: Organisation domain layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/organisation/OrganisationId.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/organisation/Organisation.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/organisation/OrganisationStore.java`

**Interfaces:**
- Produces: `OrganisationId`, `Organisation`, `OrganisationStore` in package `abe.fvjc.tournament.organisation.domain`

- [ ] **Step 1: Create OrganisationId**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/organisation/OrganisationId.java
package abe.fvjc.tournament.organisation.domain;

import java.util.UUID;

public record OrganisationId(UUID value) {

    public static OrganisationId of(final UUID value) {
        return new OrganisationId(value);
    }

    public static OrganisationId empty() {
        return new OrganisationId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 2: Create Organisation**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/organisation/Organisation.java
package abe.fvjc.tournament.organisation.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Organisation {
    OrganisationId id;
    String responsibleFirstName;
    String responsibleLastName;
    TournamentId tournamentId;
}
```

- [ ] **Step 3: Create OrganisationStore**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/organisation/OrganisationStore.java
package abe.fvjc.tournament.organisation.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationStore {
    Organisation save(Organisation organisation);
    Optional<Organisation> findById(UUID id);
    void deleteById(UUID id);
}
```

- [ ] **Step 4: Run backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/organisation/
git commit -m "BE - Add Organisation domain layer"
```

---

### Task 3: Team domain layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamId.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/Team.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamRegisterRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamUpdateRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java`

**Interfaces:**
- Produces: all types in package `abe.fvjc.tournament.team.domain`

- [ ] **Step 1: Create TeamId**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamId.java
package abe.fvjc.tournament.team.domain;

import java.util.UUID;

public record TeamId(UUID value) {

    public static TeamId of(final UUID value) {
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

- [ ] **Step 2: Create Team**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/Team.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Team {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    TournamentId tournamentId;
}
```

- [ ] **Step 3: Create TeamStore**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamStore.java
package abe.fvjc.tournament.team.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamStore {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findAllByTournamentId(UUID tournamentId);
    void deleteById(UUID id);
    long countByOrganisationId(UUID organisationId);
}
```

- [ ] **Step 4: Create TeamRegisterRequest**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamRegisterRequest.java
package abe.fvjc.tournament.team.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeamRegisterRequest {
    String name;
    String responsibleFirstName;
    String responsibleLastName;
    int count;
    List<Boolean> paid;
}
```

- [ ] **Step 5: Create TeamUpdateRequest**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamUpdateRequest.java
package abe.fvjc.tournament.team.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamUpdateRequest {
    String name;
    String responsibleFirstName;
    String responsibleLastName;
    boolean paid;
}
```

- [ ] **Step 6: Create TeamView**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamView {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    String responsibleFirstName;
    String responsibleLastName;
    TournamentId tournamentId;
}
```

- [ ] **Step 7: Run tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/team/
git commit -m "BE - Add Team domain layer"
```

---

### Task 4: TeamValidator

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamValidator.java`

**Interfaces:**
- Produces: `validateTeamRegisterRequest(TeamRegisterRequest)` and `validateTeamUpdateRequest(TeamUpdateRequest)` — both `public static void`, package `abe.fvjc.tournament.team.domain`

- [ ] **Step 1: Create TeamValidator**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamValidator.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class TeamValidator {

    public static void validateTeamRegisterRequest(final TeamRegisterRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getPaid() != null && request.getPaid().size() != request.getCount()) {
            errors.add(new ValidationException.FieldError("paid",
                "Le nombre de flags de paiement doit correspondre au nombre d'équipes"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static void validateTeamUpdateRequest(final TeamUpdateRequest request) {
        // Structural validation is handled by @Valid on the DTO.
        // No additional business-rule validation required for update.
    }
}
```

- [ ] **Step 2: Run tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/team/TeamValidator.java
git commit -m "BE - Add TeamValidator"
```

---

### Task 5: TeamService and unit tests (TDD)

**Files:**
- Create: `backend/src/test/java/abe/fvjc/tournament/team/domain/IdGenerator.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/team/domain/TeamFakes.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/team/domain/TeamServiceTest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java`

**Interfaces:**
- Consumes: `OrganisationStore`, `TeamStore`, `TournamentStore`, `ConflictException`, `NotFoundException`, `TeamValidator`
- Produces: `TeamService` in package `abe.fvjc.tournament.team.domain` with methods: `registerTeams`, `findAllByTournamentId`, `updateTeam`, `deleteTeam`, `markPaid`

- [ ] **Step 1: Create IdGenerator**

```java
// backend/src/test/java/abe/fvjc/tournament/team/domain/IdGenerator.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static TeamId teamId() {
        return TeamId.of(UUID.randomUUID());
    }

    static OrganisationId organisationId() {
        return OrganisationId.of(UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Create TeamFakes**

```java
// backend/src/test/java/abe/fvjc/tournament/team/domain/TeamFakes.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.UUID;

@UtilityClass
public class TeamFakes {

    public static Organisation buildOrganisation() {
        return Organisation.builder()
            .id(IdGenerator.organisationId())
            .responsibleFirstName("Jean")
            .responsibleLastName("Dupont")
            .tournamentId(TournamentId.of(UUID.randomUUID()))
            .build();
    }

    public static Organisation buildOrganisationForTournament(final TournamentId tournamentId) {
        return Organisation.builder()
            .id(IdGenerator.organisationId())
            .responsibleFirstName("Jean")
            .responsibleLastName("Dupont")
            .tournamentId(tournamentId)
            .build();
    }

    public static Team buildTeam(final OrganisationId organisationId, final TournamentId tournamentId) {
        return Team.builder()
            .id(IdGenerator.teamId())
            .name("Les Aigles")
            .paid(false)
            .organisationId(organisationId)
            .tournamentId(tournamentId)
            .build();
    }

    public static TeamRegisterRequest buildRegisterRequest() {
        return TeamRegisterRequest.builder()
            .name("Les Aigles")
            .responsibleFirstName("Jean")
            .responsibleLastName("Dupont")
            .count(1)
            .paid(List.of(false))
            .build();
    }

    public static TeamRegisterRequest buildRegisterRequestWithCount(final int count) {
        return TeamRegisterRequest.builder()
            .name("Les Aigles")
            .responsibleFirstName("Jean")
            .responsibleLastName("Dupont")
            .count(count)
            .paid(java.util.Collections.nCopies(count, false))
            .build();
    }

    public static TeamUpdateRequest buildUpdateRequest() {
        return TeamUpdateRequest.builder()
            .name("Les Aigles Modifié")
            .responsibleFirstName("Marie")
            .responsibleLastName("Martin")
            .paid(true)
            .build();
    }
}
```

- [ ] **Step 3: Write the failing tests**

```java
// backend/src/test/java/abe/fvjc/tournament/team/domain/TeamServiceTest.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.team.domain.IdGenerator.organisationId;
import static abe.fvjc.tournament.team.domain.TeamFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private OrganisationStore organisationStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private TeamService teamService;

    @Test
    void registerTeamsWhenCount1ShouldCreateOneTeamWithExactName() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(organisationStore.findById(any())).then(inv ->
            Optional.of(buildOrganisationForTournament(tournament.getId())
                .withId(abe.fvjc.tournament.organisation.domain.OrganisationId.of(inv.getArgument(0)))));

        final var teamsCreated = teamService.registerTeams(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(1, teamsCreated.size());
        assertEquals("Les Aigles", teamsCreated.get(0).getName());
    }

    @Test
    void registerTeamsWhenCountGreaterThan1ShouldAppendNumbers() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequestWithCount(3);

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(organisationStore.findById(any())).then(inv ->
            Optional.of(buildOrganisationForTournament(tournament.getId())
                .withId(abe.fvjc.tournament.organisation.domain.OrganisationId.of(inv.getArgument(0)))));

        final var teamsCreated = teamService.registerTeams(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore, times(3)).save(any(Team.class));

        assertEquals(3, teamsCreated.size());
        assertEquals("Les Aigles 1", teamsCreated.get(0).getName());
        assertEquals("Les Aigles 2", teamsCreated.get(1).getName());
        assertEquals("Les Aigles 3", teamsCreated.get(2).getName());
    }

    @Test
    void registerTeamsWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.registerTeams(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void registerTeamsWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var id = abe.fvjc.tournament.tournament.domain.IdGenerator.tournamentId().value();
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> teamService.registerTeams(id, request));

        verify(tournamentStore).findById(id);
    }

    @Test
    void findAllByTournamentIdShouldReturnTeamViewsWithOrganisationData() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(team));
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));

        final var teamsFound = teamService.findAllByTournamentId(tournament.getId().value());

        verify(teamStore).findAllByTournamentId(tournament.getId().value());
        verify(organisationStore).findById(org.getId().value());

        assertEquals(1, teamsFound.size());
        assertEquals(team.getName(), teamsFound.get(0).getName());
        assertEquals(org.getResponsibleFirstName(), teamsFound.get(0).getResponsibleFirstName());
    }

    @Test
    void updateTeamWhenValidShouldUpdateNameAndResponsible() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());
        final var request = buildUpdateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamUpdated = teamService.updateTeam(tournament.getId().value(), team.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(team.getId().value());
        verify(organisationStore).findById(org.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(request.getName(), teamUpdated.getName());
        assertEquals(request.getResponsibleFirstName(), teamUpdated.getResponsibleFirstName());
    }

    @Test
    void updateTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = IdGenerator.teamId().value();
        final var request = buildUpdateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.updateTeam(tournament.getId().value(), teamId, request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void deleteTeamWhenLastInOrgShouldDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = organisationId();
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(orgId.value())).thenReturn(0L);

        teamService.deleteTeam(tournament.getId().value(), team.getId().value());

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(team.getId().value());
        verify(teamStore).deleteById(team.getId().value());
        verify(teamStore).countByOrganisationId(orgId.value());
        verify(organisationStore).deleteById(orgId.value());
    }

    @Test
    void deleteTeamWhenNotLastInOrgShouldNotDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = organisationId();
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(orgId.value())).thenReturn(1L);

        teamService.deleteTeam(tournament.getId().value(), team.getId().value());

        verify(teamStore).deleteById(team.getId().value());
        verify(teamStore).countByOrganisationId(orgId.value());
        verify(organisationStore, never()).deleteById(any());
    }

    @Test
    void deleteTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = IdGenerator.teamId().value();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.deleteTeam(tournament.getId().value(), teamId));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void markPaidWhenExistsShouldUpdatePaidStatus() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));

        final var teamUpdated = teamService.markPaid(team.getId().value(), true);

        verify(teamStore).findById(team.getId().value());
        verify(teamStore).save(any(Team.class));
        verify(organisationStore).findById(org.getId().value());

        assertTrue(teamUpdated.isPaid());
    }

    @Test
    void markPaidWhenNotFoundShouldThrowNotFoundException() {
        final var teamId = IdGenerator.teamId().value();

        when(teamStore.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.markPaid(teamId, true));

        verify(teamStore).findById(teamId);
    }
}
```

- [ ] **Step 4: Run tests — confirm they fail**

```bash
cd backend && ./mvnw test -Dtest=TeamServiceTest
```

Expected: COMPILATION ERROR (TeamService does not exist yet)

- [ ] **Step 5: Create TeamService**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static abe.fvjc.tournament.team.domain.TeamValidator.validateTeamRegisterRequest;
import static abe.fvjc.tournament.team.domain.TeamValidator.validateTeamUpdateRequest;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final OrganisationStore organisationStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public List<TeamView> registerTeams(final UUID tournamentId, final TeamRegisterRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        validateTeamRegisterRequest(request);
        final var org = organisationStore.save(Organisation.builder()
            .id(OrganisationId.of(UUID.randomUUID()))
            .responsibleFirstName(request.getResponsibleFirstName())
            .responsibleLastName(request.getResponsibleLastName())
            .tournamentId(TournamentId.of(tournamentId))
            .build());
        return IntStream.rangeClosed(1, request.getCount())
            .mapToObj(i -> {
                final var name = request.getCount() == 1
                    ? request.getName()
                    : request.getName() + " " + i;
                final var team = teamStore.save(Team.builder()
                    .id(TeamId.of(UUID.randomUUID()))
                    .name(name)
                    .paid(request.getPaid().get(i - 1))
                    .organisationId(org.getId())
                    .tournamentId(TournamentId.of(tournamentId))
                    .build());
                return buildTeamView(team, org);
            })
            .toList();
    }

    public List<TeamView> findAllByTournamentId(final UUID tournamentId) {
        return teamStore.findAllByTournamentId(tournamentId).stream()
            .map(team -> {
                final var org = organisationStore.findById(team.getOrganisationId().value())
                    .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
                return buildTeamView(team, org);
            })
            .toList();
    }

    public TeamView updateTeam(final UUID tournamentId, final UUID teamId, final TeamUpdateRequest request) {
        validateTeamUpdateRequest(request);
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var org = organisationStore.findById(team.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
        final var updatedOrg = organisationStore.save(org
            .withResponsibleFirstName(request.getResponsibleFirstName())
            .withResponsibleLastName(request.getResponsibleLastName()));
        final var updatedTeam = teamStore.save(team
            .withName(request.getName())
            .withPaid(request.isPaid()));
        return buildTeamView(updatedTeam, updatedOrg);
    }

    public void deleteTeam(final UUID tournamentId, final UUID teamId) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        teamStore.deleteById(teamId);
        final var remaining = teamStore.countByOrganisationId(team.getOrganisationId().value());
        if (remaining == 0) {
            organisationStore.deleteById(team.getOrganisationId().value());
        }
    }

    public TeamView markPaid(final UUID teamId, final boolean paid) {
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var updatedTeam = teamStore.save(team.withPaid(paid));
        final var org = organisationStore.findById(updatedTeam.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", updatedTeam.getOrganisationId().value()));
        return buildTeamView(updatedTeam, org);
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation");
        }
    }

    private static TeamView buildTeamView(final Team team, final Organisation org) {
        return TeamView.builder()
            .id(team.getId())
            .name(team.getName())
            .paid(team.isPaid())
            .organisationId(team.getOrganisationId())
            .responsibleFirstName(org.getResponsibleFirstName())
            .responsibleLastName(org.getResponsibleLastName())
            .tournamentId(team.getTournamentId())
            .build();
    }
}
```

Note: `TournamentFakes` and `TournamentStatus` are from the `tournament.domain` package. The test also references `abe.fvjc.tournament.tournament.domain.IdGenerator` — add a static import for `tournamentId()` in the test.

- [ ] **Step 6: Fix the test import for TournamentFakes.IdGenerator**

In `TeamServiceTest.java`, the line:
```java
final var id = abe.fvjc.tournament.tournament.domain.IdGenerator.tournamentId().value();
```
needs `IdGenerator` from `tournament.domain` to be accessible. Since `TournamentFakes.IdGenerator` is package-private, use `TournamentFakes.buildTournament().getId().value()` instead (discard the tournament — just use its ID):

Replace the `registerTeamsWhenTournamentNotFoundShouldThrowNotFoundException` test body with:

```java
@Test
void registerTeamsWhenTournamentNotFoundShouldThrowNotFoundException() {
    final var id = UUID.randomUUID();
    final var request = buildRegisterRequest();

    when(tournamentStore.findById(id)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class,
        () -> teamService.registerTeams(id, request));

    verify(tournamentStore).findById(id);
}
```

Add `import java.util.UUID;` to the test.

- [ ] **Step 7: Run tests — confirm they pass**

```bash
cd backend && ./mvnw test -Dtest=TeamServiceTest
```

Expected: BUILD SUCCESS, all tests GREEN

- [ ] **Step 8: Run all backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java
git add backend/src/test/java/abe/fvjc/tournament/team/
git commit -m "BE - Add TeamService with unit tests (TDD)"
```

---

### Task 6: Database changelogs

**Files:**
- Create: `backend/src/main/resources/db/changelog/20260702120000_create_organisations_table.xml`
- Create: `backend/src/main/resources/db/changelog/20260702120001_create_teams_table.xml`
- Modify: `backend/src/main/resources/db/master.xml`

- [ ] **Step 1: Create organisations changelog**

```xml
<!-- backend/src/main/resources/db/changelog/20260702120000_create_organisations_table.xml -->
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260702120000" author="aberney">
        <createTable tableName="organisations">
            <column name="id" type="TEXT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="responsible_first_name" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="responsible_last_name" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="tournament_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Create teams changelog**

```xml
<!-- backend/src/main/resources/db/changelog/20260702120001_create_teams_table.xml -->
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260702120001" author="aberney">
        <createTable tableName="teams">
            <column name="id" type="TEXT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="paid" type="BOOLEAN">
                <constraints nullable="false"/>
            </column>
            <column name="organisation_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="tournament_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 3: Register both changelogs in master.xml**

Add these two lines after the existing `<include ...tournaments.../>` line:

```xml
<include relativeToChangelogFile="true" file="changelog/20260702120000_create_organisations_table.xml"/>
<include relativeToChangelogFile="true" file="changelog/20260702120001_create_teams_table.xml"/>
```

- [ ] **Step 4: Run all backend tests (includes Spring context boot)**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS — Liquibase will run the new changelogs against the test H2 database.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "BE - Add Liquibase changelogs for organisations and teams tables"
```

---

### Task 7: Organisation persistence

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationDbMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/organisation/JpaOrganisationStore.java`

**Interfaces:**
- Produces: `JpaOrganisationStore implements OrganisationStore` in package `abe.fvjc.tournament.organisation.persistence`

- [ ] **Step 1: Create OrganisationEntity**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationEntity.java
package abe.fvjc.tournament.organisation.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organisations")
@Getter
@Setter
@NoArgsConstructor
class OrganisationEntity {
    @Id
    private UUID id;
    private String responsibleFirstName;
    private String responsibleLastName;
    private UUID tournamentId;
}
```

- [ ] **Step 2: Create OrganisationRepository**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationRepository.java
package abe.fvjc.tournament.organisation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {}
```

- [ ] **Step 3: Create OrganisationDbMapper**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/organisation/OrganisationDbMapper.java
package abe.fvjc.tournament.organisation.persistence;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class OrganisationDbMapper {

    static Organisation toOrganisation(final OrganisationEntity entity) {
        return Organisation.builder()
            .id(OrganisationId.of(entity.getId()))
            .responsibleFirstName(entity.getResponsibleFirstName())
            .responsibleLastName(entity.getResponsibleLastName())
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static OrganisationEntity toOrganisationEntity(final Organisation organisation) {
        final var entity = new OrganisationEntity();
        final var id = organisation.getId().isEmpty()
            ? UUID.randomUUID()
            : organisation.getId().value();
        entity.setId(id);
        entity.setResponsibleFirstName(organisation.getResponsibleFirstName());
        entity.setResponsibleLastName(organisation.getResponsibleLastName());
        entity.setTournamentId(organisation.getTournamentId().value());
        return entity;
    }
}
```

- [ ] **Step 4: Create JpaOrganisationStore**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/organisation/JpaOrganisationStore.java
package abe.fvjc.tournament.organisation.persistence;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.organisation.persistence.OrganisationDbMapper.toOrganisation;
import static abe.fvjc.tournament.organisation.persistence.OrganisationDbMapper.toOrganisationEntity;

@Repository
@RequiredArgsConstructor
class JpaOrganisationStore implements OrganisationStore {
    private final OrganisationRepository organisationRepository;

    @Override
    @Transactional
    public Organisation save(final Organisation organisation) {
        final var entity = toOrganisationEntity(organisation);
        final var savedEntity = organisationRepository.save(entity);
        return toOrganisation(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organisation> findById(final UUID id) {
        return organisationRepository.findById(id)
            .map(OrganisationDbMapper::toOrganisation);
    }

    @Override
    @Transactional
    public void deleteById(final UUID id) {
        organisationRepository.deleteById(id);
    }
}
```

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/persistence/organisation/
git commit -m "BE - Add Organisation persistence layer"
```

---

### Task 8: Team persistence

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamDbMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java`

**Interfaces:**
- Produces: `JpaTeamStore implements TeamStore` in package `abe.fvjc.tournament.team.persistence`

- [ ] **Step 1: Create TeamEntity**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamEntity.java
package abe.fvjc.tournament.team.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
class TeamEntity {
    @Id
    private UUID id;
    private String name;
    private boolean paid;
    private UUID organisationId;
    private UUID tournamentId;
}
```

- [ ] **Step 2: Create TeamRepository**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java
package abe.fvjc.tournament.team.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
    List<TeamEntity> findByTournamentId(UUID tournamentId);
    long countByOrganisationId(UUID organisationId);
}
```

- [ ] **Step 3: Create TeamDbMapper**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamDbMapper.java
package abe.fvjc.tournament.team.persistence;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class TeamDbMapper {

    static Team toTeam(final TeamEntity entity) {
        return Team.builder()
            .id(TeamId.of(entity.getId()))
            .name(entity.getName())
            .paid(entity.isPaid())
            .organisationId(OrganisationId.of(entity.getOrganisationId()))
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static TeamEntity toTeamEntity(final Team team) {
        final var entity = new TeamEntity();
        final var id = team.getId().isEmpty()
            ? UUID.randomUUID()
            : team.getId().value();
        entity.setId(id);
        entity.setName(team.getName());
        entity.setPaid(team.isPaid());
        entity.setOrganisationId(team.getOrganisationId().value());
        entity.setTournamentId(team.getTournamentId().value());
        return entity;
    }
}
```

- [ ] **Step 4: Create JpaTeamStore**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java
package abe.fvjc.tournament.team.persistence;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.team.persistence.TeamDbMapper.toTeam;
import static abe.fvjc.tournament.team.persistence.TeamDbMapper.toTeamEntity;

@Repository
@RequiredArgsConstructor
class JpaTeamStore implements TeamStore {
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public Team save(final Team team) {
        final var entity = toTeamEntity(team);
        final var savedEntity = teamRepository.save(entity);
        return toTeam(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(final UUID id) {
        return teamRepository.findById(id)
            .map(TeamDbMapper::toTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAllByTournamentId(final UUID tournamentId) {
        return teamRepository.findByTournamentId(tournamentId).stream()
            .map(TeamDbMapper::toTeam)
            .toList();
    }

    @Override
    @Transactional
    public void deleteById(final UUID id) {
        teamRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrganisationId(final UUID organisationId) {
        return teamRepository.countByOrganisationId(organisationId);
    }
}
```

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/persistence/team/
git commit -m "BE - Add Team persistence layer"
```

---

### Task 9: Team API layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamRegisterRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamUpdateRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamPaidRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/team/TeamController.java`

**Interfaces:**
- Produces: REST endpoints at `/api/tournaments/{tournamentId}/teams` in package `abe.fvjc.tournament.team.api`

- [ ] **Step 1: Create TeamDto**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamDto.java
package abe.fvjc.tournament.team.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class TeamDto {
    UUID id;
    String name;
    boolean paid;
    UUID organisationId;
    String responsibleFirstName;
    String responsibleLastName;
}
```

- [ ] **Step 2: Create TeamRegisterRequestDto**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamRegisterRequestDto.java
package abe.fvjc.tournament.team.api;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

@Value
@Builder
@JsonDeserialize(builder = TeamRegisterRequestDto.TeamRegisterRequestDtoBuilder.class)
public class TeamRegisterRequestDto {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 250, message = "Le nom ne peut pas dépasser 250 caractères")
    String name;

    @NotBlank(message = "Le prénom du responsable est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String responsibleFirstName;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String responsibleLastName;

    @NotNull(message = "Le nombre d'équipes est obligatoire")
    @Min(value = 1, message = "Le nombre d'équipes doit être d'au moins 1")
    Integer count;

    @NotNull(message = "Le tableau de paiement est obligatoire")
    List<Boolean> paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamRegisterRequestDtoBuilder {}
}
```

- [ ] **Step 3: Create TeamUpdateRequestDto**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamUpdateRequestDto.java
package abe.fvjc.tournament.team.api;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = TeamUpdateRequestDto.TeamUpdateRequestDtoBuilder.class)
public class TeamUpdateRequestDto {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 250, message = "Le nom ne peut pas dépasser 250 caractères")
    String name;

    @NotBlank(message = "Le prénom du responsable est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String responsibleFirstName;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String responsibleLastName;

    @NotNull(message = "Le statut de paiement est obligatoire")
    Boolean paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamUpdateRequestDtoBuilder {}
}
```

- [ ] **Step 4: Create TeamPaidRequestDto**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamPaidRequestDto.java
package abe.fvjc.tournament.team.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = TeamPaidRequestDto.TeamPaidRequestDtoBuilder.class)
public class TeamPaidRequestDto {

    @NotNull(message = "Le statut de paiement est obligatoire")
    Boolean paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamPaidRequestDtoBuilder {}
}
```

- [ ] **Step 5: Create TeamApiMapper**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamApiMapper.java
package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.team.domain.TeamRegisterRequest;
import abe.fvjc.tournament.team.domain.TeamUpdateRequest;
import abe.fvjc.tournament.team.domain.TeamView;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeamApiMapper {

    static TeamDto toTeamDto(final TeamView view) {
        return TeamDto.builder()
            .id(view.getId().value())
            .name(view.getName())
            .paid(view.isPaid())
            .organisationId(view.getOrganisationId().value())
            .responsibleFirstName(view.getResponsibleFirstName())
            .responsibleLastName(view.getResponsibleLastName())
            .build();
    }

    static TeamRegisterRequest toTeamRegisterRequest(final TeamRegisterRequestDto dto) {
        return TeamRegisterRequest.builder()
            .name(dto.getName())
            .responsibleFirstName(dto.getResponsibleFirstName())
            .responsibleLastName(dto.getResponsibleLastName())
            .count(dto.getCount())
            .paid(dto.getPaid())
            .build();
    }

    static TeamUpdateRequest toTeamUpdateRequest(final TeamUpdateRequestDto dto) {
        return TeamUpdateRequest.builder()
            .name(dto.getName())
            .responsibleFirstName(dto.getResponsibleFirstName())
            .responsibleLastName(dto.getResponsibleLastName())
            .paid(dto.getPaid())
            .build();
    }
}
```

- [ ] **Step 6: Create TeamController**

```java
// backend/src/main/java/abe/fvjc/tournament/api/team/TeamController.java
package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.team.domain.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.team.api.TeamApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/teams")
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeamDto> register(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid TeamRegisterRequestDto request) {
        return teamService.registerTeams(tournamentId, toTeamRegisterRequest(request)).stream()
            .map(TeamApiMapper::toTeamDto)
            .toList();
    }

    @GetMapping
    public List<TeamDto> getAll(@PathVariable UUID tournamentId) {
        return teamService.findAllByTournamentId(tournamentId).stream()
            .map(TeamApiMapper::toTeamDto)
            .toList();
    }

    @PutMapping("/{teamId}")
    public TeamDto update(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamUpdateRequestDto request) {
        return toTeamDto(teamService.updateTeam(tournamentId, teamId, toTeamUpdateRequest(request)));
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tournamentId, @PathVariable UUID teamId) {
        teamService.deleteTeam(tournamentId, teamId);
    }

    @PatchMapping("/{teamId}/paid")
    public TeamDto updatePaid(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamPaidRequestDto request) {
        return toTeamDto(teamService.markPaid(teamId, request.getPaid()));
    }
}
```

- [ ] **Step 7: Run all backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/api/team/
git commit -m "BE - Add Team API layer (controller, DTOs, mapper)"
```

---

### Task 10: Frontend team API layer

**Files:**
- Create: `frontend/src/app/api/team/team.api.dto.ts`
- Create: `frontend/src/app/api/team/team.api.service.ts`
- Create: `frontend/src/app/api/team/team.api.mapper.ts`

- [ ] **Step 1: Create team.api.dto.ts**

```typescript
// frontend/src/app/api/team/team.api.dto.ts
export interface TeamDto {
  id: string;
  name: string;
  paid: boolean;
  organisationId: string;
  responsibleFirstName: string;
  responsibleLastName: string;
}

export interface TeamRegisterRequestDto {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  count: number;
  paid: boolean[];
}

export interface TeamUpdateRequestDto {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  paid: boolean;
}

export interface TeamPaidRequestDto {
  paid: boolean;
}
```

- [ ] **Step 2: Create team.api.service.ts**

```typescript
// frontend/src/app/api/team/team.api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TeamDto, TeamPaidRequestDto, TeamRegisterRequestDto, TeamUpdateRequestDto } from '@app/api/team/team.api.dto';

@Injectable({ providedIn: 'root' })
export class TeamApiService {

  private readonly http = inject(HttpClient);

  private baseUrl(tournamentId: string): string {
    return `/api/tournaments/${tournamentId}/teams`;
  }

  getAll$(tournamentId: string): Observable<TeamDto[]> {
    return this.http.get<TeamDto[]>(this.baseUrl(tournamentId));
  }

  register$(tournamentId: string, request: TeamRegisterRequestDto): Observable<TeamDto[]> {
    return this.http.post<TeamDto[]>(this.baseUrl(tournamentId), request);
  }

  update$(tournamentId: string, teamId: string, request: TeamUpdateRequestDto): Observable<TeamDto> {
    return this.http.put<TeamDto>(`${this.baseUrl(tournamentId)}/${teamId}`, request);
  }

  delete$(tournamentId: string, teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(tournamentId)}/${teamId}`);
  }

  markPaid$(tournamentId: string, teamId: string, paid: boolean): Observable<TeamDto> {
    const body: TeamPaidRequestDto = { paid };
    return this.http.patch<TeamDto>(`${this.baseUrl(tournamentId)}/${teamId}/paid`, body);
  }
}
```

- [ ] **Step 3: Create team.api.mapper.ts**

```typescript
// frontend/src/app/api/team/team.api.mapper.ts
import { TeamDto, TeamRegisterRequestDto, TeamUpdateRequestDto } from '@app/api/team/team.api.dto';
import { Team, TeamRegistration, TeamUpdate } from '@app/domain/team/team.model';

export class TeamApiMapper {

  static toDomain(dto: TeamDto): Team {
    return { ...dto };
  }

  static toRegisterRequest(registration: TeamRegistration): TeamRegisterRequestDto {
    return {
      name: registration.name,
      responsibleFirstName: registration.responsibleFirstName,
      responsibleLastName: registration.responsibleLastName,
      count: registration.count,
      paid: registration.paid,
    };
  }

  static toUpdateRequest(update: TeamUpdate): TeamUpdateRequestDto {
    return {
      name: update.name,
      responsibleFirstName: update.responsibleFirstName,
      responsibleLastName: update.responsibleLastName,
      paid: update.paid,
    };
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/api/team/
git commit -m "FE - Add team API layer"
```

---

### Task 11: Frontend team domain layer

**Files:**
- Create: `frontend/src/app/domain/team/team.model.ts`
- Create: `frontend/src/app/domain/team/team.fakes.ts`
- Create: `frontend/src/app/domain/team/team.actions.ts`
- Create: `frontend/src/app/domain/team/team.domain.service.ts`
- Create: `frontend/src/app/domain/team/team.domain.service.spec.ts`
- Create: `frontend/src/app/domain/team/team.state.ts`

- [ ] **Step 1: Create team.model.ts**

```typescript
// frontend/src/app/domain/team/team.model.ts
export interface Team {
  id: string;
  name: string;
  paid: boolean;
  organisationId: string;
  responsibleFirstName: string;
  responsibleLastName: string;
}

export interface TeamRegistration {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  count: number;
  paid: boolean[];
}

export interface TeamUpdate {
  name: string;
  responsibleFirstName: string;
  responsibleLastName: string;
  paid: boolean;
}

export interface TeamGroup {
  organisationId: string;
  responsibleName: string;
  teams: Team[];
}
```

- [ ] **Step 2: Create team.fakes.ts**

```typescript
// frontend/src/app/domain/team/team.fakes.ts
import { Team } from '@app/domain/team/team.model';

export class TeamFakes {

  static aTeam(overrides?: Partial<Team>): Team {
    return {
      id: 'team-id-1',
      name: 'Les Aigles',
      paid: false,
      organisationId: 'org-id-1',
      responsibleFirstName: 'Jean',
      responsibleLastName: 'Dupont',
      ...overrides,
    };
  }

  static aList(count: number, overrides?: Partial<Team>): Team[] {
    return Array.from({ length: count }, (_, i) =>
      TeamFakes.aTeam({ id: `team-id-${i + 1}`, name: `Équipe ${i + 1}`, ...overrides })
    );
  }
}
```

- [ ] **Step 3: Write team.domain.service.spec.ts (failing)**

```typescript
// frontend/src/app/domain/team/team.domain.service.spec.ts
import { TeamDomainService } from '@app/domain/team/team.domain.service';
import { TeamFakes } from '@app/domain/team/team.fakes';

describe('TeamDomainService', () => {

  describe('groupByOrganisation', () => {

    it('should return empty array when no teams', () => {
      // setup — (no data needed)

      // call
      const result = TeamDomainService.groupByOrganisation([]);

      // assert
      expect(result).toEqual([]);
    });

    it('should group teams from the same organisation into one group', () => {
      // setup
      const teams = TeamFakes.aList(2, { organisationId: 'org-1', responsibleFirstName: 'Jean', responsibleLastName: 'Dupont' });

      // call
      const result = TeamDomainService.groupByOrganisation(teams);

      // assert
      expect(result).toHaveLength(1);
      expect(result[0].organisationId).toBe('org-1');
      expect(result[0].responsibleName).toBe('Jean Dupont');
      expect(result[0].teams).toHaveLength(2);
    });

    it('should create separate groups for different organisations', () => {
      // setup
      const team1 = TeamFakes.aTeam({ organisationId: 'org-1', responsibleFirstName: 'Jean', responsibleLastName: 'Dupont' });
      const team2 = TeamFakes.aTeam({ id: 'team-id-2', organisationId: 'org-2', responsibleFirstName: 'Marie', responsibleLastName: 'Martin' });

      // call
      const result = TeamDomainService.groupByOrganisation([team1, team2]);

      // assert
      expect(result).toHaveLength(2);
      expect(result[0].organisationId).toBe('org-1');
      expect(result[1].organisationId).toBe('org-2');
    });
  });
});
```

- [ ] **Step 4: Run spec — confirm it fails**

```bash
cd frontend && npx ng test --include="**/team.domain.service.spec.ts" --watch=false
```

Expected: ERROR — `TeamDomainService` not found

- [ ] **Step 5: Create team.domain.service.ts**

```typescript
// frontend/src/app/domain/team/team.domain.service.ts
import { Injectable } from '@angular/core';
import { Team, TeamGroup } from '@app/domain/team/team.model';

@Injectable({ providedIn: 'root' })
export class TeamDomainService {

  static groupByOrganisation(teams: Team[]): TeamGroup[] {
    const map = new Map<string, TeamGroup>();
    for (const team of teams) {
      if (!map.has(team.organisationId)) {
        map.set(team.organisationId, {
          organisationId: team.organisationId,
          responsibleName: `${team.responsibleFirstName} ${team.responsibleLastName}`,
          teams: [],
        });
      }
      map.get(team.organisationId)!.teams.push(team);
    }
    return Array.from(map.values());
  }
}
```

- [ ] **Step 6: Run spec — confirm it passes**

```bash
cd frontend && npx ng test --include="**/team.domain.service.spec.ts" --watch=false
```

Expected: 3 specs, 0 failures

- [ ] **Step 7: Create team.actions.ts**

```typescript
// frontend/src/app/domain/team/team.actions.ts
import { TeamRegistration, TeamUpdate } from '@app/domain/team/team.model';

export class LoadTeams {
  static readonly type = '[Team] Load Teams';
  constructor(public readonly tournamentId: string) {}
}

export class RegisterTeams {
  static readonly type = '[Team] Register Teams';
  constructor(
    public readonly tournamentId: string,
    public readonly registration: TeamRegistration,
  ) {}
}

export class UpdateTeam {
  static readonly type = '[Team] Update Team';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
    public readonly update: TeamUpdate,
  ) {}
}

export class DeleteTeam {
  static readonly type = '[Team] Delete Team';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
  ) {}
}

export class MarkTeamPaid {
  static readonly type = '[Team] Mark Team Paid';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId: string,
    public readonly paid: boolean,
  ) {}
}
```

- [ ] **Step 8: Create team.state.ts**

```typescript
// frontend/src/app/domain/team/team.state.ts
import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Team, TeamGroup } from '@app/domain/team/team.model';
import { DeleteTeam, LoadTeams, MarkTeamPaid, RegisterTeams, UpdateTeam } from '@app/domain/team/team.actions';
import { TeamApiService } from '@app/api/team/team.api.service';
import { TeamApiMapper } from '@app/api/team/team.api.mapper';
import { TeamDomainService } from '@app/domain/team/team.domain.service';

export interface ITeamState {
  teams: Team[];
}

@State<ITeamState>({
  name: 'team',
  defaults: { teams: [] },
})
@Injectable()
export class TeamState {

  private readonly teamApiService = inject(TeamApiService);

  @Selector()
  static getTeams(state: ITeamState): Team[] {
    return state.teams;
  }

  @Selector()
  static getTeamsGroupedByOrganisation(state: ITeamState): TeamGroup[] {
    return TeamDomainService.groupByOrganisation(state.teams);
  }

  @Action(LoadTeams)
  loadTeams(ctx: StateContext<ITeamState>, { tournamentId }: LoadTeams) {
    return this.teamApiService.getAll$(tournamentId).pipe(
      tap(dtos => {
        ctx.patchState({ teams: dtos.map(TeamApiMapper.toDomain) });
      })
    );
  }

  @Action(RegisterTeams)
  registerTeams(ctx: StateContext<ITeamState>, { tournamentId, registration }: RegisterTeams) {
    return this.teamApiService.register$(tournamentId, TeamApiMapper.toRegisterRequest(registration)).pipe(
      tap(dtos => {
        const newTeams = dtos.map(TeamApiMapper.toDomain);
        ctx.patchState({ teams: [...ctx.getState().teams, ...newTeams] });
      })
    );
  }

  @Action(UpdateTeam)
  updateTeam(ctx: StateContext<ITeamState>, { tournamentId, teamId, update }: UpdateTeam) {
    return this.teamApiService.update$(tournamentId, teamId, TeamApiMapper.toUpdateRequest(update)).pipe(
      tap(dto => {
        const updated = TeamApiMapper.toDomain(dto);
        ctx.patchState({
          teams: ctx.getState().teams.map(t => t.id === updated.id ? updated : t),
        });
      })
    );
  }

  @Action(DeleteTeam)
  deleteTeam(ctx: StateContext<ITeamState>, { tournamentId, teamId }: DeleteTeam) {
    return this.teamApiService.delete$(tournamentId, teamId).pipe(
      tap(() => {
        ctx.patchState({ teams: ctx.getState().teams.filter(t => t.id !== teamId) });
      })
    );
  }

  @Action(MarkTeamPaid)
  markTeamPaid(ctx: StateContext<ITeamState>, { tournamentId, teamId, paid }: MarkTeamPaid) {
    return this.teamApiService.markPaid$(tournamentId, teamId, paid).pipe(
      tap(dto => {
        const updated = TeamApiMapper.toDomain(dto);
        ctx.patchState({
          teams: ctx.getState().teams.map(t => t.id === updated.id ? updated : t),
        });
      })
    );
  }
}
```

- [ ] **Step 9: Commit**

```bash
git add frontend/src/app/domain/team/
git commit -m "FE - Add team domain layer (model, actions, state, domain service + tests)"
```

---

### Task 12: Tournament detail page and navigation

**Files:**
- Create: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts`
- Create: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.scss`
- Modify: `frontend/src/app/modules/tournament.routes.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-list/tournament-list.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-list/tournament-list.page.html`

- [ ] **Step 1: Create tournament-detail.page.ts (skeleton — modals added in later tasks)**

```typescript
// frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { Team, TeamGroup } from '@app/domain/team/team.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { TeamState } from '@app/domain/team/team.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { DeleteTeam, LoadTeams, MarkTeamPaid } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-tournament-detail-page',
  templateUrl: './tournament-detail.page.html',
  styleUrl: './tournament-detail.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
})
export class TournamentDetailPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  protected readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<TeamGroup[]> = this.store.select(TeamState.getTeamsGroupedByOrganisation);

  private tournamentId!: string;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadTeams(this.tournamentId)]);
  }

  openRegisterModal(): void {
    // wired in Task 14
  }

  openEditModal(team: Team): void {
    // wired in Task 15
  }

  deleteTeam(team: Team): void {
    this.store.dispatch(new DeleteTeam(this.tournamentId, team.id));
  }

  markPaid(team: Team, paid: boolean): void {
    this.store.dispatch(new MarkTeamPaid(this.tournamentId, team.id, paid));
  }
}
```

- [ ] **Step 2: Create tournament-detail.page.html**

```html
<!-- frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html -->
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <div class="header-left">
      <button mat-icon-button routerLink="/">
        <mat-icon>arrow_back</mat-icon>
      </button>
      <div>
        <h1>{{ tournament.name }}</h1>
        <span class="subtitle">{{ tournament.sport }} · {{ tournament.date | date:'dd MMM yyyy' }}</span>
      </div>
    </div>
    @if (tournament.status === 'DRAFT') {
      <button mat-raised-button color="primary" (click)="openRegisterModal()">
        <mat-icon>group_add</mat-icon>
        Inscrire des équipes
      </button>
    }
  </div>

  <section class="teams-section">
    <h2>Équipes inscrites</h2>
    @if (groups$ | async; as groups) {
      @if (groups.length === 0) {
        <div class="empty-state">
          <mat-icon>groups</mat-icon>
          <p>Aucune équipe inscrite</p>
        </div>
      } @else {
        @for (group of groups; track group.organisationId) {
          <div class="org-group">
            <div class="org-header">
              <mat-icon>person</mat-icon>
              <span>{{ group.responsibleName }}</span>
            </div>
            @for (team of group.teams; track team.id) {
              <div class="team-row">
                <span class="team-name">{{ team.name }}</span>
                <div class="team-actions">
                  <button mat-icon-button
                          [color]="team.paid ? 'primary' : ''"
                          (click)="markPaid(team, !team.paid)"
                          [title]="team.paid ? 'Marquer non payé' : 'Marquer payé'">
                    <mat-icon>{{ team.paid ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
                  </button>
                  @if (tournament.status === 'DRAFT') {
                    <button mat-icon-button (click)="openEditModal(team)">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button color="warn" (click)="deleteTeam(team)">
                      <mat-icon>delete</mat-icon>
                    </button>
                  }
                </div>
              </div>
            }
          </div>
        }
      }
    }
  </section>
}
```

- [ ] **Step 3: Create tournament-detail.page.scss**

```scss
// frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.scss
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;

    h1 { margin: 0; font-size: 24px; }
    .subtitle { color: var(--mat-sys-on-surface-variant); font-size: 14px; }
  }
}

.teams-section {
  h2 { margin-bottom: 16px; }
}

.org-group {
  margin-bottom: 24px;
  border: 1px solid var(--mat-sys-outline-variant);
  border-radius: 8px;
  overflow: hidden;
}

.org-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--mat-sys-surface-variant);
  font-weight: 500;
}

.team-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-top: 1px solid var(--mat-sys-outline-variant);

  .team-name { font-size: 14px; }
  .team-actions { display: flex; gap: 4px; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
  color: var(--mat-sys-on-surface-variant);

  mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; }
}
```

- [ ] **Step 4: Update tournament.routes.ts**

Replace the file content:

```typescript
// frontend/src/app/modules/tournament.routes.ts
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
    ],
  },
];
```

- [ ] **Step 5: Add "Voir" button to tournament-list.page.ts**

Add `Router` and `RouterLink` to the imports in `tournament-list.page.ts`:

```typescript
import { RouterLink } from '@angular/router';
```

Add `RouterLink` to the `imports` array of the component decorator.

- [ ] **Step 6: Add "Voir" button to tournament-list.page.html**

Inside the `<mat-card-actions>` block, after the chip set, add:

```html
<button mat-icon-button [routerLink]="[tournament.id]" title="Voir le tournoi">
  <mat-icon>arrow_forward</mat-icon>
</button>
```

- [ ] **Step 7: Build to verify compilation**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: Build success (0 errors)

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/display/tournament/pages/tournament-detail/
git add frontend/src/app/modules/tournament.routes.ts
git add frontend/src/app/display/tournament/pages/tournament-list/
git commit -m "FE - Add tournament detail page and navigation"
```

---

### Task 13: Team register modal

**Files:**
- Create: `frontend/src/app/display/tournament/pages/team-register/team-register.modal.ts`
- Create: `frontend/src/app/display/tournament/pages/team-register/team-register.modal.html`
- Create: `frontend/src/app/display/tournament/pages/team-register/team-register.modal.scss`
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts`

- [ ] **Step 1: Create team-register.modal.ts**

```typescript
// frontend/src/app/display/tournament/pages/team-register/team-register.modal.ts
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { RegisterTeams } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-register-modal',
  templateUrl: './team-register.modal.html',
  styleUrl: './team-register.modal.scss',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
})
export class TeamRegisterModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamRegisterModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  get paidArray(): FormArray {
    return this.form.get('paid') as FormArray;
  }

  get paidControls() {
    return this.paidArray.controls;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(250)]],
      responsibleFirstName: ['', [Validators.required, Validators.maxLength(100)]],
      responsibleLastName: ['', [Validators.required, Validators.maxLength(100)]],
      count: [1, [Validators.required, Validators.min(1)]],
      paid: this.fb.array([this.fb.control(false)]),
    });

    this.form.get('count')!.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(count => {
      this.syncPaidArray(Number(count) || 1);
    });

    this.actions$.pipe(
      ofActionSuccessful(RegisterTeams),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, responsibleFirstName, responsibleLastName, count, paid } = this.form.value;
    this.store.dispatch(new RegisterTeams(this.data.tournamentId, {
      name,
      responsibleFirstName,
      responsibleLastName,
      count: Number(count),
      paid: paid as boolean[],
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private syncPaidArray(count: number): void {
    while (this.paidArray.length < count) {
      this.paidArray.push(this.fb.control(false));
    }
    while (this.paidArray.length > count) {
      this.paidArray.removeAt(this.paidArray.length - 1);
    }
  }
}
```

- [ ] **Step 2: Create team-register.modal.html**

```html
<!-- frontend/src/app/display/tournament/pages/team-register/team-register.modal.html -->
<h2 mat-dialog-title>Inscrire des équipes</h2>

<mat-dialog-content>
  <form [formGroup]="form" class="register-form">
    <mat-form-field appearance="outline">
      <mat-label>Nom de base de l'équipe</mat-label>
      <input matInput formControlName="name" />
      @if (form.get('name')?.hasError('required')) {
        <mat-error>Le nom de l'équipe est obligatoire</mat-error>
      } @else if (form.get('name')?.hasError('maxlength')) {
        <mat-error>Le nom ne peut pas dépasser 250 caractères</mat-error>
      }
    </mat-form-field>

    <div class="responsible-row">
      <mat-form-field appearance="outline">
        <mat-label>Prénom du responsable</mat-label>
        <input matInput formControlName="responsibleFirstName" />
        @if (form.get('responsibleFirstName')?.hasError('required')) {
          <mat-error>Le prénom du responsable est obligatoire</mat-error>
        } @else if (form.get('responsibleFirstName')?.hasError('maxlength')) {
          <mat-error>Maximum 100 caractères</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Nom du responsable</mat-label>
        <input matInput formControlName="responsibleLastName" />
        @if (form.get('responsibleLastName')?.hasError('required')) {
          <mat-error>Le nom du responsable est obligatoire</mat-error>
        } @else if (form.get('responsibleLastName')?.hasError('maxlength')) {
          <mat-error>Maximum 100 caractères</mat-error>
        }
      </mat-form-field>
    </div>

    <mat-form-field appearance="outline">
      <mat-label>Nombre d'équipes</mat-label>
      <input matInput type="number" min="1" formControlName="count" />
      @if (form.get('count')?.hasError('required')) {
        <mat-error>Le nombre d'équipes est obligatoire</mat-error>
      } @else if (form.get('count')?.hasError('min')) {
        <mat-error>Le nombre d'équipes doit être d'au moins 1</mat-error>
      }
    </mat-form-field>

    <div class="paid-section" formArrayName="paid">
      <p class="paid-label">Paiement par équipe</p>
      @for (ctrl of paidControls; track $index; let i = $index) {
        <div class="paid-row">
          <span>Équipe {{ form.get('count')?.value === 1 ? form.get('name')?.value || '—' : ((form.get('name')?.value || '—') + ' ' + (i + 1)) }}</span>
          <mat-checkbox [formControlName]="i">Payé</mat-checkbox>
        </div>
      }
    </div>
  </form>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
  <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="submit()">
    Inscrire
  </button>
</mat-dialog-actions>
```

- [ ] **Step 3: Create team-register.modal.scss**

```scss
// frontend/src/app/display/tournament/pages/team-register/team-register.modal.scss
.register-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 420px;
  padding-top: 8px;
}

.responsible-row {
  display: flex;
  gap: 8px;

  mat-form-field { flex: 1; }
}

.paid-section {
  margin-top: 8px;

  .paid-label {
    font-size: 14px;
    color: var(--mat-sys-on-surface-variant);
    margin-bottom: 8px;
  }
}

.paid-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 14px;
}
```

- [ ] **Step 4: Wire modal in TournamentDetailPage**

In `tournament-detail.page.ts`, add the import and update `openRegisterModal()`:

```typescript
import { TeamRegisterModal } from '@app/display/tournament/pages/team-register/team-register.modal';
```

Add `TeamRegisterModal` to the component `imports` array.

Replace the `openRegisterModal()` method body:

```typescript
openRegisterModal(): void {
  this.dialog.open(TeamRegisterModal, {
    data: { tournamentId: this.tournamentId },
  });
}
```

- [ ] **Step 5: Build to verify**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: Build success

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/display/tournament/pages/team-register/
git add frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts
git commit -m "FE - Add team register modal"
```

---

### Task 14: Team edit modal

**Files:**
- Create: `frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.ts`
- Create: `frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.html`
- Create: `frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.scss`
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts`

- [ ] **Step 1: Create team-edit.modal.ts**

```typescript
// frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.ts
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { Team } from '@app/domain/team/team.model';
import { UpdateTeam } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-edit-modal',
  templateUrl: './team-edit.modal.html',
  styleUrl: './team-edit.modal.scss',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
})
export class TeamEditModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamEditModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string; team: Team }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    const { team } = this.data;
    this.form = this.fb.group({
      name: [team.name, [Validators.required, Validators.maxLength(250)]],
      responsibleFirstName: [team.responsibleFirstName, [Validators.required, Validators.maxLength(100)]],
      responsibleLastName: [team.responsibleLastName, [Validators.required, Validators.maxLength(100)]],
      paid: [team.paid],
    });

    this.actions$.pipe(
      ofActionSuccessful(UpdateTeam),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    const { name, responsibleFirstName, responsibleLastName, paid } = this.form.value;
    this.store.dispatch(new UpdateTeam(this.data.tournamentId, this.data.team.id, {
      name,
      responsibleFirstName,
      responsibleLastName,
      paid,
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

- [ ] **Step 2: Create team-edit.modal.html**

```html
<!-- frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.html -->
<h2 mat-dialog-title>Modifier l'équipe</h2>

<mat-dialog-content>
  <form [formGroup]="form" class="edit-form">
    <mat-form-field appearance="outline">
      <mat-label>Nom de l'équipe</mat-label>
      <input matInput formControlName="name" />
      @if (form.get('name')?.hasError('required')) {
        <mat-error>Le nom de l'équipe est obligatoire</mat-error>
      } @else if (form.get('name')?.hasError('maxlength')) {
        <mat-error>Le nom ne peut pas dépasser 250 caractères</mat-error>
      }
    </mat-form-field>

    <div class="responsible-row">
      <mat-form-field appearance="outline">
        <mat-label>Prénom du responsable</mat-label>
        <input matInput formControlName="responsibleFirstName" />
        @if (form.get('responsibleFirstName')?.hasError('required')) {
          <mat-error>Le prénom du responsable est obligatoire</mat-error>
        } @else if (form.get('responsibleFirstName')?.hasError('maxlength')) {
          <mat-error>Maximum 100 caractères</mat-error>
        }
      </mat-form-field>

      <mat-form-field appearance="outline">
        <mat-label>Nom du responsable</mat-label>
        <input matInput formControlName="responsibleLastName" />
        @if (form.get('responsibleLastName')?.hasError('required')) {
          <mat-error>Le nom du responsable est obligatoire</mat-error>
        } @else if (form.get('responsibleLastName')?.hasError('maxlength')) {
          <mat-error>Maximum 100 caractères</mat-error>
        }
      </mat-form-field>
    </div>

    <mat-checkbox formControlName="paid">Payé</mat-checkbox>
  </form>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
  <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="submit()">
    Enregistrer
  </button>
</mat-dialog-actions>
```

- [ ] **Step 3: Create team-edit.modal.scss**

```scss
// frontend/src/app/display/tournament/pages/team-edit/team-edit.modal.scss
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 420px;
  padding-top: 8px;
}

.responsible-row {
  display: flex;
  gap: 8px;

  mat-form-field { flex: 1; }
}
```

- [ ] **Step 4: Wire modal in TournamentDetailPage**

In `tournament-detail.page.ts`, add:

```typescript
import { TeamEditModal } from '@app/display/tournament/pages/team-edit/team-edit.modal';
```

Add `TeamEditModal` to the component `imports` array.

Replace `openEditModal()` body:

```typescript
openEditModal(team: Team): void {
  this.dialog.open(TeamEditModal, {
    data: { tournamentId: this.tournamentId, team },
  });
}
```

- [ ] **Step 5: Build to verify**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -20
```

Expected: Build success

- [ ] **Step 6: Run all frontend tests**

```bash
cd frontend && npx ng test --watch=false
```

Expected: All specs pass

- [ ] **Step 7: Run all backend tests**

```bash
cd backend && ./mvnw test
```

Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/display/tournament/pages/team-edit/
git add frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts
git commit -m "FE - Add team edit modal"
```

---

## Self-Review

**Spec coverage check:**

| Spec requirement | Task |
|---|---|
| UC-02.1 Register team(s), count=1 name exact | Task 5 (service), Task 9 (API), Task 13 (modal) |
| UC-02.1 Register team(s), count>1 numbered names | Task 5 (service) |
| UC-02.1 Validation rules (all 8 rules) | Task 4 (validator), Task 9 (DTO annotations) |
| UC-02.1 Tournament not DRAFT → 409 | Task 1 (exception), Task 5 (service), Task 9 (controller) |
| UC-02.1 POST response 201 with list | Task 9 (controller) |
| UC-02.2 List teams | Task 5 (service), Task 9 (controller), Task 11 (state) |
| UC-02.3 Edit team (name, responsible, paid) | Task 5 (service), Task 9 (controller), Task 14 (modal) |
| UC-02.3 Editing responsible updates shared org | Task 5 (updateTeam — saves org with new names) |
| UC-02.4 Delete team | Task 5 (service), Task 9 (controller) |
| UC-02.4 Delete cascade org when last team | Task 5 (deleteTeam test: `deleteTeamWhenLastInOrgShouldDeleteOrg`) |
| UC-02.5 Mark paid (any status) | Task 5 (markPaid — no DRAFT check), Task 9 (PATCH) |
| Tournament detail page | Task 12 |
| Teams grouped by organisation in UI | Task 11 (TeamDomainService), Task 12 (page HTML) |
| Navigation from list | Task 12 |

**Placeholder scan:** No TBDs. All code steps are complete.

**Type consistency check:**
- `TeamRegistration` defined in `team.model.ts`, used in `team.actions.ts` (`RegisterTeams`) and `team.api.mapper.ts` (`toRegisterRequest`) ✓
- `TeamUpdate` defined in `team.model.ts`, used in `team.actions.ts` (`UpdateTeam`) and `team.api.mapper.ts` (`toUpdateRequest`) ✓
- `TeamGroup` defined in `team.model.ts`, used in `team.domain.service.ts`, `team.state.ts` selector, and `tournament-detail.page.html` ✓
- `TeamView` defined in Task 3, consumed by `TeamService` (Task 5) and `TeamApiMapper` (Task 9) ✓
- `OrganisationStore` defined in Task 2, implemented in Task 7, injected by `TeamService` in Task 5 ✓

**One gap found and fixed:** `team.fakes.ts` imports from `@app/domain/team/team.model` — the file path is `frontend/src/app/domain/team/team.fakes.ts` and uses the `@app/` alias, consistent with all other frontend files ✓
