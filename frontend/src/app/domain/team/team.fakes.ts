import { Team } from '@app/domain/team/team.model';

export class TeamFakes {

  static aTeam(overrides?: Partial<Team>): Team {
    return {
      id: 'team-id-1',
      name: 'Les Aigles',
      paid: false,
      organisationId: 'org-id-1',
      responsible: { firstName: 'Jean', lastName: 'Dupont' },
      ...overrides,
    };
  }

  static aList(count: number, overrides?: Partial<Team>): Team[] {
    return Array.from({ length: count }, (_, i) =>
      TeamFakes.aTeam({ id: `team-id-${i + 1}`, name: `Équipe ${i + 1}`, ...overrides })
    );
  }
}
