# UC-04 — Generate Group Stage Schedule — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement round-robin group stage schedule generation: matches per group, assigned to fields, organised into global rounds with computed start times, with full BE API and Angular FE.

**Architecture:** Backend follows layer-first directories (`api/schedule/`, `domain/schedule/`, `persistence/schedule/`) with feature-first package names (`schedule.api`, `schedule.domain`, `schedule.persistence`). A `ScheduleService` runs the algorithm entirely in memory, persists via `RoundStore`/`MatchStore`, and assembles a `ScheduleOverview` projection. Frontend adds NGXS `ScheduleState`, an API service, a schedule page, and a generate modal.

**Tech Stack:** Java 21, Spring Boot, Liquibase (SQLite dialect), Lombok, Mockito + JUnit 5 (BE); Angular 21, TypeScript strict, NGXS, Angular Material (FE).

## Global Constraints

- All local variables: `final var`; `var` only when reassigned
- No nested calls — break into `final var` steps, one operation per line
- Stream chains: one method per line, chained with indentation
- Multi-line ternary: condition line 1, `?` line 2, `:` line 3
- `@Transactional` per method on `JpaXxxStore` only — never on services or controllers
- `@Transactional(readOnly = true)` on all read methods
- JPA entities and Spring Data repositories are package-private (not public)
- `@UtilityClass` on validators, DB mappers, API mappers — makes them final (required by ArchUnit)
- All method parameters `final`
- DTOs: `@Value @Builder @Jacksonized` — request DTOs carry validation annotations, response DTOs do not
- Domain classes: `@Value @Builder @With`
- `@Mock private` + `@InjectMocks private` + `@ExtendWith(MockitoExtension.class)` in tests
- No `ArgumentCaptor` — use `returnsFirstArg()` or assert on return value
- No AssertJ — JUnit 5 assertions only
- Angular: standalone components, `inject()` not constructor injection, `@app/` alias
- Test fakes: `@UtilityClass`, prefix `build`, always include ID via package-private `IdGenerator`
- Imports run command from project root: `cd /path/to/fvjc-sport-tournament`

---

## File Map

### Task 0 — Rename View → Overview (backend refactor)
| Action | Path |
|--------|------|
| Rename class + file | `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupView.java` → `GroupOverview.java` |
| Rename class + file | `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java` → `TeamOverview.java` |
| Update references | `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupService.java` |
| Update references | `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java` |
| Update references | `backend/src/main/java/abe/fvjc/tournament/api/group/GroupApiMapper.java` |
| Update references | `backend/src/main/java/abe/fvjc/tournament/api/team/TeamApiMapper.java` |

### Task 1 — BE domain types
| Action | Path |
|--------|------|
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/RoundId.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/Round.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchId.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/Match.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/TeamRef.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchOverview.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/RoundOverview.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleOverview.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleGenerateRequest.java` |

### Task 2 — BE store interfaces + validator
| Action | Path |
|--------|------|
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/RoundStore.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchStore.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleValidator.java` |

### Task 3 — DB migrations
| Action | Path |
|--------|------|
| Create | `backend/src/main/resources/db/changelog/20260703120002_create_rounds_table.xml` |
| Create | `backend/src/main/resources/db/changelog/20260703120003_create_matches_table.xml` |
| Modify | `backend/src/main/resources/db/master.xml` |

### Task 4 — BE persistence layer
| Action | Path |
|--------|------|
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/RoundEntity.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/RoundRepository.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/RoundDbMapper.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/JpaRoundStore.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchEntity.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchRepository.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchDbMapper.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/JpaMatchStore.java` |

### Task 5 — BE ScheduleService (TDD)
| Action | Path |
|--------|------|
| Create | `backend/src/test/java/abe/fvjc/tournament/schedule/domain/IdGenerator.java` |
| Create | `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleFakes.java` |
| Create | `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java` |

### Task 6 — BE API layer
| Action | Path |
|--------|------|
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/ScheduleGenerateRequestDto.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/MatchTeamDto.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/MatchDto.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/RoundDto.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/ScheduleDto.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/ScheduleApiMapper.java` |
| Create | `backend/src/main/java/abe/fvjc/tournament/api/schedule/ScheduleController.java` |

### Task 7 — FE API layer
| Action | Path |
|--------|------|
| Create | `frontend/src/app/api/schedule/schedule.api.dto.ts` |
| Create | `frontend/src/app/api/schedule/schedule.api.service.ts` |
| Create | `frontend/src/app/api/schedule/schedule.api.mapper.ts` |

### Task 8 — FE domain layer
| Action | Path |
|--------|------|
| Create | `frontend/src/app/domain/schedule/schedule.model.ts` |
| Create | `frontend/src/app/domain/schedule/schedule.actions.ts` |
| Create | `frontend/src/app/domain/schedule/schedule.state.ts` |

### Task 9 — FE display + routes
| Action | Path |
|--------|------|
| Create | `frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.ts` |
| Create | `frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.html` |
| Create | `frontend/src/app/display/tournament/pages/schedule-generate/schedule-generate.modal.ts` |
| Create | `frontend/src/app/display/tournament/pages/schedule-generate/schedule-generate.modal.html` |
| Modify | `frontend/src/app/modules/tournament.routes.ts` |
| Modify | `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html` |

---

## Task 0: Rename View → Overview

**Files:** See File Map above — no new files, pure rename.

**Interfaces:**
- Produces: `GroupOverview` (replaces `GroupView`), `TeamOverview` (replaces `TeamView`) — same fields, new class names

- [ ] **Step 1: Rename GroupView.java to GroupOverview.java**

Replace the entire content of `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupView.java`:

```java
package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupOverview {
    GroupId id;
    String name;
    TournamentId tournamentId;
    List<Team> teams;
}
```

Then rename the file: `mv backend/src/main/java/abe/fvjc/tournament/domain/group/GroupView.java backend/src/main/java/abe/fvjc/tournament/domain/group/GroupOverview.java`

- [ ] **Step 2: Rename TeamView.java to TeamOverview.java**

Replace the entire content of `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java`:

```java
package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamOverview {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    Person responsible;
    TournamentId tournamentId;
}
```

Then rename: `mv backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java backend/src/main/java/abe/fvjc/tournament/domain/team/TeamOverview.java`

- [ ] **Step 3: Update GroupService.java — replace GroupView with GroupOverview**

