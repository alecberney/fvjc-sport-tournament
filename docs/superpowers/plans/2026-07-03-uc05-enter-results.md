# UC-05 — Start Tournament & Enter Results — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement UC-05 — start a tournament (DRAFT→IN_PROGRESS), submit match results with inline ranking, and display a dedicated results page.

**Architecture:** Backend adds DB migration, new value objects, `RankingService`+`ResultService`, updates `TournamentService.start()`, and exposes three new endpoints. Frontend adds a `ResultState`, a `TournamentResultsPage` (side-by-side schedule+form), and a start button on the schedule page.

**Tech Stack:** Java 21 / Spring Boot, Lombok (`@Value @Builder @With @UtilityClass`), Liquibase (SQLite), JPA, Angular 19 standalone, NGXS, Angular Material.

## Global Constraints

- All local variables: `final var`
- Domain layer never imports from `api` or `persistence`
- `@Transactional` on `JpaXxxStore` methods only, `readOnly = true` on reads
- Lombok `@Value @Builder @With` on domain classes; `@Value @Builder @Jacksonized` on DTOs
- No `toBuilder()` — use `@With` for field overrides
- Static imports for enum values, validator calls, and mapper methods
- File path ≠ package name: value objects live in `domain/schedule/` dir, interfaces+validators in `schedule/domain/` dir
- Exceptions: `abe.fvjc.tournament.shared.exception.{ConflictException,NotFoundException,ValidationException}`
- Tests: JUnit 5 assertions only, no AssertJ, no `ArgumentCaptor`, `returnsFirstArg()` for save stubs
- Angular: `@app/` import alias, `inject()` (no constructor injection), all components standalone

---

### Task 1: DB Migration — add result columns to matches

**Files:**
- Create: `backend/src/main/resources/db/changelog/20260703120004_alter_matches_add_result_columns.xml`
- Modify: `backend/src/main/resources/db/master.xml`

**Interfaces:**
- Produces: `result_score1 INTEGER NULL`, `result_score2 INTEGER NULL` columns on `matches` table

- [ ] **Step 1: Create migration file**

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260703120004" author="aberney">
        <addColumn tableName="matches">
            <column name="result_score1" type="INT"/>
            <column name="result_score2" type="INT"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

Save to: `backend/src/main/resources/db/changelog/20260703120004_alter_matches_add_result_columns.xml`

- [ ] **Step 2: Register in master.xml**

Add this line before `</databaseChangeLog>` in `backend/src/main/resources/db/master.xml`:

```xml
    <include relativeToChangelogFile="true" file="changelog/20260703120004_alter_matches_add_result_columns.xml"/>
```

- [ ] **Step 3: Verify the app starts**

```bash
cd backend && ./mvnw spring-boot:run -q 2>&1 | head -30
```

Expected: no Liquibase errors, app starts on port 8080.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "UC-05: DB migration — add result_score1/score2 to matches"
```

---

### Task 2: BE Domain — value objects, store interface updates, ScheduleFakes

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchResult.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/SubmitMatchResultRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/GroupRankingEntry.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/GroupRanking.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/Match.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchOverview.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/domain/MatchStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/domain/RoundStore.java`
- Modify: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleFakes.java`

**Interfaces:**
- Produces:
  - `MatchResult(int score1, int score2)`
  - `Match.result: MatchResult` (nullable, `@With`)
  - `MatchOverview.result: MatchResult` (nullable)
  - `SubmitMatchResultRequest(Integer score1, Integer score2)`
  - `GroupRankingEntry(int rank, TeamRef team, int played, int wins, int draws, int defeats, int goalsFor, int goalsAgainst, int goalDifference, int points)`
  - `GroupRanking(GroupId groupId, String groupName, List<GroupRankingEntry> entries)`
  - `MatchStore.findById(UUID)`, `MatchStore.save(Match)`, `MatchStore.findAllByGroupId(UUID)`, `MatchStore.existsResultByRoundIds(List<UUID>)`
  - `RoundStore.countByTournamentId(UUID)`

- [ ] **Step 1: Create MatchResult**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchResult.java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchResult {
    int score1;
    int score2;
}
```

- [ ] **Step 2: Create SubmitMatchResultRequest**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/SubmitMatchResultRequest.java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubmitMatchResultRequest {
    Integer score1;
    Integer score2;
}
```

- [ ] **Step 3: Create GroupRankingEntry**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/GroupRankingEntry.java
package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupRankingEntry {
    int rank;
    TeamRef team;
    int played;
    int wins;
    int draws;
    int defeats;
    int goalsFor;
    int goalsAgainst;
    int goalDifference;
    int points;
}
```

- [ ] **Step 4: Create GroupRanking**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/GroupRanking.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupRanking {
    GroupId groupId;
    String groupName;
    List<GroupRankingEntry> entries;
}
```

- [ ] **Step 5: Update Match — add nullable result field with @With**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/domain/schedule/Match.java`:

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
    MatchResult result;
}
```

- [ ] **Step 6: Update MatchOverview — add nullable result field**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/domain/schedule/MatchOverview.java`:

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
    MatchResult result;
}
```

- [ ] **Step 7: Update MatchStore interface**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/schedule/domain/MatchStore.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchStore {
    void saveAll(List<Match> matches);
    Match save(Match match);
    Optional<Match> findById(UUID matchId);
    List<Match> findAllByRoundIds(List<UUID> roundIds);
    List<Match> findAllByGroupId(UUID groupId);
    void deleteAllByRoundIds(List<UUID> roundIds);
    boolean existsResultByRoundIds(List<UUID> roundIds);
}
```

- [ ] **Step 8: Update RoundStore interface**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/schedule/domain/RoundStore.java`:

```java
package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.UUID;

public interface RoundStore {
    void saveAll(List<Round> rounds);
    List<Round> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
    int countByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 9: Update ScheduleFakes — add result field to buildMatch**

Replace `buildMatch` in `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleFakes.java`:

```java
    public static Match buildMatch(final RoundId roundId) {
        return Match.builder()
                .id(IdGenerator.matchId())
                .roundId(roundId)
                .field(1)
                .groupId(GroupId.of(UUID.randomUUID()))
                .team1Id(TeamId.of(UUID.randomUUID()))
                .team2Id(TeamId.of(UUID.randomUUID()))
                .result(null)
                .build();
    }
```

- [ ] **Step 10: Compile to verify**

```bash
cd backend && ./mvnw compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ backend/src/main/java/abe/fvjc/tournament/schedule/domain/ backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleFakes.java
git commit -m "UC-05: BE domain — MatchResult, GroupRanking VOs, updated Match/MatchOverview/MatchStore/RoundStore"
```

---

### Task 3: BE Domain — ResultValidator + tests

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/domain/ResultValidator.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultValidatorTest.java`

**Interfaces:**
- Produces: `ResultValidator.validateSubmitMatchResultRequest(SubmitMatchResultRequest)` — throws `ValidationException`

- [ ] **Step 1: Write the failing tests**

```java
// backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultValidatorTest.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static abe.fvjc.tournament.schedule.domain.ResultValidator.validateSubmitMatchResultRequest;
import static org.junit.jupiter.api.Assertions.*;

class ResultValidatorTest {

