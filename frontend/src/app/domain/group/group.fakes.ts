import { Group } from '@app/domain/group/group.model';
import { TeamFakes } from '@app/domain/team/team.fakes';

export class GroupFakes {

  static aGroup(overrides?: Partial<Group>): Group {
    return {
      id: 'group-id-1',
      name: 'A',
      teams: TeamFakes.aList(4),
      ...overrides,
    };
  }

  static aList(count: number, overrides?: Partial<Group>): Group[] {
    return Array.from({ length: count }, (_, i) =>
      GroupFakes.aGroup({ id: `group-id-${i + 1}`, name: String.fromCharCode(65 + i), ...overrides })
    );
  }
}
