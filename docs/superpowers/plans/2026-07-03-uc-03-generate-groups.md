# UC-03 Generate Groups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add group draw management to a DRAFT tournament — generate named groups via randomised draw, get groups, swap teams between groups.

**Architecture:** Teams get a nullable `groupId` field; finding a group's teams is a `findAllByGroupId` query. `GroupService` assembles `GroupView` objects by loading `Team` objects per group. The frontend adds a separate `/tournaments/:id/groups` page.

**Tech Stack:** Spring Boot 4.1, SQLite, Liquibase, Lombok, JUnit 5, Mockito / Angular 21, NGXS, Angular Material

## Global Constraints

- Packages are feature-first: `abe.fvjc.tournament.group.domain`, `abe.fvjc.tournament.group.persistence`, `abe.fvjc.tournament.group.api`
- Directory structure is layer-first: `domain/group/`, `persistence/group/`, `api/group/`
- All local variables use `final var`; method parameters are `final`
- `@Transactional` per method on `JpaXxxStore` only — never on services
- JPA entities and Spring Data repos are package-private
- DTOs use `@Value @Builder @Jacksonized`
- Domain objects use `@Value @Builder @With`
- All `public static` methods and enum values are statically imported at call sites
- Backend tests: JUnit 5 assertions only, `@ExtendWith(MockitoExtension.class)`, `final var`
- Frontend: `inject()` everywhere, standalone components, `@app/` alias imports

---

### Task 1: Backend — Team groupId extension

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/team/Team.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamEntity.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamDbMapper.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java`
- Create: `backend/src/main/resources/db/changelog/20260703120000_alter_teams_add_group_id.xml`
- Modify: `backend/src/main/resources/db/master.xml`

**Interfaces:**
- Produces: `Team` with `groupId: GroupId` field and `withGroupId(GroupId)` method; `TeamStore.findAllByGroupId(UUID)`

- [ ] **Step 1: Add GroupId field to Team**

`backend/src/main/java/abe/fvjc/tournament/domain/team/Team.java`:
```java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.group.domain.GroupId;
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
    @Builder.Default GroupId groupId = GroupId.empty();
}
```

- [ ] **Step 2: Create GroupId record** (needed to compile Team)

`backend/src/main/java/abe/fvjc/tournament/domain/group/GroupId.java`:
```java
package abe.fvjc.tournament.group.domain;

import java.util.UUID;

public record GroupId(UUID value) {

    public static GroupId of(final UUID value) {
        return new GroupId(value);
    }

