# UC-08: Enter Knockout Stage Results — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add knockout match result entry with automatic winner/loser advancement, including TROISIEME_PLACE round generation.

**Architecture:** Backend extends BracketMatch with `loserNextMatchId`, updates generation to append a TROISIEME_PLACE round, and adds an `enterResult` endpoint. Frontend adds an action + state handler, and a two-panel UI on the bracket page (click match → enter scores → submit).

**Tech Stack:** Java 21 / Spring Boot / Liquibase / Lombok / Mockito (backend); Angular 19 / NGXS / Angular Material (frontend).

## Global Constraints

- All local variables: `final var` (reassigned: `var`); never nest calls — break into named steps.
- Method parameters always `final`.
- `@Value @Builder @With` on domain classes; `@Jacksonized` on all DTOs.
- `inject()` everywhere in Angular — no constructor injection.
- All Angular components are standalone; styles in `.scss` via `styleUrl`.
- API service methods end with `$`.
- Domain service transformation methods are `static`.
- No `ArgumentCaptor` in tests — use `returnsFirstArg()` + assert on return value.
- Test method naming: `methodTestedNameWhenXxxxShouldXxx`.

---

### Task 1: DB migration and domain model update

**Files:**
- Create: `backend/src/main/resources/db/changelog/20260706120002_alter_bracket_matches_add_loser_next_match_id.xml`
- Modify: `backend/src/main/resources/db/master.xml`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketMatch.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/bracket/BracketMatchEntity.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/persistence/bracket/BracketDbMapper.java`

**Interfaces:**
- Produces: `BracketMatch.loserNextMatchId: BracketMatchId?` — used by Tasks 2 and 3.

- [ ] **Step 1: Create Liquibase migration file**

`backend/src/main/resources/db/changelog/20260706120002_alter_bracket_matches_add_loser_next_match_id.xml`:
```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

    <changeSet id="20260706120002" author="aberney">
        <addColumn tableName="bracket_matches">
            <column name="loser_next_match_id" type="TEXT"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Register migration in master.xml**

Add this line at the end of `backend/src/main/resources/db/master.xml`, inside `<databaseChangeLog>`, after the last `<include>`:
```xml
    <include relativeToChangelogFile="true" file="changelog/20260706120002_alter_bracket_matches_add_loser_next_match_id.xml"/>
```

- [ ] **Step 3: Add `loserNextMatchId` to the domain model**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketMatch.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class BracketMatch {
    BracketMatchId id;
    BracketRoundId roundId;
    int field;
    TeamRef team1;
    TeamRef team2;
    MatchResult result;
    BracketMatchId nextMatchId;
    int nextMatchTeamSlot;
    BracketMatchId loserNextMatchId;
}
```

- [ ] **Step 4: Add `loserNextMatchId` to the JPA entity**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/persistence/bracket/BracketMatchEntity.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "bracket_matches")
@Getter
@Setter
@NoArgsConstructor
class BracketMatchEntity {
    @Id
    private UUID id;
    private UUID roundId;
    private int field;
    private UUID team1Id;
    private String team1Name;
    private UUID team2Id;
    private String team2Name;
    private Integer score1;
    private Integer score2;
    private UUID nextMatchId;
    private int nextMatchTeamSlot;
    private UUID loserNextMatchId;
}
```

- [ ] **Step 5: Update persistence mapper to handle `loserNextMatchId`**

