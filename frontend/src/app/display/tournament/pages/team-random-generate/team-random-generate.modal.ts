import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { GenerateRandomTeams } from '@app/domain/team/team.actions';

@Component({
  selector: 'app-team-random-generate-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './team-random-generate.modal.html',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
})
export class TeamRandomGenerateModal implements OnInit {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TeamRandomGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroyRef = inject(DestroyRef);

  readonly form = this.fb.group({
    count: [16, [Validators.required, Validators.min(1), Validators.max(200)]],
  });

  ngOnInit(): void {
    this.actions$.pipe(
      ofActionSuccessful(GenerateRandomTeams),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.dialogRef.close());
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateRandomTeams(this.data.tournamentId, Number(this.form.value.count)));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
