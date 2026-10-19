import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-tournament-end-confirm-modal',
  templateUrl: './tournament-end-confirm.modal.html',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
})
export class TournamentEndConfirmModal {

  private readonly dialogRef = inject(MatDialogRef<TournamentEndConfirmModal>);

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
