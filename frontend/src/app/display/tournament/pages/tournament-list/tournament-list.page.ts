import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { TournamentCardView, TournamentListPageView } from '@app/domain/tournament/tournament.view';
import { TournamentState } from '@app/domain/tournament/tournament.state';
import { DeleteTournament, LoadTournaments } from '@app/domain/tournament/tournament.actions';
import { TournamentCreateModal } from '@app/display/tournament/pages/tournament-create/tournament-create.modal';
import { TournamentDeleteConfirmModal } from '@app/display/tournament/components/tournament-delete-confirm/tournament-delete-confirm.modal';
import { DateFormatPipe } from '@app/display/shared/pipes/date-format.pipe';

@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  styleUrl: './tournament-list.page.scss',
  standalone: true,
  imports: [AsyncPipe, DateFormatPipe, RouterLink, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule],
})
export class TournamentListPage implements OnInit {

  private readonly store = inject(Store);
  private readonly dialog = inject(MatDialog);

  readonly vm$: Observable<TournamentListPageView> = this.store.select(TournamentState.getTournamentListPageView());

  ngOnInit(): void {
    this.store.dispatch(new LoadTournaments());
  }

  openCreateModal(): void {
    this.dialog.open(TournamentCreateModal);
  }

  openDeleteModal(tournament: TournamentCardView, event: MouseEvent): void {
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