    @Test
    void validateWhenScore1NullShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(null)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().get(0).field());
    }

    @Test
    void validateWhenScore2NullShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(3)
                .score2(null)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score2", exception.getErrors().get(0).field());
    }

    @Test
    void validateWhenScore1NegativeShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(-1)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().get(0).field());
    }

    @Test
    void validateWhenScore1Above500ShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(501)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().get(0).field());
    }

    @Test
    void validateWhenValidShouldNotThrow() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(3)
                .score2(1)
                .build();

        assertDoesNotThrow(() -> validateSubmitMatchResultRequest(request));
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=ResultValidatorTest -q 2>&1 | tail -10
```

Expected: FAILURE — `ResultValidator` does not exist yet.

- [ ] **Step 3: Implement ResultValidator**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/domain/ResultValidator.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class ResultValidator {

    public static void validateSubmitMatchResultRequest(final SubmitMatchResultRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();
        validateScore(request.getScore1(), "score1", errors);
        validateScore(request.getScore2(), "score2", errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateScore(final Integer score, final String field,
                                       final java.util.List<ValidationException.FieldError> errors) {
        if (score == null) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score de l'équipe est obligatoire"));
        } else if (score < 0) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score ne peut pas être négatif"));
        } else if (score > 500) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score ne peut pas dépasser 500"));
        }
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=ResultValidatorTest -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/schedule/domain/ResultValidator.java backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultValidatorTest.java
git commit -m "UC-05: ResultValidator with tests"
```

---

### Task 4: BE Domain — RankingService + tests

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/RankingService.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java`

**Interfaces:**
- Consumes: `TeamStore.findAllByGroupId(UUID)`, `MatchStore.findAllByGroupId(UUID)`, `GroupStore.findAllByTournamentId(UUID)`
- Produces: `RankingService.computeGroupRanking(UUID tournamentId, UUID groupId) → GroupRanking`

- [ ] **Step 1: Write the failing tests**

```java
// backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @InjectMocks
    private RankingService rankingService;

    @Test
    void computeGroupRankingWhenNoResultsShouldReturnZeroStats() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(null);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

        assertEquals(2, rankingFound.getEntries().size());
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPlayed() == 0));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 0));
    }

    @Test
    void computeGroupRankingWhenWinShouldGive2Points() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(3).score2(1).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

        final var winner = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(t1.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(2, winner.getPoints());
        assertEquals(1, winner.getWins());
        assertEquals(1, winner.getRank());

        final var loser = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(t2.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(0, loser.getPoints());
        assertEquals(2, loser.getRank());
    }

    @Test
    void computeGroupRankingWhenDrawShouldGive1PointEach() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(2).score2(2).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getDraws() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeGroupRankingWhenMultipleResultsShouldOrderByPointsThenGoalDiffThenGoalsFor() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var m1 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t1.getId()).withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(3).score2(0).build());
        final var m2 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t1.getId()).withTeam2Id(t3.getId())
                .withResult(MatchResult.builder().score1(1).score2(0).build());
        final var m3 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t2.getId()).withTeam2Id(t3.getId())
                .withResult(MatchResult.builder().score1(2).score2(0).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2, t3));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(m1, m2, m3));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(matchStore).findAllByGroupId(groupId.value());

        assertEquals(t1.getId().value(), rankingFound.getEntries().get(0).getTeam().getId().value());
        assertEquals(1, rankingFound.getEntries().get(0).getRank());
        assertEquals(t2.getId().value(), rankingFound.getEntries().get(1).getTeam().getId().value());
        assertEquals(2, rankingFound.getEntries().get(1).getRank());
        assertEquals(t3.getId().value(), rankingFound.getEntries().get(2).getTeam().getId().value());
        assertEquals(3, rankingFound.getEntries().get(2).getRank());
    }

    @Test
    void computeGroupRankingWhenFullyTiedShouldShareRank() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of());

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(teamStore).findAllByGroupId(groupId.value());

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=RankingServiceTest -q 2>&1 | tail -10
```

Expected: FAILURE — `RankingService` does not exist.

- [ ] **Step 3: Implement RankingService**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/RankingService.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.team.domain.TeamStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;

    public GroupRanking computeGroupRanking(final UUID tournamentId, final UUID groupId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var group = groups.stream()
                .filter(g -> g.getId().value().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new abe.fvjc.tournament.shared.exception.NotFoundException("Group", groupId));
        final var teams = teamStore.findAllByGroupId(groupId);
        final var matches = matchStore.findAllByGroupId(groupId);
        final var statsMap = buildStatsMap(teams);
        accumulateStats(matches, statsMap);
        final var entries = buildRankedEntries(teams, statsMap);
        return GroupRanking.builder()
                .groupId(GroupId.of(groupId))
                .groupName(group.getName())
                .entries(entries)
                .build();
    }

    private static Map<UUID, int[]> buildStatsMap(final List<Team> teams) {
        final var map = new HashMap<UUID, int[]>();
        for (final var team : teams) {
            map.put(team.getId().value(), new int[6]); // [played, wins, draws, defeats, goalsFor, goalsAgainst]
        }
        return map;
    }

    private static void accumulateStats(final List<Match> matches, final Map<UUID, int[]> statsMap) {
        for (final var match : matches) {
            if (match.getResult() == null) continue;
            final var r = match.getResult();
            final var s1 = statsMap.get(match.getTeam1Id().value());
            final var s2 = statsMap.get(match.getTeam2Id().value());
            if (s1 == null || s2 == null) continue;
            s1[0]++; s2[0]++;
            s1[4] += r.getScore1(); s1[5] += r.getScore2();
            s2[4] += r.getScore2(); s2[5] += r.getScore1();
            if (r.getScore1() > r.getScore2()) { s1[1]++; s2[3]++; }
            else if (r.getScore1() < r.getScore2()) { s2[1]++; s1[3]++; }
            else { s1[2]++; s2[2]++; }
        }
    }

    private static List<GroupRankingEntry> buildRankedEntries(
            final List<Team> teams, final Map<UUID, int[]> statsMap) {
        final var sorted = teams.stream()
                .sorted(Comparator
                        .comparingInt((Team t) -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[1] * 2 + s[2]);
                        })
                        .thenComparingInt(t -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[4] - s[5]);
                        })
                        .thenComparingInt(t -> -statsMap.get(t.getId().value())[4]))
                .toList();
        final var entries = new java.util.ArrayList<GroupRankingEntry>();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            final var team = sorted.get(i);
            final var s = statsMap.get(team.getId().value());
            final var points = s[1] * 2 + s[2];
            final var goalDiff = s[4] - s[5];
            if (i > 0) {
                final var prev = sorted.get(i - 1);
                final var ps = statsMap.get(prev.getId().value());
                final var prevPoints = ps[1] * 2 + ps[2];
                final var prevGoalDiff = ps[4] - ps[5];
                if (points != prevPoints || goalDiff != prevGoalDiff || s[4] != ps[4]) {
                    rank = i + 1;
                }
            }
            entries.add(GroupRankingEntry.builder()
                    .rank(rank)
                    .team(TeamRef.builder()
                            .id(TeamId.of(team.getId().value()))
                            .name(team.getName())
                            .build())
                    .played(s[0])
                    .wins(s[1])
                    .draws(s[2])
                    .defeats(s[3])
                    .goalsFor(s[4])
                    .goalsAgainst(s[5])
                    .goalDifference(goalDiff)
                    .points(points)
                    .build());
        }
        return entries;
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=RankingServiceTest -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS, 5 tests passed.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/RankingService.java backend/src/test/java/abe/fvjc/tournament/schedule/domain/RankingServiceTest.java
git commit -m "UC-05: RankingService with tests"
```

---

### Task 5: BE Domain — ResultService + tests

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ResultService.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultServiceTest.java`

**Interfaces:**
- Consumes: `TournamentStore.findById`, `MatchStore.findById`, `MatchStore.save`, `GroupStore.findAllByTournamentId`, `TeamStore` (via `MatchStore` — teams loaded in `MatchOverview` assembly), `ResultValidator.validateSubmitMatchResultRequest`
- Produces: `ResultService.submitResult(UUID tournamentId, UUID matchId, SubmitMatchResultRequest) → MatchOverview`

- [ ] **Step 1: Write the failing tests**

```java
// backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultServiceTest.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
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

