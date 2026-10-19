import { AsyncPipe } from '@angular/common';
import { Component, inject, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { TeamPageView, TeamRowView } from '@app/domain/team/team.view';
import { TeamState } from '@app/domain/team/team.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { DeleteTeam, LoadTeams, MarkTeamPaid } from '@app/domain/team/team.actions';
import { TeamRegisterModal } from '@app/display/tournament/pages/team-register/team-register.modal';
import { TeamEditModal } from '@app/display/tournament/pages/team-edit/team-edit.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';
import { TournamentHeaderComponent } from '@app/display/tournament/components/tournament-header/tournament-header.component';
import { TournamentActionsComponent } from '@app/display/tournament/components/tournament-actions/tournament-actions.component';

@Component({
  selector: 'app-tournament-detail-page',
  templateUrl: './tournament-detail.page.html',
  styleUrl: './tournament-detail.page.scss',
  standalone: true,
  imports: [AsyncPipe, MatButtonModule, MatIconModule, TournamentNavComponent, TournamentHeaderComponent, TournamentActionsComponent],
})
export class TournamentDetailPage implements OnInit {

  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);

  readonly vm$: Observable<TeamPageView | null> = this.store.select(TeamState.getTeamPageView());

  @Input({ alias: 'id', required: true }) tournamentId!: string;

  ngOnInit(): void {
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadTeams(this.tournamentId)]);
  }

  openRegisterModal(): void {
    this.dialog.open(TeamRegisterModal, {
      data: { tournamentId: this.tournamentId },
    });
  }

  openEditModal(row: TeamRowView): void {
    this.dialog.open(TeamEditModal, {
      data: { tournamentId: this.tournamentId, team: row.team },
    });
  }

  deleteTeam(row: TeamRowView): void {
    this.store.dispatch(new DeleteTeam(this.tournamentId, row.id));
  }

  togglePaid(row: TeamRowView): void {
    this.store.dispatch(new MarkTeamPaid(this.tournamentId, row.id, !row.paid));
  }
}
