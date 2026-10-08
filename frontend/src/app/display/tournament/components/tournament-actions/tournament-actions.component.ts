import { Component, inject, Input } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { Store } from '@ngxs/store';
import { Tournament, TournamentStatus } from '@app/domain/tournament/tournament.model';
import { DeleteTournament } from '@app/domain/tournament/tournament.actions';
import { GenerateRandomResults } from '@app/domain/result/result.actions';
import { GenerateRandomBracketResults } from '@app/domain/bracket/bracket.actions';
import { TournamentDeleteConfirmModal } from '@app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal';
import { TeamRandomGenerateModal } from '@app/display/tournament/pages/team-random-generate/team-random-generate.modal';

// Tournament-wide header actions (delete + settings cog) shown on every tab of a tournament.
// Unlike other components it talks to the store and opens dialogs itself, so the six tab pages
// don't each have to duplicate the same handlers.
@Component({
  selector: 'app-tournament-actions',
  templateUrl: './tournament-actions.component.html',
  styleUrl: './tournament-actions.component.scss',
  standalone: true,
  imports: [MatButtonModule, MatDividerModule, MatIconModule, MatMenuModule],
})
export class TournamentActionsComponent {
  @Input({ required: true }) tournament!: Tournament;

  private readonly store = inject(Store);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  protected isDraft(): boolean {
    return this.tournament.status === TournamentStatus.DRAFT;
  }

  protected isInProgress(): boolean {
    return this.tournament.status === TournamentStatus.IN_PROGRESS;
  }

  openDeleteModal(): void {
    this.dialog
      .open(TournamentDeleteConfirmModal, { data: { name: this.tournament.name } })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.store
            .dispatch(new DeleteTournament(this.tournament.id))
            .subscribe(() => this.router.navigate(['/']));
        }
      });
  }

  openRandomTeamsModal(): void {
    this.dialog.open(TeamRandomGenerateModal, {
      data: { tournamentId: this.tournament.id },
    });
  }

  generateRandomResults(): void {
    this.store.dispatch(new GenerateRandomResults(this.tournament.id));
  }

  generateRandomBracketResults(): void {
    this.store.dispatch(new GenerateRandomBracketResults(this.tournament.id));
  }
}
