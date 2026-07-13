package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.team.TeamSearchService;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.domain.group.GroupValidator.validateGroupGenerateRequest;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupStore groupStore;
    private final TeamStore teamStore;
    private final TeamSearchService teamSearchService;
    private final TournamentSearchService tournamentSearchService;

    public GroupDistribution distribution(final TournamentId tournamentId, final GroupGenerateRequest request) {
        final var teams = teamStore.findAllByTournamentId(tournamentId);
        validateGroupGenerateRequest(request, teams.size());
        return computeDistribution(teams.size(), request.getGroupSize());
    }

    public List<GroupOverview> generate(final TournamentId tournamentId, final GroupGenerateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        assertDraft(tournament.getStatus());

        final var teams = teamStore.findAllByTournamentId(tournamentId);
        validateGroupGenerateRequest(request, teams.size());

        teams.forEach(team -> teamStore.save(team.withGroupId(GroupId.empty())));
        groupStore.deleteAllByTournamentId(tournamentId);

        final var draws = computeDraw(teams, request.getGroupSize());
        final var groups = new ArrayList<Group>();
        for (int i = 0; i < draws.size(); i++) {
            groups.add(buildGroup(tournament.getId(), groupName(i)));
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

    public List<GroupOverview> findAllByTournamentId(final TournamentId tournamentId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        return groups.stream()
                .map(this::toGroupOverview)
                .toList();
    }

    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        groupStore.deleteAllByTournamentId(tournamentId);
    }

    public List<GroupOverview> swap(final TournamentId tournamentId, final GroupSwapRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        assertDraft(tournament.getStatus());

        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var team1 = teamSearchService.findById(TeamId.of(request.getTeamId1()));
        final var team2 = teamSearchService.findById(TeamId.of(request.getTeamId2()));

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

        return List.of(toGroupOverview(group1), toGroupOverview(group2));
    }

    private GroupOverview toGroupOverview(final Group group) {
        final var teams = teamStore.findAllByGroupId(group.getId());
        return GroupOverview.builder()
            .id(group.getId())
            .name(group.getName())
            .tournamentId(group.getTournamentId())
            .teams(teams)
            .build();
    }

    private List<GroupOverview> buildOverviews(final List<Group> groups) {
        return groups.stream().map(this::toGroupOverview).toList();
    }

    private static Group buildGroup(final TournamentId tournamentId, final String name) {
        return Group.builder()
            .id(GroupId.of(UUID.randomUUID()))
            .name(name)
            .tournamentId(tournamentId)
            .build();
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
