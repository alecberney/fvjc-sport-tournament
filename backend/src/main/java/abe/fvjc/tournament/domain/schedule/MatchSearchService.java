package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MatchSearchService {
    private final MatchStore matchStore;

    public Match findById(final MatchId matchId) {
        return matchStore.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match", matchId.value()));
    }
}
