# Tournament Create Modal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the `/create` route with a `MatDialog` modal opened from a "Créer un nouveau tournoi" button on the tournament list page.

**Architecture:** The existing `TournamentCreatePage` is converted to a modal component (`TournamentCreateModal`) that injects `MatDialogRef` to close itself on successful creation. The list page injects `MatDialog` and opens the modal on button click. The `/create` child route is removed.

**Tech Stack:** Angular 19 (standalone), Angular Material 19 (`MatDialog`, `MatDialogRef`), NGXS, Reactive Forms.

## Global Constraints

- All components are standalone.
- Use `inject()` everywhere — no constructor injection.
- No tests for components/pages — frontend test rules cover domain service static methods only.
- Follow existing naming conventions: pages stay `*.page.ts`, modal becomes `*.modal.ts`.
- `provideAnimationsAsync()` is required for `MatDialog` and must be added to `app.config.ts`.

---

### Task 1: Add `provideAnimationsAsync` to app config

**Files:**
- Modify: `frontend/src/app/app.config.ts`

**Interfaces:**
- Produces: animations available app-wide (required by `MatDialog`)

- [ ] **Step 1: Add `provideAnimationsAsync` to `app.config.ts`**

Replace the content of `frontend/src/app/app.config.ts`:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideStore } from '@ngxs/store';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
    provideStore([]),
    provideAnimationsAsync(),
  ],
};
```

- [ ] **Step 2: Verify the app still compiles**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -5
```

Expected: `Build at: ... - Hash: ...` with no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/app/app.config.ts
git commit -m "feat: add provideAnimationsAsync for Angular Material dialog support"
```

---

### Task 2: Convert create page to a modal component

**Files:**
- Create: `frontend/src/app/tournament/display/pages/tournament-create/tournament-create.modal.ts`
- Create: `frontend/src/app/tournament/display/pages/tournament-create/tournament-create.modal.html`
- Delete: `frontend/src/app/tournament/display/pages/tournament-create/tournament-create.page.ts`
- Delete: `frontend/src/app/tournament/display/pages/tournament-create/tournament-create.page.html`

**Interfaces:**
- Consumes: `CreateTournament` action, `MatDialogRef<TournamentCreateModal>`
- Produces: `TournamentCreateModal` component (opened by list page in Task 3)

- [ ] **Step 1: Create `tournament-create.modal.ts`**

```typescript
import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { Actions, ofActionSuccessful, Store } from '@ngxs/store';
import { Subject, takeUntil } from 'rxjs';
import { Sport } from '@app/tournament/domain/tournament.model';
import { CreateTournament } from '@app/tournament/domain/tournament.actions';

@Component({
  selector: 'app-tournament-create-modal',
  templateUrl: './tournament-create.modal.html',
  standalone: true,
  imports: [ReactiveFormsModule],
})
export class TournamentCreateModal implements OnInit, OnDestroy {

  private readonly store = inject(Store);
  private readonly dialogRef = inject(MatDialogRef<TournamentCreateModal>);
  private readonly formBuilder = inject(FormBuilder);
  private readonly actions$ = inject(Actions);
  private readonly destroy$ = new Subject<void>();

