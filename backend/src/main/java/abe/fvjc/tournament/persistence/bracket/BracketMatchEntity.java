package abe.fvjc.tournament.bracket.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "bracket_matches")
@Getter
@Setter
@NoArgsConstructor
class BracketMatchEntity {
    @Id
    private UUID id;
    private UUID roundId;
    private int field;
    private UUID team1Id;
    private String team1Name;
    private UUID team2Id;
    private String team2Name;
    private Integer score1;
    private Integer score2;
    private UUID nextMatchId;
    private int nextMatchTeamSlot;
}
