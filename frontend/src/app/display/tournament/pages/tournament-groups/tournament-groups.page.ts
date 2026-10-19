import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Group } from '@app/domain/group/group.model';
import { GroupPageView } from '@app/domain/group/group.view';
import { GroupState } from '@app/domain/group/group.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { GroupListComponent, SwapRequest } from '@app/display/tournament/components/group-list/group-list.component';
import { GroupGenerateModal } from '@app/display/tournament/pages/group-generate/group-generate.modal';
import { TeamSwapModal } from '@app/display/tournament/pages/team-swap/team-swap.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';

@Component({
  selector: 'app-tournament-groups-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tournament-groups.page.html',
  standalone: true,
  imports: [AsyncPipe, MatButtonModule, MatIconModule, GroupListComponent, TournamentNavComponent, TournamentHeaderComponent, TournamentActionsComponent],
})
export class TournamentGroupsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);

  readonly vm$: Observable<GroupPageView | null> = this.store.select(GroupState.getGroupPageView());

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadGroups(this.tournamentId)]);
  }

  openGenerateModal(): void {
    this.dialog.open(GroupGenerateModal, {
      data: { tournamentId: this.tournamentId },
    });
  }

  openSwapModal(groups: Group[], { team, currentGroupName }: SwapRequest): void {
    this.dialog.open(TeamSwapModal, {
      data: { tournamentId: this.tournamentId, team, currentGroupName, groups },
      width: '400px',
    });
  }
}