import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.buildMatch;
import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private ResultService resultService;

    @Test
    void submitResultWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var tournamentId = UUID.randomUUID();
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
                () -> resultService.submitResult(tournamentId, matchId, request));

        verify(tournamentStore).findById(tournamentId);

        assertTrue(exception.getMessage().contains("Tournament"));
    }

    @Test
    void submitResultWhenTournamentNotInProgressShouldThrowConflictException() {
        final var tournament = buildTournament(); // DRAFT
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
                () -> resultService.submitResult(tournament.getId().value(), matchId, request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void submitResultWhenMatchNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(matchStore.findById(matchId)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
                () -> resultService.submitResult(tournament.getId().value(), matchId, request));

        verify(matchStore).findById(matchId);

        assertTrue(exception.getMessage().contains("Match"));
    }

    @Test
    void submitResultWhenValidShouldSaveAndReturnMatchOverview() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var round = buildRound(tournament.getId());
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId());
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(matchStore.findById(match.getId().value())).thenReturn(Optional.of(match));
        when(matchStore.save(any(Match.class))).then(returnsFirstArg());
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));

        final var matchOverviewFound = resultService.submitResult(tournament.getId().value(), match.getId().value(), request);

        verify(matchStore).save(any(Match.class));

        assertEquals(match.getId().value(), matchOverviewFound.getId().value());
        assertNotNull(matchOverviewFound.getResult());
        assertEquals(3, matchOverviewFound.getResult().getScore1());
        assertEquals(1, matchOverviewFound.getResult().getScore2());
        assertEquals("A", matchOverviewFound.getGroupName());
        assertEquals(t1.getName(), matchOverviewFound.getTeam1().getName());
        assertEquals(t2.getName(), matchOverviewFound.getTeam2().getName());
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=ResultServiceTest -q 2>&1 | tail -10
```

Expected: FAILURE — `ResultService` does not exist.

- [ ] **Step 3: Implement ResultService**

```java
// backend/src/main/java/abe/fvjc/tournament/domain/schedule/ResultService.java
package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.schedule.domain.ResultValidator.validateSubmitMatchResultRequest;

