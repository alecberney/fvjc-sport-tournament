package abe.fvjc.tournament.organisation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface OrganisationRepository extends JpaRepository<OrganisationEntity, UUID> {
}
