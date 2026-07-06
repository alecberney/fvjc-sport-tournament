import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Store } from '@ngxs/store';
import { GenerateBracket } from '@app/domain/bracket/bracket.actions';
import { TieBreaker } from '@app/domain/bracket/bracket.model';

@Component({
  selector: 'app-bracket-generate-modal',
  templateUrl: './bracket-generate-modal.component.html',
  styleUrl: './bracket-generate-modal.component.scss',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
})
export class BracketGenerateModal {
  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<BracketGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);

  readonly tieBreakerOptions = [
    { value: TieBreaker.POINTS_DIFF, label: 'Meilleure différence de points' },
    { value: TieBreaker.POINTS_SCORED, label: 'Plus de points marqués' },
    { value: TieBreaker.POINTS_TAKEN, label: 'Moins de points encaissés' },
  ];

  errorMessage: string | null = null;

  form: FormGroup = this.fb.group({
    totalQualifiedTeams: [8, [Validators.required, Validators.min(2)]],
    tieBreaker: [TieBreaker.POINTS_DIFF, [Validators.required]],
    startTime: ['14:00', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    matchDurationMinutes: [20, [Validators.required, Validators.min(1)]],
    breakDurationMinutes: [5, [Validators.required, Validators.min(0)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.errorMessage = null;
    this.store.dispatch(new GenerateBracket(this.data.tournamentId, {
      totalQualifiedTeams: Number(this.form.value.totalQualifiedTeams),
      tieBreaker: String(this.form.value.tieBreaker),
      startTime: String(this.form.value.startTime),
      matchDurationMinutes: Number(this.form.value.matchDurationMinutes),
      breakDurationMinutes: Number(this.form.value.breakDurationMinutes),
    })).subscribe({
      next: () => this.dialogRef.close(),
      error: (err) => {
        const apiErrors: { message: string }[] = err?.error?.errors;
        this.errorMessage = apiErrors?.length > 0
          ? apiErrors.map(e => e.message).join(' ')
          : 'Erreur lors de la génération.';
      },
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