Replace the full content of `backend/src/main/java/abe/fvjc/tournament/persistence/bracket/BracketDbMapper.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketMatchId;
import abe.fvjc.tournament.bracket.domain.BracketRound;
import abe.fvjc.tournament.bracket.domain.BracketRoundId;
import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import abe.fvjc.tournament.team.domain.TeamId;

import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
class BracketDbMapper {

    static BracketRound toBracketRound(final BracketRoundEntity entity) {
        return BracketRound.builder()
                .id(BracketRoundId.of(entity.getId()))
                .tournamentId(TournamentId.of(entity.getTournamentId()))
                .number(entity.getNumber())
                .name(entity.getName())
                .startTime(LocalDateTime.parse(entity.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(List.of())
                .build();
    }

    static BracketRoundEntity toBracketRoundEntity(final BracketRound round) {
        final var entity = new BracketRoundEntity();
        entity.setId(round.getId().value());
        entity.setTournamentId(round.getTournamentId().value());
        entity.setNumber(round.getNumber());
        entity.setName(round.getName());
        entity.setStartTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }

    static BracketMatch toBracketMatch(final BracketMatchEntity entity) {
        final var team1 = entity.getTeam1Id() != null
                ? toTeamRef(TeamId.of(entity.getTeam1Id()), entity.getTeam1Name())
                : null;
        final var team2 = entity.getTeam2Id() != null
                ? toTeamRef(TeamId.of(entity.getTeam2Id()), entity.getTeam2Name())
                : null;
        final var result = entity.getScore1() != null
                ? MatchResult.builder().score1(entity.getScore1()).score2(entity.getScore2()).build()
                : null;
        final var nextMatchId = entity.getNextMatchId() != null
                ? BracketMatchId.of(entity.getNextMatchId())
                : null;
        final var loserNextMatchId = entity.getLoserNextMatchId() != null
                ? BracketMatchId.of(entity.getLoserNextMatchId())
                : null;
        return BracketMatch.builder()
                .id(BracketMatchId.of(entity.getId()))
                .roundId(BracketRoundId.of(entity.getRoundId()))
                .field(entity.getField())
                .team1(team1)
                .team2(team2)
                .result(result)
                .nextMatchId(nextMatchId)
                .nextMatchTeamSlot(entity.getNextMatchTeamSlot())
                .loserNextMatchId(loserNextMatchId)
                .build();
    }

    static BracketMatchEntity toBracketMatchEntity(final BracketMatch match) {
        final var entity = new BracketMatchEntity();
        entity.setId(match.getId().value());
        entity.setRoundId(match.getRoundId().value());
        entity.setField(match.getField());
        if (match.getTeam1() != null) {
            entity.setTeam1Id(match.getTeam1().getId().value());
            entity.setTeam1Name(match.getTeam1().getName());
        }
        if (match.getTeam2() != null) {
            entity.setTeam2Id(match.getTeam2().getId().value());
            entity.setTeam2Name(match.getTeam2().getName());
        }
        if (match.getResult() != null) {
            entity.setScore1(match.getResult().getScore1());
            entity.setScore2(match.getResult().getScore2());
        }
        entity.setNextMatchId(match.getNextMatchId() != null ? match.getNextMatchId().value() : null);
        entity.setNextMatchTeamSlot(match.getNextMatchTeamSlot());
        entity.setLoserNextMatchId(match.getLoserNextMatchId() != null ? match.getLoserNextMatchId().value() : null);
        return entity;
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/ backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketMatch.java backend/src/main/java/abe/fvjc/tournament/persistence/bracket/
git commit -m "UC-08: add loserNextMatchId to BracketMatch model and DB migration

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Update bracket generation to include TROISIEME_PLACE

**Files:**
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketService.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java`

**Interfaces:**
- Consumes: `BracketMatch.loserNextMatchId` from Task 1.
- Produces: `generate()` now returns `totalRounds + 1` rounds when `totalTeams >= 4`; the last round is always "Troisième place".

- [ ] **Step 1: Update the three existing generate tests to expect a TROISIEME_PLACE round**

In `BracketServiceTest.java`, update `generateWhenValidRequestShouldCreateRoundsAndMatches` (8-team case):

Change the verify block:
```java
// Before:
verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
verify(bracketMatchStore, times(7)).save(any(BracketMatch.class));

// After:
verify(bracketRoundStore, times(4)).save(any(BracketRound.class));
verify(bracketMatchStore, times(8)).save(any(BracketMatch.class));
```

Change the assert block:
```java
// Before:
assertEquals(3, roundsGenerated.size());
assertEquals("Quarts de finale", roundsGenerated.get(0).getName());
assertEquals("Demi-finales", roundsGenerated.get(1).getName());
assertEquals("Finale", roundsGenerated.get(2).getName());
assertEquals(4, roundsGenerated.get(0).getMatches().size());
assertEquals(2, roundsGenerated.get(1).getMatches().size());
assertEquals(1, roundsGenerated.get(2).getMatches().size());

// After:
assertEquals(4, roundsGenerated.size());
assertEquals("Quarts de finale", roundsGenerated.get(0).getName());
assertEquals("Demi-finales", roundsGenerated.get(1).getName());
assertEquals("Finale", roundsGenerated.get(2).getName());
assertEquals("Troisième place", roundsGenerated.get(3).getName());
assertEquals(4, roundsGenerated.get(0).getMatches().size());
assertEquals(2, roundsGenerated.get(1).getMatches().size());
assertEquals(1, roundsGenerated.get(2).getMatches().size());
assertEquals(1, roundsGenerated.get(3).getMatches().size());
```

Update `generateWhenFourTeamsQualifiedShouldCreateTwoRounds` (4-team case):
```java
// Before:
verify(bracketRoundStore, times(2)).save(any(BracketRound.class));
verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));
// ...
assertEquals(2, roundsGenerated.size());
assertEquals("Demi-finales", roundsGenerated.get(0).getName());
assertEquals("Finale", roundsGenerated.get(1).getName());
assertEquals(2, roundsGenerated.get(0).getMatches().size());
assertEquals(1, roundsGenerated.get(1).getMatches().size());

// After:
verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
verify(bracketMatchStore, times(4)).save(any(BracketMatch.class));
// ...
assertEquals(3, roundsGenerated.size());
assertEquals("Demi-finales", roundsGenerated.get(0).getName());
assertEquals("Finale", roundsGenerated.get(1).getName());
assertEquals("Troisième place", roundsGenerated.get(2).getName());
assertEquals(2, roundsGenerated.get(0).getMatches().size());
assertEquals(1, roundsGenerated.get(1).getMatches().size());
assertEquals(1, roundsGenerated.get(2).getMatches().size());
```

