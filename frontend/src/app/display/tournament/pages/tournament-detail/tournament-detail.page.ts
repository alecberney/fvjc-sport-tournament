import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { Team, TeamGroup } from '@app/domain/team/team.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { TeamState } from '@app/domain/team/team.state';
import { LoadTournamentById } from '@app/domain/tournament/tournament.actions';
import { DeleteTeam, LoadTeams, MarkTeamPaid } from '@app/domain/team/team.actions';
import { TeamRegisterModal } from '@app/display/tournament/pages/team-register/team-register.modal';
import { TeamEditModal } from '@app/display/tournament/pages/team-edit/team-edit.modal';
import { TournamentNavComponent } from '@app/display/tournament/components/tournament-nav/tournament-nav.component';

@Component({
  selector: 'app-tournament-detail-page',
  templateUrl: './tournament-detail.page.html',
  styleUrl: './tournament-detail.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, TournamentNavComponent],
})
export class TournamentDetailPage implements OnInit {

  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly tournament$: Observable<Tournament | undefined> = this.store.select(TournamentState.getSelected);
  readonly groups$: Observable<TeamGroup[]> = this.store.select(TeamState.getTeamsGroupedByOrganisation);

  protected tournamentId!: string;

  protected isDraft(tournament: Tournament): boolean {
    return tournament.status === TournamentStatus.DRAFT;
  }

  ngOnInit(): void {
    this.tournamentId = this.route.snapshot.paramMap.get('id')!;
    this.store.dispatch([new LoadTournamentById(this.tournamentId), new LoadTeams(this.tournamentId)]);
  }

  openRegisterModal(): void {
    this.dialog.open(TeamRegisterModal, {
      data: { tournamentId: this.tournamentId },
    });
  }

  openEditModal(team: Team): void {
    this.dialog.open(TeamEditModal, {
      data: { tournamentId: this.tournamentId, team },
    });
  }

  deleteTeam(team: Team): void {
    this.store.dispatch(new DeleteTeam(this.tournamentId, team.id));
  }

  markPaid(team: Team, paid: boolean): void {
    this.store.dispatch(new MarkTeamPaid(this.tournamentId, team.id, paid));
  }
}
