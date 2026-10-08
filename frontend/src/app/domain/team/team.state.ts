import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { concatMap, from, tap, toArray } from 'rxjs';
import { Team, TeamGroup } from '@app/domain/team/team.model';
import { DeleteTeam, GenerateRandomTeams, LoadTeams, MarkTeamPaid, RegisterTeams, UpdateTeam } from '@app/domain/team/team.actions';
import { TeamApiService } from '@app/api/team/team.api.service';
import { TeamApiMapper } from '@app/api/team/team.api.mapper';
import { TeamDomainService } from '@app/domain/team/team.domain.service';
import { SeedingDomainService } from '@app/domain/seeding/seeding.domain.service';

export interface ITeamState {
  teams: Team[];
}

@State<ITeamState>({
  name: 'team',
  defaults: { teams: [] },
})
@Injectable()
export class TeamState {

  private readonly teamApiService = inject(TeamApiService);

  @Selector()
  static getTeams(state: ITeamState): Team[] {
    return state.teams;
  }

  @Selector()
  static getTeamsGroupedByOrganisation(state: ITeamState): TeamGroup[] {
    return TeamDomainService.groupByOrganisation(state.teams);
  }

  @Action(LoadTeams)
  loadTeams(ctx: StateContext<ITeamState>, { tournamentId }: LoadTeams) {
    return this.teamApiService.getAll$(tournamentId).pipe(
      tap(dtos => {
        ctx.patchState({ teams: dtos.map(TeamApiMapper.toDomain) });
      })
    );
  }

  @Action(RegisterTeams)
  registerTeams(ctx: StateContext<ITeamState>, { tournamentId, registration }: RegisterTeams) {
    return this.teamApiService.register$(tournamentId, TeamApiMapper.toRegisterRequest(registration)).pipe(
      tap(dtos => {
        const newTeams = dtos.map(TeamApiMapper.toDomain);
        ctx.patchState({ teams: [...ctx.getState().teams, ...newTeams] });
      })
    );
  }

  // Registers each random organisation sequentially, then appends all created teams at once.
  @Action(GenerateRandomTeams)
  generateRandomTeams(ctx: StateContext<ITeamState>, { tournamentId, count }: GenerateRandomTeams) {
    const registrations = SeedingDomainService.buildRandomRegistrations(count);
    return from(registrations).pipe(
      concatMap(registration => this.teamApiService.register$(tournamentId, TeamApiMapper.toRegisterRequest(registration))),
      toArray(),
      tap(dtoBatches => {
        const newTeams = dtoBatches.flat().map(TeamApiMapper.toDomain);
        ctx.patchState({ teams: [...ctx.getState().teams, ...newTeams] });
      })
    );
  }

  @Action(UpdateTeam)
  updateTeam(ctx: StateContext<ITeamState>, { tournamentId, teamId, update }: UpdateTeam) {
    return this.teamApiService.update$(tournamentId, teamId, TeamApiMapper.toUpdateRequest(update)).pipe(
      tap(dto => {
        const updated = TeamApiMapper.toDomain(dto);
        ctx.patchState({
          teams: ctx.getState().teams.map(t => t.id === updated.id ? updated : t),
        });
      })
    );
  }

  @Action(DeleteTeam)
  deleteTeam(ctx: StateContext<ITeamState>, { tournamentId, teamId }: DeleteTeam) {
    return this.teamApiService.delete$(tournamentId, teamId).pipe(
      tap(() => {
        ctx.patchState({ teams: ctx.getState().teams.filter(t => t.id !== teamId) });
      })
    );
  }

  @Action(MarkTeamPaid)
  markTeamPaid(ctx: StateContext<ITeamState>, { tournamentId, teamId, paid }: MarkTeamPaid) {
    return this.teamApiService.markPaid$(tournamentId, teamId, paid).pipe(
      tap(dto => {
        const updated = TeamApiMapper.toDomain(dto);
        ctx.patchState({
          teams: ctx.getState().teams.map(t => t.id === updated.id ? updated : t),
        });
      })
    );
  }
}