Update `generateWhenExtraQualifiersShouldSelectByTieBreakerAndCreateBracket` (also 4 qualifying teams):
Same change as the 4-team test above:
```java
// Before:
verify(bracketRoundStore, times(2)).save(any(BracketRound.class));
verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));
// ...
assertEquals(2, roundsGenerated.size());
assertEquals("Demi-finales", roundsGenerated.get(0).getName());
assertEquals("Finale", roundsGenerated.get(1).getName());
assertEquals(2, roundsGenerated.get(0).getMatches().size());
assertEquals(1, roundsGenerated.get(1).getMatches().size());

// After:
verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
verify(bracketMatchStore, times(4)).save(any(BracketMatch.class));
// ...
assertEquals(3, roundsGenerated.size());
assertEquals("Demi-finales", roundsGenerated.get(0).getName());
assertEquals("Finale", roundsGenerated.get(1).getName());
assertEquals("Troisième place", roundsGenerated.get(2).getName());
assertEquals(2, roundsGenerated.get(0).getMatches().size());
assertEquals(1, roundsGenerated.get(1).getMatches().size());
assertEquals(1, roundsGenerated.get(2).getMatches().size());
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament/backend
./mvnw test -Dtest=BracketServiceTest -q 2>&1 | tail -20
```

Expected: 3 test failures. Reason: generate returns wrong number of rounds.

- [ ] **Step 3: Implement TROISIEME_PLACE generation in BracketService**

Replace the full `generate` method in `BracketService.java` (lines 35–112):

```java
public List<BracketRound> generate(final UUID tournamentId, final BracketGenerateRequest request) {
    final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
    final var groups = groupStore.findAllByTournamentId(tournamentId);
    final var groupRankings = rankingService.computeAllGroupRankings(tournamentId, List.of());
    validateBracketGenerateRequest(request, groups.size());

    final var qualifiersPerGroup = request.getTotalQualifiedTeams() / groups.size();
    final var extraQualifiers = request.getTotalQualifiedTeams() % groups.size();

    if (extraQualifiers > 0) {
        final var eligibleGroups = groupRankings.stream()
                .filter(r -> r.getEntries().size() > qualifiersPerGroup)
                .count();
        if (eligibleGroups < extraQualifiers) {
            throw new BusinessException("Pas assez d'équipes dans les groupes pour sélectionner "
                    + extraQualifiers + " qualifié(s) supplémentaire(s)");
        }
    }

    final var round1Pairs = buildRound1Pairs(groupRankings, qualifiersPerGroup, extraQualifiers, request.getTieBreaker());
    final var totalTeams = request.getTotalQualifiedTeams();
    final var totalRounds = (int) (Math.log(totalTeams) / Math.log(2));
    final var startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
    final var firstRoundStart = LocalDateTime.of(tournament.getDate(), startTime);
    final var hasSemiFinals = totalRounds >= 2;
    final var troisiemePlaceMatchId = hasSemiFinals ? BracketMatchId.of(UUID.randomUUID()) : null;

    final var matchIdsByRound = preassignMatchIds(round1Pairs.size(), totalRounds);

    final var savedRounds = new ArrayList<BracketRound>();
    final var matchesByRound = new ArrayList<List<BracketMatch>>();

    for (int r = 0; r < totalRounds; r++) {
        final var roundStart = firstRoundStart.plusMinutes(
                (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
        final var teamsInRound = totalTeams / (int) Math.pow(2, r);
        final var roundToSave = BracketRound.builder()
                .id(BracketRoundId.of(UUID.randomUUID()))
                .tournamentId(TournamentId.of(tournamentId))
                .number(r + 1)
                .name(roundName(teamsInRound))
                .startTime(roundStart)
                .matches(List.of())
                .build();
        final var round = bracketRoundStore.save(roundToSave);

        final var matchIds = matchIdsByRound.get(r);
        final var roundMatches = new ArrayList<BracketMatch>();

        for (int m = 0; m < matchIds.size(); m++) {
            final var nextMatchId = (r < totalRounds - 1)
                    ? matchIdsByRound.get(r + 1).get(m / 2)
                    : null;
            final var slot = (m % 2) + 1;
            final var team1 = (r == 0) ? round1Pairs.get(m).first() : null;
            final var team2 = (r == 0) ? round1Pairs.get(m).second() : null;
            final var field = (m % tournament.getNumberOfFields()) + 1;
            final var loserMatchId = (hasSemiFinals && teamsInRound == 4)
                    ? troisiemePlaceMatchId
                    : null;

            final var matchToSave = BracketMatch.builder()
                    .id(matchIds.get(m))
                    .roundId(round.getId())
                    .field(field)
                    .team1(team1)
                    .team2(team2)
                    .result(null)
                    .nextMatchId(nextMatchId)
                    .nextMatchTeamSlot(slot)
                    .loserNextMatchId(loserMatchId)
                    .build();
            final var match = bracketMatchStore.save(matchToSave);
            roundMatches.add(match);
        }
        savedRounds.add(round);
        matchesByRound.add(roundMatches);
    }

    if (hasSemiFinals) {
        final var troisiemePlaceStart = firstRoundStart.plusMinutes(
                (long) (totalRounds - 1) * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
        final var troisiemePlaceRoundToSave = BracketRound.builder()
                .id(BracketRoundId.of(UUID.randomUUID()))
                .tournamentId(TournamentId.of(tournamentId))
                .number(totalRounds + 1)
                .name("Troisième place")
                .startTime(troisiemePlaceStart)
                .matches(List.of())
                .build();
        final var troisiemePlaceRound = bracketRoundStore.save(troisiemePlaceRoundToSave);
        final var troisiemePlaceMatchToSave = BracketMatch.builder()
                .id(troisiemePlaceMatchId)
                .roundId(troisiemePlaceRound.getId())
                .field(1)
                .team1(null)
                .team2(null)
                .result(null)
                .nextMatchId(null)
                .nextMatchTeamSlot(0)
                .loserNextMatchId(null)
                .build();
        final var troisiemePlaceMatch = bracketMatchStore.save(troisiemePlaceMatchToSave);
        savedRounds.add(troisiemePlaceRound);
        matchesByRound.add(List.of(troisiemePlaceMatch));
    }

    return savedRounds.stream()
            .map(r -> r.withMatches(matchesByRound.get(r.getNumber() - 1)))
            .toList();
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test -Dtest=BracketServiceTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketService.java backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java
git commit -m "UC-08: generate TROISIEME_PLACE round during bracket generation

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Enter result — service + endpoint

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketMatchResultRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/api/bracket/BracketMatchResultRequestDto.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketValidator.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketService.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/api/bracket/BracketApiMapper.java`
- Modify: `backend/src/main/java/abe/fvjc/tournament/api/bracket/BracketController.java`
- Test: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java`
- Modify: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketFakes.java`

