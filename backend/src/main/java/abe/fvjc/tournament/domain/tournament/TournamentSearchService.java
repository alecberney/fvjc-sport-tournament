package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentSearchService {
    private final TournamentStore tournamentStore;

    public Tournament findById(final UUID id) {
        return findById(TournamentId.of(id));
    }

    public Tournament findById(final TournamentId id) {
        return tournamentStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Tournament", id));
    }

    public List<Tournament> findAll() {
        return tournamentStore.findAll();
    }
}
