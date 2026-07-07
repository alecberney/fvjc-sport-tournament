package abe.fvjc.tournament.api.tournament;

import abe.fvjc.tournament.domain.tournament.Tournament;
import abe.fvjc.tournament.domain.tournament.TournamentCreateRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class TournamentApiMapper {

    static List<TournamentDto> toTournamentDtos(final List<Tournament> tournaments) {
        return emptyIfNull(tournaments).stream()
            .map(TournamentApiMapper::toTournamentDto)
            .toList();
    }

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

    static TournamentCreateRequest toTournamentCreateRequest(TournamentCreateRequestDto requestDto) {
        return TournamentCreateRequest.builder()
                .name(requestDto.getName())
                .sport(requestDto.getSport())
                .numberOfFields(requestDto.getNumberOfFields())
                .minPlayersPerTeam(requestDto.getMinPlayersPerTeam())
                .maxPlayersPerTeam(requestDto.getMaxPlayersPerTeam())
                .date(requestDto.getDate())
                .build();
    }
}
