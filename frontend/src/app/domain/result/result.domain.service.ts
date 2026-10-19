import { Injectable } from '@angular/core';
import { Group } from '@app/domain/group/group.model';
import { GroupRanking } from '@app/domain/result/result.model';

@Injectable({ providedIn: 'root' })
export class ResultDomainService {

  // Rankings in group order; a selected group name narrows the list to that group only.
  // Groups whose ranking is not loaded yet are skipped.
  static findRankingsToDisplay(
    groups: Group[],
    rankings: { [groupId: string]: GroupRanking },
    selectedGroupName: string | null,
  ): GroupRanking[] {
    return groups
      .filter(group => !selectedGroupName || group.name === selectedGroupName)
      .map(group => rankings[group.id])
      .filter((ranking): ranking is GroupRanking => ranking !== undefined);
  }
}
