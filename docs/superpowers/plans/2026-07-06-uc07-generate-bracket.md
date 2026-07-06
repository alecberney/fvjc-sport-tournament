# UC-07 — Generate Bracket — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a single-elimination knockout bracket from group stage rankings with seeded random draw, including a round schedule.

**Architecture:** New `bracket` feature package on the backend (`bracket/domain`, `bracket/api`, `bracket/persistence`), new `bracket` domain on the frontend. `BracketService.generate()` loads group rankings via `RankingService`, builds hats by finishing rank, draws randomly, creates `BracketRound` + `BracketMatch` records (null team slots for future rounds), and assigns fields/start times.

**Tech Stack:** Spring Boot 4.1 / Lombok / JPA / H2 (backend); Angular 21 standalone / NGXS / Angular Material (frontend)

## Global Constraints

- Base Java package: `abe.fvjc.tournament.bracket` (feature-first, sub-packages: `.domain`, `.api`, `.persistence`)
- Directories: `backend/src/main/java/abe/fvjc/tournament/bracket/domain|api|persistence/`
- Exception imports: `abe.fvjc.tournament.shared.exception.ValidationException` (not domain.common.problem)
- `startTime` in requests/DTOs is `String` in `"HH:mm"` format — same pattern as `ScheduleGenerateRequest`
- All domain classes: `@Value @Builder @With` (Lombok); all DTOs: `@Value @Builder @Jacksonized`
- All method parameters: `final`; all local variables: `final var`
- No blank line between class opening brace `{` and first field
- Static imports for all `public static` methods and enum values
- Architecture rules enforced by `ArchitectureTest`: `@Service` in `..domain..`, `@RestController` in `..api..`, `@Repository` in `..persistence..`, `@Entity` classes package-private, `XxxRepository` package-private
- `@Transactional` per-method on store implementations only — never on services

---

### Task 1: BE — Domain layer (models, stores, validator, service + tests)

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketRoundId.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketMatchId.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketMatch.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketRound.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketGenerateRequest.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketRoundStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketMatchStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketValidator.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/domain/BracketService.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/IdGenerator.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketFakes.java`
- Create: `backend/src/test/java/abe/fvjc/tournament/bracket/domain/BracketServiceTest.java`

**Interfaces:**
- Consumes:
  - `abe.fvjc.tournament.schedule.domain.RankingService.computeAllGroupRankings(UUID, List<String>): List<GroupRanking>`
  - `abe.fvjc.tournament.schedule.domain.GroupRanking.getEntries(): List<GroupRankingEntry>`
  - `abe.fvjc.tournament.schedule.domain.GroupRankingEntry.getRank(): int`, `.getTeam(): TeamRef`
  - `abe.fvjc.tournament.schedule.domain.TeamRef` — `TeamId getId()`, `String getName()`
  - `abe.fvjc.tournament.schedule.domain.MatchResult` — `int getScore1()`, `int getScore2()`
  - `abe.fvjc.tournament.group.domain.GroupStore.findAllByTournamentId(UUID): List<Group>`
  - `abe.fvjc.tournament.tournament.domain.TournamentStore.findById(UUID): Optional<Tournament>` — tournament has `int getNumberOfFields()`, `LocalDate getDate()`
  - `abe.fvjc.tournament.tournament.domain.TournamentId.of(UUID): TournamentId`
  - `abe.fvjc.tournament.shared.exception.ValidationException`, `NotFoundException`
- Produces:
  - `BracketRoundId`: `BracketRoundId.of(UUID)`, `.empty()`, `.isEmpty()`, `.value(): UUID`
  - `BracketMatchId`: same pattern
  - `BracketMatch`: `getId()`, `getRoundId()`, `getField()`, `getTeam1(): TeamRef` (nullable), `getTeam2(): TeamRef` (nullable), `getResult(): MatchResult` (nullable), `getNextMatchId(): BracketMatchId` (nullable), `getNextMatchTeamSlot(): int`
  - `BracketRound`: `getId()`, `getTournamentId()`, `getNumber()`, `getName()`, `getStartTime()`, `getMatches(): List<BracketMatch>`
  - `BracketService.generate(UUID tournamentId, BracketGenerateRequest request): List<BracketRound>` — rounds with matches embedded
  - `BracketService.findAll(UUID tournamentId): List<BracketRound>` — rounds with matches embedded

- [ ] **Step 1: Create typed ID records**

`BracketRoundId.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import java.util.UUID;

public record BracketRoundId(UUID value) {
    public static BracketRoundId of(final UUID value) {
        return new BracketRoundId(value);
    }
    public static BracketRoundId empty() {
        return new BracketRoundId(null);
    }
    public boolean isEmpty() {
        return value == null;
    }
}
```

`BracketMatchId.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import java.util.UUID;

public record BracketMatchId(UUID value) {
    public static BracketMatchId of(final UUID value) {
        return new BracketMatchId(value);
    }
    public static BracketMatchId empty() {
        return new BracketMatchId(null);
    }
    public boolean isEmpty() {
        return value == null;
    }
}
```

- [ ] **Step 2: Create domain models**

`BracketMatch.java`:
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
}
```

