package abe.fvjc.tournament.api.team;

import abe.fvjc.tournament.domain.organisation.Person;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamRef;
import abe.fvjc.tournament.domain.team.TeamRegisterRequest;
import abe.fvjc.tournament.domain.team.TeamUpdateRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class TeamApiMapper {
    public static TeamRefDto toTeamRefDto(TeamRef team) {
        if (team == null) {
            return null;
        }
        return TeamRefDto.builder()
                .id(team.getId().value())
                .name(team.getName())
                .build();
    }

    static List<TeamDto> toTeamDtos(final List<Team> teams) {
        return emptyIfNull(teams).stream()
                .map(TeamApiMapper::toTeamDto)
                .toList();
    }

    static TeamDto toTeamDto(final Team team) {
        return TeamDto.builder()
                .id(team.getId().value())
                .name(team.getName())
                .paid(team.isPaid())
                .organisationId(team.getOrganisationId().value())
                .build();
    }

    static TeamRegisterRequest toTeamRegisterRequest(final TeamRegisterRequestDto requestDto) {
        final var responsible = Person.builder()
                .firstName(requestDto.getResponsibleFirstName())
                .lastName(requestDto.getResponsibleLastName())
                .build();
        return TeamRegisterRequest.builder()
                .name(requestDto.getName())
                .responsible(responsible)
                .count(requestDto.getCount())
                .paid(requestDto.getPaid())
                .build();
    }

    static TeamUpdateRequest toTeamUpdateRequest(final TeamUpdateRequestDto requestDto) {
        final var responsible = Person.builder()
                .firstName(requestDto.getResponsibleFirstName())
                .lastName(requestDto.getResponsibleLastName())
                .build();
        return TeamUpdateRequest.builder()
                .name(requestDto.getName())
                .responsible(responsible)
                .paid(requestDto.getPaid())
                .build();
    }
}
