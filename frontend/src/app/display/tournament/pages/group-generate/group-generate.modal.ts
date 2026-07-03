import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { GenerateGroups } from '@app/domain/group/group.actions';

@Component({
  selector: 'app-group-generate-modal',
  templateUrl: './group-generate.modal.html',
  styleUrl: './group-generate.modal.scss',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
})
export class GroupGenerateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<GroupGenerateModal>);
  private readonly fb = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly data = inject<{ tournamentId: string }>(MAT_DIALOG_DATA);
  private readonly destroy$ = new Subject<void>();

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      groupSize: [4, [Validators.required, Validators.min(2)]],
    });

    this.actions$.pipe(
      ofActionSuccessful(GenerateGroups),
      takeUntil(this.destroy$),
    ).subscribe(() => this.dialogRef.close());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;
    this.store.dispatch(new GenerateGroups(this.data.tournamentId, Number(this.form.value.groupSize)));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