`BracketRound.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@With
public class BracketRound {
    BracketRoundId id;
    TournamentId tournamentId;
    int number;
    String name;
    LocalDateTime startTime;
    List<BracketMatch> matches;
}
```

`BracketGenerateRequest.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BracketGenerateRequest {
    int qualifiersPerGroup;
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
```

- [ ] **Step 3: Create store interfaces**

`BracketRoundStore.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BracketRoundStore {
    BracketRound save(BracketRound round);
    Optional<BracketRound> findById(UUID id);
    List<BracketRound> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
```

`BracketMatchStore.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BracketMatchStore {
    BracketMatch save(BracketMatch match);
    Optional<BracketMatch> findById(UUID id);
    List<BracketMatch> findAllByRoundId(UUID roundId);
    void deleteAllByRoundId(UUID roundId);
}
```

- [ ] **Step 4: Write failing tests**

`IdGenerator.java` (test source, package-private):
```java
package abe.fvjc.tournament.bracket.domain;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static BracketRoundId roundId() {
        return BracketRoundId.of(UUID.randomUUID());
    }
    static BracketMatchId matchId() {
        return BracketMatchId.of(UUID.randomUUID());
    }
}
```

`BracketFakes.java` (test source):
```java
package abe.fvjc.tournament.bracket.domain;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BracketFakes {

    public static BracketGenerateRequest buildGenerateRequest() {
        return BracketGenerateRequest.builder()
                .qualifiersPerGroup(2)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithOneQualifier() {
        return BracketGenerateRequest.builder()
                .qualifiersPerGroup(1)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }
}
```

`BracketServiceTest.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.GroupRankingEntry;
import abe.fvjc.tournament.schedule.domain.RankingService;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private BracketMatchStore bracketMatchStore;

    @Mock
    private BracketRoundStore bracketRoundStore;

    @Mock
    private GroupStore groupStore;

    @Mock
    private RankingService rankingService;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void generateWhenValidRequestShouldCreateRoundsAndMatches() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequest();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");
        final var groups = List.of(gA, gB, gC, gD);

        final var rankings = List.of(
                buildRanking(gA.getId(), "A", 2),
                buildRanking(gB.getId(), "B", 2),
                buildRanking(gC.getId(), "C", 2),
                buildRanking(gD.getId(), "D", 2));

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(groups);
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(rankings);
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentId, request);

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(rankingService).computeAllGroupRankings(tournamentId, List.of());
        verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(7)).save(any(BracketMatch.class));

        assertEquals(3, roundsGenerated.size());
        assertEquals("Quarts de finale", roundsGenerated.get(0).getName());
        assertEquals("Demi-finales", roundsGenerated.get(1).getName());
        assertEquals("Finale", roundsGenerated.get(2).getName());
        assertEquals(4, roundsGenerated.get(0).getMatches().size());
        assertEquals(2, roundsGenerated.get(1).getMatches().size());
        assertEquals(1, roundsGenerated.get(2).getMatches().size());
    }

    @Test
    void generateWhenTotalTeamsNotPowerOfTwoShouldThrowValidationException() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequest();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(gA, gB, gC));
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 2),
                        buildRanking(gB.getId(), "B", 2),
                        buildRanking(gC.getId(), "C", 2)));

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.generate(tournamentId, request));

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);

        assertEquals(1, exception.getErrors().size());
        assertEquals("qualifiersPerGroup", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenQualifiersPerGroupIsOneShouldCreateTwoRounds() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestWithOneQualifier();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(gA, gB, gC, gD));
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 1),
                        buildRanking(gB.getId(), "B", 1),
                        buildRanking(gC.getId(), "C", 1),
                        buildRanking(gD.getId(), "D", 1)));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentId, request);

        verify(bracketRoundStore, times(2)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));

        assertEquals(2, roundsGenerated.size());
        assertEquals("Demi-finales", roundsGenerated.get(0).getName());
        assertEquals("Finale", roundsGenerated.get(1).getName());
        assertEquals(2, roundsGenerated.get(0).getMatches().size());
        assertEquals(1, roundsGenerated.get(1).getMatches().size());
    }

    private static GroupRanking buildRanking(final GroupId groupId, final String groupName, final int numEntries) {
        final var entries = new java.util.ArrayList<GroupRankingEntry>();
        for (int i = 1; i <= numEntries; i++) {
            entries.add(GroupRankingEntry.builder()
                    .rank(i)
                    .team(TeamRef.builder()
                            .id(TeamId.of(UUID.randomUUID()))
                            .name(groupName + i)
                            .build())
                    .build());
        }
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament
mvn -f backend/pom.xml test -pl . -Dtest=BracketServiceTest -q 2>&1 | tail -20
```
Expected: FAIL — `BracketService`, `BracketValidator` do not exist yet.

- [ ] **Step 6: Create BracketValidator**

