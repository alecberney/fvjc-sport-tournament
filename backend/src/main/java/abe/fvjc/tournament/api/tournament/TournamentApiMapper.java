package abe.fvjc.tournament.api.tournament;

import abe.fvjc.tournament.domain.tournament.Tournament;
import abe.fvjc.tournament.domain.tournament.TournamentCreateRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class TournamentApiMapper {

    static TournamentDto toTournamentDto(Tournament tournament) {
        return TournamentDto.builder()
            .id(tournament.getId().value())
            .name(tournament.getName())
            .sport(tournament.getSport())
            .numberOfFields(tournament.getNumberOfFields())
            .minPlayersPerTeam(tournament.getMinPlayersPerTeam())
            .maxPlayersPerTeam(tournament.getMaxPlayersPerTeam())
            .date(tournament.getDate())
            .status(tournament.getStatus())
            .build();
    }

    static List<TournamentDto> toTournamentDtos(final List<Tournament> tournaments) {
        return emptyIfNull(tournaments).stream()
            .map(TournamentApiMapper::toTournamentDto)
            .toList();
    }

    static TournamentCreateRequest toTournamentCreateRequest(TournamentCreateRequestDto dto) {
        return TournamentCreateRequest.builder()
            .name(dto.getName())
            .sport(dto.getSport())
            .numberOfFields(dto.getNumberOfFields())
            .minPlayersPerTeam(dto.getMinPlayersPerTeam())
            .maxPlayersPerTeam(dto.getMaxPlayersPerTeam())
            .date(dto.getDate())
            .build();
    }
}
