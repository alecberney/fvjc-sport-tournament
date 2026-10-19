import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Actions, ofActionCompleted, ofActionDispatched, Store } from '@ngxs/store';
import { map, merge, Observable } from 'rxjs';
import { GenerateBracket } from '@app/domain/bracket/bracket.actions';
import { TieBreaker } from '@app/domain/bracket/bracket.model';

@Component({
  selector: 'app-bracket-generate-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bracket-generate-modal.component.html',
  styleUrl: './bracket-generate-modal.component.scss',
  standalone: true,
  imports: [AsyncPipe, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule],
})
export class BracketGenerateModal {
  private readonly store = inject(Store);
  private readonly actions$ = inject(Actions);
  private readonly dialogRef = inject(MatDialogRef<BracketGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);

  readonly tieBreakerOptions = [
    { value: TieBreaker.POINTS_DIFF, label: 'Meilleure différence de points' },
    { value: TieBreaker.POINTS_SCORED, label: 'Plus de points marqués' },
    { value: TieBreaker.POINTS_TAKEN, label: 'Moins de points encaissés' },
  ];

  // Derived from the action stream, never stored: cleared when a generation starts,
  // set from the API errors when it fails.
  protected readonly errorMessage$: Observable<string | null> = merge(
    this.actions$.pipe(ofActionDispatched(GenerateBracket), map(() => null)),
    this.actions$.pipe(
      ofActionCompleted(GenerateBracket),
      map(({ result }) => result.error ? BracketGenerateModal.toErrorMessage(result.error) : null),
    ),
  );

  readonly form = this.fb.nonNullable.group({
    totalQualifiedTeams: [8, [Validators.required, Validators.min(2)]],
    tieBreaker: [TieBreaker.POINTS_DIFF as TieBreaker, [Validators.required]],
    startTime: ['14:00', [Validators.required, Validators.pattern(/^\d{2}:\d{2}$/)]],
    matchDurationMinutes: [20, [Validators.required, Validators.min(1)]],
    breakDurationMinutes: [5, [Validators.required, Validators.min(0)]],
  });

  submit(): void {
    if (this.form.invalid) return;
    const { totalQualifiedTeams, tieBreaker, startTime, matchDurationMinutes, breakDurationMinutes } = this.form.getRawValue();
    this.store.dispatch(new GenerateBracket(this.data.tournamentId, {
      totalQualifiedTeams,
      tieBreaker,
      startTime,
      matchDurationMinutes,
      breakDurationMinutes,
    })).subscribe({
      next: () => this.dialogRef.close(),
      // The failure is displayed through errorMessage$.
      error: () => undefined,
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private static toErrorMessage(error: unknown): string {
    const apiErrors: { message: string }[] | undefined = (error as { error?: { errors?: { message: string }[] } })?.error?.errors;
    return apiErrors && apiErrors.length > 0
      ? apiErrors.map(e => e.message).join(' ')
      : 'Erreur lors de la génération.';
  }
}