In `backend/src/main/java/abe/fvjc/tournament/domain/group/GroupService.java`, replace every occurrence of `GroupView` with `GroupOverview` (return types, local variable types, builder calls, method names `toView` → `toOverview`, `buildViews` → `buildOverviews`):

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

    public List<GroupOverview> generate(final UUID tournamentId, final GroupGenerateRequest request) {
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

        return buildOverviews(groups);
    }

    public List<GroupOverview> findAllByTournamentId(final UUID tournamentId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        return groups.stream()
                .map(this::toOverview)
                .toList();
    }

    public List<GroupOverview> swap(final UUID tournamentId, final GroupSwapRequest request) {
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

        return List.of(toOverview(group1), toOverview(group2));
    }

    private GroupOverview toOverview(final Group group) {
        final var teams = teamStore.findAllByGroupId(group.getId().value());
        return GroupOverview.builder()
            .id(group.getId())
            .name(group.getName())
            .tournamentId(group.getTournamentId())
            .teams(teams)
            .build();
    }

    private List<GroupOverview> buildOverviews(final List<Group> groups) {
        return groups.stream().map(this::toOverview).toList();
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les groupes ne peuvent être générés que pour un tournoi en cours de préparation");
        }
    }

    private static GroupDistribution computeDistribution(final int totalTeams, final int groupSize) {
        var numberOfGroups = totalTeams / groupSize;
        if (totalTeams % groupSize > numberOfGroups) {
            numberOfGroups++;
        }
        final var baseSize = totalTeams / numberOfGroups;
        final var groupsOfBaseSizePlusOne = totalTeams % numberOfGroups;
        final var groupsOfBaseSize = numberOfGroups - groupsOfBaseSizePlusOne;
        return GroupDistribution.builder()
                .numberOfGroups(numberOfGroups)
                .groupsOfBaseSizePlusOne(groupsOfBaseSizePlusOne)
                .groupsOfBaseSize(groupsOfBaseSize)
                .baseSize(baseSize)
                .totalTeams(totalTeams)
                .build();
    }

    private static List<List<Team>> computeDraw(final List<Team> teams, final int groupSize) {
        final var totalTeams = teams.size();
        var numberOfGroups = totalTeams / groupSize;
        if (totalTeams % groupSize > numberOfGroups) {
            numberOfGroups++;
        }
        final var baseSize = totalTeams / numberOfGroups;
        final var groupsOfBaseSizePlusOne = totalTeams % numberOfGroups;

        final var capacities = new int[numberOfGroups];
        for (int i = 0; i < numberOfGroups; i++) {
            capacities[i] = i < groupsOfBaseSizePlusOne ? baseSize + 1 : baseSize;
        }

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
        var cursor = 0;
        for (final var orgTeams : sortedOrgs) {
            for (final var team : orgTeams) {
                groupSlots.get(cursor % numberOfGroups).add(team);
                assigned.add(team.getId().value());
                cursor++;
            }
        }

        final var remaining = teams.stream()
                .filter(t -> !assigned.contains(t.getId().value()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(remaining);

        for (int i = 0; i < numberOfGroups; i++) {
            while (groupSlots.get(i).size() < capacities[i] && !remaining.isEmpty()) {
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

- [ ] **Step 4: Update TeamService.java — replace TeamView with TeamOverview**

In `backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java`, replace all `TeamView` references with `TeamOverview` and `buildTeamView` with `buildTeamOverview`:

Key changes (show only the changed signatures and builder line — the rest of the method bodies are identical):
- `public List<TeamOverview> registerTeams(...)` 
- `public List<TeamOverview> findAllByTournamentId(...)`
- `public TeamOverview updateTeam(...)`
- `public TeamOverview markPaid(...)`
- `private static TeamOverview buildTeamOverview(final Team team, final Organisation org)`
- Builder call: `return TeamOverview.builder()...`

Full updated file:

```java
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

@Service
@RequiredArgsConstructor
public class TeamService {
    private final OrganisationStore organisationStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public List<TeamOverview> registerTeams(final UUID tournamentId, final TeamRegisterRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        validateTeamRegisterRequest(request);
        final var org = organisationStore.save(Organisation.builder()
            .id(OrganisationId.of(UUID.randomUUID()))
            .responsible(request.getResponsible())
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
                return buildTeamOverview(team, org);
            })
            .toList();
    }

    public List<TeamOverview> findAllByTournamentId(final UUID tournamentId) {
        return teamStore.findAllByTournamentId(tournamentId).stream()
            .map(team -> {
                final var org = organisationStore.findById(team.getOrganisationId().value())
                    .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
                return buildTeamOverview(team, org);
            })
            .toList();
    }

    public TeamOverview updateTeam(final UUID tournamentId, final UUID teamId, final TeamUpdateRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var org = organisationStore.findById(team.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
        final var updatedOrg = organisationStore.save(org.withResponsible(request.getResponsible()));
        final var updatedTeam = teamStore.save(team
            .withName(request.getName())
            .withPaid(request.isPaid()));
        return buildTeamOverview(updatedTeam, updatedOrg);
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

    public TeamOverview markPaid(final UUID teamId, final boolean paid) {
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var updatedTeam = teamStore.save(team.withPaid(paid));
        final var org = organisationStore.findById(updatedTeam.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", updatedTeam.getOrganisationId().value()));
        return buildTeamOverview(updatedTeam, org);
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation");
        }
    }

    private static TeamOverview buildTeamOverview(final Team team, final Organisation org) {
        return TeamOverview.builder()
            .id(team.getId())
            .name(team.getName())
            .paid(team.isPaid())
            .organisationId(team.getOrganisationId())
            .responsible(org.getResponsible())
            .tournamentId(team.getTournamentId())
            .build();
    }
}
```

- [ ] **Step 5: Update GroupApiMapper.java — replace GroupView with GroupOverview**

```java
package abe.fvjc.tournament.group.api;

import abe.fvjc.tournament.group.domain.GroupDistribution;
import abe.fvjc.tournament.group.domain.GroupGenerateRequest;
import abe.fvjc.tournament.group.domain.GroupOverview;
import abe.fvjc.tournament.group.domain.GroupSwapRequest;
import abe.fvjc.tournament.team.domain.Team;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupApiMapper {
    static GroupDto toGroupDto(final GroupOverview overview) {
        return GroupDto.builder()
                .id(overview.getId().value())
                .name(overview.getName())
                .teams(overview.getTeams()
                        .stream()
                        .map(GroupApiMapper::toGroupTeamDto)
                        .toList())
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

- [ ] **Step 6: Update TeamApiMapper.java — replace TeamView with TeamOverview**

```java
package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.team.domain.TeamOverview;
import abe.fvjc.tournament.team.domain.TeamRegisterRequest;
import abe.fvjc.tournament.team.domain.TeamUpdateRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeamApiMapper {

    static TeamDto toTeamDto(final TeamOverview overview) {
        return TeamDto.builder()
                .id(overview.getId().value())
                .name(overview.getName())
                .paid(overview.isPaid())
                .organisationId(overview.getOrganisationId().value())
                .responsibleFirstName(overview.getResponsible().getFirstName())
                .responsibleLastName(overview.getResponsible().getLastName())
                .build();
    }

    static TeamRegisterRequest toTeamRegisterRequest(final TeamRegisterRequestDto dto) {
        final var responsible = Person.builder()
                .firstName(dto.getResponsibleFirstName())
                .lastName(dto.getResponsibleLastName())
                .build();
        return TeamRegisterRequest.builder()
                .name(dto.getName())
                .responsible(responsible)
                .count(dto.getCount())
                .paid(dto.getPaid())
                .build();
    }

    static TeamUpdateRequest toTeamUpdateRequest(final TeamUpdateRequestDto dto) {
        final var responsible = Person.builder()
                .firstName(dto.getResponsibleFirstName())
                .lastName(dto.getResponsibleLastName())
                .build();
        return TeamUpdateRequest.builder()
                .name(dto.getName())
                .responsible(responsible)
                .paid(dto.getPaid())
                .build();
    }
}
```

- [ ] **Step 7: Build and verify no compilation errors**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS (no output on `-q`).

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/group/GroupOverview.java \
        backend/src/main/java/abe/fvjc/tournament/domain/team/TeamOverview.java \
        backend/src/main/java/abe/fvjc/tournament/domain/group/GroupService.java \
        backend/src/main/java/abe/fvjc/tournament/domain/team/TeamService.java \
        backend/src/main/java/abe/fvjc/tournament/api/group/GroupApiMapper.java \
        backend/src/main/java/abe/fvjc/tournament/api/team/TeamApiMapper.java && \
git rm backend/src/main/java/abe/fvjc/tournament/domain/group/GroupView.java \
       backend/src/main/java/abe/fvjc/tournament/domain/team/TeamView.java && \
git commit -m "REFACTOR - Rename GroupView → GroupOverview, TeamView → TeamOverview"
```

---

## Task 1: BE Domain Types

**Files:** See File Map — 9 new files in `backend/src/main/java/abe/fvjc/tournament/domain/schedule/`

**Interfaces:**
- Produces: `RoundId`, `Round`, `MatchId`, `Match`, `TeamRef`, `MatchOverview`, `RoundOverview`, `ScheduleOverview`, `ScheduleGenerateRequest` — consumed by Tasks 2, 4, 5, 6

- [ ] **Step 1: Create RoundId.java**

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.UUID;

public record RoundId(UUID value) {

    public static RoundId of(final UUID value) {
        return new RoundId(value);
    }

    public static RoundId empty() {
        return new RoundId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 2: Create Round.java**

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
@With
public class Round {
    RoundId id;
    TournamentId tournamentId;
    int number;
    LocalDateTime startTime;
}
```

- [ ] **Step 3: Create MatchId.java**

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.UUID;

public record MatchId(UUID value) {

    public static MatchId of(final UUID value) {
        return new MatchId(value);
    }

    public static MatchId empty() {
        return new MatchId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 4: Create Match.java**

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.team.domain.TeamId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Match {
    MatchId id;
    RoundId roundId;
    int field;
    GroupId groupId;
    TeamId team1Id;
    TeamId team2Id;
}
```

- [ ] **Step 5: Create TeamRef.java**

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.team.domain.TeamId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamRef {
    TeamId id;
    String name;
}
```

- [ ] **Step 6: Create MatchOverview.java**

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchOverview {
    MatchId id;
    int field;
    GroupId groupId;
    String groupName;
    TeamRef team1;
    TeamRef team2;
}
```

- [ ] **Step 7: Create RoundOverview.java**

```java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class RoundOverview {
    RoundId id;
    int number;
    LocalDateTime startTime;
    List<MatchOverview> matches;
}
```

- [ ] **Step 8: Create ScheduleOverview.java**

```java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ScheduleOverview {
    int totalRounds;
    int totalMatches;
    List<RoundOverview> rounds;
}
```

- [ ] **Step 9: Create ScheduleGenerateRequest.java**

```java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScheduleGenerateRequest {
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
```

- [ ] **Step 10: Compile**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ && \
git commit -m "UC-04 - BE domain types (Round, Match, Overviews)"
```

---

## Task 2: BE Store Interfaces + Validator

**Files:** `RoundStore.java`, `MatchStore.java`, `ScheduleValidator.java`

**Interfaces:**
- Produces: `RoundStore`, `MatchStore`, `ScheduleValidator.validateScheduleGenerateRequest` — consumed by Tasks 4, 5

- [ ] **Step 1: Create RoundStore.java**

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.UUID;

public interface RoundStore {
    void saveAll(List<Round> rounds);
    List<Round> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 2: Create MatchStore.java**

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.UUID;

public interface MatchStore {
    void saveAll(List<Match> matches);
    List<Match> findAllByRoundIds(List<UUID> roundIds);
    void deleteAllByRoundIds(List<UUID> roundIds);
    boolean existsResultByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 3: Create ScheduleValidator.java**

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

@UtilityClass
public class ScheduleValidator {

    public static void validateScheduleGenerateRequest(final ScheduleGenerateRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getStartTime() == null) {
            errors.add(new ValidationException.FieldError("startTime", "L'heure de début est obligatoire"));
        } else {
            try {
                LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                errors.add(new ValidationException.FieldError("startTime", "L'heure de début est obligatoire"));
            }
        }

        if (request.getMatchDurationMinutes() == null) {
            errors.add(new ValidationException.FieldError("matchDurationMinutes", "La durée d'un match est obligatoire"));
        } else if (request.getMatchDurationMinutes() < 1) {
            errors.add(new ValidationException.FieldError("matchDurationMinutes", "La durée d'un match doit être d'au moins 1 minute"));
        }

        if (request.getBreakDurationMinutes() == null) {
            errors.add(new ValidationException.FieldError("breakDurationMinutes", "La durée de pause est obligatoire"));
        } else if (request.getBreakDurationMinutes() < 0) {
            errors.add(new ValidationException.FieldError("breakDurationMinutes", "La durée de pause ne peut pas être négative"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

- [ ] **Step 4: Compile**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ && \
git commit -m "UC-04 - BE store interfaces and ScheduleValidator"
```

---

## Task 3: DB Migrations

**Files:** 2 new Liquibase changesets + update to master.xml

**Interfaces:**
- Produces: `rounds` and `matches` tables in the DB — required by Task 4

- [ ] **Step 1: Create rounds table changeset**

`backend/src/main/resources/db/changelog/20260703120002_create_rounds_table.xml`:

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260703120002" author="aberney">
        <createTable tableName="rounds">
            <column name="id" type="TEXT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tournament_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="number" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="start_time" type="TEXT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Create matches table changeset**

`backend/src/main/resources/db/changelog/20260703120003_create_matches_table.xml`:

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260703120003" author="aberney">
        <createTable tableName="matches">
            <column name="id" type="TEXT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="round_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="field" type="INT">
                <constraints nullable="false"/>
            </column>
            <column name="group_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="team1_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="team2_id" type="TEXT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 3: Register both changesets in master.xml**

Add two `<include>` lines after the last existing one in `backend/src/main/resources/db/master.xml`:

```xml
    <include relativeToChangelogFile="true" file="changelog/20260703120002_create_rounds_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260703120003_create_matches_table.xml"/>
```

Full updated master.xml:

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

    <include relativeToChangelogFile="true" file="changelog/20260701120000_create_tournaments_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260702120000_create_organisations_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260702120001_create_teams_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260703120000_alter_teams_add_group_id.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260703120001_create_groups_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260703120002_create_rounds_table.xml"/>
    <include relativeToChangelogFile="true" file="changelog/20260703120003_create_matches_table.xml"/>
</databaseChangeLog>
```

- [ ] **Step 4: Run tests to verify migrations apply cleanly**

```bash
cd backend && ./mvnw test -Dtest=TournamentApplicationTests -q
```

Expected: BUILD SUCCESS (app context loads with both new tables).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/ && \
git commit -m "UC-04 - DB migrations: rounds and matches tables"
```

---

## Task 4: BE Persistence Layer

**Files:** `RoundEntity`, `RoundRepository`, `RoundDbMapper`, `JpaRoundStore`, `MatchEntity`, `MatchRepository`, `MatchDbMapper`, `JpaMatchStore`

**Interfaces:**
- Consumes: `Round`, `RoundId`, `Match`, `MatchId`, `GroupId`, `TeamId`, `TournamentId`, `RoundStore`, `MatchStore`
- Produces: concrete implementations of `RoundStore` and `MatchStore`

- [ ] **Step 1: Create RoundEntity.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
class RoundEntity {
    @Id
    private UUID id;
    private UUID tournamentId;
    private int number;
    private String startTime;
}
```

- [ ] **Step 2: Create RoundRepository.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RoundRepository extends JpaRepository<RoundEntity, UUID> {
    List<RoundEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 3: Create RoundDbMapper.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Round;
import abe.fvjc.tournament.schedule.domain.RoundId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
class RoundDbMapper {

    static Round toRound(final RoundEntity entity) {
        return Round.builder()
                .id(RoundId.of(entity.getId()))
                .tournamentId(TournamentId.of(entity.getTournamentId()))
                .number(entity.getNumber())
                .startTime(LocalDateTime.parse(entity.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    static RoundEntity toRoundEntity(final Round round) {
        final var entity = new RoundEntity();
        entity.setId(round.getId().value());
        entity.setTournamentId(round.getTournamentId().value());
        entity.setNumber(round.getNumber());
        entity.setStartTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }
}
```

- [ ] **Step 4: Create JpaRoundStore.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Round;
import abe.fvjc.tournament.schedule.domain.RoundStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaRoundStore implements RoundStore {
    private final RoundRepository roundRepository;

    @Override
    @Transactional
    public void saveAll(final List<Round> rounds) {
        final var entities = rounds.stream()
                .map(RoundDbMapper::toRoundEntity)
                .toList();
        roundRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Round> findAllByTournamentId(final UUID tournamentId) {
        return roundRepository.findByTournamentId(tournamentId).stream()
                .map(RoundDbMapper::toRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        roundRepository.deleteByTournamentId(tournamentId);
    }
}
```

- [ ] **Step 5: Create MatchEntity.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
class MatchEntity {
    @Id
    private UUID id;
    private UUID roundId;
    private int field;
    private UUID groupId;
    private UUID team1Id;
    private UUID team2Id;
}
```

- [ ] **Step 6: Create MatchRepository.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findByRoundIdIn(List<UUID> roundIds);
    void deleteByRoundIdIn(List<UUID> roundIds);
}
```

- [ ] **Step 7: Create MatchDbMapper.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchId;
import abe.fvjc.tournament.schedule.domain.RoundId;
import abe.fvjc.tournament.team.domain.TeamId;
import lombok.experimental.UtilityClass;

@UtilityClass
class MatchDbMapper {

    static Match toMatch(final MatchEntity entity) {
        return Match.builder()
                .id(MatchId.of(entity.getId()))
                .roundId(RoundId.of(entity.getRoundId()))
                .field(entity.getField())
                .groupId(GroupId.of(entity.getGroupId()))
                .team1Id(TeamId.of(entity.getTeam1Id()))
                .team2Id(TeamId.of(entity.getTeam2Id()))
                .build();
    }

    static MatchEntity toMatchEntity(final Match match) {
        final var entity = new MatchEntity();
        entity.setId(match.getId().value());
        entity.setRoundId(match.getRoundId().value());
        entity.setField(match.getField());
        entity.setGroupId(match.getGroupId().value());
        entity.setTeam1Id(match.getTeam1Id().value());
        entity.setTeam2Id(match.getTeam2Id().value());
        return entity;
    }
}
```

- [ ] **Step 8: Create JpaMatchStore.java**

```java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaMatchStore implements MatchStore {
    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public void saveAll(final List<Match> matches) {
        final var entities = matches.stream()
                .map(MatchDbMapper::toMatchEntity)
                .toList();
        matchRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByRoundIds(final List<UUID> roundIds) {
        return matchRepository.findByRoundIdIn(roundIds).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundIds(final List<UUID> roundIds) {
        matchRepository.deleteByRoundIdIn(roundIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsResultByTournamentId(final UUID tournamentId) {
        return false; // No result columns until UC-05 adds them
    }
}
```

- [ ] **Step 9: Run all tests**

```bash
cd backend && ./mvnw test -q
```

Expected: BUILD SUCCESS — all existing tests still pass, app context loads with new persistence beans.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/persistence/schedule/ && \
git commit -m "UC-04 - BE persistence layer (Round + Match)"
```

---

## Task 5: BE ScheduleService (TDD)

**Files:** `IdGenerator.java`, `ScheduleFakes.java`, `ScheduleServiceTest.java` (test), `ScheduleService.java` (main)

**Interfaces:**
- Consumes: `RoundStore`, `MatchStore`, `GroupStore`, `TeamStore`, `TournamentStore`, `ScheduleValidator`, all domain types from Tasks 1–2
- Produces: `ScheduleService.generate(UUID, ScheduleGenerateRequest) → ScheduleOverview`, `ScheduleService.findByTournamentId(UUID) → ScheduleOverview`

- [ ] **Step 1: Create test IdGenerator.java**

`backend/src/test/java/abe/fvjc/tournament/schedule/domain/IdGenerator.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static RoundId roundId() {
        return RoundId.of(UUID.randomUUID());
    }

    static MatchId matchId() {
        return MatchId.of(UUID.randomUUID());
    }
}
```

- [ ] **Step 2: Create ScheduleFakes.java**

`backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleFakes.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class ScheduleFakes {

    public static ScheduleGenerateRequest buildGenerateRequest() {
        return ScheduleGenerateRequest.builder()
                .startTime("09:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static Round buildRound(final TournamentId tournamentId) {
        return Round.builder()
                .id(IdGenerator.roundId())
                .tournamentId(tournamentId)
                .number(1)
                .startTime(LocalDateTime.of(2027, 8, 15, 9, 0))
                .build();
    }

    public static Match buildMatch(final RoundId roundId) {
        return Match.builder()
                .id(IdGenerator.matchId())
                .roundId(roundId)
                .field(1)
                .groupId(GroupId.of(UUID.randomUUID()))
                .team1Id(TeamId.of(UUID.randomUUID()))
                .team2Id(TeamId.of(UUID.randomUUID()))
                .build();
    }
}
```

- [ ] **Step 3: Write ScheduleServiceTest.java (all tests — they will fail until Step 6)**

`backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
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

import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private RoundStore roundStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void generateWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var tournamentId = UUID.randomUUID();
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> scheduleService.generate(tournamentId, request));

        verify(tournamentStore).findById(tournamentId);
    }

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void generateWhenNoGroupsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());

        final var exception = assertThrows(ValidationException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
        verify(groupStore).findAllByTournamentId(tournament.getId().value());

        assertEquals("groups", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenResultsExistShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var group = GroupFakes.buildGroup(tournament.getId());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(matchStore.existsResultByTournamentId(tournament.getId().value())).thenReturn(true);

        assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(matchStore).existsResultByTournamentId(tournament.getId().value());
    }

    @Test
    void generateWhenValidShouldReturnScheduleOverview() {
        final var tournament = buildTournament(); // 4 fields
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        // 3 teams per group → 3 round-robin matches each; 2 groups on 2 different fields → 3 rounds, 6 matches
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t5 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t6 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(matchStore.existsResultByTournamentId(tournament.getId().value())).thenReturn(false);
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2, t3, t4, t5, t6));

        final var scheduleGenerated = scheduleService.generate(tournament.getId().value(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(6, scheduleGenerated.getTotalMatches());
        assertEquals(3, scheduleGenerated.getRounds().size());
        assertEquals(2, scheduleGenerated.getRounds().get(0).getMatches().size());
        assertEquals(1, scheduleGenerated.getRounds().get(0).getNumber());
    }

    @Test
    void generateWhenExistingRoundsShouldDeleteBeforePersisting() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId);
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var existingRound = buildRound(tournament.getId());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(matchStore.existsResultByTournamentId(tournament.getId().value())).thenReturn(false);
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(existingRound));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2, t3));

        scheduleService.generate(tournament.getId().value(), request);

        verify(matchStore).deleteAllByRoundIds(anyList());
        verify(roundStore).deleteAllByTournamentId(tournament.getId().value());
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
    }

    @Test
    void findByTournamentIdWhenNoRoundsShouldReturnEmptyOverview() {
        final var tournamentId = UUID.randomUUID();

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        final var scheduleFound = scheduleService.findByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);

        assertEquals(0, scheduleFound.getTotalRounds());
        assertEquals(0, scheduleFound.getTotalMatches());
        assertTrue(scheduleFound.getRounds().isEmpty());
    }

    @Test
    void findByTournamentIdWhenRoundsExistShouldReturnPopulatedOverview() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId).withName("A");
        final var round = buildRound(tournament.getId());
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId());

        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(round));
        when(matchStore.findAllByRoundIds(anyList())).thenReturn(List.of(match));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2));

        final var scheduleFound = scheduleService.findByTournamentId(tournament.getId().value());

        verify(roundStore).findAllByTournamentId(tournament.getId().value());
        verify(matchStore).findAllByRoundIds(anyList());

        assertEquals(1, scheduleFound.getTotalRounds());
        assertEquals(1, scheduleFound.getTotalMatches());
        assertEquals(1, scheduleFound.getRounds().size());
        assertEquals("A", scheduleFound.getRounds().get(0).getMatches().get(0).getGroupName());
        assertEquals(t1.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam1().getName());
        assertEquals(t2.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam2().getName());
    }
}
```

- [ ] **Step 4: Run tests to confirm they all fail (class not found)**

```bash
cd backend && ./mvnw test -Dtest=ScheduleServiceTest 2>&1 | tail -5
```

Expected: COMPILATION ERROR or test failures — `ScheduleService` does not exist yet.

- [ ] **Step 5: Create ScheduleService.java**

`backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.schedule.domain.ScheduleValidator.validateScheduleGenerateRequest;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final RoundStore roundStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public ScheduleOverview generate(final UUID tournamentId, final ScheduleGenerateRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new ConflictException("Le calendrier ne peut être généré que pour un tournoi en préparation");
        }
        validateScheduleGenerateRequest(request);
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        if (groups.isEmpty()) {
            throw new ValidationException(List.of(new ValidationException.FieldError(
                    "groups", "Aucun groupe n'a été généré pour ce tournoi")));
        }
        if (matchStore.existsResultByTournamentId(tournamentId)) {
            throw new ConflictException("Impossible de régénérer le calendrier : des résultats ont déjà été saisis");
        }
        final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
        if (!existingRounds.isEmpty()) {
            final var roundIds = existingRounds.stream()
                    .map(r -> r.getId().value())
                    .toList();
            matchStore.deleteAllByRoundIds(roundIds);
            roundStore.deleteAllByTournamentId(tournamentId);
        }
        final var allTeams = teamStore.findAllByTournamentId(tournamentId);
        final var teamsByGroupId = allTeams.stream()
                .collect(Collectors.groupingBy(t -> t.getGroupId().value()));
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var matchesByGroupId = new LinkedHashMap<UUID, List<MatchPair>>();
        for (final var group : groups) {
            final var groupTeams = teamsByGroupId.getOrDefault(group.getId().value(), List.of());
            final var teamIds = groupTeams.stream().map(Team::getId).toList();
            matchesByGroupId.put(group.getId().value(), generateGroupMatches(group.getId(), teamIds));
        }
        final var fieldQueues = buildFieldQueues(groups, matchesByGroupId, tournament.getNumberOfFields());
        final var totalRounds = fieldQueues.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        final var startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        final var firstRoundStart = LocalDateTime.of(tournament.getDate(), startTime);
        final var rounds = new ArrayList<Round>();
        final var matches = new ArrayList<Match>();
        for (int r = 0; r < totalRounds; r++) {
            final var roundStart = firstRoundStart.plusMinutes(
                    (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var roundId = RoundId.of(UUID.randomUUID());
            rounds.add(Round.builder()
                    .id(roundId)
                    .tournamentId(TournamentId.of(tournamentId))
                    .number(r + 1)
                    .startTime(roundStart)
                    .build());
            for (final var entry : fieldQueues.entrySet()) {
                final var field = entry.getKey();
                final var queue = entry.getValue();
                if (r < queue.size()) {
                    final var pair = queue.get(r);
                    matches.add(Match.builder()
                            .id(MatchId.of(UUID.randomUUID()))
                            .roundId(roundId)
                            .field(field)
                            .groupId(pair.groupId())
                            .team1Id(pair.team1Id())
                            .team2Id(pair.team2Id())
                            .build());
                }
            }
        }
        roundStore.saveAll(rounds);
        matchStore.saveAll(matches);
        return buildScheduleOverview(rounds, matches, groupById, teamById);
    }

    public ScheduleOverview findByTournamentId(final UUID tournamentId) {
        final var rounds = roundStore.findAllByTournamentId(tournamentId);
        if (rounds.isEmpty()) {
            return ScheduleOverview.builder()
                    .totalRounds(0)
                    .totalMatches(0)
                    .rounds(List.of())
                    .build();
        }
        final var roundIds = rounds.stream().map(r -> r.getId().value()).toList();
        final var matches = matchStore.findAllByRoundIds(roundIds);
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var allTeams = teamStore.findAllByTournamentId(tournamentId);
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return buildScheduleOverview(rounds, matches, groupById, teamById);
    }

    private static ScheduleOverview buildScheduleOverview(
            final List<Round> rounds,
            final List<Match> matches,
            final Map<UUID, Group> groupById,
            final Map<UUID, Team> teamById) {
        final var matchesByRoundId = matches.stream()
                .collect(Collectors.groupingBy(m -> m.getRoundId().value()));
        final var roundOverviews = rounds.stream()
                .map(r -> {
                    final var roundMatches = matchesByRoundId.getOrDefault(r.getId().value(), List.of());
                    final var matchOverviews = roundMatches.stream()
                            .map(m -> MatchOverview.builder()
                                    .id(m.getId())
                                    .field(m.getField())
                                    .groupId(m.getGroupId())
                                    .groupName(groupById.get(m.getGroupId().value()).getName())
                                    .team1(TeamRef.builder()
                                            .id(m.getTeam1Id())
                                            .name(teamById.get(m.getTeam1Id().value()).getName())
                                            .build())
                                    .team2(TeamRef.builder()
                                            .id(m.getTeam2Id())
                                            .name(teamById.get(m.getTeam2Id().value()).getName())
                                            .build())
                                    .build())
                            .toList();
                    return RoundOverview.builder()
                            .id(r.getId())
                            .number(r.getNumber())
                            .startTime(r.getStartTime())
                            .matches(matchOverviews)
                            .build();
                })
                .toList();
        return ScheduleOverview.builder()
                .totalRounds(rounds.size())
                .totalMatches(matches.size())
                .rounds(roundOverviews)
                .build();
    }

    private record MatchPair(GroupId groupId, TeamId team1Id, TeamId team2Id) {}

    private static List<MatchPair> generateGroupMatches(final GroupId groupId, final List<TeamId> teams) {
        final var circle = new ArrayList<>(teams);
        if (teams.size() % 2 != 0) {
            circle.add(null);
        }
        final var circleSize = circle.size();
        final var pairs = new ArrayList<MatchPair>();
        for (int r = 0; r < circleSize - 1; r++) {
            for (int m = 0; m < circleSize / 2; m++) {
                final var t1 = circle.get(m);
                final var t2 = circle.get(circleSize - 1 - m);
                if (t1 != null && t2 != null) {
                    pairs.add(new MatchPair(groupId, t1, t2));
                }
            }
            final var last = circle.remove(circleSize - 1);
            circle.add(1, last);
        }
        return pairs;
    }

    private static Map<Integer, List<MatchPair>> buildFieldQueues(
            final List<Group> groups,
            final Map<UUID, List<MatchPair>> matchesByGroupId,
            final int numFields) {
        final var groupMatchesByField = new LinkedHashMap<Integer, List<List<MatchPair>>>();
        for (int f = 1; f <= numFields; f++) {
            groupMatchesByField.put(f, new ArrayList<>());
        }
        for (int i = 0; i < groups.size(); i++) {
            final var field = (i % numFields) + 1;
            final var groupMatches = matchesByGroupId.getOrDefault(groups.get(i).getId().value(), List.of());
            groupMatchesByField.get(field).add(new ArrayList<>(groupMatches));
        }
        final var fieldQueues = new LinkedHashMap<Integer, List<MatchPair>>();
        for (int f = 1; f <= numFields; f++) {
            final var queue = new ArrayList<MatchPair>();
            final var groupLists = groupMatchesByField.get(f);
            final var indices = new int[groupLists.size()];
            while (true) {
                var added = false;
                for (int g = 0; g < groupLists.size(); g++) {
                    if (indices[g] < groupLists.get(g).size()) {
                        queue.add(groupLists.get(g).get(indices[g]));
                        indices[g]++;
                        added = true;
                    }
                }
                if (!added) break;
            }
            fieldQueues.put(f, queue);
        }
        return fieldQueues;
    }
}
```

- [ ] **Step 6: Run ScheduleServiceTest — all tests must pass**

```bash
cd backend && ./mvnw test -Dtest=ScheduleServiceTest
```

Expected: BUILD SUCCESS — 8 tests pass.

- [ ] **Step 7: Run all tests including ArchitectureTest**

```bash
cd backend && ./mvnw test -q
```

Expected: BUILD SUCCESS — all tests pass including ArchUnit rules.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java \
        backend/src/test/java/abe/fvjc/tournament/schedule/domain/ && \
git commit -m "UC-04 - BE ScheduleService with round-robin algorithm"
```

