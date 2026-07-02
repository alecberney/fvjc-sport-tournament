package abe.fvjc.tournament.tournament.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentStore tournamentStore;

    public Tournament create(Tournament tournament) {
        throw new UnsupportedOperationException();
    }

    public Tournament findById(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<Tournament> findAll() {
        throw new UnsupportedOperationException();
    }

    public void delete(UUID id) {
        throw new UnsupportedOperationException();
    }
}
