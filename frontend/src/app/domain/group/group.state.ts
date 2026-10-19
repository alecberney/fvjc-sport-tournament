import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext, createSelector } from '@ngxs/store';
import { tap } from 'rxjs';
import { Group } from '@app/domain/group/group.model';
import { GenerateGroups, LoadGroups, SwapTeams } from '@app/domain/group/group.actions';
import { GroupApiService } from '@app/api/group/group.api.service';
import { GroupApiMapper } from '@app/api/group/group.api.mapper';
import { GroupPageView } from '@app/domain/group/group.view';
import { GroupViewMapper } from '@app/domain/group/group.view.mapper';
import { TournamentState } from '@app/domain/tournament/tournament.state';

export interface IGroupState {
  groups: Group[];
}

@State<IGroupState>({
  name: 'group',
  defaults: { groups: [] },
})
@Injectable()
export class GroupState {

  private readonly groupApiService = inject(GroupApiService);

  @Selector()
  static getGroups(state: IGroupState): Group[] {
    return state.groups;
  }


  // Built on demand, never stored.
  static getGroupPageView() {
    return createSelector(
      [TournamentState.getSelected, GroupState.getGroups],
      (tournament, groups): GroupPageView | null => tournament
        ? GroupViewMapper.toGroupPageView(tournament, groups)
        : null,
    );
  }

  @Action(LoadGroups)
  loadGroups(ctx: StateContext<IGroupState>, { tournamentId }: LoadGroups) {
    return this.groupApiService.getAll$(tournamentId).pipe(
      tap(dtos => {
        ctx.patchState({ groups: dtos.map(GroupApiMapper.toDomain) });
      })
    );
  }

  @Action(GenerateGroups)
  generateGroups(ctx: StateContext<IGroupState>, { tournamentId, groupSize }: GenerateGroups) {
    return this.groupApiService.generate$(tournamentId, { groupSize }).pipe(
      tap(dtos => {
        ctx.patchState({ groups: dtos.map(GroupApiMapper.toDomain) });
      })
    );
  }

  @Action(SwapTeams)
  swapTeams(ctx: StateContext<IGroupState>, { tournamentId, teamId1, teamId2 }: SwapTeams) {
    return this.groupApiService.swap$(tournamentId, { teamId1, teamId2 }).pipe(
      tap(dtos => {
        const updatedGroups = dtos.map(GroupApiMapper.toDomain);
        ctx.patchState({
          groups: ctx.getState().groups.map(g => updatedGroups.find(ug => ug.id === g.id) ?? g),
        });
      })
    );
  }
}