`BracketValidator.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

@UtilityClass
public class BracketValidator {

    public static void validateBracketGenerateRequest(final BracketGenerateRequest request, final int numGroups) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getQualifiersPerGroup() < 1) {
            errors.add(new ValidationException.FieldError("qualifiersPerGroup",
                    "Le nombre de qualifiés par groupe doit être d'au moins 1"));
        }

        if (request.getStartTime() == null) {
            errors.add(new ValidationException.FieldError("startTime", "L'heure de début est obligatoire"));
        } else {
            try {
                LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                errors.add(new ValidationException.FieldError("startTime", "Format d'heure invalide (HH:mm attendu)"));
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

        final var totalTeams = numGroups * request.getQualifiersPerGroup();
        if (totalTeams > 1 && (totalTeams & (totalTeams - 1)) != 0) {
            errors.add(new ValidationException.FieldError("qualifiersPerGroup",
                    "Le nombre total d'équipes qualifiées doit être une puissance de 2"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
```

- [ ] **Step 7: Create BracketService**

`BracketService.java`:
```java
package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.RankingService;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.bracket.domain.BracketValidator.validateBracketGenerateRequest;

@Service
@RequiredArgsConstructor
public class BracketService {
    private final BracketMatchStore bracketMatchStore;
    private final BracketRoundStore bracketRoundStore;
    private final GroupStore groupStore;
    private final RankingService rankingService;
    private final TournamentStore tournamentStore;

    public List<BracketRound> generate(final UUID tournamentId, final BracketGenerateRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupRankings = rankingService.computeAllGroupRankings(tournamentId, List.of());
        validateBracketGenerateRequest(request, groups.size());

        final var hats = buildHats(groupRankings, request.getQualifiersPerGroup());
        final var round1Pairs = buildMatchPairs(hats);
        final var totalTeams = groups.size() * request.getQualifiersPerGroup();
        final var totalRounds = (int) (Math.log(totalTeams) / Math.log(2));
        final var startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        final var firstRoundStart = LocalDateTime.of(tournament.getDate(), startTime);

        final var matchIdsByRound = preassignMatchIds(round1Pairs.size(), totalRounds);

        final var savedRounds = new ArrayList<BracketRound>();
        final var matchesByRound = new ArrayList<List<BracketMatch>>();

        for (int r = 0; r < totalRounds; r++) {
            final var roundStart = firstRoundStart.plusMinutes(
                    (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var teamsInRound = totalTeams / (int) Math.pow(2, r);
            final var round = bracketRoundStore.save(BracketRound.builder()
                    .id(BracketRoundId.of(UUID.randomUUID()))
                    .tournamentId(TournamentId.of(tournamentId))
                    .number(r + 1)
                    .name(roundName(teamsInRound))
                    .startTime(roundStart)
                    .matches(List.of())
                    .build());

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

                final var match = bracketMatchStore.save(BracketMatch.builder()
                        .id(matchIds.get(m))
                        .roundId(round.getId())
                        .field(field)
                        .team1(team1)
                        .team2(team2)
                        .result(null)
                        .nextMatchId(nextMatchId)
                        .nextMatchTeamSlot(slot)
                        .build());
                roundMatches.add(match);
            }
            savedRounds.add(round);
            matchesByRound.add(roundMatches);
        }

        return savedRounds.stream()
                .map(r -> r.withMatches(matchesByRound.get(r.getNumber() - 1)))
                .toList();
    }

    public List<BracketRound> findAll(final UUID tournamentId) {
        return bracketRoundStore.findAllByTournamentId(tournamentId).stream()
                .map(r -> r.withMatches(bracketMatchStore.findAllByRoundId(r.getId().value())))
                .toList();
    }

    private static List<List<BracketMatchId>> preassignMatchIds(final int round1Count, final int totalRounds) {
        final var result = new ArrayList<List<BracketMatchId>>();
        for (int r = 0; r < totalRounds; r++) {
            final var count = round1Count / (int) Math.pow(2, r);
            final var ids = new ArrayList<BracketMatchId>();
            for (int i = 0; i < count; i++) {
                ids.add(BracketMatchId.of(UUID.randomUUID()));
            }
            result.add(ids);
        }
        return result;
    }

    private static List<List<TeamRef>> buildHats(final List<GroupRanking> rankings, final int qualifiersPerGroup) {
        final var hats = new ArrayList<List<TeamRef>>();
        for (int pos = 0; pos < qualifiersPerGroup; pos++) {
            hats.add(new ArrayList<>());
        }
        for (final var ranking : rankings) {
            for (int pos = 0; pos < qualifiersPerGroup; pos++) {
                hats.get(pos).add(ranking.getEntries().get(pos).getTeam());
            }
        }
        return hats;
    }

    private static List<Pair> buildMatchPairs(final List<List<TeamRef>> hats) {
        final var k = hats.size();
        final var pairs = new ArrayList<Pair>();
        if (k == 1) {
            final var shuffled = new ArrayList<>(hats.get(0));
            Collections.shuffle(shuffled);
            for (int i = 0; i < shuffled.size(); i += 2) {
                pairs.add(new Pair(shuffled.get(i), shuffled.get(i + 1)));
            }
        } else {
            for (int i = 0; i < k / 2; i++) {
                final var top = new ArrayList<>(hats.get(i));
                final var bottom = new ArrayList<>(hats.get(k - 1 - i));
                Collections.shuffle(top);
                Collections.shuffle(bottom);
                for (int j = 0; j < top.size(); j++) {
                    pairs.add(new Pair(top.get(j), bottom.get(j)));
                }
            }
        }
        return pairs;
    }

    private static String roundName(final int teamsCount) {
        return switch (teamsCount) {
            case 16 -> "Huitièmes de finale";
            case 8 -> "Quarts de finale";
            case 4 -> "Demi-finales";
            case 2 -> "Finale";
            default -> "Tour de " + teamsCount;
        };
    }

    private record Pair(TeamRef first, TeamRef second) {}
}
```

