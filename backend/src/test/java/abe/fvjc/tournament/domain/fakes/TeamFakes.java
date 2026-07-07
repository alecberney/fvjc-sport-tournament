package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamRegisterRequest;
import abe.fvjc.tournament.domain.team.TeamUpdateRequest;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomGroupId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomOrganisationId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTeamId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTournamentId;
import static abe.fvjc.tournament.domain.fakes.PersonFakes.buildJeanDupont;
import static abe.fvjc.tournament.domain.fakes.PersonFakes.buildMarieMartin;

@UtilityClass
public class TeamFakes {

    public static Team buildTeam() {
        return buildTeam(randomOrganisationId(), randomTournamentId(), randomGroupId());
    }

    public static Team buildTeam(final TournamentId tournamentId) {
        return buildTeam(randomOrganisationId(), tournamentId, randomGroupId());
    }

    public static Team buildTeam(final OrganisationId organisationId, final TournamentId tournamentId) {
        return buildTeam(organisationId, tournamentId, randomGroupId());
    }

    public static Team buildTeam(
            final OrganisationId organisationId,
            final TournamentId tournamentId,
            final GroupId groupId) {
        return Team.builder()
                .id(randomTeamId())
                .name("Les Aigles")
                .paid(false)
                .organisationId(organisationId)
                .tournamentId(tournamentId)
                .groupId(groupId)
                .build();
    }

    public static TeamRegisterRequest buildRegisterRequest() {
        return TeamRegisterRequest.builder()
                .name("Les Aigles")
                .responsible(buildJeanDupont())
                .count(1)
                .paid(List.of(false))
                .build();
    }

    public static TeamRegisterRequest buildRegisterRequestWithCount(final int count) {
        return TeamRegisterRequest.builder()
                .name("Les Aigles")
                .responsible(buildJeanDupont())
                .count(count)
                .paid(Collections.nCopies(count, false))
                .build();
    }

    public static TeamUpdateRequest buildUpdateRequest() {
        return TeamUpdateRequest.builder()
                .name("Les Aigles Modifié")
                .responsible(buildMarieMartin())
                .paid(true)
                .build();
    }
}
