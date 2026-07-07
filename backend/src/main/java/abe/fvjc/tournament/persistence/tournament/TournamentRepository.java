package abe.fvjc.tournament.persistence.tournament;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TournamentRepository extends JpaRepository<TournamentEntity, UUID> {}
