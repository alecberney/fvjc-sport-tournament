package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Tournament;
import abe.fvjc.tournament.tournament.domain.TournamentCreateRequest;
import lombok.experimental.UtilityClass;

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
