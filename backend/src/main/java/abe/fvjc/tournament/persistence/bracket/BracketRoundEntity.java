package abe.fvjc.tournament.persistence.bracket;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "bracket_rounds")
@Getter
@Setter
@NoArgsConstructor
class BracketRoundEntity {
    @Id
    private UUID id;
    private UUID tournamentId;
    private int number;
    private String name;
    private String startTime;
}