---

## Task 6: BE API Layer

**Files:** `ScheduleGenerateRequestDto`, `MatchTeamDto`, `MatchDto`, `RoundDto`, `ScheduleDto`, `ScheduleApiMapper`, `ScheduleController`

**Interfaces:**
- Consumes: `ScheduleService.generate`, `ScheduleService.findByTournamentId`, all overview types
- Produces: `POST /api/tournaments/{id}/schedule/generate` (201), `GET /api/tournaments/{id}/schedule` (200)

- [ ] **Step 1: Create ScheduleGenerateRequestDto.java**

```java
package abe.fvjc.tournament.schedule.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ScheduleGenerateRequestDto {
    @NotNull String startTime;
    @NotNull @Min(1) Integer matchDurationMinutes;
    @NotNull @Min(0) Integer breakDurationMinutes;
}
```

- [ ] **Step 2: Create MatchTeamDto.java**

```java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class MatchTeamDto {
    UUID id;
    String name;
}
```

- [ ] **Step 3: Create MatchDto.java**

```java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class MatchDto {
    UUID id;
    int field;
    UUID groupId;
    String groupName;
    MatchTeamDto team1;
    MatchTeamDto team2;
}
```

- [ ] **Step 4: Create RoundDto.java**

```java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class RoundDto {
    UUID id;
    int number;
    String startTime;
    List<MatchDto> matches;
}
```

