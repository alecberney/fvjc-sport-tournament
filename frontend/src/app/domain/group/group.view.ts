import { Group } from '@app/domain/group/group.model';
import { Tournament } from '@app/domain/tournament/tournament.model';

export interface GroupPageView {
  tournament: Tournament;
  groups: Group[];
  canEditGroups: boolean;
  isEmpty: boolean;
}
