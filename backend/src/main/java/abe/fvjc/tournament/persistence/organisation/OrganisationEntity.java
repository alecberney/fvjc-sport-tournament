package abe.fvjc.tournament.persistence.organisation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "organisations")
@Getter
@Setter
@NoArgsConstructor
class OrganisationEntity {
    @Id
    private UUID id;
    private String responsibleFirstName;
    private String responsibleLastName;
    private UUID tournamentId;
}
