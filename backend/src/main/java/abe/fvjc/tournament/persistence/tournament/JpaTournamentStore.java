package abe.fvjc.tournament.persistence.tournament;

import abe.fvjc.tournament.domain.tournament.Tournament;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.persistence.tournament.TournamentDbMapper.toTournament;
import static abe.fvjc.tournament.persistence.tournament.TournamentDbMapper.toTournamentEntity;

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
    public Optional<Tournament> findById(TournamentId id) {
        return tournamentRepository.findById(id.value())
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
    public void deleteById(TournamentId id) {
        tournamentRepository.deleteById(id.value());
    }
}
