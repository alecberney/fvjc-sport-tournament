package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TournamentSearchService {
    private final TournamentStore tournamentStore;

    public Tournament findById(final TournamentId tournamentId) {
        return tournamentStore.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament", tournamentId.value()));
    }
}
