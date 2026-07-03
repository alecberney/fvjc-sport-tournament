package abe.fvjc.tournament.schedule.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rounds")
@Getter
@Setter
@NoArgsConstructor
class RoundEntity {
    @Id
    private UUID id;
    private UUID tournamentId;
    private int number;
    private String startTime;
}