**Interfaces:**
- Consumes: `BracketMatch.loserNextMatchId` (Task 1), `BracketMatch.nextMatchTeamSlot` (existing).
- Produces: `PUT /api/tournaments/{tournamentId}/bracket/matches/{matchId}/result` → `BracketMatchDto`.

- [ ] **Step 1: Create the domain request object**

Create `backend/src/main/java/abe/fvjc/tournament/domain/bracket/BracketMatchResultRequest.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BracketMatchResultRequest {
    Integer score1;
    Integer score2;
}
```

- [ ] **Step 2: Add test helpers to BracketFakes**

Add these two methods to `BracketFakes.java` (inside the class, after the existing methods). Add the missing imports at the top: `import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;` and `import abe.fvjc.tournament.team.domain.TeamId;` and `import java.util.UUID;`.

```java
public static BracketMatch buildMatch() {
    return BracketMatch.builder()
            .id(IdGenerator.matchId())
            .roundId(IdGenerator.roundId())
            .field(1)
            .team1(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 1"))
            .team2(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 2"))
            .result(null)
            .nextMatchId(IdGenerator.matchId())
            .nextMatchTeamSlot(1)
            .loserNextMatchId(null)
            .build();
}

public static BracketMatchResultRequest buildMatchResultRequest() {
    return BracketMatchResultRequest.builder()
            .score1(3)
            .score2(1)
            .build();
}
```

- [ ] **Step 3: Add enterResult stub to BracketService**

Add this method stub at the end of `BracketService.java` (before the closing `}` of the class, after `findAll`):

```java
public BracketMatch enterResult(final UUID matchId, final BracketMatchResultRequest request) {
    throw new UnsupportedOperationException("not yet implemented");
}
```

- [ ] **Step 4: Write failing tests**

Add these six test methods to `BracketServiceTest.java` (after the existing test methods, before the helper methods):

