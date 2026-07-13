package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BracketMatchSearchService {
    private final BracketMatchStore bracketMatchStore;

    public BracketMatch findById(final BracketMatchId matchId) {
        return bracketMatchStore.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match", matchId.value()));
    }
}
