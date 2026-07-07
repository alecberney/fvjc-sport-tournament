package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.bracket.BracketMatchId;
import abe.fvjc.tournament.domain.bracket.BracketRoundId;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.schedule.MatchId;
import abe.fvjc.tournament.domain.schedule.RoundId;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static BracketRoundId randomBracketRoundId() {
        return BracketRoundId.of(UUID.randomUUID());
    }

    static BracketMatchId randomBracketMatchId() {
        return BracketMatchId.of(UUID.randomUUID());
    }

    static GroupId randomGroupId() {
        return GroupId.of(UUID.randomUUID());
    }

    static RoundId randomRoundId() {
        return RoundId.of(UUID.randomUUID());
    }

    static MatchId randomMatchId() {
        return MatchId.of(UUID.randomUUID());
    }

    static OrganisationId randomOrganisationId() {
        return OrganisationId.of(UUID.randomUUID());
    }

    static TournamentId randomTournamentId() {
        return TournamentId.of(UUID.randomUUID());
    }

    static TeamId randomTeamId() {
        return TeamId.of(UUID.randomUUID());
    }
}