```java
@Test
void enterResultWhenTerminalMatchShouldSaveResultOnly() {
    final var matchId = UUID.randomUUID();
    final var match = BracketFakes.buildMatch()
            .withId(BracketMatchId.of(matchId))
            .withNextMatchId(null)
            .withLoserNextMatchId(null);
    final var request = BracketFakes.buildMatchResultRequest();

    when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
    when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

    final var matchUpdated = bracketService.enterResult(matchId, request);

    verify(bracketMatchStore).findById(matchId);
    verify(bracketMatchStore).save(any(BracketMatch.class));

    assertNotNull(matchUpdated.getResult());
    assertEquals(3, matchUpdated.getResult().getScore1());
    assertEquals(1, matchUpdated.getResult().getScore2());
}

@Test
void enterResultWhenHasNextMatchShouldAdvanceWinnerToNextMatch() {
    final var matchId = UUID.randomUUID();
    final var nextMatchId = IdGenerator.matchId();
    final var match = BracketFakes.buildMatch()
            .withId(BracketMatchId.of(matchId))
            .withNextMatchId(nextMatchId)
            .withNextMatchTeamSlot(1)
            .withLoserNextMatchId(null);
    final var nextMatch = BracketFakes.buildMatch()
            .withId(nextMatchId)
            .withTeam1(null)
            .withTeam2(null);
    final var request = BracketFakes.buildMatchResultRequest();

    when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
    when(bracketMatchStore.findById(nextMatchId.value())).thenReturn(Optional.of(nextMatch));
    when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

    final var matchUpdated = bracketService.enterResult(matchId, request);

    verify(bracketMatchStore, times(2)).save(any(BracketMatch.class));

    assertNotNull(matchUpdated.getResult());
}

@Test
void enterResultWhenDemiFinaleLoserShouldAdvanceToTroisiemePlace() {
    final var matchId = UUID.randomUUID();
    final var nextMatchId = IdGenerator.matchId();
    final var loserMatchId = IdGenerator.matchId();
    final var match = BracketFakes.buildMatch()
            .withId(BracketMatchId.of(matchId))
            .withNextMatchId(nextMatchId)
            .withNextMatchTeamSlot(2)
            .withLoserNextMatchId(loserMatchId);
    final var nextMatch = BracketFakes.buildMatch().withId(nextMatchId).withTeam1(null).withTeam2(null);
    final var loserMatch = BracketFakes.buildMatch().withId(loserMatchId).withTeam1(null).withTeam2(null);
    final var request = BracketFakes.buildMatchResultRequest();

    when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
    when(bracketMatchStore.findById(nextMatchId.value())).thenReturn(Optional.of(nextMatch));
    when(bracketMatchStore.findById(loserMatchId.value())).thenReturn(Optional.of(loserMatch));
    when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

    final var matchUpdated = bracketService.enterResult(matchId, request);

    verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));

    assertNotNull(matchUpdated.getResult());
}

@Test
void enterResultWhenDrawShouldThrowValidationException() {
    final var matchId = UUID.randomUUID();
    final var request = BracketMatchResultRequest.builder().score1(2).score2(2).build();

    final var exception = assertThrows(ValidationException.class,
            () -> bracketService.enterResult(matchId, request));

    assertEquals(1, exception.getErrors().size());
    assertEquals("scores", exception.getErrors().get(0).field());
}

@Test
void enterResultWhenScoreNegativeShouldThrowValidationException() {
    final var matchId = UUID.randomUUID();
    final var request = BracketMatchResultRequest.builder().score1(-1).score2(2).build();

    final var exception = assertThrows(ValidationException.class,
            () -> bracketService.enterResult(matchId, request));

    assertEquals(1, exception.getErrors().size());
}

@Test
void enterResultWhenMatchNotFoundShouldThrowNotFoundException() {
    final var matchId = UUID.randomUUID();
    final var request = BracketFakes.buildMatchResultRequest();

    when(bracketMatchStore.findById(matchId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> bracketService.enterResult(matchId, request));

    verify(bracketMatchStore).findById(matchId);
}
```

Add these imports to `BracketServiceTest.java` if not already present:
```java
import abe.fvjc.tournament.bracket.domain.BracketMatchId;
import abe.fvjc.tournament.bracket.domain.BracketMatchResultRequest;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.shared.exception.ValidationException;
```

- [ ] **Step 5: Run tests to verify 6 new tests fail**

```bash
./mvnw test -Dtest=BracketServiceTest -q 2>&1 | tail -20
```

Expected: 6 new tests fail (UnsupportedOperationException or similar). The 4 generate tests still pass.

- [ ] **Step 6: Implement validateBracketMatchResult in BracketValidator**

Add this method to `BracketValidator.java` (after the existing `validateBracketGenerateRequest` method):

```java
public static void validateBracketMatchResult(final BracketMatchResultRequest request) {
    final var errors = new ArrayList<ValidationException.FieldError>();

    if (request.getScore1() == null || request.getScore2() == null) {
        errors.add(new ValidationException.FieldError("scores", "Les deux scores sont obligatoires"));
    } else {
        if (request.getScore1() < 0 || request.getScore2() < 0) {
            errors.add(new ValidationException.FieldError("scores", "Le score ne peut pas être négatif"));
        }
        if (request.getScore1() > 500 || request.getScore2() > 500) {
            errors.add(new ValidationException.FieldError("scores", "Le score ne peut pas dépasser 500"));
        }
        if (request.getScore1().equals(request.getScore2())) {
            errors.add(new ValidationException.FieldError("scores",
                    "Un match éliminatoire ne peut pas se terminer sur un match nul"));
        }
    }

    if (!errors.isEmpty()) {
        throw new ValidationException(errors);
    }
}
```

Add `import abe.fvjc.tournament.bracket.domain.BracketMatchResultRequest;` to `BracketValidator.java`.

Also add the static import at the call site in `BracketService.java`:
```java
import static abe.fvjc.tournament.bracket.domain.BracketValidator.validateBracketMatchResult;
```

- [ ] **Step 7: Implement enterResult in BracketService**

Replace the stub `enterResult` method with:

```java
public BracketMatch enterResult(final UUID matchId, final BracketMatchResultRequest request) {
    validateBracketMatchResult(request);

    final var match = bracketMatchStore.findById(matchId)
            .orElseThrow(() -> new NotFoundException("Match", matchId));

    final var result = MatchResult.builder()
            .score1(request.getScore1())
            .score2(request.getScore2())
            .build();
    final var savedMatch = bracketMatchStore.save(match.withResult(result));

    final var winner = request.getScore1() > request.getScore2()
            ? match.getTeam1()
            : match.getTeam2();
    final var loser = request.getScore1() > request.getScore2()
            ? match.getTeam2()
            : match.getTeam1();

    if (savedMatch.getNextMatchId() != null) {
        final var nextMatch = bracketMatchStore.findById(savedMatch.getNextMatchId().value())
                .orElseThrow(() -> new NotFoundException("Match", savedMatch.getNextMatchId().value()));
        final var updatedNextMatch = savedMatch.getNextMatchTeamSlot() == 1
                ? nextMatch.withTeam1(winner)
                : nextMatch.withTeam2(winner);
        bracketMatchStore.save(updatedNextMatch);
    }

    if (savedMatch.getLoserNextMatchId() != null) {
        final var loserMatch = bracketMatchStore.findById(savedMatch.getLoserNextMatchId().value())
                .orElseThrow(() -> new NotFoundException("Match", savedMatch.getLoserNextMatchId().value()));
        final var updatedLoserMatch = savedMatch.getNextMatchTeamSlot() == 1
                ? loserMatch.withTeam1(loser)
                : loserMatch.withTeam2(loser);
        bracketMatchStore.save(updatedLoserMatch);
    }

    return savedMatch;
}
```