- [ ] **Step 5: Create ScheduleDto.java**

```java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class ScheduleDto {
    int totalRounds;
    int totalMatches;
    List<RoundDto> rounds;
}
```

- [ ] **Step 6: Create ScheduleApiMapper.java**

```java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.MatchOverview;
import abe.fvjc.tournament.schedule.domain.RoundOverview;
import abe.fvjc.tournament.schedule.domain.ScheduleGenerateRequest;
import abe.fvjc.tournament.schedule.domain.ScheduleOverview;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class ScheduleApiMapper {

    static ScheduleGenerateRequest toScheduleGenerateRequest(final ScheduleGenerateRequestDto dto) {
        return ScheduleGenerateRequest.builder()
                .startTime(dto.getStartTime())
                .matchDurationMinutes(dto.getMatchDurationMinutes())
                .breakDurationMinutes(dto.getBreakDurationMinutes())
                .build();
    }

    static ScheduleDto toScheduleDto(final ScheduleOverview overview) {
        return ScheduleDto.builder()
                .totalRounds(overview.getTotalRounds())
                .totalMatches(overview.getTotalMatches())
                .rounds(overview.getRounds().stream()
                        .map(ScheduleApiMapper::toRoundDto)
                        .toList())
                .build();
    }

    private static RoundDto toRoundDto(final RoundOverview overview) {
        return RoundDto.builder()
                .id(overview.getId().value())
                .number(overview.getNumber())
                .startTime(overview.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(overview.getMatches().stream()
                        .map(ScheduleApiMapper::toMatchDto)
                        .toList())
                .build();
    }

    private static MatchDto toMatchDto(final MatchOverview overview) {
        return MatchDto.builder()
                .id(overview.getId().value())
                .field(overview.getField())
                .groupId(overview.getGroupId().value())
                .groupName(overview.getGroupName())
                .team1(toMatchTeamDto(overview.getTeam1()))
                .team2(toMatchTeamDto(overview.getTeam2()))
                .build();
    }

    private static MatchTeamDto toMatchTeamDto(final TeamRef ref) {
        return MatchTeamDto.builder()
                .id(ref.getId().value())
                .name(ref.getName())
                .build();
    }
}
```

