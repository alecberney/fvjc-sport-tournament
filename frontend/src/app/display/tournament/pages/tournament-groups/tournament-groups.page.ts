import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { Group } from '@app/domain/group/group.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { GroupState } from '@app/domain/group/group.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { LoadGroups } from '@app/domain/group/group.actions';
import { GroupListComponent, SwapRequest } from '@app/display/tournament/components/group-list/group-list.component';
import { GroupGenerateModal } from '@app/display/tournament/pages/group-generate/group-generate.modal';
import { TeamSwapModal } from '@app/display/tournament/pages/team-swap/team-swap.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';

@Component({
  selector: 'app-tournament-groups-page',
  templateUrl: './tournament-groups.page.html',
  styleUrl: './tournament-groups.page.scss',
  standalone: true,
  imports: [AsyncPipe, MatButtonModule, MatIconModule, GroupListComponent, TournamentNavComponent],
})
export class TournamentGroupsPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<Group[]> = this.store.select(GroupState.getGroups);

  protected tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
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