Add this import to `BracketService.java` if not already present:
```java
import abe.fvjc.tournament.schedule.domain.MatchResult;
```

- [ ] **Step 8: Run all tests to verify all pass**

```bash
./mvnw test -Dtest=BracketServiceTest -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`, 10 tests pass.

- [ ] **Step 9: Create the request DTO**

Create `backend/src/main/java/abe/fvjc/tournament/api/bracket/BracketMatchResultRequestDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketMatchResultRequestDto {
    @NotNull Integer score1;
    @NotNull Integer score2;
}
```

- [ ] **Step 10: Add mapper method to BracketApiMapper**

Add this static method to `BracketApiMapper.java`:

```java
static BracketMatchResultRequest toBracketMatchResultRequest(final BracketMatchResultRequestDto dto) {
    return BracketMatchResultRequest.builder()
            .score1(dto.getScore1())
            .score2(dto.getScore2())
            .build();
}
```

Add this import:
```java
import abe.fvjc.tournament.bracket.domain.BracketMatchResultRequest;
```

- [ ] **Step 11: Add endpoint to BracketController**

Add this method to `BracketController.java` (after `getAll`):

```java
@PutMapping("/matches/{matchId}/result")
public BracketMatchDto enterResult(
        @PathVariable final UUID tournamentId,
        @PathVariable final UUID matchId,
        @RequestBody @Valid final BracketMatchResultRequestDto request) {
    return toBracketMatchDto(bracketService.enterResult(matchId, toBracketMatchResultRequest(request)));
}
```

Update the static import line at the top of `BracketController.java` to include both methods:
```java
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketGenerateRequest;
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketMatchDto;
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketMatchResultRequest;
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketRoundDto;
```

- [ ] **Step 12: Build to verify no compilation errors**

```bash
./mvnw compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 13: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/domain/bracket/ backend/src/main/java/abe/fvjc/tournament/api/bracket/ backend/src/test/java/abe/fvjc/tournament/bracket/domain/
git commit -m "UC-08: enter knockout match result with winner/loser advancement

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 4: Frontend — API layer and state

**Files:**
- Modify: `frontend/src/app/api/bracket/bracket.api.dto.ts`
- Modify: `frontend/src/app/api/bracket/bracket.api.service.ts`
- Modify: `frontend/src/app/api/bracket/bracket.api.mapper.ts`
- Modify: `frontend/src/app/domain/bracket/bracket.actions.ts`
- Modify: `frontend/src/app/domain/bracket/bracket.state.ts`

**Interfaces:**
- Produces: `EnterBracketMatchResult` action, `BracketState` handles it by calling the API then dispatching `LoadBracket`.

- [ ] **Step 1: Add request DTO to bracket.api.dto.ts**

Add at the end of `frontend/src/app/api/bracket/bracket.api.dto.ts`:
```typescript
export interface BracketMatchResultRequestDto {
  score1: number;
  score2: number;
}
```

- [ ] **Step 2: Add API service method**

Add to `bracket.api.service.ts` (after `generateBracket$`):
```typescript
submitBracketMatchResult$(tournamentId: string, matchId: string, request: BracketMatchResultRequestDto): Observable<BracketMatchDto> {
  return this.http.put<BracketMatchDto>(`/api/tournaments/${tournamentId}/bracket/matches/${matchId}/result`, request);
}
```

Update the import at the top of `bracket.api.service.ts` to include `BracketMatchResultRequestDto`:
```typescript
import { BracketGenerateRequestDto, BracketMatchDto, BracketMatchResultRequestDto, BracketRoundDto } from '@app/api/bracket/bracket.api.dto';
```

- [ ] **Step 3: Add mapper method to bracket.api.mapper.ts**

Add to `BracketApiMapper` (after `toRoundDomain`):
```typescript
static toSubmitResultRequest(score1: number, score2: number): BracketMatchResultRequestDto {
  return { score1, score2 };
}
```

Update the import at the top of `bracket.api.mapper.ts`:
```typescript
import { BracketMatchDto, BracketMatchResultRequestDto, BracketRoundDto } from '@app/api/bracket/bracket.api.dto';
```

- [ ] **Step 4: Add EnterBracketMatchResult action**

Add to `frontend/src/app/domain/bracket/bracket.actions.ts`:
```typescript
export class EnterBracketMatchResult {
  static readonly type = '[Bracket] Enter Match Result';
  constructor(
    public readonly tournamentId: string,
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}
```

- [ ] **Step 5: Handle EnterBracketMatchResult in BracketState**

Replace the full content of `frontend/src/app/domain/bracket/bracket.state.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { State, Action, StateContext, Selector } from '@ngxs/store';
import { switchMap, tap } from 'rxjs';
import { BracketApiService } from '@app/api/bracket/bracket.api.service';
import { BracketApiMapper } from '@app/api/bracket/bracket.api.mapper';
import { BracketRound } from './bracket.model';
import { EnterBracketMatchResult, GenerateBracket, LoadBracket } from './bracket.actions';

export interface IBracketState {
  rounds: BracketRound[];
}

@State<IBracketState>({
  name: 'bracket',
  defaults: { rounds: [] },
})
@Injectable()
export class BracketState {
  private readonly bracketApiService = inject(BracketApiService);

