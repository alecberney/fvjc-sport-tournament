import { Group } from '@app/domain/group/group.model';
import { GroupPageView } from '@app/domain/group/group.view';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class GroupViewMapper {

  static toGroupPageView(tournament: Tournament, groups: Group[]): GroupPageView {
    return {
      tournament,
      groups,
      canEditGroups: TournamentDomainService.isDraft(tournament),
      isEmpty: groups.length === 0,
    };
  }
}
