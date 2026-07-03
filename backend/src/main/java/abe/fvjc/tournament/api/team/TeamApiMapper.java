package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.team.domain.TeamRegisterRequest;
import abe.fvjc.tournament.team.domain.TeamUpdateRequest;
import abe.fvjc.tournament.team.domain.TeamView;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TeamApiMapper {

    static TeamDto toTeamDto(final TeamView view) {
        return TeamDto.builder()
                .id(view.getId().value())
                .name(view.getName())
                .paid(view.isPaid())
                .organisationId(view.getOrganisationId().value())
                .responsibleFirstName(view.getResponsible().getFirstName())
                .responsibleLastName(view.getResponsible().getLastName())
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