@Service
@RequiredArgsConstructor
public class ResultService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public MatchOverview submitResult(final UUID tournamentId, final UUID matchId,
                                      final SubmitMatchResultRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new ConflictException("Les résultats ne peuvent être saisis que pour un tournoi en cours");
        }
        final var match = matchStore.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match", matchId));
        validateSubmitMatchResultRequest(request);
        final var result = MatchResult.builder()
                .score1(request.getScore1())
                .score2(request.getScore2())
                .build();
        final var savedMatch = matchStore.save(match.withResult(result));
        return buildMatchOverview(savedMatch, tournamentId);
    }

    private MatchOverview buildMatchOverview(final Match match, final UUID tournamentId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupName = groups.stream()
                .filter(g -> g.getId().value().equals(match.getGroupId().value()))
                .map(abe.fvjc.tournament.group.domain.Group::getName)
                .findFirst()
                .orElse("");
        final var teams = teamStore.findAllByGroupId(match.getGroupId().value());
        final var teamById = teams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return MatchOverview.builder()
                .id(match.getId())
                .field(match.getField())
                .groupId(match.getGroupId())
                .groupName(groupName)
                .team1(buildTeamRef(teamById.get(match.getTeam1Id().value())))
                .team2(buildTeamRef(teamById.get(match.getTeam2Id().value())))
                .result(match.getResult())
                .build();
    }

    private static TeamRef buildTeamRef(final Team team) {
        return TeamRef.builder()
                .id(TeamId.of(team.getId().value()))
                .name(team.getName())
                .build();
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=ResultServiceTest -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/schedule/ResultService.java backend/src/test/java/abe/fvjc/tournament/schedule/domain/ResultServiceTest.java
git commit -m "UC-05: ResultService with tests"
```

---

### Task 6: BE Domain — TournamentService.start() + fix ScheduleService

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/tournament/TournamentService.java`
- Modify: `backend/src/test/java/abe/fvjc/tournament/tournament/domain/TournamentServiceTest.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java`
- Modify: `backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java`

**Interfaces:**
- Consumes: `RoundStore.countByTournamentId(UUID)` (in `TournamentService`)
- Produces: `TournamentService.start(UUID id) → Tournament`

- [ ] **Step 1: Write failing tests for TournamentService.start()**

Add these four tests to `TournamentServiceTest.java` (after the existing tests):

```java
    @Mock
    private abe.fvjc.tournament.schedule.domain.RoundStore roundStore;

    @Test
    void startWhenNotFoundShouldThrowNotFoundException() {
        final var id = abe.fvjc.tournament.tournament.domain.IdGenerator.tournamentId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentService.start(id));

        verify(tournamentStore).findById(id);

        assertTrue(exception.getMessage().contains("Tournament"));
    }

    @Test
    void startWhenAlreadyInProgressShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId().value()));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void startWhenNoRoundsShouldThrowConflictException() {
        final var tournament = buildTournament();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(roundStore.countByTournamentId(tournament.getId().value())).thenReturn(0);

        assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId().value()));

        verify(roundStore).countByTournamentId(tournament.getId().value());
    }

    @Test
    void startWhenValidShouldTransitionToInProgress() {
        final var tournament = buildTournament();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(roundStore.countByTournamentId(tournament.getId().value())).thenReturn(3);
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentStarted = tournamentService.start(tournament.getId().value());

        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(IN_PROGRESS, tournamentStarted.getStatus());
    }
```

Also add these imports at the top of `TournamentServiceTest.java`:

```java
import abe.fvjc.tournament.shared.exception.ConflictException;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=TournamentServiceTest -q 2>&1 | tail -10
```

Expected: FAILURE — `start` method does not exist.

- [ ] **Step 3: Implement TournamentService.start()**

Add `roundStore` field and `start()` method to `TournamentService.java`:

```java
// Add field (alphabetically after tournamentStore):
private final abe.fvjc.tournament.schedule.domain.RoundStore roundStore;

// Add method:
public Tournament start(final UUID id) {
    final var tournament = tournamentStore.findById(id)
            .orElseThrow(() -> new NotFoundException("Tournament", id));
    if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
        throw new ConflictException("Le tournoi est déjà démarré");
    }
    if (roundStore.countByTournamentId(id) == 0) {
        throw new ConflictException("Impossible de démarrer le tournoi sans calendrier généré");
    }
    return tournamentStore.save(tournament.withStatus(TournamentStatus.IN_PROGRESS));
}
```

Also add the import at the top:
```java
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.schedule.domain.RoundStore;
```

- [ ] **Step 4: Run TournamentService tests to confirm they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=TournamentServiceTest -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS, 8 tests passed.

- [ ] **Step 5: Fix ScheduleService — replace existsResultByTournamentId with existsResultByRoundIds**

In `ScheduleService.java`, replace the call to `matchStore.existsResultByTournamentId(tournamentId)`:

```java
// BEFORE (remove this block):
if (matchStore.existsResultByTournamentId(tournamentId)) {
    throw new ConflictException("Impossible de régénérer le calendrier : des résultats ont déjà été saisis");
}
final var existingRounds = roundStore.findAllByTournamentId(tournamentId);

// AFTER (replace with):
final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
final var existingRoundIds = existingRounds.stream()
        .map(r -> r.getId().value())
        .toList();
if (!existingRoundIds.isEmpty() && matchStore.existsResultByRoundIds(existingRoundIds)) {
    throw new ConflictException("Impossible de régénérer le calendrier : des résultats ont déjà été saisis");
}
```

Note: The `existingRounds` variable is now declared before the conflict check. Remove the second `existingRounds` declaration that follows (since it was originally re-declared).

- [ ] **Step 6: Fix ScheduleServiceTest — update mocks for existsResultByRoundIds**

In `ScheduleServiceTest.java`, replace all occurrences of:
```java
when(matchStore.existsResultByTournamentId(tournament.getId().value())).thenReturn(true);
```
with:
```java
when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(buildRound(tournament.getId())));
when(matchStore.existsResultByRoundIds(anyList())).thenReturn(true);
```

And replace all occurrences of:
```java
when(matchStore.existsResultByTournamentId(tournament.getId().value())).thenReturn(false);
```
with:
```java
when(matchStore.existsResultByRoundIds(anyList())).thenReturn(false);
```

Also update the `verify` call in the test `generateWhenResultsExistShouldThrowConflictException`:
```java
// BEFORE:
verify(matchStore).existsResultByTournamentId(tournament.getId().value());
// AFTER:
verify(matchStore).existsResultByRoundIds(anyList());
```

- [ ] **Step 7: Run all schedule domain tests**

```bash
cd backend && ./mvnw test -pl . -Dtest="ScheduleServiceTest,TournamentServiceTest,ResultServiceTest,RankingServiceTest,ResultValidatorTest" -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/tournament/TournamentService.java \
        backend/src/test/java/abe/fvjc/tournament/tournament/domain/TournamentServiceTest.java \
        backend/src/main/java/abe/fvjc/tournament/domain/schedule/ScheduleService.java \
        backend/src/test/java/abe/fvjc/tournament/schedule/domain/ScheduleServiceTest.java
git commit -m "UC-05: TournamentService.start(), fix ScheduleService to use existsResultByRoundIds"
```

---

### Task 7: BE Persistence — implement new store methods

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchEntity.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchDbMapper.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchRepository.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/JpaMatchStore.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/RoundRepository.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/schedule/JpaRoundStore.java`

**Interfaces:**
- Consumes: `MatchStore` interface (Task 2), `RoundStore` interface (Task 2), `MatchResult` (Task 2)
- Produces: Full persistence implementations for all new methods

- [ ] **Step 1: Update MatchEntity — add result columns**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchEntity.java
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
    private Integer resultScore1;
    private Integer resultScore2;
}
```

- [ ] **Step 2: Update MatchDbMapper — map result fields**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchDbMapper.java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchId;
import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.RoundId;
import abe.fvjc.tournament.team.domain.TeamId;
import lombok.experimental.UtilityClass;

@UtilityClass
class MatchDbMapper {

    static Match toMatch(final MatchEntity entity) {
        final var result = (entity.getResultScore1() != null && entity.getResultScore2() != null)
                ? MatchResult.builder()
                        .score1(entity.getResultScore1())
                        .score2(entity.getResultScore2())
                        .build()
                : null;
        return Match.builder()
                .id(MatchId.of(entity.getId()))
                .roundId(RoundId.of(entity.getRoundId()))
                .field(entity.getField())
                .groupId(GroupId.of(entity.getGroupId()))
                .team1Id(TeamId.of(entity.getTeam1Id()))
                .team2Id(TeamId.of(entity.getTeam2Id()))
                .result(result)
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
        if (match.getResult() != null) {
            entity.setResultScore1(match.getResult().getScore1());
            entity.setResultScore2(match.getResult().getScore2());
        }
        return entity;
    }
}
```

- [ ] **Step 3: Update MatchRepository — add new query methods**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/schedule/MatchRepository.java
package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findByRoundIdIn(List<UUID> roundIds);
    void deleteByRoundIdIn(List<UUID> roundIds);
    List<MatchEntity> findByGroupId(UUID groupId);
    boolean existsByRoundIdInAndResultScore1IsNotNull(List<UUID> roundIds);
}
```

- [ ] **Step 4: Update JpaMatchStore — implement new methods**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/schedule/JpaMatchStore.java
package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    @Transactional
    public Match save(final Match match) {
        final var entity = MatchDbMapper.toMatchEntity(match);
        final var savedEntity = matchRepository.save(entity);
        return MatchDbMapper.toMatch(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findById(final UUID matchId) {
        return matchRepository.findById(matchId)
                .map(MatchDbMapper::toMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByRoundIds(final List<UUID> roundIds) {
        return matchRepository.findByRoundIdIn(roundIds).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByGroupId(final UUID groupId) {
        return matchRepository.findByGroupId(groupId).stream()
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
    public boolean existsResultByRoundIds(final List<UUID> roundIds) {
        return matchRepository.existsByRoundIdInAndResultScore1IsNotNull(roundIds);
    }
}
```

- [ ] **Step 5: Update RoundRepository — add count method**

```java
// backend/src/main/java/abe/fvjc/tournament/persistence/schedule/RoundRepository.java
package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RoundRepository extends JpaRepository<RoundEntity, UUID> {
    List<RoundEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
    int countByTournamentId(UUID tournamentId);
}
```

- [ ] **Step 6: Update JpaRoundStore — implement countByTournamentId**

Add to `JpaRoundStore.java` (after `deleteAllByTournamentId`):

```java
    @Override
    @Transactional(readOnly = true)
    public int countByTournamentId(final UUID tournamentId) {
        return roundRepository.countByTournamentId(tournamentId);
    }
```

- [ ] **Step 7: Compile and run all tests**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/persistence/schedule/
git commit -m "UC-05: persistence — MatchEntity result columns, JpaMatchStore/JpaRoundStore new methods"
```

---

### Task 8: BE API — controllers, DTOs, mappers

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/SubmitMatchResultRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchResultDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchResultResponseDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingEntryDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/ResultApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/ResultController.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingController.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchDto.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/schedule/api/ScheduleApiMapper.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/api/tournament/TournamentController.java`

- [ ] **Step 1: Create MatchResultDto**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchResultDto.java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class MatchResultDto {
    int score1;
    int score2;
}
```

- [ ] **Step 2: Update MatchDto — add nullable result field**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchDto.java
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
    MatchResultDto result;
}
```

- [ ] **Step 3: Update ScheduleApiMapper.toMatchDto — map result**

In `ScheduleApiMapper.java`, update `toMatchDto`:

```java
    private static MatchDto toMatchDto(final MatchOverview overview) {
        final var result = overview.getResult() != null
                ? MatchResultDto.builder()
                        .score1(overview.getResult().getScore1())
                        .score2(overview.getResult().getScore2())
                        .build()
                : null;
        return MatchDto.builder()
                .id(overview.getId().value())
                .field(overview.getField())
                .groupId(overview.getGroupId().value())
                .groupName(overview.getGroupName())
                .team1(toMatchTeamDto(overview.getTeam1()))
                .team2(toMatchTeamDto(overview.getTeam2()))
                .result(result)
                .build();
    }
```

- [ ] **Step 4: Create SubmitMatchResultRequestDto**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/SubmitMatchResultRequestDto.java
package abe.fvjc.tournament.schedule.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SubmitMatchResultRequestDto {
    @NotNull @Min(0) @Max(500) Integer score1;
    @NotNull @Min(0) @Max(500) Integer score2;
}
```

- [ ] **Step 5: Create GroupRankingEntryDto**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingEntryDto.java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupRankingEntryDto {
    int rank;
    MatchTeamDto team;
    int played;
    int wins;
    int draws;
    int defeats;
    int goalsFor;
    int goalsAgainst;
    int goalDifference;
    int points;
}
```

- [ ] **Step 6: Create GroupRankingDto**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/GroupRankingDto.java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupRankingDto {
    UUID groupId;
    String groupName;
    List<GroupRankingEntryDto> entries;
}
```

- [ ] **Step 7: Create MatchResultResponseDto**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/MatchResultResponseDto.java
package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class MatchResultResponseDto {
    MatchDto match;
    GroupRankingDto ranking;
}
```

- [ ] **Step 8: Create RankingApiMapper**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingApiMapper.java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.GroupRankingEntry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RankingApiMapper {

    static GroupRankingDto toGroupRankingDto(final GroupRanking ranking) {
        return GroupRankingDto.builder()
                .groupId(ranking.getGroupId().value())
                .groupName(ranking.getGroupName())
                .entries(ranking.getEntries().stream()
                        .map(RankingApiMapper::toGroupRankingEntryDto)
                        .toList())
                .build();
    }

    private static GroupRankingEntryDto toGroupRankingEntryDto(final GroupRankingEntry entry) {
        return GroupRankingEntryDto.builder()
                .rank(entry.getRank())
                .team(MatchTeamDto.builder()
                        .id(entry.getTeam().getId().value())
                        .name(entry.getTeam().getName())
                        .build())
                .played(entry.getPlayed())
                .wins(entry.getWins())
                .draws(entry.getDraws())
                .defeats(entry.getDefeats())
                .goalsFor(entry.getGoalsFor())
                .goalsAgainst(entry.getGoalsAgainst())
                .goalDifference(entry.getGoalDifference())
                .points(entry.getPoints())
                .build();
    }
}
```

- [ ] **Step 9: Create ResultApiMapper**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/ResultApiMapper.java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.MatchOverview;
import abe.fvjc.tournament.schedule.domain.SubmitMatchResultRequest;
import lombok.experimental.UtilityClass;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;
import static abe.fvjc.tournament.schedule.api.ScheduleApiMapper.toMatchDto;

@UtilityClass
public class ResultApiMapper {

    static SubmitMatchResultRequest toSubmitMatchResultRequest(final SubmitMatchResultRequestDto dto) {
        return SubmitMatchResultRequest.builder()
                .score1(dto.getScore1())
                .score2(dto.getScore2())
                .build();
    }

    static MatchResultResponseDto toMatchResultResponseDto(final MatchOverview overview,
                                                            final GroupRanking ranking) {
        return MatchResultResponseDto.builder()
                .match(toMatchDto(overview))
                .ranking(toGroupRankingDto(ranking))
                .build();
    }
}
```

Note: `ScheduleApiMapper.toMatchDto` is currently `private static`. Change it to `static` (package-private) so `ResultApiMapper` can call it:

In `ScheduleApiMapper.java`, change:
```java
private static MatchDto toMatchDto(final MatchOverview overview) {
```
to:
```java
static MatchDto toMatchDto(final MatchOverview overview) {
```

- [ ] **Step 10: Create ResultController**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/ResultController.java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import abe.fvjc.tournament.schedule.domain.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.ResultApiMapper.toMatchResultResponseDto;
import static abe.fvjc.tournament.schedule.api.ResultApiMapper.toSubmitMatchResultRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class ResultController {
    private final RankingService rankingService;
    private final ResultService resultService;

    @PutMapping("/matches/{matchId}/result")
    public MatchResultResponseDto submitResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid SubmitMatchResultRequestDto request) {
        final var matchOverview = resultService.submitResult(tournamentId, matchId, toSubmitMatchResultRequest(request));
        final var ranking = rankingService.computeGroupRanking(tournamentId, matchOverview.getGroupId().value());
        return toMatchResultResponseDto(matchOverview, ranking);
    }
}
```

- [ ] **Step 11: Create RankingController**

```java
// backend/src/main/java/abe/fvjc/tournament/schedule/api/RankingController.java
package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class RankingController {
    private final RankingService rankingService;

    @GetMapping("/groups/{groupId}/ranking")
    public GroupRankingDto getGroupRanking(
            @PathVariable UUID tournamentId,
            @PathVariable UUID groupId) {
        return toGroupRankingDto(rankingService.computeGroupRanking(tournamentId, groupId));
    }
}
```

- [ ] **Step 12: Add start endpoint to TournamentController**

Add to `TournamentController.java` (after the `create` method):

```java
    @PostMapping("/{id}/start")
    public TournamentDto start(@PathVariable UUID id) {
        return toTournamentDto(tournamentService.start(id));
    }
```

- [ ] **Step 13: Compile and run all tests**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 14: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/schedule/api/ backend/src/main/java/abe/fvjc/tournament/api/tournament/TournamentController.java
git commit -m "UC-05: BE API — ResultController, RankingController, DTOs, mappers, tournament start endpoint"
```

---

### Task 9: FE API + Domain layers

**Files:**
- Modify: `frontend/src/app/api/schedule/schedule.api.dto.ts`
- Modify: `frontend/src/app/api/schedule/schedule.api.mapper.ts`
- Modify: `frontend/src/app/domain/schedule/schedule.model.ts`
- Modify: `frontend/src/app/api/tournament/tournament.api.service.ts`
- Modify: `frontend/src/app/domain/tournament/tournament.actions.ts`
- Modify: `frontend/src/app/domain/tournament/tournament.state.ts`
- Create: `frontend/src/app/api/result/result.api.dto.ts`
- Create: `frontend/src/app/api/result/result.api.service.ts`
- Create: `frontend/src/app/api/result/result.api.mapper.ts`
- Create: `frontend/src/app/domain/result/result.model.ts`
- Create: `frontend/src/app/domain/result/result.actions.ts`
- Create: `frontend/src/app/domain/result/result.state.ts`
- Modify: `frontend/src/app/domain/schedule/schedule.state.ts`

- [ ] **Step 1: Update schedule.api.dto.ts — add result to MatchDto**

```typescript
// frontend/src/app/api/schedule/schedule.api.dto.ts
export interface MatchResultDto {
  score1: number;
  score2: number;
}

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
  result: MatchResultDto | null;
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

- [ ] **Step 2: Update schedule.model.ts — add result to Match**

```typescript
// frontend/src/app/domain/schedule/schedule.model.ts
export interface MatchResult {
  score1: number;
  score2: number;
}

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
  result: MatchResult | null;
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

- [ ] **Step 3: Update schedule.api.mapper.ts — map result**

```typescript
// frontend/src/app/api/schedule/schedule.api.mapper.ts
import { Schedule, Round, Match, MatchResult } from '@app/domain/schedule/schedule.model';
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

  static toMatchDomain(dto: MatchDto): Match {
    return {
      id: dto.id,
      field: dto.field,
      groupId: dto.groupId,
      groupName: dto.groupName,
      team1: dto.team1,
      team2: dto.team2,
      result: dto.result ? { score1: dto.result.score1, score2: dto.result.score2 } : null,
    };
  }
}
```

- [ ] **Step 4: Create result.api.dto.ts**

```typescript
// frontend/src/app/api/result/result.api.dto.ts
import { MatchDto } from '@app/api/schedule/schedule.api.dto';

export interface SubmitMatchResultRequestDto {
  score1: number;
  score2: number;
}

export interface GroupRankingEntryDto {
  rank: number;
  team: { id: string; name: string };
  played: number;
  wins: number;
  draws: number;
  defeats: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface GroupRankingDto {
  groupId: string;
  groupName: string;
  entries: GroupRankingEntryDto[];
}

export interface MatchResultResponseDto {
  match: MatchDto;
  ranking: GroupRankingDto;
}
```

- [ ] **Step 5: Create result.model.ts**

```typescript
// frontend/src/app/domain/result/result.model.ts
export interface GroupRankingEntry {
  rank: number;
  team: { id: string; name: string };
  played: number;
  wins: number;
  draws: number;
  defeats: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface GroupRanking {
  groupId: string;
  groupName: string;
  entries: GroupRankingEntry[];
}
```

- [ ] **Step 6: Create result.api.mapper.ts**

```typescript
// frontend/src/app/api/result/result.api.mapper.ts
import { GroupRanking, GroupRankingEntry } from '@app/domain/result/result.model';
import { GroupRankingDto, GroupRankingEntryDto } from '@app/api/result/result.api.dto';

export class ResultApiMapper {

  static toGroupRankingDomain(dto: GroupRankingDto): GroupRanking {
    return {
      groupId: dto.groupId,
      groupName: dto.groupName,
      entries: dto.entries.map(ResultApiMapper.toGroupRankingEntryDomain),
    };
  }

  private static toGroupRankingEntryDomain(dto: GroupRankingEntryDto): GroupRankingEntry {
    return { ...dto };
  }
}
```

- [ ] **Step 7: Create result.api.service.ts**

```typescript
// frontend/src/app/api/result/result.api.service.ts
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TournamentDto } from '@app/api/tournament/tournament.api.dto';
import { GroupRankingDto, MatchResultResponseDto, SubmitMatchResultRequestDto } from '@app/api/result/result.api.dto';

@Injectable({ providedIn: 'root' })
export class ResultApiService {

  private readonly http = inject(HttpClient);

  startTournament$(tournamentId: string): Observable<TournamentDto> {
    return this.http.post<TournamentDto>(`/api/tournaments/${tournamentId}/start`, {});
  }

  submitResult$(tournamentId: string, matchId: string, request: SubmitMatchResultRequestDto): Observable<MatchResultResponseDto> {
    return this.http.put<MatchResultResponseDto>(`/api/tournaments/${tournamentId}/matches/${matchId}/result`, request);
  }

  loadGroupRanking$(tournamentId: string, groupId: string): Observable<GroupRankingDto> {
    return this.http.get<GroupRankingDto>(`/api/tournaments/${tournamentId}/groups/${groupId}/ranking`);
  }
}
```

- [ ] **Step 8: Create result.actions.ts**

```typescript
// frontend/src/app/domain/result/result.actions.ts
export class StartTournament {
  static readonly type = '[Result] Start Tournament';
  constructor(public readonly tournamentId: string) {}
}

export class SubmitResult {
  static readonly type = '[Result] Submit Result';
  constructor(
    public readonly tournamentId: string,
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}

export class LoadGroupRanking {
  static readonly type = '[Result] Load Group Ranking';
  constructor(
    public readonly tournamentId: string,
    public readonly groupId: string,
  ) {}
}
```

- [ ] **Step 9: Create result.state.ts**

```typescript
// frontend/src/app/domain/result/result.state.ts
import { inject, Injectable } from '@angular/core';
import { Action, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { GroupRanking } from '@app/domain/result/result.model';
import { LoadGroupRanking, StartTournament, SubmitResult } from '@app/domain/result/result.actions';
import { ResultApiService } from '@app/api/result/result.api.service';
import { ResultApiMapper } from '@app/api/result/result.api.mapper';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { TournamentApiMapper } from '@app/api/tournament/tournament.api.mapper';

export interface IResultState {
  rankings: { [groupId: string]: GroupRanking };
}

@State<IResultState>({
  name: 'result',
  defaults: { rankings: {} },
})
@Injectable()
export class ResultState {

  private readonly resultApiService = inject(ResultApiService);

  @Action(StartTournament)
  startTournament(ctx: StateContext<IResultState>, { tournamentId }: StartTournament) {
    return this.resultApiService.startTournament$(tournamentId).pipe(
      tap((dto) => {
        const tournamentState = ctx.getState();
        const store = (ctx as any).store;
        ctx.dispatch({ type: '[Tournament] Patch Selected Status', status: dto.status });
      })
    );
  }

  @Action(SubmitResult)
  submitResult(ctx: StateContext<IResultState>, { tournamentId, matchId, score1, score2 }: SubmitResult) {
    return this.resultApiService.submitResult$(tournamentId, matchId, { score1, score2 }).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto.ranking);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }

  @Action(LoadGroupRanking)
  loadGroupRanking(ctx: StateContext<IResultState>, { tournamentId, groupId }: LoadGroupRanking) {
    return this.resultApiService.loadGroupRanking$(tournamentId, groupId).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }
}
```

- [ ] **Step 10: Add StartTournament action handling to TournamentState**

Add `PatchSelectedStatus` action to `tournament.actions.ts`:

```typescript
export class PatchSelectedStatus {
  static readonly type = '[Tournament] Patch Selected Status';
  constructor(public readonly status: import('@app/domain/tournament/tournament.model').TournamentStatus) {}
}
```

Add `@Action(PatchSelectedStatus)` handler to `TournamentState`:

```typescript
  @Action(PatchSelectedStatus)
  patchSelectedStatus(ctx: StateContext<ITournamentState>, { status }: PatchSelectedStatus) {
    const selected = ctx.getState().selected;
    if (selected) {
      ctx.patchState({ selected: { ...selected, status } });
    }
  }
```

Add imports: `PatchSelectedStatus` to the imports list in `tournament.state.ts`.

- [ ] **Step 11: Fix ResultState — use proper dispatch for TournamentState patch**

Replace the `StartTournament` action in `result.state.ts` with a clean implementation:

```typescript
  @Action(StartTournament)
  startTournament(ctx: StateContext<IResultState>, { tournamentId }: StartTournament) {
    return this.resultApiService.startTournament$(tournamentId).pipe(
      tap((dto) => {
        ctx.dispatch(new (require('@app/domain/tournament/tournament.actions').PatchSelectedStatus)(dto.status));
      })
    );
  }
```

Actually, use proper static import. Replace the entire import block at the top of `result.state.ts` and the action:

```typescript
// frontend/src/app/domain/result/result.state.ts
import { inject, Injectable } from '@angular/core';
import { Action, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { GroupRanking } from '@app/domain/result/result.model';
import { LoadGroupRanking, StartTournament, SubmitResult } from '@app/domain/result/result.actions';
import { ResultApiService } from '@app/api/result/result.api.service';
import { ResultApiMapper } from '@app/api/result/result.api.mapper';
import { PatchSelectedStatus } from '@app/domain/tournament/tournament.actions';

export interface IResultState {
  rankings: { [groupId: string]: GroupRanking };
}

@State<IResultState>({
  name: 'result',
  defaults: { rankings: {} },
})
@Injectable()
export class ResultState {

  private readonly resultApiService = inject(ResultApiService);

  @Action(StartTournament)
  startTournament(ctx: StateContext<IResultState>, { tournamentId }: StartTournament) {
    return this.resultApiService.startTournament$(tournamentId).pipe(
      tap((dto) => {
        ctx.dispatch(new PatchSelectedStatus(dto.status));
      })
    );
  }

  @Action(SubmitResult)
  submitResult(ctx: StateContext<IResultState>, { tournamentId, matchId, score1, score2 }: SubmitResult) {
    return this.resultApiService.submitResult$(tournamentId, matchId, { score1, score2 }).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto.ranking);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }

  @Action(LoadGroupRanking)
  loadGroupRanking(ctx: StateContext<IResultState>, { tournamentId, groupId }: LoadGroupRanking) {
    return this.resultApiService.loadGroupRanking$(tournamentId, groupId).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
      })
    );
  }
}
```

- [ ] **Step 12: Add @Action(SubmitResult) to ScheduleState to update match inline**

In `schedule.state.ts`, add import and action handler:

```typescript
import { SubmitResult } from '@app/domain/result/result.actions';
import { ScheduleApiMapper } from '@app/api/schedule/schedule.api.mapper';
```

Add selector and action handler:

```typescript
  @Selector()
  static getRankings(state: IScheduleState): Schedule | undefined {
    return state.schedule;
  }

  @Action(SubmitResult)
  handleSubmitResult(ctx: StateContext<IScheduleState>, { matchId }: SubmitResult) {
    // Updated match result comes back via ResultState;
    // we need to reload the schedule or patch inline.
    // Since we don't have the updated MatchDto here directly, we do nothing —
    // the TournamentResultsPage reloads schedule on init.
    // For live inline updates, see Task 10 which patches via the response.
  }
```

Actually, the design says ScheduleState listens to `SubmitResult` with its own `@Action` handler and updates the relevant match's result inline. But `SubmitResult` action doesn't carry the response DTO — only tournamentId, matchId, score1, score2. The response comes back via the Observable in `ResultState`. 

The cleanest approach: after `ResultState.submitResult` completes, we get `dto.match` back. We need ScheduleState to update the match. We can dispatch a second internal action from `ResultState`:

Add `UpdateMatchResult` action to `result.actions.ts`:
```typescript
export class UpdateMatchResult {
  static readonly type = '[Result] Update Match Result';
  constructor(
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}
```

Then in `ResultState.submitResult`, after patching rankings also dispatch:
```typescript
ctx.dispatch(new UpdateMatchResult(dto.match.id, dto.match.result!.score1, dto.match.result!.score2));
```

And in `ScheduleState`, handle `UpdateMatchResult`:
```typescript
import { UpdateMatchResult } from '@app/domain/result/result.actions';

  @Action(UpdateMatchResult)
  updateMatchResult(ctx: StateContext<IScheduleState>, { matchId, score1, score2 }: UpdateMatchResult) {
    const schedule = ctx.getState().schedule;
    if (!schedule) return;
    const updatedRounds = schedule.rounds.map(round => ({
      ...round,
      matches: round.matches.map(match =>
        match.id === matchId
          ? { ...match, result: { score1, score2 } }
          : match
      ),
    }));
    ctx.patchState({ schedule: { ...schedule, rounds: updatedRounds } });
  }
```

Update `result.state.ts` `submitResult` to dispatch `UpdateMatchResult`:
```typescript
  @Action(SubmitResult)
  submitResult(ctx: StateContext<IResultState>, { tournamentId, matchId, score1, score2 }: SubmitResult) {
    return this.resultApiService.submitResult$(tournamentId, matchId, { score1, score2 }).pipe(
      tap((dto) => {
        const ranking = ResultApiMapper.toGroupRankingDomain(dto.ranking);
        ctx.patchState({
          rankings: { ...ctx.getState().rankings, [ranking.groupId]: ranking },
        });
        if (dto.match.result) {
          ctx.dispatch(new UpdateMatchResult(dto.match.id, dto.match.result.score1, dto.match.result.score2));
        }
      })
    );
  }
```

Add `UpdateMatchResult` import to `result.state.ts`:
```typescript
import { LoadGroupRanking, StartTournament, SubmitResult, UpdateMatchResult } from '@app/domain/result/result.actions';
```

- [ ] **Step 13: Add ResultState selector**

Add to `result.state.ts`:

```typescript
import { Selector } from '@ngxs/store';

  @Selector()
  static getRankings(state: IResultState): { [groupId: string]: GroupRanking } {
    return state.rankings;
  }

  @Selector()
  static getRankingForGroup(state: IResultState): (groupId: string) => GroupRanking | undefined {
    return (groupId: string) => state.rankings[groupId];
  }
```

- [ ] **Step 14: Compile frontend**

```bash
cd frontend && npm run build -- --configuration development 2>&1 | tail -20
```

Expected: no TypeScript errors.

- [ ] **Step 15: Commit**

```bash
git add frontend/src/app/api/schedule/ frontend/src/app/domain/schedule/ frontend/src/app/api/result/ frontend/src/app/domain/result/ frontend/src/app/domain/tournament/
git commit -m "UC-05: FE API + domain — result DTOs, ResultState, schedule/tournament state updates"
```

---

### Task 10: FE Display — TournamentResultsPage + schedule page start button + routes

**Files:**
- Create: `frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.ts`
- Create: `frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.scss`
- Modify: `frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-schedule/tournament-schedule.page.html`
- Modify: `frontend/src/app/modules/tournament.routes.ts`

- [ ] **Step 1: Create tournament-results page TypeScript**

```typescript
// frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.ts
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { Store } from '@ngxs/store';
import { Observable, map } from 'rxjs';
import { Schedule, Match } from '@app/domain/schedule/schedule.model';
import { GroupRanking } from '@app/domain/result/result.model';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { ResultState } from '@app/domain/result/result.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { LoadGroupRanking, SubmitResult } from '@app/domain/result/result.actions';

@Component({
  selector: 'app-tournament-results-page',
  templateUrl: './tournament-results.page.html',
  styleUrl: './tournament-results.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, FormsModule, RouterLink, MatButtonModule, MatIconModule, MatInputModule, MatFormFieldModule],
})
export class TournamentResultsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly schedule$: Observable<Schedule | undefined> = this.store.select(ScheduleState.getSchedule);
  readonly rankings$: Observable<{ [groupId: string]: GroupRanking }> = this.store.select(ResultState.getRankings);

  selectedMatchId: string | null = null;
  score1: number | null = null;
  score2: number | null = null;

  private tournamentId!: string;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadSchedule(this.tournamentId),
    ]);
  }

  selectMatch(match: Match): void {
    this.selectedMatchId = match.id;
    this.score1 = match.result?.score1 ?? null;
    this.score2 = match.result?.score2 ?? null;
  }

  submitResult(): void {
    if (this.selectedMatchId == null || this.score1 == null || this.score2 == null) return;
    this.store.dispatch(new SubmitResult(this.tournamentId, this.selectedMatchId, this.score1, this.score2));
  }

  getSelectedMatch(schedule: Schedule | undefined): Match | null {
    if (!schedule || !this.selectedMatchId) return null;
    for (const round of schedule.rounds) {
      const found = round.matches.find(m => m.id === this.selectedMatchId);
      if (found) return found;
    }
    return null;
  }

  getRankingForMatch(match: Match, rankings: { [groupId: string]: GroupRanking }): GroupRanking | null {
    return rankings[match.groupId] ?? null;
  }
}
```

- [ ] **Step 2: Create tournament-results page HTML**

```html
<!-- frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.html -->
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <div class="header-left">
      <button mat-icon-button [routerLink]="['..', 'schedule']">
        <mat-icon>arrow_back</mat-icon>
      </button>
      <div>
        <h1>{{ tournament.name }}</h1>
        <span class="subtitle">Résultats</span>
      </div>
    </div>
  </div>

  <div class="results-layout">
    <div class="schedule-panel">
      @if (schedule$ | async; as schedule) {
        @for (round of schedule.rounds; track round.id) {
          <div class="round-section">
            <div class="round-header">
              <span class="round-number">Round {{ round.number }}</span>
              <span class="round-time">{{ round.startTime | date:'HH:mm' }}</span>
            </div>
            @for (match of round.matches; track match.id) {
              <div class="match-row"
                   [class.selected]="selectedMatchId === match.id"
                   [class.has-result]="match.result !== null"
                   (click)="selectMatch(match)">
                <span class="match-field">T{{ match.field }}</span>
                <span class="match-group">{{ match.groupName }}</span>
                <span class="match-teams">{{ match.team1.name }} vs {{ match.team2.name }}</span>
                @if (match.result) {
                  <span class="match-score">{{ match.result.score1 }} - {{ match.result.score2 }}</span>
                }
              </div>
            }
          </div>
        }
      }
    </div>

    <div class="detail-panel">
      @if (schedule$ | async; as schedule) {
        @if (rankings$ | async; as rankings) {
          @if (getSelectedMatch(schedule); as match) {
            <div class="result-form">
              <h2>{{ match.team1.name }} vs {{ match.team2.name }}</h2>
              <p class="form-subtitle">{{ match.groupName }} — Terrain {{ match.field }}</p>

              <div class="score-inputs">
                <mat-form-field appearance="outline">
                  <mat-label>{{ match.team1.name }}</mat-label>
                  <input matInput type="number" min="0" max="500" [(ngModel)]="score1">
                </mat-form-field>
                <span class="score-separator">—</span>
                <mat-form-field appearance="outline">
                  <mat-label>{{ match.team2.name }}</mat-label>
                  <input matInput type="number" min="0" max="500" [(ngModel)]="score2">
                </mat-form-field>
              </div>

              <button mat-raised-button color="primary" (click)="submitResult()">
                Enregistrer
              </button>
            </div>

            @if (getRankingForMatch(match, rankings); as ranking) {
              <div class="ranking-panel">
                <h3>Classement — Groupe {{ ranking.groupName }}</h3>
                <table class="ranking-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Équipe</th>
                      <th>J</th>
                      <th>V</th>
                      <th>N</th>
                      <th>D</th>
                      <th>BP</th>
                      <th>BC</th>
                      <th>Diff</th>
                      <th>Pts</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (entry of ranking.entries; track entry.team.id) {
                      <tr>
                        <td>{{ entry.rank }}</td>
                        <td>{{ entry.team.name }}</td>
                        <td>{{ entry.played }}</td>
                        <td>{{ entry.wins }}</td>
                        <td>{{ entry.draws }}</td>
                        <td>{{ entry.defeats }}</td>
                        <td>{{ entry.goalsFor }}</td>
                        <td>{{ entry.goalsAgainst }}</td>
                        <td>{{ entry.goalDifference }}</td>
                        <td><strong>{{ entry.points }}</strong></td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          } @else {
            <div class="empty-detail">
              <mat-icon>touch_app</mat-icon>
              <p>Sélectionnez un match pour saisir le résultat</p>
            </div>
          }
        }
      }
    </div>
  </div>
}
```

- [ ] **Step 3: Create tournament-results page SCSS**

```scss
// frontend/src/app/display/tournament/pages/tournament-results/tournament-results.page.scss
.results-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  height: calc(100vh - 80px);
  overflow: hidden;
}

.schedule-panel {
  overflow-y: auto;
  padding: 16px;
}

.round-section {
  margin-bottom: 16px;
}

.round-header {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--mat-sys-surface-variant);
  border-radius: 4px;
  margin-bottom: 4px;
  font-weight: 500;
}

.match-row {
  display: grid;
  grid-template-columns: 40px 60px 1fr auto;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 4px;
  border: 1px solid transparent;
  margin-bottom: 2px;

  &:hover {
    background: var(--mat-sys-surface-variant);
  }

  &.selected {
    border-color: var(--mat-sys-primary);
    background: var(--mat-sys-primary-container);
  }

  &.has-result {
    .match-score {
      font-weight: 600;
      color: var(--mat-sys-primary);
    }
  }
}

.detail-panel {
  overflow-y: auto;
  padding: 16px;
}

.result-form {
  margin-bottom: 24px;

  h2 { margin: 0 0 4px; }
  .form-subtitle { color: var(--mat-sys-on-surface-variant); margin: 0 0 16px; }
}

.score-inputs {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  mat-form-field { flex: 1; }
  .score-separator { font-size: 20px; font-weight: bold; }
}

.ranking-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 8px 6px;
    text-align: center;
    border-bottom: 1px solid var(--mat-sys-outline-variant);

    &:nth-child(2) { text-align: left; }
  }

  th { font-weight: 600; }
}