  readonly sports = Object.values(Sport);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(250)]],
      sport: [null, Validators.required],
      numberOfFields: [null, [Validators.required, Validators.min(1), Validators.max(500)]],
      minPlayersPerTeam: [null, [Validators.required, Validators.min(1)]],
      maxPlayersPerTeam: [null, Validators.required],
      date: [null, Validators.required],
    });

    this.actions$.pipe(
      ofActionSuccessful(CreateTournament),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.dialogRef.close();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.form.invalid) return;

    const { date, ...rest } = this.form.value;
    this.store.dispatch(new CreateTournament({
      ...rest,
      date: new Date(date),
    }));
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
```

- [ ] **Step 2: Create `tournament-create.modal.html`**

```html
<form [formGroup]="form" (ngSubmit)="submit()">
  <div>
    <label for="name">Nom du tournoi</label>
    <input id="name" type="text" formControlName="name" />
  </div>

  <div>
    <label for="sport">Sport</label>
    <select id="sport" formControlName="sport">
      <option [value]="null" disabled>Choisir un sport</option>
      @for (sport of sports; track sport) {
        <option [value]="sport">{{ sport }}</option>
      }
    </select>
  </div>

  <div>
    <label for="numberOfFields">Nombre de terrains</label>
    <input id="numberOfFields" type="number" formControlName="numberOfFields" />
  </div>

  <div>
    <label for="minPlayersPerTeam">Joueurs min par équipe</label>
    <input id="minPlayersPerTeam" type="number" formControlName="minPlayersPerTeam" />
  </div>

  <div>
    <label for="maxPlayersPerTeam">Joueurs max par équipe</label>
    <input id="maxPlayersPerTeam" type="number" formControlName="maxPlayersPerTeam" />
  </div>

  <div>
    <label for="date">Date</label>
    <input id="date" type="date" formControlName="date" />
  </div>

  <button type="button" (click)="cancel()">Annuler</button>
  <button type="submit" [disabled]="form.invalid">Créer le tournoi</button>
</form>
```

- [ ] **Step 3: Delete the old page files**

```bash
rm frontend/src/app/tournament/display/pages/tournament-create/tournament-create.page.ts
rm frontend/src/app/tournament/display/pages/tournament-create/tournament-create.page.html
```

- [ ] **Step 4: Verify the app compiles**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -10
```

Expected: Build succeeds (there will be an error about `TournamentCreatePage` still referenced in routes — that is fixed in Task 3).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/tournament/display/pages/tournament-create/
git commit -m "feat: convert tournament create page to MatDialog modal component"
```

---

### Task 3: Update routes and wire button on list page

**Files:**
- Modify: `frontend/src/app/tournament/modules/tournament.routes.ts`
- Modify: `frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.ts`
- Modify: `frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.html`

**Interfaces:**
- Consumes: `TournamentCreateModal` from Task 2, `MatDialog`

- [ ] **Step 1: Remove the `/create` route from `tournament.routes.ts`**

```typescript
import { Routes } from '@angular/router';
import { provideStates } from '@ngxs/store';
import { TournamentState } from '@app/tournament/domain/tournament.state';
import { TournamentListPage } from '@app/tournament/display/pages/tournament-list/tournament-list.page';

export const TOURNAMENT_ROUTES: Routes = [
  {
    path: '',
    providers: [provideStates([TournamentState])],
    children: [
      { path: '', component: TournamentListPage },
    ],
  },
];
```

- [ ] **Step 2: Update `tournament-list.page.ts` to open the modal**

```typescript
import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngxs/store';
import { Observable } from 'rxjs';
import { Tournament } from '@app/tournament/domain/tournament.model';
import { TournamentState } from '@app/tournament/domain/tournament.state';
import { LoadTournaments } from '@app/tournament/domain/tournament.actions';
import { TournamentCreateModal } from '@app/tournament/display/pages/tournament-create/tournament-create.modal';

@Component({
  selector: 'app-tournament-list-page',
  templateUrl: './tournament-list.page.html',
  standalone: true,
  imports: [AsyncPipe],
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
}
```

- [ ] **Step 3: Update `tournament-list.page.html` to add the button**

```html
<button type="button" (click)="openCreateModal()">Créer un nouveau tournoi</button>

<p>Tournament list</p>
```

- [ ] **Step 4: Verify the app compiles and runs**

```bash
cd frontend && npx ng build --configuration development 2>&1 | tail -5
```

Expected: `Build at: ...` with no errors.

- [ ] **Step 5: Manual smoke test**
  1. Start the app: `cd frontend && npx ng serve`
  2. Open the browser at `http://localhost:4200`
  3. Verify the "Créer un nouveau tournoi" button is visible on the list page
  4. Click the button — a modal should open with the creation form
  5. Click "Annuler" — the modal should close
  6. Click the button again, fill in the form, and submit — the modal should close and the new tournament should appear in the list

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/tournament/modules/tournament.routes.ts \
        frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.ts \
        frontend/src/app/tournament/display/pages/tournament-list/tournament-list.page.html
git commit -m "feat: add create tournament button and open creation form as dialog"
```
