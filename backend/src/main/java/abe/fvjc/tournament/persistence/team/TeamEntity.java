package abe.fvjc.tournament.team.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
class TeamEntity {
    @Id
    private UUID id;
    private String name;
    private boolean paid;
    private UUID organisationId;
    private UUID tournamentId;
}