- [ ] **Step 7: Create ScheduleController.java**

```java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.ScheduleApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/schedule")
@RequiredArgsConstructor
class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleDto generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid ScheduleGenerateRequestDto request) {
        return toScheduleDto(scheduleService.generate(tournamentId, toScheduleGenerateRequest(request)));
    }

    @GetMapping
    public ScheduleDto getSchedule(@PathVariable UUID tournamentId) {
        return toScheduleDto(scheduleService.findByTournamentId(tournamentId));
    }
}
```

- [ ] **Step 8: Run all tests**

```bash
cd backend && ./mvnw test -q
```

Expected: BUILD SUCCESS — all tests pass.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/api/schedule/ && \
git commit -m "UC-04 - BE API layer (ScheduleController, DTOs, mapper)"
```

---

## Task 7: FE API Layer

**Files:** `schedule.api.dto.ts`, `schedule.api.service.ts`, `schedule.api.mapper.ts`

**Interfaces:**
- Produces: `ScheduleDto`, `ScheduleApiService.generate$`, `ScheduleApiService.getSchedule$`, `ScheduleApiMapper.toDomain` — consumed by Tasks 8–9

- [ ] **Step 1: Create schedule.api.dto.ts**

`frontend/src/app/api/schedule/schedule.api.dto.ts`:

```typescript
export interface MatchTeamDto {
  id: string;
  name: string;
}

