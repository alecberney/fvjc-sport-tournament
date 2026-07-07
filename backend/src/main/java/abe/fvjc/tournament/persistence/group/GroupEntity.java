package abe.fvjc.tournament.persistence.group;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
class GroupEntity {
    @Id
    private UUID id;
    private String name;
    private UUID tournamentId;
}