    public static GroupId empty() {
        return new GroupId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 3: Add findAllByGroupId to TeamStore**

```java
package abe.fvjc.tournament.team.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamStore {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findAllByTournamentId(UUID tournamentId);
    List<Team> findAllByGroupId(UUID groupId);
    void deleteById(UUID id);
    long countByOrganisationId(UUID organisationId);
}
```

- [ ] **Step 4: Add groupId to TeamEntity**

```java
package abe.fvjc.tournament.team.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private UUID groupId;
}
```

- [ ] **Step 5: Update TeamDbMapper to map groupId**

```java
package abe.fvjc.tournament.team.persistence;

import abe.fvjc.tournament.group.domain.GroupId;
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
            .groupId(entity.getGroupId() != null ? GroupId.of(entity.getGroupId()) : GroupId.empty())
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
        entity.setGroupId(team.getGroupId().isEmpty() ? null : team.getGroupId().value());
        return entity;
    }
}
```

- [ ] **Step 6: Add findByGroupId to TeamRepository**

```java
package abe.fvjc.tournament.team.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
    List<TeamEntity> findByTournamentId(UUID tournamentId);
    List<TeamEntity> findByGroupId(UUID groupId);
    long countByOrganisationId(UUID organisationId);
}
```

- [ ] **Step 7: Add findAllByGroupId to JpaTeamStore**

Add this method to `JpaTeamStore` (keep all existing methods unchanged):
```java
@Override
@Transactional(readOnly = true)
public List<Team> findAllByGroupId(final UUID groupId) {
    return teamRepository.findByGroupId(groupId).stream()
            .map(TeamDbMapper::toTeam)
            .toList();
}
```

- [ ] **Step 8: Create Liquibase changelog to add group_id column**

`backend/src/main/resources/db/changelog/20260703120000_alter_teams_add_group_id.xml`:
```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260703120000" author="aberney">
        <addColumn tableName="teams">
            <column name="group_id" type="TEXT"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 9: Add changelog to master.xml**

Add before the closing tag:
```xml
    <include relativeToChangelogFile="true" file="changelog/20260703120000_alter_teams_add_group_id.xml"/>
```

- [ ] **Step 10: Run existing tests to verify no regressions**

```bash
cd backend && ./mvnw test -q
```
Expected: BUILD SUCCESS — all existing tests pass.

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/group/GroupId.java \
        backend/src/main/java/abe/fvjc/tournament/domain/team/Team.java \
        backend/src/main/java/abe/fvjc/tournament/domain/team/TeamStore.java \
        backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamEntity.java \
        backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamDbMapper.java \
        backend/src/main/java/abe/fvjc/tournament/persistence/team/TeamRepository.java \
        backend/src/main/java/abe/fvjc/tournament/persistence/team/JpaTeamStore.java \
        backend/src/main/resources/db/changelog/20260703120000_alter_teams_add_group_id.xml \
        backend/src/main/resources/db/master.xml
git commit -m "BE - Add groupId to Team for group assignment"
```

---

### Task 2: Backend — Group domain model + validator

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/Group.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupView.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupDistribution.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupGenerateRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupSwapRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupValidator.java`

**Interfaces:**
- Produces: All group domain types consumed by Task 3

- [ ] **Step 1: Create Group**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Group {
    GroupId id;
    String name;
    TournamentId tournamentId;
}
```

- [ ] **Step 2: Create GroupView**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupView {
    GroupId id;
    String name;
    TournamentId tournamentId;
    List<Team> teams;
}
```

- [ ] **Step 3: Create GroupDistribution**

```java
package abe.fvjc.tournament.group.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupDistribution {
    int numberOfGroups;
    int groupsOfBaseSize;
    int groupsOfBaseSizePlusOne;
    int baseSize;
    int totalTeams;
}
```

- [ ] **Step 4: Create GroupStore**

```java
package abe.fvjc.tournament.group.domain;

import java.util.List;
import java.util.UUID;

public interface GroupStore {
    Group save(Group group);
    void saveAll(List<Group> groups);
    List<Group> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 5: Create GroupGenerateRequest**

```java
package abe.fvjc.tournament.group.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupGenerateRequest {
    Integer groupSize;
}
```

- [ ] **Step 6: Create GroupSwapRequest**

```java
package abe.fvjc.tournament.group.domain;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class GroupSwapRequest {
    UUID teamId1;
    UUID teamId2;
}
```

- [ ] **Step 7: Create GroupValidator**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class GroupValidator {

    public static void validateGroupGenerateRequest(final GroupGenerateRequest request, final int totalTeams) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getGroupSize() == null) {
            errors.add(new ValidationException.FieldError("groupSize", "La taille du groupe est obligatoire"));
        } else if (request.getGroupSize() < 2) {
            errors.add(new ValidationException.FieldError("groupSize", "La taille du groupe doit être d'au moins 2"));
        } else if (totalTeams < request.getGroupSize()) {
            errors.add(new ValidationException.FieldError("groupSize", "Pas assez d'équipes pour former un groupe"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

- [ ] **Step 8: Compile check**

```bash
cd backend && ./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/group/
git commit -m "BE - Add Group domain model and validator"
```

---

### Task 3: Backend — GroupService + tests (TDD)

**Files:**
- Create: `backend/src/test/java/abe/fvjc/tournament/group/domain/IdGenerator.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/group/domain/GroupFakes.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/group/domain/GroupServiceTest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupService.java`

**Interfaces:**
- Consumes: All types from Tasks 1–2
- Produces: `GroupService` with `distribution`, `generate`, `findAllByTournamentId`, `swap`

- [ ] **Step 1: Create IdGenerator**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static GroupId groupId() {
        return GroupId.of(UUID.randomUUID());
    }

    static TournamentId tournamentId() {
        return TournamentId.of(UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Create GroupFakes**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class GroupFakes {

    public static Group buildGroup(final TournamentId tournamentId) {
        return Group.builder()
            .id(IdGenerator.groupId())
            .name("A")
            .tournamentId(tournamentId)
            .build();
    }

    public static GroupGenerateRequest buildGenerateRequest() {
        return GroupGenerateRequest.builder()
            .groupSize(4)
            .build();
    }

    public static GroupSwapRequest buildSwapRequest(final UUID teamId1, final UUID teamId2) {
        return GroupSwapRequest.builder()
            .teamId1(teamId1)
            .teamId2(teamId2)
            .build();
    }
}
```

- [ ] **Step 3: Write GroupServiceTest**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.group.domain.GroupFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private GroupService groupService;

    @Test
    void generateWhenValidShouldCreateOneGroupForFourTeams() {
        final var tournament = buildTournament();
        final var org1 = OrganisationId.of(UUID.randomUUID());
        final var org2 = OrganisationId.of(UUID.randomUUID());
        final var teams = List.of(
            TeamFakes.buildTeam(org1, tournament.getId()),
            TeamFakes.buildTeam(org1, tournament.getId()),
            TeamFakes.buildTeam(org2, tournament.getId()),
            TeamFakes.buildTeam(org2, tournament.getId())
        );
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(teams);

        final var groupsGenerated = groupService.generate(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findAllByTournamentId(tournament.getId().value());
        verify(groupStore).deleteAllByTournamentId(tournament.getId().value());
        verify(groupStore).saveAll(anyList());

        assertEquals(1, groupsGenerated.size());
        assertEquals("A", groupsGenerated.get(0).getName());
        assertNotNull(groupsGenerated.get(0).getId());
    }

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void generateWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());

        assertEquals("groupSize", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenNotEnoughTeamsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void distributionWhenValidShouldReturnCorrectCounts() {
        final var tournamentId = UUID.randomUUID();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var teams = List.of(
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId())
        );
        final var request = GroupGenerateRequest.builder().groupSize(2).build();

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var distribution = groupService.distribution(tournamentId, request);

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals(2, distribution.getNumberOfGroups());
        assertEquals(1, distribution.getGroupsOfBaseSizePlusOne());
        assertEquals(1, distribution.getGroupsOfBaseSize());
        assertEquals(2, distribution.getBaseSize());
        assertEquals(5, distribution.getTotalTeams());
    }

    @Test
    void distributionWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournamentId = UUID.randomUUID();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        assertThrows(ValidationException.class,
            () -> groupService.distribution(tournamentId, request));

        verify(teamStore).findAllByTournamentId(tournamentId);
    }

    @Test
    void findAllByTournamentIdShouldReturnGroupViewsWithTeams() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = IdGenerator.groupId();
        final var group = buildGroup(tournament.getId()).withId(groupId);
        final var team = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(team));

        final var groupsFound = groupService.findAllByTournamentId(tournament.getId().value());

        verify(groupStore).findAllByTournamentId(tournament.getId().value());
        verify(teamStore).findAllByGroupId(groupId.value());

        assertEquals(1, groupsFound.size());
        assertEquals("A", groupsFound.get(0).getName());
        assertEquals(1, groupsFound.get(0).getTeams().size());
    }

    @Test
    void swapWhenValidShouldExchangeTeamGroups() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = IdGenerator.groupId();
        final var groupId2 = IdGenerator.groupId();
        final var group1 = buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = buildGroup(tournament.getId()).withId(groupId2).withName("B");
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(teamStore.findById(team1.getId().value())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId().value())).thenReturn(Optional.of(team2));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(groupId1.value())).thenReturn(List.of(team2));
        when(teamStore.findAllByGroupId(groupId2.value())).thenReturn(List.of(team1));

        final var groupsUpdated = groupService.swap(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore, times(2)).save(any(Team.class));

        assertEquals(2, groupsUpdated.size());
    }

    @Test
    void swapWhenSameGroupShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = IdGenerator.groupId();
        final var group = buildGroup(tournament.getId()).withId(groupId);
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findById(team1.getId().value())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId().value())).thenReturn(Optional.of(team2));

        assertThrows(ValidationException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void swapWhenTeamNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament();
        final var teamId1 = UUID.randomUUID();
        final var request = buildSwapRequest(teamId1, UUID.randomUUID());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findById(teamId1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void swapWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildSwapRequest(UUID.randomUUID(), UUID.randomUUID());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }
}
```

- [ ] **Step 4: Run tests to confirm they fail (GroupService not yet created)**

```bash
cd backend && ./mvnw test -Dtest=GroupServiceTest -q 2>&1 | tail -5
```
Expected: FAILURE — `GroupService` not found.

- [ ] **Step 5: Implement GroupService**

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.group.domain.GroupValidator.validateGroupGenerateRequest;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupStore groupStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public GroupDistribution distribution(final UUID tournamentId, final GroupGenerateRequest request) {
        final var teams = teamStore.findAllByTournamentId(tournamentId);
        validateGroupGenerateRequest(request, teams.size());
        return computeDistribution(teams.size(), request.getGroupSize());
    }

    public List<GroupView> generate(final UUID tournamentId, final GroupGenerateRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());

        final var teams = teamStore.findAllByTournamentId(tournamentId);
        validateGroupGenerateRequest(request, teams.size());

        teams.forEach(team -> teamStore.save(team.withGroupId(GroupId.empty())));
        groupStore.deleteAllByTournamentId(tournamentId);

        final var draws = computeDraw(teams, request.getGroupSize());
        final var groups = new ArrayList<Group>();
        for (int i = 0; i < draws.size(); i++) {
            groups.add(Group.builder()
                .id(GroupId.of(UUID.randomUUID()))
                .name(groupName(i))
                .tournamentId(tournament.getId())
                .build());
        }
        groupStore.saveAll(groups);

        for (int i = 0; i < draws.size(); i++) {
            final var group = groups.get(i);
            for (final var team : draws.get(i)) {
                teamStore.save(team.withGroupId(group.getId()));
            }
        }

        return buildViews(groups);
    }

    public List<GroupView> findAllByTournamentId(final UUID tournamentId) {
        return groupStore.findAllByTournamentId(tournamentId).stream()
            .map(this::toView)
            .toList();
    }

    public List<GroupView> swap(final UUID tournamentId, final GroupSwapRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());

        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var team1 = teamStore.findById(request.getTeamId1())
            .orElseThrow(() -> new NotFoundException("Team", request.getTeamId1()));
        final var team2 = teamStore.findById(request.getTeamId2())
            .orElseThrow(() -> new NotFoundException("Team", request.getTeamId2()));

        final var group1 = groups.stream()
            .filter(g -> g.getId().equals(team1.getGroupId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Group for team", request.getTeamId1()));
        final var group2 = groups.stream()
            .filter(g -> g.getId().equals(team2.getGroupId()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Group for team", request.getTeamId2()));

        if (group1.getId().equals(group2.getId())) {
            throw new ValidationException(List.of(new ValidationException.FieldError(
                "teamId2", "Les deux équipes sont déjà dans le même groupe")));
        }

        teamStore.save(team1.withGroupId(group2.getId()));
        teamStore.save(team2.withGroupId(group1.getId()));

        return List.of(toView(group1), toView(group2));
    }

    private GroupView toView(final Group group) {
        final var teams = teamStore.findAllByGroupId(group.getId().value());
        return GroupView.builder()
            .id(group.getId())
            .name(group.getName())
            .tournamentId(group.getTournamentId())
            .teams(teams)
            .build();
    }

    private List<GroupView> buildViews(final List<Group> groups) {
        return groups.stream().map(this::toView).toList();
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les groupes ne peuvent être générés que pour un tournoi en cours de préparation");
        }
    }

    private static GroupDistribution computeDistribution(final int totalTeams, final int groupSize) {
        final var numberOfGroups = totalTeams / groupSize;
        final var remainder = totalTeams % groupSize;
        return GroupDistribution.builder()
            .numberOfGroups(numberOfGroups)
            .groupsOfBaseSizePlusOne(remainder)
            .groupsOfBaseSize(numberOfGroups - remainder)
            .baseSize(groupSize)
            .totalTeams(totalTeams)
            .build();
    }

    private static List<List<Team>> computeDraw(final List<Team> teams, final int groupSize) {
        final var totalTeams = teams.size();
        final var numberOfGroups = totalTeams / groupSize;
        final var remainder = totalTeams % groupSize;

        final var groupSlots = new ArrayList<List<Team>>();
        for (int i = 0; i < numberOfGroups; i++) {
            groupSlots.add(new ArrayList<>());
        }

        final var byOrg = teams.stream()
            .collect(Collectors.groupingBy(t -> t.getOrganisationId().value()));
        final var sortedOrgs = byOrg.values().stream()
            .sorted((a, b) -> b.size() - a.size())
            .toList();

        final var assigned = new HashSet<UUID>();
        for (final var orgTeams : sortedOrgs) {
            for (int i = 0; i < orgTeams.size(); i++) {
                final var team = orgTeams.get(i);
                groupSlots.get(i % numberOfGroups).add(team);
                assigned.add(team.getId().value());
            }
        }

        final var remaining = teams.stream()
            .filter(t -> !assigned.contains(t.getId().value()))
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(remaining);

        for (int i = 0; i < numberOfGroups; i++) {
            final var capacity = i < remainder ? groupSize + 1 : groupSize;
            while (groupSlots.get(i).size() < capacity && !remaining.isEmpty()) {
                groupSlots.get(i).add(remaining.remove(0));
            }
        }

        return groupSlots;
    }

    private static String groupName(final int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return groupName(index / 26 - 1) + (char) ('A' + index % 26);
    }
}
```

- [ ] **Step 6: Run GroupServiceTest**

```bash
cd backend && ./mvnw test -Dtest=GroupServiceTest -q
```
Expected: BUILD SUCCESS — all 9 tests pass.

- [ ] **Step 7: Run full test suite**

```bash
cd backend && ./mvnw test -q
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/test/java/abe/fvjc/tournament/group/ \
        backend/src/main/java/abe/fvjc/tournament/domain/group/GroupService.java
git commit -m "BE - Add GroupService with TDD"
```

---

### Task 4: Backend — Group persistence

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/group/GroupEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/group/GroupRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/group/GroupDbMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/persistence/group/JpaGroupStore.java`
- Create: `backend/src/main/resources/db/changelog/20260703120001_create_groups_table.xml`
- Modify: `backend/src/main/resources/db/master.xml`

- [ ] **Step 1: Create GroupEntity**

```java
package abe.fvjc.tournament.group.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
class GroupEntity {
    @Id
    private UUID id;
    private String name;
    private UUID tournamentId;
}
```

- [ ] **Step 2: Create GroupRepository**

```java
package abe.fvjc.tournament.group.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GroupRepository extends JpaRepository<GroupEntity, UUID> {
    List<GroupEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 3: Create GroupDbMapper**

```java
package abe.fvjc.tournament.group.persistence;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class GroupDbMapper {

    static Group toGroup(final GroupEntity entity) {
        return Group.builder()
            .id(GroupId.of(entity.getId()))
            .name(entity.getName())
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static GroupEntity toGroupEntity(final Group group) {
        final var entity = new GroupEntity();
        final var id = group.getId().isEmpty()
                ? UUID.randomUUID()
                : group.getId().value();
        entity.setId(id);
        entity.setName(group.getName());
        entity.setTournamentId(group.getTournamentId().value());
        return entity;
    }
}
```

- [ ] **Step 4: Create JpaGroupStore**

```java
package abe.fvjc.tournament.group.persistence;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.group.persistence.GroupDbMapper.toGroup;
import static abe.fvjc.tournament.group.persistence.GroupDbMapper.toGroupEntity;

@Repository
@RequiredArgsConstructor
class JpaGroupStore implements GroupStore {
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Group save(final Group group) {
        final var entity = toGroupEntity(group);
        final var savedEntity = groupRepository.save(entity);
        return toGroup(savedEntity);
    }

    @Override
    @Transactional
    public void saveAll(final List<Group> groups) {
        groups.forEach(group -> groupRepository.save(toGroupEntity(group)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Group> findAllByTournamentId(final UUID tournamentId) {
        return groupRepository.findByTournamentId(tournamentId).stream()
                .map(GroupDbMapper::toGroup)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        groupRepository.deleteByTournamentId(tournamentId);
    }
}
```

- [ ] **Step 5: Create groups table changelog**

`backend/src/main/resources/db/changelog/20260703120001_create_groups_table.xml`:
```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260703120001" author="aberney">
        <createTable tableName="groups">
            <column name="id" type="TEXT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="tournament_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 6: Add changelog to master.xml**

Add after the `20260703120000` include line:
```xml
    <include relativeToChangelogFile="true" file="changelog/20260703120001_create_groups_table.xml"/>
```

- [ ] **Step 7: Run full test suite**

```bash
cd backend && ./mvnw test -q
```
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/persistence/group/ \
        backend/src/main/resources/db/changelog/20260703120001_create_groups_table.xml \
        backend/src/main/resources/db/master.xml
git commit -m "BE - Add Group persistence layer"
```

---

### Task 5: Backend — Group API

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupTeamDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupDistributionDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupGenerateRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupSwapRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/group/GroupController.java`

- [ ] **Step 1: Create GroupTeamDto**

```java
package abe.fvjc.tournament.group.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupTeamDto {
    UUID id;
    String name;
    UUID organisationId;
}
```

- [ ] **Step 2: Create GroupDto**

```java
package abe.fvjc.tournament.group.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupDto {
    UUID id;
    String name;
    List<GroupTeamDto> teams;
}
```

- [ ] **Step 3: Create GroupDistributionDto**

```java
package abe.fvjc.tournament.group.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupDistributionDto {
    int numberOfGroups;
    int groupsOfBaseSize;
    int groupsOfBaseSizePlusOne;
    int baseSize;
    int totalTeams;
}
```

- [ ] **Step 4: Create GroupGenerateRequestDto**

```java
package abe.fvjc.tournament.group.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupGenerateRequestDto {
    @NotNull Integer groupSize;
}
```

- [ ] **Step 5: Create GroupSwapRequestDto**

```java
package abe.fvjc.tournament.group.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupSwapRequestDto {
    @NotNull UUID teamId1;
    @NotNull UUID teamId2;
}
```

- [ ] **Step 6: Create GroupApiMapper**

```java
package abe.fvjc.tournament.group.api;

import abe.fvjc.tournament.group.domain.GroupDistribution;
import abe.fvjc.tournament.group.domain.GroupGenerateRequest;
import abe.fvjc.tournament.group.domain.GroupSwapRequest;
import abe.fvjc.tournament.group.domain.GroupView;
import abe.fvjc.tournament.team.domain.Team;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupApiMapper {

    static GroupDto toGroupDto(final GroupView view) {
        return GroupDto.builder()
            .id(view.getId().value())
            .name(view.getName())
            .teams(view.getTeams().stream().map(GroupApiMapper::toGroupTeamDto).toList())
            .build();
    }

    static GroupTeamDto toGroupTeamDto(final Team team) {
        return GroupTeamDto.builder()
            .id(team.getId().value())
            .name(team.getName())
            .organisationId(team.getOrganisationId().value())
            .build();
    }

    static GroupDistributionDto toGroupDistributionDto(final GroupDistribution distribution) {
        return GroupDistributionDto.builder()
            .numberOfGroups(distribution.getNumberOfGroups())
            .groupsOfBaseSize(distribution.getGroupsOfBaseSize())
            .groupsOfBaseSizePlusOne(distribution.getGroupsOfBaseSizePlusOne())
            .baseSize(distribution.getBaseSize())
            .totalTeams(distribution.getTotalTeams())
            .build();
    }

    static GroupGenerateRequest toGroupGenerateRequest(final GroupGenerateRequestDto dto) {
        return GroupGenerateRequest.builder()
            .groupSize(dto.getGroupSize())
            .build();
    }

    static GroupSwapRequest toGroupSwapRequest(final GroupSwapRequestDto dto) {
        return GroupSwapRequest.builder()
            .teamId1(dto.getTeamId1())
            .teamId2(dto.getTeamId2())
            .build();
    }
}
```

- [ ] **Step 7: Create GroupController**

```java
package abe.fvjc.tournament.group.api;

import abe.fvjc.tournament.group.domain.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.group.api.GroupApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/groups")
@RequiredArgsConstructor
class GroupController {
    private final GroupService groupService;

    @PostMapping("/distribution")
    public GroupDistributionDto distribution(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto request) {
        return toGroupDistributionDto(groupService.distribution(tournamentId, toGroupGenerateRequest(request)));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GroupDto> generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto request) {
        return groupService.generate(tournamentId, toGroupGenerateRequest(request)).stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    @GetMapping
    public List<GroupDto> getAll(@PathVariable UUID tournamentId) {
        return groupService.findAllByTournamentId(tournamentId).stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    @PostMapping("/swap")
    public List<GroupDto> swap(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupSwapRequestDto request) {
        return groupService.swap(tournamentId, toGroupSwapRequest(request)).stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }
}
```

- [ ] **Step 8: Run full test suite**

```bash
cd backend && ./mvnw test -q
```
Expected: BUILD SUCCESS — architecture tests pass (GroupController in `..api..`, GroupService in `..domain..`, etc.)

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/api/group/
git commit -m "BE - Add Group API controller and mapper"
```

---

### Task 6: Frontend — Group API + domain layer

**Files:**
- Create: `frontend/src/app/api/group/group.api.dto.ts`
- Create: `frontend/src/app/api/group/group.api.service.ts`
- Create: `frontend/src/app/api/group/group.api.mapper.ts`
- Create: `frontend/src/app/domain/group/group.model.ts`
- Create: `frontend/src/app/domain/group/group.actions.ts`
- Create: `frontend/src/app/domain/group/group.state.ts`

- [ ] **Step 1: Create group.api.dto.ts**

```typescript
export interface GroupTeamDto {
  id: string;
  name: string;
  organisationId: string;
}

export interface GroupDto {
  id: string;
  name: string;
  teams: GroupTeamDto[];
}

export interface GroupDistributionDto {
  numberOfGroups: number;
  groupsOfBaseSize: number;
  groupsOfBaseSizePlusOne: number;
  baseSize: number;
  totalTeams: number;
}

export interface GroupGenerateRequestDto {
  groupSize: number;
}

export interface GroupSwapRequestDto {
  teamId1: string;
  teamId2: string;
}
```

- [ ] **Step 2: Create group.api.service.ts**

```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupDto, GroupGenerateRequestDto, GroupSwapRequestDto } from '@app/api/group/group.api.dto';

@Injectable({ providedIn: 'root' })
export class GroupApiService {

  private readonly http = inject(HttpClient);

  generate$(tournamentId: string, request: GroupGenerateRequestDto): Observable<GroupDto[]> {
    return this.http.post<GroupDto[]>(`/api/tournaments/${tournamentId}/groups/generate`, request);
  }

  getAll$(tournamentId: string): Observable<GroupDto[]> {
    return this.http.get<GroupDto[]>(`/api/tournaments/${tournamentId}/groups`);
  }

  swap$(tournamentId: string, request: GroupSwapRequestDto): Observable<GroupDto[]> {
    return this.http.post<GroupDto[]>(`/api/tournaments/${tournamentId}/groups/swap`, request);
  }
}
```

- [ ] **Step 3: Create group.api.mapper.ts**

```typescript
import { GroupDto, GroupTeamDto } from '@app/api/group/group.api.dto';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';

export class GroupApiMapper {

  static toDomain(dto: GroupDto): Group {
    return {
      id: dto.id,
      name: dto.name,
      teams: dto.teams.map(GroupApiMapper.teamToDomain),
    };
  }

  private static teamToDomain(dto: GroupTeamDto): Team {
    return {
      id: dto.id,
      name: dto.name,
      organisationId: dto.organisationId,
      paid: false,
      responsibleFirstName: '',
      responsibleLastName: '',
    };
  }
}
```

- [ ] **Step 4: Create group.model.ts**

```typescript
import { Team } from '@app/domain/team/team.model';

export interface Group {
  id: string;
  name: string;
  teams: Team[];
}
```

- [ ] **Step 5: Create group.actions.ts**

```typescript
export class LoadGroups {
  static readonly type = '[Group] Load Groups';
  constructor(public readonly tournamentId: string) {}
}

export class GenerateGroups {
  static readonly type = '[Group] Generate Groups';
  constructor(
    public readonly tournamentId: string,
    public readonly groupSize: number,
  ) {}
}

export class SwapTeams {
  static readonly type = '[Group] Swap Teams';
  constructor(
    public readonly tournamentId: string,
    public readonly teamId1: string,
    public readonly teamId2: string,
  ) {}
}
```

- [ ] **Step 6: Create group.state.ts**

```typescript
import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Group } from '@app/domain/group/group.model';
import { GenerateGroups, LoadGroups, SwapTeams } from '@app/domain/group/group.actions';
import { GroupApiService } from '@app/api/group/group.api.service';
import { GroupApiMapper } from '@app/api/group/group.api.mapper';

export interface IGroupState {
  groups: Group[];
}

@State<IGroupState>({
  name: 'group',
  defaults: { groups: [] },
})
@Injectable()
export class GroupState {

  private readonly groupApiService = inject(GroupApiService);

  @Selector()
  static getGroups(state: IGroupState): Group[] {
    return state.groups;
  }

  @Action(LoadGroups)
  loadGroups(ctx: StateContext<IGroupState>, { tournamentId }: LoadGroups) {
    return this.groupApiService.getAll$(tournamentId).pipe(
      tap(dtos => {
        ctx.patchState({ groups: dtos.map(GroupApiMapper.toDomain) });
      })
    );
  }

  @Action(GenerateGroups)
  generateGroups(ctx: StateContext<IGroupState>, { tournamentId, groupSize }: GenerateGroups) {
    return this.groupApiService.generate$(tournamentId, { groupSize }).pipe(
      tap(dtos => {
        ctx.patchState({ groups: dtos.map(GroupApiMapper.toDomain) });
      })
    );
  }

  @Action(SwapTeams)
  swapTeams(ctx: StateContext<IGroupState>, { tournamentId, teamId1, teamId2 }: SwapTeams) {
    return this.groupApiService.swap$(tournamentId, { teamId1, teamId2 }).pipe(
      tap(dtos => {
        const updatedGroups = dtos.map(GroupApiMapper.toDomain);
        ctx.patchState({
          groups: ctx.getState().groups.map(g => updatedGroups.find(ug => ug.id === g.id) ?? g),
        });
      })
    );
  }
}
```

- [ ] **Step 7: TypeScript compile check**

```bash
cd frontend && npx tsc --noEmit
```
Expected: no errors

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/api/group/ frontend/src/app/domain/group/
git commit -m "FE - Add Group API and domain layer"
```

---

### Task 7: Frontend — Groups page, components, modals, routing

**Files:**
- Create: `frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.ts`
- Create: `frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.scss`
- Create: `frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.ts`
- Create: `frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.html`
- Create: `frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.scss`
- Create: `frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.ts`
- Create: `frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.html`
- Create: `frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.scss`
- Create: `frontend/src/app/display/tournament/components/group-list/group-list.component.ts`
- Create: `frontend/src/app/display/tournament/components/group-list/group-list.component.html`
- Create: `frontend/src/app/display/tournament/components/group-list/group-list.component.scss`
- Modify: `frontend/src/app/modules/tournament.routes.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html`

- [ ] **Step 1: Create GroupListComponent**

`frontend/src/app/display/tournament/components/group-list/group-list.component.ts`:
```typescript
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';

export interface SwapRequest {
  team: Team;
  currentGroupName: string;
}

@Component({
  selector: 'app-group-list',
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.scss',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
})
export class GroupListComponent {
  @Input({ required: true }) groups!: Group[];
  @Input({ required: true }) isDraft!: boolean;
  @Output() swapRequested = new EventEmitter<SwapRequest>();

  requestSwap(team: Team, groupName: string): void {
    this.swapRequested.emit({ team, currentGroupName: groupName });
  }
}
```

`frontend/src/app/display/tournament/components/group-list/group-list.component.html`:
```html
@for (group of groups; track group.id) {
  <div class="group-card">
    <div class="group-header">
      <span class="group-name">Groupe {{ group.name }}</span>
      <span class="team-count">{{ group.teams.length }} équipes</span>
    </div>
    @for (team of group.teams; track team.id) {
      <div class="team-row">
        <span class="team-name">{{ team.name }}</span>
        @if (isDraft) {
          <button mat-icon-button title="Échanger" (click)="requestSwap(team, group.name)">
            <mat-icon>swap_horiz</mat-icon>
          </button>
        }
      </div>
    }
  </div>
}
```

`frontend/src/app/display/tournament/components/group-list/group-list.component.scss`:
```scss
.group-card {
  border: 1px solid var(--mat-sys-outline-variant);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--mat-sys-surface-variant);
  font-weight: 500;

  .group-name { font-size: 16px; }
  .team-count { font-size: 12px; color: var(--mat-sys-on-surface-variant); }
}

.team-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-top: 1px solid var(--mat-sys-outline-variant);

  .team-name { font-size: 14px; }
}
```

- [ ] **Step 2: Create GroupGenerateModal**

`frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.ts`:
```typescript
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { GenerateGroups } from '@app/domain/group/group.actions';

@Component({
  selector: 'app-group-generate-modal',
  templateUrl: './group-generate.modal.html',
  styleUrl: './group-generate.modal.scss',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
})
export class GroupGenerateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<GroupGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      groupSize: [4, [Validators.required, Validators.min(2)]],
    });

    this.actions$.pipe(
      ofActionSuccessful(GenerateGroups),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateGroups(this.data.tournamentId, Number(this.form.value.groupSize)));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

`frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.html`:
```html
<h2 mat-dialog-title>Générer les groupes</h2>

<mat-dialog-content>
  <form [formGroup]="form">
    <mat-form-field appearance="outline">
      <mat-label>Taille des groupes</mat-label>
      <input matInput type="number" min="2" formControlName="groupSize" />
      @if (form.get('groupSize')?.hasError('required')) {
        <mat-error>La taille du groupe est obligatoire</mat-error>
      } @else if (form.get('groupSize')?.hasError('min')) {
        <mat-error>La taille du groupe doit être d'au moins 2</mat-error>
      }
    </mat-form-field>
  </form>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
  <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="submit()">
    Générer
  </button>
</mat-dialog-actions>
```

`frontend/src/app/display/tournament/pages/group-generate/group-generate.modal.scss`: empty file.

- [ ] **Step 3: Create TeamSwapModal**

`frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.ts`:
```typescript
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';
import { SwapTeams } from '@app/domain/group/group.actions';

export interface TeamSwapData {
  tournamentId: string;
  team: Team;
  currentGroupName: string;
  groups: Group[];
}

@Component({
  selector: 'app-team-swap-modal',
  templateUrl: './team-swap.modal.html',
  styleUrl: './team-swap.modal.scss',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
})
export class TeamSwapModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamSwapModal>);
  private readonly actions$ = inject(Actions);
  readonly data = inject<TeamSwapData>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  get otherGroups(): Group[] {
    return this.data.groups.filter(g => g.name !== this.data.currentGroupName);
  }

  ngOnInit(): void {
    this.actions$.pipe(
      ofActionSuccessful(SwapTeams),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  swap(targetTeam: Team): void {
    this.store.dispatch(new SwapTeams(this.data.tournamentId, this.data.team.id, targetTeam.id));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

`frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.html`:
```html
<h2 mat-dialog-title>Échanger {{ data.team.name }}</h2>

<mat-dialog-content>
  <p class="subtitle">Groupe actuel : <strong>{{ data.currentGroupName }}</strong></p>
  <p>Choisissez l'équipe à échanger :</p>

  @for (group of otherGroups; track group.id) {
    <div class="group-section">
      <div class="group-label">Groupe {{ group.name }}</div>
      @for (team of group.teams; track team.id) {
        <button mat-stroked-button class="team-btn" (click)="swap(team)">
          {{ team.name }}
        </button>
      }
    </div>
  }
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
</mat-dialog-actions>
```

`frontend/src/app/display/tournament/pages/team-swap/team-swap.modal.scss`:
```scss
.subtitle { margin-bottom: 8px; color: var(--mat-sys-on-surface-variant); }

.group-section {
  margin-bottom: 16px;

  .group-label {
    font-weight: 500;
    margin-bottom: 8px;
    color: var(--mat-sys-on-surface-variant);
    font-size: 12px;
    text-transform: uppercase;
  }
}

.team-btn {
  display: block;
  width: 100%;
  margin-bottom: 4px;
  text-align: left;
}
```

- [ ] **Step 4: Create TournamentGroupsPage**

`frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.ts`:
```typescript
import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { GroupState } from '@app/domain/group/group.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { GroupListComponent, SwapRequest } from '@app/display/tournament/components/group-list/group-list.component';
import { GroupGenerateModal } from '@app/display/tournament/pages/group-generate/group-generate.modal';
import { TeamSwapModal } from '@app/display/tournament/pages/team-swap/team-swap.modal';

@Component({
  selector: 'app-tournament-groups-page',
  templateUrl: './tournament-groups.page.html',
  styleUrl: './tournament-groups.page.scss',
  standalone: true,
  imports: [AsyncPipe, RouterLink, MatButtonModule, MatIconModule, GroupListComponent],
})
export class TournamentGroupsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<Group[]> = this.store.select(GroupState.getGroups);

  private tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadGroups(this.tournamentId)]);
  }

  openGenerateModal(): void {
    this.dialog.open(GroupGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }

  openSwapModal(groups: Group[], { team, currentGroupName }: SwapRequest): void {
    this.dialog.open(TeamSwapModal, {
      data: { tournamentId: this.tournamentId, team, currentGroupName, groups },
      width: '400px',
    });
  }
}
```

`frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.html`:
```html
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <div class="header-left">
      <button mat-icon-button [routerLink]="['..']">
        <mat-icon>arrow_back</mat-icon>
      </button>
      <div>
        <h1>{{ tournament.name }}</h1>
        <span class="subtitle">Groupes</span>
      </div>
    </div>
    @if (isDraft(tournament)) {
      <button mat-raised-button color="primary" (click)="openGenerateModal()">
        <mat-icon>shuffle</mat-icon>
        Générer les groupes
      </button>
    }
  </div>

  @if (groups$ | async; as groups) {
    @if (groups.length === 0) {
      <div class="empty-state">
        <mat-icon>grid_view</mat-icon>
        <p>Aucun groupe généré</p>
        @if (isDraft(tournament)) {
          <button mat-stroked-button (click)="openGenerateModal()">Générer les groupes</button>
        }
      </div>
    } @else {
      <app-group-list
        [groups]="groups"
        [isDraft]="isDraft(tournament)"
        (swapRequested)="openSwapModal(groups, $event)">
      </app-group-list>
    }
  }
}
```

`frontend/src/app/display/tournament/pages/tournament-groups/tournament-groups.page.scss`:
```scss
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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
  color: var(--mat-sys-on-surface-variant);

  mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; }
  p { margin-bottom: 16px; }
}
```

- [ ] **Step 5: Update tournament.routes.ts**

```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
    ],
  },
];
```

- [ ] **Step 6: Add Groups button to tournament detail page**

In `tournament-detail.page.ts`, add `RouterLink` to imports:
```typescript
imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
```
(already imported — no change needed)

In `tournament-detail.page.html`, add a "Groupes" button in the header after the existing register button:
```html
@if (isDraft(tournament)) {
  <button mat-raised-button color="primary" (click)="openRegisterModal()">
    <mat-icon>group_add</mat-icon>
    Inscrire des équipes
  </button>
  <button mat-stroked-button [routerLink]="[tournament.id, 'groups']" routerLinkActive="active">
    <mat-icon>grid_view</mat-icon>
    Groupes
  </button>
}
```

Replace the existing `@if (isDraft(tournament))` block in the header with:
```html
@if (isDraft(tournament)) {
  <div class="header-actions">
    <button mat-raised-button color="primary" (click)="openRegisterModal()">
      <mat-icon>group_add</mat-icon>
      Inscrire des équipes
    </button>
    <button mat-stroked-button [routerLink]="['..', tournament.id, 'groups']">
      <mat-icon>grid_view</mat-icon>
      Groupes
    </button>
  </div>
}
```

Add `.header-actions { display: flex; gap: 8px; }` to `tournament-detail.page.scss`.

- [ ] **Step 7: TypeScript compile check**

```bash
cd frontend && npx tsc --noEmit
```
Expected: no errors

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/display/tournament/ \
        frontend/src/app/modules/tournament.routes.ts
git commit -m "FE - Add Groups page, modals, and routing for UC-03"
```

---

### Self-Review Checklist

- [x] UC-03.1 distribution endpoint — Task 5 `GroupController.distribution`
- [x] UC-03.2 generate endpoint — Task 5 `GroupController.generate`
- [x] UC-03.3 get groups endpoint — Task 5 `GroupController.getAll`
- [x] UC-03.4 swap endpoint — Task 5 `GroupController.swap`
- [x] Draw algorithm (org round-robin + shuffle remainder) — Task 3 `GroupService.computeDraw`
- [x] Group naming A-Z, AA-AB… — Task 3 `GroupService.groupName`
- [x] Distribution algorithm — Task 3 `GroupService.computeDistribution`
- [x] Validation rules (groupSize null/too small/not enough teams) — Task 2 `GroupValidator`
- [x] Tournament DRAFT check on generate and swap — Task 3 `GroupService`
- [x] Delete existing groups on regenerate — Task 3 step 5
- [x] Tests: generate happy path, not-DRAFT, groupSize<2, not enough teams — Task 3
- [x] Tests: distribution happy path, groupSize<2 — Task 3
- [x] Tests: swap happy path, same group, team not found, not-DRAFT — Task 3
- [x] Tests: findAllByTournamentId — Task 3
- [x] Frontend: LoadGroups, GenerateGroups, SwapTeams actions — Task 6
- [x] Frontend: GroupState with selector — Task 6
- [x] Frontend: TournamentGroupsPage, GroupListComponent, GroupGenerateModal, TeamSwapModal — Task 7
- [x] Frontend: groups route + navigation button on detail page — Task 7
- [x] Architecture tests pass (packages in correct layers) — Task 5 step 8
