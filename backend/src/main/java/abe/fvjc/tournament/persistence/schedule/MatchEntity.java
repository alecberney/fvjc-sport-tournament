package abe.fvjc.tournament.persistence.schedule;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
class MatchEntity {
    @Id
    private UUID id;
    private UUID roundId;
    private int field;
    private UUID groupId;
    private UUID team1Id;
    private UUID team2Id;
    private Integer resultScore1;
    private Integer resultScore2;
}
