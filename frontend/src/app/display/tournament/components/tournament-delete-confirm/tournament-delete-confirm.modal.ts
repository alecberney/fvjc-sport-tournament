import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface TournamentDeleteConfirmData {
  name: string;
}

@Component({
  selector: 'app-tournament-delete-confirm-modal',
  templateUrl: './tournament-delete-confirm.modal.html',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
})
export class TournamentDeleteConfirmModal {

  private readonly dialogRef = inject(MatDialogRef<TournamentDeleteConfirmModal>);

  readonly data = inject<TournamentDeleteConfirmData>(MAT_DIALOG_DATA);

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
