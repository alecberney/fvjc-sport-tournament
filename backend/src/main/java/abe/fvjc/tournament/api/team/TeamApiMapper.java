package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.team.domain.TeamRegisterRequest;
import abe.fvjc.tournament.team.domain.TeamUpdateRequest;
import abe.fvjc.tournament.team.domain.TeamOverview;
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