.empty-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--mat-sys-on-surface-variant);

  mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
}
```

- [ ] **Step 4: Add "Démarrer le tournoi" button to schedule page TS**

In `tournament-schedule.page.ts`, add import and method:

```typescript
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { StartTournament } from '@app/domain/result/result.actions';
import { TournamentStatus } from '@app/domain/tournament/tournament.model';
```

Add to the class:
```typescript
  private readonly router = inject(Router);

  isInProgress(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.IN_PROGRESS;
  }

  startTournament(): void {
    if (confirm('Démarrer le tournoi ? Cette action est irréversible.')) {
      this.store.dispatch(new StartTournament(this.tournamentId)).subscribe(() => {
        this.router.navigate(['/', this.tournamentId, 'results']);
      });
    }
  }
```

- [ ] **Step 5: Add start button to schedule page HTML**

In `tournament-schedule.page.html`, replace the header `@if` block:

```html
    <div class="header-right">
      @if (isDraft(tournament)) {
        <button mat-raised-button color="primary" (click)="openGenerateModal()">
          <mat-icon>calendar_month</mat-icon>
          Générer l'horaire
        </button>
      }
      @if (isDraft(tournament) && (schedule$ | async)?.totalRounds! > 0) {
        <button mat-raised-button color="accent" (click)="startTournament()">
          <mat-icon>play_arrow</mat-icon>
          Démarrer le tournoi
        </button>
      }
      @if (isInProgress(tournament)) {
        <button mat-stroked-button [routerLink]="['..', tournament.id, 'results']">
          <mat-icon>emoji_events</mat-icon>
          Voir les résultats
        </button>
      }
    </div>
