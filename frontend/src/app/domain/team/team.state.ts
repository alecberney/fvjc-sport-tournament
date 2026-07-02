import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { tap } from 'rxjs';
import { Team, TeamGroup } from '@app/domain/team/team.model';
import { DeleteTeam, LoadTeams, MarkTeamPaid, RegisterTeams, UpdateTeam } from '@app/domain/team/team.actions';
import { TeamApiService } from '@app/api/team/team.api.service';
import { TeamApiMapper } from '@app/api/team/team.api.mapper';
import { TeamDomainService } from '@app/domain/team/team.domain.service';

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