  @Selector()
  static getRounds(state: IBracketState): BracketRound[] {
    return state.rounds;
  }

  @Selector()
  static hasBracket(state: IBracketState): boolean {
    return state.rounds.length > 0;
  }

  @Action(LoadBracket)
  loadBracket(ctx: StateContext<IBracketState>, { tournamentId }: LoadBracket) {
    return this.bracketApiService.loadBracket$(tournamentId).pipe(
      tap((dtos) => {
        ctx.patchState({ rounds: dtos.map(BracketApiMapper.toRoundDomain) });
      }),
    );
  }

  @Action(GenerateBracket)
  generateBracket(ctx: StateContext<IBracketState>, { tournamentId, request }: GenerateBracket) {
    return this.bracketApiService.generateBracket$(tournamentId, request).pipe(
      tap((dtos) => {
        ctx.patchState({ rounds: dtos.map(BracketApiMapper.toRoundDomain) });
      }),
    );
  }

  @Action(EnterBracketMatchResult)
  enterMatchResult(ctx: StateContext<IBracketState>, { tournamentId, matchId, score1, score2 }: EnterBracketMatchResult) {
    const request = BracketApiMapper.toSubmitResultRequest(score1, score2);
    return this.bracketApiService.submitBracketMatchResult$(tournamentId, matchId, request).pipe(
      switchMap(() => ctx.dispatch(new LoadBracket(tournamentId))),
    );
  }
}
```

- [ ] **Step 6: Build to verify TypeScript compiles**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament/frontend
node node_modules/@angular/cli/bin/ng.js build --configuration development 2>&1 | tail -20
```

Expected: `Build at:` line and `Complete.` with no errors.

- [ ] **Step 7: Commit**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament
git add frontend/src/app/api/bracket/ frontend/src/app/domain/bracket/
git commit -m "UC-08: frontend API service, mapper, action, and state for result entry

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Frontend — UI

**Files:**
- Modify: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.ts`
- Modify: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.html`
- Modify: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.scss`

**Interfaces:**
- Consumes: `EnterBracketMatchResult` (Task 4), `BracketState.getRounds` (existing), `BracketMatch.team1/team2/result` (existing).

- [ ] **Step 1: Update the page component**

Replace the full content of `tournament-bracket.page.ts`:
```typescript
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { ScheduleState } from '@app/domain/schedule/schedule.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadBracket } from '@app/domain/bracket/bracket.actions';
import { EnterBracketMatchResult } from '@app/domain/bracket/bracket.actions';
import { LoadSchedule } from '@app/domain/schedule/schedule.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [
    AsyncPipe,
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TournamentNavComponent,
  ],
})
export class TournamentBracketPage implements OnInit {
  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly rounds$: Observable<BracketRound[]> = this.store.select(BracketState.getRounds);
  readonly hasBracket$: Observable<boolean> = this.store.select(BracketState.hasBracket);
  readonly hasAllResults$: Observable<boolean> = this.store.select(ScheduleState.hasAllResults);

