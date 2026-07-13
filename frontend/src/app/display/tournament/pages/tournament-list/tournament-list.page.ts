import { AsyncPipe, DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { DeleteTournament, LoadTournaments } from '@app/domain/tournament/tournament.actions';
import { TournamentCreateModal } from '@app/display/tournament/pages/tournament-create/tournament-create.modal';
import { TournamentDeleteConfirmModal } from '@app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal';

@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  styleUrl: './tournament-list.page.scss',
  standalone: true,
  imports: [AsyncPipe, DatePipe, RouterLink, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule],
})
export class TournamentListPage implements OnInit {

  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);

  readonly tournaments$: Observable<Tournament[]> = this.store.select(TournamentState.getTournaments);

  ngOnInit(): void {
    this.store.dispatch(new LoadTournaments());
  }

  openCreateModal(): void {
    this.dialog.open(TournamentCreateModal);
  }

  openDeleteModal(tournament: Tournament, event: MouseEvent): void {
    event.stopPropagation();
    event.preventDefault();
    this.dialog
      .open(TournamentDeleteConfirmModal, { data: { name: tournament.name } })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.store.dispatch(new DeleteTournament(tournament.id));
        }
      });
  }
}
