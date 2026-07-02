package abe.fvjc.tournament.tournament.persistence;

import abe.fvjc.tournament.tournament.domain.Tournament;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaTournamentStore implements TournamentStore {

    private final TournamentRepository repository;

    @Override
    @Transactional
    public Tournament save(Tournament tournament) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tournament> findById(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tournament> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        throw new UnsupportedOperationException();
    }
}
