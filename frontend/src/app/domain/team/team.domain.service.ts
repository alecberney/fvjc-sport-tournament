import { Injectable } from '@angular/core';
import { Team, TeamGroup } from '@app/domain/team/team.model';

@Injectable({ providedIn: 'root' })
export class TeamDomainService {

  static groupByOrganisation(teams: Team[]): TeamGroup[] {
    const map = new Map<string, TeamGroup>();
    for (const team of teams) {
      if (!map.has(team.organisationId)) {
        map.set(team.organisationId, {
          organisationId: team.organisationId,
          responsibleName: `${team.responsible.firstName} ${team.responsible.lastName}`,
          teams: [],
        });
      }
      map.get(team.organisationId)!.teams.push(team);
    }
    return Array.from(map.values());
  }
}