export interface MatchDto {
  id: string;
  field: number;
  groupId: string;
  groupName: string;
  team1: MatchTeamDto;
  team2: MatchTeamDto;
}

export interface RoundDto {
  id: string;
  number: number;
  startTime: string;
  matches: MatchDto[];
}

export interface ScheduleDto {
  totalRounds: number;
  totalMatches: number;
  rounds: RoundDto[];
}

export interface ScheduleGenerateRequestDto {
  startTime: string;
  matchDurationMinutes: number;
  breakDurationMinutes: number;
}
```

- [ ] **Step 2: Create schedule.api.service.ts**

`frontend/src/app/api/schedule/schedule.api.service.ts`:

```typescript
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScheduleDto, ScheduleGenerateRequestDto } from '@app/api/schedule/schedule.api.dto';

@Injectable({ providedIn: 'root' })
export class ScheduleApiService {

  private readonly http = inject(HttpClient);

  generate$(tournamentId: string, request: ScheduleGenerateRequestDto): Observable<ScheduleDto> {
    return this.http.post<ScheduleDto>(`/api/tournaments/${tournamentId}/schedule/generate`, request);
  }

  getSchedule$(tournamentId: string): Observable<ScheduleDto> {
    return this.http.get<ScheduleDto>(`/api/tournaments/${tournamentId}/schedule`);
  }
}
```

- [ ] **Step 3: Create schedule.api.mapper.ts**

`frontend/src/app/api/schedule/schedule.api.mapper.ts`:

```typescript
import { Schedule, Round, Match } from '@app/domain/schedule/schedule.model';
import { MatchDto, RoundDto, ScheduleDto } from '@app/api/schedule/schedule.api.dto';

