package abe.fvjc.tournament.persistence.tournament;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
class TournamentEntity {
    @Id
    private UUID id;
    private String name;
    private String sport;
    private int numberOfFields;
    private int minPlayersPerTeam;
    private int maxPlayersPerTeam;
    private String date;
    private String status;
}
