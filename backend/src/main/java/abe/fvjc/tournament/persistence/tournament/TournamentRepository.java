package abe.fvjc.tournament.tournament.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TournamentRepository extends JpaRepository<TournamentEntity, UUID> {}