export class ScheduleApiMapper {

  static toDomain(dto: ScheduleDto): Schedule {
    return {
      totalRounds: dto.totalRounds,
      totalMatches: dto.totalMatches,
      rounds: dto.rounds.map(ScheduleApiMapper.toRoundDomain),
    };
  }

  private static toRoundDomain(dto: RoundDto): Round {
    return {
      id: dto.id,
      number: dto.number,
      startTime: new Date(dto.startTime),
      matches: dto.matches.map(ScheduleApiMapper.toMatchDomain),
    };
  }

  private static toMatchDomain(dto: MatchDto): Match {
    return {
      id: dto.id,
      field: dto.field,
      groupId: dto.groupId,
      groupName: dto.groupName,
      team1: dto.team1,
      team2: dto.team2,
    };
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/api/schedule/ && \
git commit -m "UC-04 - FE API layer (schedule DTO, service, mapper)"
```

---

## Task 8: FE Domain Layer

**Files:** `schedule.model.ts`, `schedule.actions.ts`, `schedule.state.ts`

**Interfaces:**
- Consumes: `ScheduleApiService`, `ScheduleApiMapper`
- Produces: `ScheduleState.getSchedule`, `GenerateSchedule`, `LoadSchedule` — consumed by Task 9

- [ ] **Step 1: Create schedule.model.ts**

`frontend/src/app/domain/schedule/schedule.model.ts`:

```typescript
export interface MatchTeam {
  id: string;
  name: string;
}

export interface Match {
  id: string;
  field: number;
  groupId: string;
  groupName: string;
  team1: MatchTeam;
  team2: MatchTeam;
}

export interface Round {
  id: string;
  number: number;
  startTime: Date;
  matches: Match[];
}

export interface Schedule {
  totalRounds: number;
  totalMatches: number;
  rounds: Round[];
}
```

- [ ] **Step 2: Create schedule.actions.ts**

`frontend/src/app/domain/schedule/schedule.actions.ts`:

```typescript
export class GenerateSchedule {
  static readonly type = '[Schedule] Generate Schedule';
  constructor(
    public readonly tournamentId: string,
    public readonly startTime: string,
    public readonly matchDurationMinutes: number,
    public readonly breakDurationMinutes: number,
  ) {}
}

export class LoadSchedule {
  static readonly type = '[Schedule] Load Schedule';
  constructor(public readonly tournamentId: string) {}
}
```

- [ ] **Step 3: Create schedule.state.ts**

`frontend/src/app/domain/schedule/schedule.state.ts`:

```typescript
import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Schedule } from '@app/domain/schedule/schedule.model';
import { GenerateSchedule, LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { ScheduleApiService } from '@app/api/schedule/schedule.api.service';
import { ScheduleApiMapper } from '@app/api/schedule/schedule.api.mapper';

export interface IScheduleState {
  schedule: Schedule | undefined;
}

@State<IScheduleState>({
  name: 'schedule',
  defaults: { schedule: undefined },
})
@Injectable()
export class ScheduleState {

  private readonly scheduleApiService = inject(ScheduleApiService);

  @Selector()
  static getSchedule(state: IScheduleState): Schedule | undefined {
    return state.schedule;
  }

  @Action(LoadSchedule)
  loadSchedule(ctx: StateContext<IScheduleState>, { tournamentId }: LoadSchedule) {
    return this.scheduleApiService.getSchedule$(tournamentId).pipe(
      tap(dto => {
        ctx.patchState({ schedule: ScheduleApiMapper.toDomain(dto) });
      })
    );
  }

  @Action(GenerateSchedule)
  generateSchedule(ctx: StateContext<IScheduleState>, { tournamentId, startTime, matchDurationMinutes, breakDurationMinutes }: GenerateSchedule) {
    return this.scheduleApiService.generate$(tournamentId, { startTime, matchDurationMinutes, breakDurationMinutes }).pipe(
      tap(dto => {
        ctx.patchState({ schedule: ScheduleApiMapper.toDomain(dto) });
      })
    );
  }
}
```

- [ ] **Step 4: Compile FE to check for errors**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/domain/schedule/ && \
git commit -m "UC-04 - FE domain layer (schedule model, actions, state)"
```

---

## Task 9: FE Display + Routes

**Files:** `tournament-schedule.page.ts/.html`, `schedule-generate.modal.ts/.html`, updated `tournament.routes.ts`, updated `tournament-detail.page.html`

**Interfaces:**
- Consumes: `ScheduleState.getSchedule`, `TournamentState.getSelected`, `GenerateSchedule`, `LoadSchedule`, `Tournament`, `Schedule`
- Produces: `/:id/schedule` route, "Calendrier" button in tournament detail

- [ ] **Step 1: Create schedule-generate.modal.ts**

`frontend/src/app/display/tournament/pages/schedule-generate/schedule-generate.modal.ts`:

```typescript
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { GenerateSchedule } from '@app/domain/schedule/schedule.actions';

@Component({
  selector: 'app-schedule-generate-modal',
  templateUrl: './schedule-generate.modal.html',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
})
export class ScheduleGenerateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<ScheduleGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      startTime: ['09:00', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
      matchDurationMinutes: [20, [Validators.required, Validators.min(1)]],
      breakDurationMinutes: [5, [Validators.required, Validators.min(0)]],
    });

    this.actions$.pipe(
      ofActionSuccessful(GenerateSchedule),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateSchedule(
      this.data.tournamentId,
      String(this.form.value.startTime),
      Number(this.form.value.matchDurationMinutes),
      Number(this.form.value.breakDurationMinutes),
    ));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

- [ ] **Step 2: Create schedule-generate.modal.html**

`frontend/src/app/display/tournament/pages/schedule-generate/schedule-generate.modal.html`:

```html
<h2 mat-dialog-title>Générer le calendrier</h2>

<mat-dialog-content>
  <form [formGroup]="form">
    <mat-form-field appearance="outline">
      <mat-label>Heure de début (HH:mm)</mat-label>
      <input matInput type="text" formControlName="startTime" placeholder="09:00" />
      @if (form.get('startTime')?.hasError('required') || form.get('startTime')?.hasError('pattern')) {
        <mat-error>L'heure de début est obligatoire (format HH:mm)</mat-error>
      }
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Durée d'un match (minutes)</mat-label>
      <input matInput type="number" min="1" formControlName="matchDurationMinutes" />
      @if (form.get('matchDurationMinutes')?.hasError('required')) {
        <mat-error>La durée d'un match est obligatoire</mat-error>
      } @else if (form.get('matchDurationMinutes')?.hasError('min')) {
        <mat-error>La durée d'un match doit être d'au moins 1 minute</mat-error>
      }
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Durée de pause (minutes)</mat-label>
      <input matInput type="number" min="0" formControlName="breakDurationMinutes" />
      @if (form.get('breakDurationMinutes')?.hasError('required')) {
        <mat-error>La durée de pause est obligatoire</mat-error>
      } @else if (form.get('breakDurationMinutes')?.hasError('min')) {
        <mat-error>La durée de pause ne peut pas être négative</mat-error>
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

- [ ] **Step 3: Create tournament-schedule.page.ts**

`frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.ts`:

```typescript
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { Schedule } from '@app/domain/schedule/schedule.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { ScheduleGenerateModal } from '@app/display/tournament/pages/schedule-generate/schedule-generate.modal';

@Component({
  selector: 'app-tournament-schedule-page',
  templateUrl: './tournament-schedule.page.html',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule],
})
export class TournamentSchedulePage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly schedule$: Observable<Schedule | undefined> = this.store.select(ScheduleState.getSchedule);

  private tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadSchedule(this.tournamentId)]);
  }

  openGenerateModal(): void {
    this.dialog.open(ScheduleGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }
}
```

- [ ] **Step 4: Create tournament-schedule.page.html**

`frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.html`:

```html
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <div class="header-left">
      <button mat-icon-button [routerLink]="['..']">
        <mat-icon>arrow_back</mat-icon>
      </button>
      <div>
        <h1>{{ tournament.name }}</h1>
        <span class="subtitle">Calendrier</span>
      </div>
    </div>
    @if (isDraft(tournament)) {
      <button mat-raised-button color="primary" (click)="openGenerateModal()">
        <mat-icon>calendar_month</mat-icon>
        Générer le calendrier
      </button>
    }
  </div>

  @if (schedule$ | async; as schedule) {
    @if (schedule.totalRounds === 0) {
      <div class="empty-state">
        <mat-icon>calendar_today</mat-icon>
        <p>Aucun calendrier généré</p>
        @if (isDraft(tournament)) {
          <button mat-stroked-button (click)="openGenerateModal()">Générer le calendrier</button>
        }
      </div>
    } @else {
      <div class="rounds-list">
        @for (round of schedule.rounds; track round.id) {
          <div class="round-card">
            <div class="round-header">
              <span class="round-number">Round {{ round.number }}</span>
              <span class="round-time">{{ round.startTime | date:'HH:mm' }}</span>
            </div>
            <div class="matches-list">
              @for (match of round.matches; track match.id) {
                <div class="match-row">
                  <span class="field-label">Terrain {{ match.field }}</span>
                  <span class="group-label">({{ match.groupName }})</span>
                  <span class="teams">{{ match.team1.name }} — {{ match.team2.name }}</span>
                </div>
              }
            </div>
          </div>
        }
      </div>
    }
  }
}
```

- [ ] **Step 5: Update tournament.routes.ts — add ScheduleState and schedule route**

Replace the full content of `frontend/src/app/modules/tournament.routes.ts`:

```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { ScheduleState } from '../domain/schedule/schedule.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';
import { TournamentSchedulePage } from '../display/tournament/pages/tournament-schedule/tournament-schedule.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
      { path: ':id/schedule', component: TournamentSchedulePage },
    ],
  },
];
```

- [ ] **Step 6: Update tournament-detail.page.html — add "Calendrier" button**

In `frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html`, replace the `header-actions` div to add the Calendrier button:

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
        <button mat-stroked-button [routerLink]="['..', tournament.id, 'schedule']">
          <mat-icon>calendar_month</mat-icon>
          Calendrier
        </button>
      </div>
    }
```

- [ ] **Step 7: Build FE to check for errors**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -10
```

Expected: no errors.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/display/tournament/pages/schedule-generate/ \
        frontend/src/app/display/tournament/pages/tournament-schedule/ \
        frontend/src/app/modules/tournament.routes.ts \
        frontend/src/app/display/tournament/pages/tournament-detail/tournament-detail.page.html && \
git commit -m "UC-04 - FE display layer (schedule page, generate modal, routes, navigation)"
```

---

## Final Verification

- [ ] **Run all BE tests**

```bash
cd backend && ./mvnw test -q
```

Expected: BUILD SUCCESS.

- [ ] **Run FE build**

```bash
cd frontend && npx ng build --configuration development -q
```

Expected: no errors.

- [ ] **Tag the UC-04 implementation complete**

```bash
git log --oneline -10
```

Verify all UC-04 commits are present in sequence.
