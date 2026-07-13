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

    public void deleteAllByTournamentId(final UUID tournamentId) {
        groupStore.deleteAllByTournamentId(tournamentId);
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
