package abe.fvjc.tournament.tournament.persistence;

import jakarta.persistence.*;
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