- [ ] **Step 8: Run tests and verify they pass**

```bash
mvn -f backend/pom.xml test -Dtest=BracketServiceTest -q 2>&1 | tail -10
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 9: Run full test suite to check for regressions**

```bash
mvn -f backend/pom.xml test -q 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: all existing tests still pass.

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/bracket/ \
        backend/src/test/java/abe/fvjc/tournament/bracket/
git commit -m "UC-07: BE - Add bracket domain layer, service, and tests"
```

---

### Task 2: BE — Persistence layer

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/BracketRoundEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/BracketMatchEntity.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/BracketRoundRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/BracketMatchRepository.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/BracketDbMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/JpaBracketRoundStore.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/persistence/JpaBracketMatchStore.java`

**Interfaces:**
- Consumes: all types from Task 1 (BracketRound, BracketMatch, BracketRoundId, BracketMatchId, BracketRoundStore, BracketMatchStore)
- Produces: implementations of `BracketRoundStore` and `BracketMatchStore`

- [ ] **Step 1: Create JPA entities (package-private)**

`BracketRoundEntity.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bracket_rounds")
@Getter
@Setter
@NoArgsConstructor
class BracketRoundEntity {
    @Id
    private UUID id;
    private UUID tournamentId;
    private int number;
    private String name;
    private LocalDateTime startTime;
}
```

