package abe.fvjc.tournament.api.team;

import abe.fvjc.tournament.domain.organisation.Person;
import abe.fvjc.tournament.domain.team.TeamRef;
import abe.fvjc.tournament.domain.team.TeamRegisterRequest;
import abe.fvjc.tournament.domain.team.TeamUpdateRequest;
import abe.fvjc.tournament.domain.team.TeamOverview;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class TeamApiMapper {

    public static TeamRefDto toTeamRefDto(final TeamRef team) {
        return TeamRefDto.builder()
                .id(team.getId().value())
                .name(team.getName())
                .build();
    }

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

    static List<TeamDto> toTeamDtos(final List<TeamOverview> overviews) {
        return emptyIfNull(overviews).stream()
                .map(TeamApiMapper::toTeamDto)
                .toList();
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
