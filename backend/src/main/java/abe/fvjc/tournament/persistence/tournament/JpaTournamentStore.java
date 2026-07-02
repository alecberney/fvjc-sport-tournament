package abe.fvjc.tournament.tournament.persistence;

import abe.fvjc.tournament.tournament.domain.Tournament;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.tournament.persistence.TournamentDbMapper.toTournament;
import static abe.fvjc.tournament.tournament.persistence.TournamentDbMapper.toTournamentEntity;

@Repository
@RequiredArgsConstructor
class JpaTournamentStore implements TournamentStore {
    private final TournamentRepository tournamentRepository;

    @Override
    @Transactional
    public Tournament save(Tournament tournament) {
        final var entity = toTournamentEntity(tournament);
        final var savedEntity = tournamentRepository.save(entity);
        return toTournament(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tournament> findById(UUID id) {
        return tournamentRepository.findById(id)
                .map(TournamentDbMapper::toTournament);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tournament> findAll() {
        return tournamentRepository.findAll().stream()
                .map(TournamentDbMapper::toTournament)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        tournamentRepository.deleteById(id);
    }
}
