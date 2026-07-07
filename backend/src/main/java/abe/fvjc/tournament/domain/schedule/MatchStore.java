package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupId;

import java.util.List;
import java.util.Optional;

public interface MatchStore {
    void saveAll(List<Match> matches);

    Match save(Match match);

    Optional<Match> findById(MatchId matchId);

    List<Match> findAllByRoundIds(List<RoundId> roundIds);

    List<Match> findAllByGroupId(GroupId groupId);

    void deleteAllByRoundIds(List<RoundId> roundIds);

    boolean existsResultByRoundIds(List<RoundId> roundIds);
}
