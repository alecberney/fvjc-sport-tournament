package abe.fvjc.tournament.domain.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildTeamIdsByGroupId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RoundRobinServiceTest {

    @InjectMocks
    private RoundRobinService roundRobinService;

    @Test
    void planRoundsWhenTwoTriangleGroupsShouldPlayOneMatchPerGroupPerRound() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(3, 3);

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 4);

        assertEquals(3, roundsPlanned.size());
        roundsPlanned.forEach(round -> assertEquals(2, round.size()));
    }

    @Test
    void planRoundsWhenSingleFieldShouldScheduleOneMatchPerRound() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(2, 4);

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 1);

        assertEquals(7, roundsPlanned.size());
        roundsPlanned.forEach(round -> assertEquals(1, round.size()));
    }

    @Test
    void planRoundsWhenSingleFieldShouldScheduleIdleGroupEarlyForRestFairness() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(2, 4);
        final var groupAId = teamIdsByGroupId.keySet().iterator().next();

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 1);

        final var groupARoundIndex = IntStream.range(0, roundsPlanned.size())
                .filter(i -> roundsPlanned.get(i).stream().anyMatch(pair -> pair.groupId().equals(groupAId)))
                .findFirst()
                .orElseThrow();

        assertEquals(7, roundsPlanned.size());
        assertTrue(groupARoundIndex < 3,
                "Group A should be scheduled early for rest fairness but was round " + (groupARoundIndex + 1));
    }

    @Test
    void planRoundsWhenGroupLargerThanFieldsShouldPackSameGroupMatchesInOneRound() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(4);

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 4);

        assertEquals(3, roundsPlanned.size());
        roundsPlanned.forEach(round -> assertEquals(2, round.size()));
    }

    @Test
    void planRoundsShouldFillAllFieldsAndNeverScheduleTeamTwiceInSameRound() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(4, 4);

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 4);

        final var totalMatches = roundsPlanned.stream()
                .mapToInt(List::size)
                .sum();

        assertEquals(3, roundsPlanned.size());
        assertEquals(12, totalMatches);
        roundsPlanned.forEach(round -> {
            final var teamIds = round.stream()
                    .flatMap(pair -> Stream.of(pair.team1Id(), pair.team2Id()))
                    .toList();
            final var distinctTeamIds = teamIds.stream()
                    .distinct()
                    .toList();

            assertEquals(4, round.size());
            assertEquals(teamIds.size(), distinctTeamIds.size());
        });
    }

    @Test
    void planRoundsWhenOddGroupShouldPlayEveryPairingExactlyOnce() {
        final var teamIdsByGroupId = buildTeamIdsByGroupId(5);

        final var roundsPlanned = roundRobinService.planRounds(teamIdsByGroupId, 4);

        final var pairings = roundsPlanned.stream()
                .flatMap(List::stream)
                .map(pair -> Stream.of(pair.team1Id().value().toString(), pair.team2Id().value().toString())
                        .sorted()
                        .toList())
                .toList();
        final var distinctPairings = pairings.stream()
                .distinct()
                .toList();

        assertEquals(10, pairings.size());
        assertEquals(10, distinctPairings.size());
    }
}