  protected tournamentId!: string;
  selectedMatch: BracketMatch | null = null;
  score1: number | null = null;
  score2: number | null = null;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadBracket(this.tournamentId),
      new LoadSchedule(this.tournamentId),
    ]);
  }

  openGenerateModal(): void {
    this.dialog.open(BracketGenerateModal, {
      data: { tournamentId: this.tournamentId },
      width: '480px',
    });
  }

  selectMatch(match: BracketMatch): void {
    if (!match.team1 || !match.team2 || match.result) return;
    this.selectedMatch = match;
    this.score1 = null;
    this.score2 = null;
  }

  submitResult(): void {
    if (!this.selectedMatch || this.score1 === null || this.score2 === null) return;
    this.store.dispatch(
      new EnterBracketMatchResult(this.tournamentId, this.selectedMatch.id, this.score1, this.score2),
    ).subscribe(() => {
      this.selectedMatch = null;
      this.score1 = null;
      this.score2 = null;
    });
  }
}
```

- [ ] **Step 2: Update the page template**

Replace the full content of `tournament-bracket.page.html`:
```html
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <h1>{{ tournament.name }}</h1>
  </div>
  <app-tournament-nav [tournamentId]="tournamentId" />

  <div class="bracket-layout">
    @if (hasBracket$ | async) {
      <div class="bracket-panel">
        @for (round of rounds$ | async; track round.id) {
          <div class="round-section">
            <div class="round-header">
              <span class="round-name">{{ round.name }}</span>
              <span class="round-time">{{ round.startTime | date:'HH:mm' }}</span>
            </div>
            <table class="bracket-table">
              <thead>
                <tr>
                  <th>Terrain</th>
                  <th>Équipe 1</th>
                  <th></th>
                  <th>Équipe 2</th>
                  <th>Score</th>
                </tr>
              </thead>
              <tbody>
                @for (match of round.matches; track match.id) {
                  <tr
                    [class.selected]="selectedMatch?.id === match.id"
                    [class.playable]="match.team1 && match.team2 && !match.result"
                    (click)="selectMatch(match)">
                    <td>T{{ match.field }}</td>
                    <td>{{ match.team1?.name ?? 'À déterminer' }}</td>
                    <td class="vs">vs</td>
                    <td>{{ match.team2?.name ?? 'À déterminer' }}</td>
                    <td>
                      @if (match.result) {
                        {{ match.result.score1 }} - {{ match.result.score2 }}
                      } @else {
                        —
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      <div class="detail-panel">
        @if (selectedMatch) {
          <div class="result-form">
            <h2>{{ selectedMatch.team1?.name }} vs {{ selectedMatch.team2?.name }}</h2>
            <div class="score-inputs">
              <mat-form-field appearance="outline">
                <mat-label>{{ selectedMatch.team1?.name }}</mat-label>
                <input matInput type="number" min="0" max="500" [(ngModel)]="score1">
              </mat-form-field>
              <span class="score-separator">—</span>
              <mat-form-field appearance="outline">
                <mat-label>{{ selectedMatch.team2?.name }}</mat-label>
                <input matInput type="number" min="0" max="500" [(ngModel)]="score2">
              </mat-form-field>
            </div>
            <button mat-raised-button color="primary" (click)="submitResult()">
              Enregistrer
            </button>
          </div>
        } @else {
          <div class="empty-detail">
            <mat-icon>touch_app</mat-icon>
            <p>Sélectionnez un match pour saisir le résultat</p>
          </div>
        }
      </div>
    } @else {
      <div class="empty-state">
        <p>Le bracket n'a pas encore été généré.</p>
        @if (!(hasAllResults$ | async)) {
          <div class="warning-banner">
            <mat-icon>warning</mat-icon>
            <span>Tous les matchs de la phase de groupes doivent avoir un résultat avant de générer le bracket.</span>
          </div>
        }
        <button mat-raised-button color="primary" (click)="openGenerateModal()">
          Générer le bracket
        </button>
      </div>
    }
  </div>
}
```

- [ ] **Step 3: Update the page styles**

Replace the full content of `tournament-bracket.page.scss`:
```scss
.page-header {
  padding: 16px 24px 0;
  h1 { margin: 0; }
}

.bracket-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
  padding: 16px 24px;
  height: calc(100vh - 120px);
  overflow: hidden;
}

.bracket-panel {
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.round-section {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  overflow: hidden;
}

.round-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f5f5;
  font-weight: 500;
}

.round-name { font-size: 1rem; }
.round-time { color: #666; font-size: 0.875rem; }

.bracket-table {
  width: 100%;
  border-collapse: collapse;

  th, td {
    padding: 10px 16px;
    text-align: left;
    border-bottom: 1px solid #eee;
  }

  th { font-size: 0.75rem; color: #888; text-transform: uppercase; }
  .vs { color: #aaa; text-align: center; }

  tr.playable {
    cursor: pointer;
    &:hover { background: var(--mat-sys-surface-variant); }
  }

  tr.selected {
    background: var(--mat-sys-primary-container);
    border-left: 3px solid var(--mat-sys-primary);
  }
}

.detail-panel {
  overflow-y: auto;
}

.result-form {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  padding: 24px;

  h2 {
    margin: 0 0 20px;
    font-size: 1rem;
    font-weight: 500;
  }
}

.score-inputs {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  mat-form-field { flex: 1; }
}

.score-separator {
  font-size: 1.25rem;
  color: #aaa;
  flex-shrink: 0;
}

.empty-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #aaa;
  gap: 8px;

  mat-icon { font-size: 36px; height: 36px; width: 36px; }
  p { margin: 0; font-size: 0.875rem; }
}

.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 48px 24px;
  p { color: #666; margin-bottom: 16px; }
}

.warning-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
  padding: 10px 16px;
  border-radius: 4px;
  background: var(--mat-sys-warning-container, #fff3e0);
  color: var(--mat-sys-on-warning-container, #e65100);
  font-size: 0.875rem;

  mat-icon { font-size: 20px; height: 20px; width: 20px; }
}
```

- [ ] **Step 4: Build to verify no errors**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament/frontend
node node_modules/@angular/cli/bin/ng.js build --configuration development 2>&1 | tail -20
```

Expected: `Build at:` line and `Complete.` with no TypeScript or template errors.

- [ ] **Step 5: Commit**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament
git add frontend/src/app/display/tournament/pages/tournament-bracket/
git commit -m "UC-08: bracket page UI with match selection and result entry

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
```