`BracketMatchEntity.java`:
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
}
```

- [ ] **Step 2: Create Spring Data repositories (package-private)**

`BracketRoundRepository.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BracketRoundRepository extends JpaRepository<BracketRoundEntity, UUID> {
    List<BracketRoundEntity> findAllByTournamentIdOrderByNumberAsc(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
```

`BracketMatchRepository.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BracketMatchRepository extends JpaRepository<BracketMatchEntity, UUID> {
    List<BracketMatchEntity> findAllByRoundId(UUID roundId);
    void deleteAllByRoundId(UUID roundId);
}
```

- [ ] **Step 3: Create DB mapper**

`BracketDbMapper.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketMatchId;
import abe.fvjc.tournament.bracket.domain.BracketRound;
import abe.fvjc.tournament.bracket.domain.BracketRoundId;
import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class BracketDbMapper {

    static BracketRound toBracketRound(final BracketRoundEntity entity) {
        return BracketRound.builder()
                .id(BracketRoundId.of(entity.getId()))
                .tournamentId(TournamentId.of(entity.getTournamentId()))
                .number(entity.getNumber())
                .name(entity.getName())
                .startTime(entity.getStartTime())
                .matches(List.of())
                .build();
    }

    static BracketRoundEntity toBracketRoundEntity(final BracketRound round) {
        final var entity = new BracketRoundEntity();
        entity.setId(round.getId().value());
        entity.setTournamentId(round.getTournamentId().value());
        entity.setNumber(round.getNumber());
        entity.setName(round.getName());
        entity.setStartTime(round.getStartTime());
        return entity;
    }

    static BracketMatch toBracketMatch(final BracketMatchEntity entity) {
        final var team1 = entity.getTeam1Id() != null
                ? TeamRef.builder().id(TeamId.of(entity.getTeam1Id())).name(entity.getTeam1Name()).build()
                : null;
        final var team2 = entity.getTeam2Id() != null
                ? TeamRef.builder().id(TeamId.of(entity.getTeam2Id())).name(entity.getTeam2Name()).build()
                : null;
        final var result = entity.getScore1() != null
                ? MatchResult.builder().score1(entity.getScore1()).score2(entity.getScore2()).build()
                : null;
        final var nextMatchId = entity.getNextMatchId() != null
                ? BracketMatchId.of(entity.getNextMatchId())
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
        return entity;
    }
}
```

- [ ] **Step 4: Create store implementations**

`JpaBracketRoundStore.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketRound;
import abe.fvjc.tournament.bracket.domain.BracketRoundStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaBracketRoundStore implements BracketRoundStore {
    private final BracketRoundRepository bracketRoundRepository;

    @Override
    @Transactional
    public BracketRound save(final BracketRound round) {
        final var entity = BracketDbMapper.toBracketRoundEntity(round);
        final var savedEntity = bracketRoundRepository.save(entity);
        return BracketDbMapper.toBracketRound(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BracketRound> findById(final UUID id) {
        return bracketRoundRepository.findById(id)
                .map(BracketDbMapper::toBracketRound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketRound> findAllByTournamentId(final UUID tournamentId) {
        return bracketRoundRepository.findAllByTournamentIdOrderByNumberAsc(tournamentId).stream()
                .map(BracketDbMapper::toBracketRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        bracketRoundRepository.deleteAllByTournamentId(tournamentId);
    }
}
```

`JpaBracketMatchStore.java`:
```java
package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketMatchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaBracketMatchStore implements BracketMatchStore {
    private final BracketMatchRepository bracketMatchRepository;

    @Override
    @Transactional
    public BracketMatch save(final BracketMatch match) {
        final var entity = BracketDbMapper.toBracketMatchEntity(match);
        final var savedEntity = bracketMatchRepository.save(entity);
        return BracketDbMapper.toBracketMatch(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BracketMatch> findById(final UUID id) {
        return bracketMatchRepository.findById(id)
                .map(BracketDbMapper::toBracketMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketMatch> findAllByRoundId(final UUID roundId) {
        return bracketMatchRepository.findAllByRoundId(roundId).stream()
                .map(BracketDbMapper::toBracketMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundId(final UUID roundId) {
        bracketMatchRepository.deleteAllByRoundId(roundId);
    }
}
```

- [ ] **Step 5: Run full test suite to verify persistence wires correctly**

```bash
mvn -f backend/pom.xml test -q 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: all tests pass including `TournamentApplicationTests` (Spring context loads).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/bracket/persistence/
git commit -m "UC-07: BE - Add bracket persistence layer"
```

---

### Task 3: BE — API layer (DTOs, mapper, controller)

**Files:**
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketGenerateRequestDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketTeamDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketMatchResultDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketMatchDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketRoundDto.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketApiMapper.java`
- Create: `backend/src/main/java/abe/fvjc/tournament/bracket/api/BracketController.java`

**Interfaces:**
- Consumes: `BracketService.generate()`, `BracketService.findAll()` from Task 1; all domain types
- Produces:
  - `GET /api/tournaments/{tournamentId}/bracket` → `200 List<BracketRoundDto>`
  - `POST /api/tournaments/{tournamentId}/bracket/generate` → `201 List<BracketRoundDto>`

- [ ] **Step 1: Create DTOs**

`BracketGenerateRequestDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketGenerateRequestDto {
    @Min(1) int qualifiersPerGroup;
    @NotNull String startTime;
    @NotNull @Min(1) Integer matchDurationMinutes;
    @NotNull @Min(0) Integer breakDurationMinutes;
}
```

`BracketTeamDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketTeamDto {
    UUID id;
    String name;
}
```

`BracketMatchResultDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketMatchResultDto {
    int score1;
    int score2;
}
```

`BracketMatchDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketMatchDto {
    UUID id;
    int field;
    BracketTeamDto team1;
    BracketTeamDto team2;
    BracketMatchResultDto result;
}
```

`BracketRoundDto.java`:
```java
package abe.fvjc.tournament.bracket.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketRoundDto {
    UUID id;
    int number;
    String name;
    String startTime;
    List<BracketMatchDto> matches;
}
```

- [ ] **Step 2: Create API mapper**

`BracketApiMapper.java`:
```java
package abe.fvjc.tournament.bracket.api;

import abe.fvjc.tournament.bracket.domain.BracketGenerateRequest;
import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketRound;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class BracketApiMapper {

    static BracketRoundDto toBracketRoundDto(final BracketRound round) {
        return BracketRoundDto.builder()
                .id(round.getId().value())
                .number(round.getNumber())
                .name(round.getName())
                .startTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(round.getMatches().stream()
                        .map(BracketApiMapper::toBracketMatchDto)
                        .toList())
                .build();
    }

    static BracketMatchDto toBracketMatchDto(final BracketMatch match) {
        final var team1 = match.getTeam1() != null
                ? BracketTeamDto.builder()
                        .id(match.getTeam1().getId().value())
                        .name(match.getTeam1().getName())
                        .build()
                : null;
        final var team2 = match.getTeam2() != null
                ? BracketTeamDto.builder()
                        .id(match.getTeam2().getId().value())
                        .name(match.getTeam2().getName())
                        .build()
                : null;
        final var result = match.getResult() != null
                ? BracketMatchResultDto.builder()
                        .score1(match.getResult().getScore1())
                        .score2(match.getResult().getScore2())
                        .build()
                : null;
        return BracketMatchDto.builder()
                .id(match.getId().value())
                .field(match.getField())
                .team1(team1)
                .team2(team2)
                .result(result)
                .build();
    }

    static BracketGenerateRequest toBracketGenerateRequest(final BracketGenerateRequestDto dto) {
        return BracketGenerateRequest.builder()
                .qualifiersPerGroup(dto.getQualifiersPerGroup())
                .startTime(dto.getStartTime())
                .matchDurationMinutes(dto.getMatchDurationMinutes())
                .breakDurationMinutes(dto.getBreakDurationMinutes())
                .build();
    }
}
```

- [ ] **Step 3: Create controller**

`BracketController.java`:
```java
package abe.fvjc.tournament.bracket.api;

import abe.fvjc.tournament.bracket.domain.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketGenerateRequest;
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketRoundDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/bracket")
@RequiredArgsConstructor
public class BracketController {
    private final BracketService bracketService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BracketRoundDto> generate(
            @PathVariable final UUID tournamentId,
            @RequestBody @Valid final BracketGenerateRequestDto request) {
        return bracketService.generate(tournamentId, toBracketGenerateRequest(request)).stream()
                .map(round -> toBracketRoundDto(round))
                .toList();
    }

    @GetMapping
    public List<BracketRoundDto> getAll(@PathVariable final UUID tournamentId) {
        return bracketService.findAll(tournamentId).stream()
                .map(round -> toBracketRoundDto(round))
                .toList();
    }
}
```

- [ ] **Step 4: Run full test suite**

```bash
mvn -f backend/pom.xml test -q 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: all tests pass (including `ArchitectureTest`).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/abe/fvjc/tournament/bracket/api/
git commit -m "UC-07: BE - Add bracket API layer (DTOs, mapper, controller)"
```

---

### Task 4: FE — API + Domain layers

**Files:**
- Create: `frontend/src/app/api/bracket/bracket.api.dto.ts`
- Create: `frontend/src/app/api/bracket/bracket.api.service.ts`
- Create: `frontend/src/app/api/bracket/bracket.api.mapper.ts`
- Create: `frontend/src/app/domain/bracket/bracket.model.ts`
- Create: `frontend/src/app/domain/bracket/bracket.actions.ts`
- Create: `frontend/src/app/domain/bracket/bracket.state.ts`

**Interfaces:**
- Consumes:
  - `GET /api/tournaments/{id}/bracket` → `BracketRoundDto[]`
  - `POST /api/tournaments/{id}/bracket/generate` → `BracketRoundDto[]`
- Produces:
  - `BracketState.getRounds`: `BracketRound[]`
  - `BracketState.hasBracket`: `boolean`
  - Actions: `LoadBracket(tournamentId)`, `GenerateBracket(tournamentId, request)`

- [ ] **Step 1: Create API DTOs**

`bracket.api.dto.ts`:
```typescript
export interface BracketTeamDto {
  id: string;
  name: string;
}

export interface BracketMatchResultDto {
  score1: number;
  score2: number;
}

export interface BracketMatchDto {
  id: string;
  field: number;
  team1: BracketTeamDto | null;
  team2: BracketTeamDto | null;
  result: BracketMatchResultDto | null;
}

export interface BracketRoundDto {
  id: string;
  number: number;
  name: string;
  startTime: string;
  matches: BracketMatchDto[];
}

export interface BracketGenerateRequestDto {
  qualifiersPerGroup: number;
  startTime: string;
  matchDurationMinutes: number;
  breakDurationMinutes: number;
}
```

- [ ] **Step 2: Create API service**

`bracket.api.service.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BracketGenerateRequestDto, BracketRoundDto } from './bracket.api.dto';

@Injectable({ providedIn: 'root' })
export class BracketApiService {
  private readonly http = inject(HttpClient);

  loadBracket$(tournamentId: string): Observable<BracketRoundDto[]> {
    return this.http.get<BracketRoundDto[]>(`/api/tournaments/${tournamentId}/bracket`);
  }

  generateBracket$(tournamentId: string, request: BracketGenerateRequestDto): Observable<BracketRoundDto[]> {
    return this.http.post<BracketRoundDto[]>(`/api/tournaments/${tournamentId}/bracket/generate`, request);
  }
}
```

- [ ] **Step 3: Create domain models**

`bracket.model.ts`:
```typescript
export interface BracketTeam {
  id: string;
  name: string;
}

export interface BracketMatchResult {
  score1: number;
  score2: number;
}

export interface BracketMatch {
  id: string;
  field: number;
  team1: BracketTeam | null;
  team2: BracketTeam | null;
  result: BracketMatchResult | null;
}

export interface BracketRound {
  id: string;
  number: number;
  name: string;
  startTime: Date;
  matches: BracketMatch[];
}
```

- [ ] **Step 4: Create API mapper**

`bracket.api.mapper.ts`:
```typescript
import { BracketMatch, BracketMatchResult, BracketRound, BracketTeam } from '@app/domain/bracket/bracket.model';
import { BracketMatchDto, BracketRoundDto } from './bracket.api.dto';

export class BracketApiMapper {
  static toRoundDomain(dto: BracketRoundDto): BracketRound {
    return {
      id: dto.id,
      number: dto.number,
      name: dto.name,
      startTime: new Date(dto.startTime),
      matches: dto.matches.map(BracketApiMapper.toMatchDomain),
    };
  }

  private static toMatchDomain(dto: BracketMatchDto): BracketMatch {
    return {
      id: dto.id,
      field: dto.field,
      team1: dto.team1 ? ({ id: dto.team1.id, name: dto.team1.name } as BracketTeam) : null,
      team2: dto.team2 ? ({ id: dto.team2.id, name: dto.team2.name } as BracketTeam) : null,
      result: dto.result ? ({ score1: dto.result.score1, score2: dto.result.score2 } as BracketMatchResult) : null,
    };
  }
}
```

- [ ] **Step 5: Create NGXS actions**

`bracket.actions.ts`:
```typescript
import { BracketGenerateRequestDto } from '@app/api/bracket/bracket.api.dto';

export class LoadBracket {
  static readonly type = '[Bracket] Load Bracket';
  constructor(public readonly tournamentId: string) {}
}

export class GenerateBracket {
  static readonly type = '[Bracket] Generate Bracket';
  constructor(
    public readonly tournamentId: string,
    public readonly request: BracketGenerateRequestDto,
  ) {}
}
```

- [ ] **Step 6: Create NGXS state**

`bracket.state.ts`:
```typescript
import { Injectable, inject } from '@angular/core';
import { State, Action, StateContext, Selector } from '@ngxs/store';
import { tap } from 'rxjs';
import { BracketApiService } from '@app/api/bracket/bracket.api.service';
import { BracketApiMapper } from '@app/api/bracket/bracket.api.mapper';
import { BracketRound } from './bracket.model';
import { GenerateBracket, LoadBracket } from './bracket.actions';

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
}
```

- [ ] **Step 7: Verify TypeScript compilation**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament/frontend
npx tsc --noEmit 2>&1 | head -30
```
Expected: no output (zero errors).

- [ ] **Step 8: Commit**

```bash
git add frontend/src/app/api/bracket/ frontend/src/app/domain/bracket/
git commit -m "UC-07: FE - Add bracket API and domain layers"
```

---

### Task 5: FE — Display layer (modal, page, nav, routes)

**Files:**
- Create: `frontend/src/app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component.ts`
- Create: `frontend/src/app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.ts`
- Create: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.html`
- Create: `frontend/src/app/display/tournament/pages/tournament-bracket/tournament-bracket.page.scss`
- Modify: `frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html`
- Modify: `frontend/src/app/modules/tournament.routes.ts`

**Interfaces:**
- Consumes: `BracketState.getRounds`, `BracketState.hasBracket`, `GenerateBracket`, `LoadBracket` actions, `TournamentState.getSelected`, `LoadTournamentById`
- Produces: `/tournaments/:id/bracket` route showing bracket; "Arbre" nav tab enabled

- [ ] **Step 1: Create BracketGenerateModal component**

`bracket-generate-modal.component.ts`:
```typescript
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { GenerateBracket } from '@app/domain/bracket/bracket.actions';

@Component({
  selector: 'app-bracket-generate-modal',
  templateUrl: './bracket-generate-modal.component.html',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
})
export class BracketGenerateModal implements OnInit, OnDestroy {
  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<BracketGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      qualifiersPerGroup: [2, [Validators.required, Validators.min(1)]],
      startTime: ['14:00', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
      matchDurationMinutes: [20, [Validators.required, Validators.min(1)]],
      breakDurationMinutes: [5, [Validators.required, Validators.min(0)]],
    });

    this.actions$.pipe(
      ofActionSuccessful(GenerateBracket),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateBracket(this.data.tournamentId, {
      qualifiersPerGroup: Number(this.form.value.qualifiersPerGroup),
      startTime: String(this.form.value.startTime),
      matchDurationMinutes: Number(this.form.value.matchDurationMinutes),
      breakDurationMinutes: Number(this.form.value.breakDurationMinutes),
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

`bracket-generate-modal.component.html`:
```html
<h2 mat-dialog-title>Générer le bracket</h2>

<mat-dialog-content>
  <form [formGroup]="form">
    <mat-form-field appearance="outline">
      <mat-label>Qualifiés par groupe</mat-label>
      <input matInput type="number" min="1" formControlName="qualifiersPerGroup">
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Heure de début (HH:mm)</mat-label>
      <input matInput type="text" formControlName="startTime" placeholder="14:00">
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Durée d'un match (min)</mat-label>
      <input matInput type="number" min="1" formControlName="matchDurationMinutes">
    </mat-form-field>

    <mat-form-field appearance="outline">
      <mat-label>Durée de pause (min)</mat-label>
      <input matInput type="number" min="0" formControlName="breakDurationMinutes">
    </mat-form-field>
  </form>
</mat-dialog-content>

<mat-dialog-actions align="end">
  <button mat-button (click)="cancel()">Annuler</button>
  <button mat-raised-button color="primary" (click)="submit()" [disabled]="form.invalid">Générer</button>
</mat-dialog-actions>
```

- [ ] **Step 2: Create TournamentBracketPage**

`tournament-bracket.page.ts`:
```typescript
import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { BracketState } from '@app/domain/bracket/bracket.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadBracket } from '@app/domain/bracket/bracket.actions';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { BracketGenerateModal } from '@app/display/tournament/components/bracket-generate-modal/bracket-generate-modal.component';

@Component({
  selector: 'app-tournament-bracket-page',
  templateUrl: './tournament-bracket.page.html',
  styleUrl: './tournament-bracket.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, MatButtonModule, TournamentNavComponent],
})
export class TournamentBracketPage implements OnInit {
  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly rounds$: Observable<BracketRound[]> = this.store.select(BracketState.getRounds);
  readonly hasBracket$: Observable<boolean> = this.store.select(BracketState.hasBracket);

  protected tournamentId!: string;

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([
      new LoadTournamentById(this.tournamentId),
      new LoadBracket(this.tournamentId),
    ]);
  }

  openGenerateModal(): void {
    this.dialog.open(BracketGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }
}
```

`tournament-bracket.page.html`:
```html
@if (tournament$ | async; as tournament) {
  <div class="page-header">
    <h1>{{ tournament.name }}</h1>
  </div>
  <app-tournament-nav [tournamentId]="tournamentId" />

  <div class="bracket-layout">
    @if (hasBracket$ | async) {
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
                <tr>
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
    } @else {
      <div class="empty-state">
        <p>Le bracket n'a pas encore été généré.</p>
        <button mat-raised-button color="primary" (click)="openGenerateModal()">
          Générer le bracket
        </button>
      </div>
    }
  </div>
}
```

`tournament-bracket.page.scss`:
```scss
.page-header {
  padding: 16px 24px 0;
  h1 { margin: 0; }
}

.bracket-layout {
  padding: 16px 24px;
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
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  p { color: #666; margin-bottom: 16px; }
}
```

- [ ] **Step 3: Enable "Arbre" nav tab**

In `frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html`, replace the disabled "Arbre" tab:

```html
  <a mat-tab-link disabled>
    <mat-icon>account_tree</mat-icon>
    Arbre
  </a>
```

with:

```html
  <a mat-tab-link
     [routerLink]="['/', tournamentId, 'bracket']"
     routerLinkActive
     #bracketLink="routerLinkActive"
     [active]="bracketLink.isActive">
    <mat-icon>account_tree</mat-icon>
    Bracket
  </a>
```

- [ ] **Step 4: Add BracketState and route**

In `frontend/src/app/modules/tournament.routes.ts`:

Add import at top:
```typescript
import { BracketState } from '../domain/bracket/bracket.state';
import { TournamentBracketPage } from '../display/tournament/pages/tournament-bracket/tournament-bracket.page';
```

Update providers to include `BracketState`:
```typescript
providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState, ResultState, BracketState])],
```

Add route (before closing `]`):
```typescript
{ path: ':id/bracket', component: TournamentBracketPage },
```

Full updated file:
```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '../domain/tournament/tournament.state';
import { TeamState } from '../domain/team/team.state';
import { GroupState } from '../domain/group/group.state';
import { ScheduleState } from '../domain/schedule/schedule.state';
import { ResultState } from '../domain/result/result.state';
import { BracketState } from '../domain/bracket/bracket.state';
import { TournamentListPage } from '../display/tournament/pages/tournament-list/tournament-list.page';
import { TournamentDetailPage } from '../display/tournament/pages/tournament-detail/tournament-detail.page';
import { TournamentGroupsPage } from '../display/tournament/pages/tournament-groups/tournament-groups.page';
import { TournamentSchedulePage } from '../display/tournament/pages/tournament-schedule/tournament-schedule.page';
import { TournamentResultsPage } from '../display/tournament/pages/tournament-results/tournament-results.page';
import { TournamentRankingsPage } from '../display/tournament/pages/tournament-rankings/tournament-rankings.page';
import { TournamentBracketPage } from '../display/tournament/pages/tournament-bracket/tournament-bracket.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState, TeamState, GroupState, ScheduleState, ResultState, BracketState])],
    children: [
      { path: '', component: TournamentListPage },
      { path: ':id', component: TournamentDetailPage },
      { path: ':id/groups', component: TournamentGroupsPage },
      { path: ':id/schedule', component: TournamentSchedulePage },
      { path: ':id/results', component: TournamentResultsPage },
      { path: ':id/rankings', component: TournamentRankingsPage },
      { path: ':id/bracket', component: TournamentBracketPage },
    ],
  },
];
```

- [ ] **Step 5: Verify TypeScript compilation**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament/frontend
npx tsc --noEmit 2>&1 | head -30
```
Expected: no output (zero errors).

- [ ] **Step 6: Run backend tests one final time to confirm no regression**

```bash
cd /Users/aberney/code/perso/fvjc-sport-tournament
mvn -f backend/pom.xml test -q 2>&1 | grep -E "Tests run:|BUILD"
```
Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/app/display/tournament/components/bracket-generate-modal/ \
        frontend/src/app/display/tournament/pages/tournament-bracket/ \
        frontend/src/app/display/tournament/components/tournament-nav/tournament-nav.component.html \
        frontend/src/app/modules/tournament.routes.ts
git commit -m "UC-07: FE - Add bracket page, generate modal, nav tab, and route"
```
