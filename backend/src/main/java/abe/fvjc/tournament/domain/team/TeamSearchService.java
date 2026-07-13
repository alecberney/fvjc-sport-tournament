package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamSearchService {
    private final TeamStore teamStore;

    public Team findById(final TeamId teamId) {
        return teamStore.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team", teamId.value()));
    }
}