```

Replace the existing `<div class="header-left">` ... closing `</div>` wrapper, changing:
```html
  <div class="page-header">
    <div class="header-left">
```
to wrap both header divs:
```html
  <div class="page-header">
    <div class="header-left">
      ...
    </div>
    <div class="header-right">
      @if (isDraft(tournament)) { ... }
      @if (...) { ... }
    </div>
```

- [ ] **Step 6: Update tournament.routes.ts**

```typescript
// frontend/src/app/modules/tournament.routes.ts
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { ScheduleState } from '../domain/schedule/schedule.state';
import { ResultState } from '../domain/result/result.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';
import { TournamentSchedulePage } from '../display/tournament/pages/tournament-schedule/tournament-schedule.page';
import { TournamentResultsPage } from '../display/tournament/pages/tournament-results/tournament-results.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState, ResultState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
      { path: ':id/schedule', component: TournamentSchedulePage },
      { path: ':id/results', component: TournamentResultsPage },
    ],
  },
];
```

- [ ] **Step 7: Build and verify**

```bash
cd frontend && npm run build -- --configuration development 2>&1 | tail -20
```

Expected: no TypeScript or template errors.

- [ ] **Step 8: Start both servers and manually test**

```bash
# Terminal 1 — backend
cd backend && ./mvnw spring-boot:run -q

# Terminal 2 — frontend
cd frontend && npm start
```

Test flow:
1. Create a tournament, generate groups, generate schedule
2. Visit `/schedule` — verify "Démarrer le tournoi" button appears when schedule exists
3. Click it, confirm → should navigate to `/results`
4. Click a match on the left panel → score inputs appear on right
5. Enter scores, click "Enregistrer" → ranking table appears
6. Click another match → verify score inputs reset correctly
7. Reload page — verify matches with results show scores in the list

- [ ] **Step 9: Commit**

```bash
git add frontend/src/app/display/tournament/ frontend/src/app/modules/tournament.routes.ts
git commit -m "UC-05: FE display — TournamentResultsPage, schedule start button, routes"
```

---

### Final: Full test suite

- [ ] **Step 1: Run all backend tests**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -15
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 2: Run frontend build**

```bash
cd frontend && npm run build 2>&1 | tail -10
```

Expected: Build successful.

- [ ] **Step 3: Final commit**

```bash
git add -A
git status
# Should be clean — everything committed in prior tasks
```
