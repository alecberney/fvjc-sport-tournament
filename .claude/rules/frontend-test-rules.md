# Frontend Test Rules

## Scope

Unit tests are written for **domain service static methods only** — the transformation and business logic functions.  
NGXS state, actions, components, and pages are not unit-tested at this stage.

---

## Why Static Methods Only

Domain service transformation methods are static pure functions — they take data in and return data out, with no side effects and no Angular dependencies. This means:

- No `TestBed` setup needed.
- No mocking needed.
- Tests are plain Jest — fast, simple, zero Angular overhead.

---

## Test Structure

Every unit test follows a 3-part structure (no `when`/`verify` since there are no mocks):

```typescript
it('should return select options from tournaments', () => {
  // setup
  const tournaments = TournamentFakes.aList(3);

  // call
  const result = TournamentDomainService.toSelectOptions(tournaments);

  // assert
  expect(result).toHaveLength(3);
  expect(result[0]).toEqual({ value: tournaments[0].id, label: tournaments[0].name, trackId: tournaments[0].id });
});
```

| Part | Purpose |
|------|---------|
| `// setup` | Build test data using Fakes |
| `// call` | Invoke the static method under test — exactly one call |
| `// assert` | Assert the return value with `expect` |

---

## Test File Structure

Test files live alongside the domain service, suffixed with `.spec.ts`:

```
src/app/tournament/domain/
├── tournament.domain.service.ts
├── tournament.domain.service.spec.ts
├── tournament.model.ts
└── tournament.fakes.ts
```

```typescript
// tournament.domain.service.spec.ts
describe('TournamentDomainService', () => {

  describe('toSelectOptions', () => {
    it('should map tournaments to select options', () => {
      // setup
      const tournaments = TournamentFakes.aList(2);

      // call
      const result = TournamentDomainService.toSelectOptions(tournaments);

      // assert
      expect(result).toHaveLength(2);
      expect(result[0].value).toBe(tournaments[0].id);
      expect(result[0].label).toBe(tournaments[0].name);
    });

    it('should return empty array when no tournaments', () => {
      // setup — (empty, no data needed)

      // call
      const result = TournamentDomainService.toSelectOptions([]);

      // assert
      expect(result).toEqual([]);
    });
  });

  describe('isDraft', () => {
    it('should return true when tournament status is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.isDraft(tournament);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.isDraft(tournament);

      // assert
      expect(result).toBe(false);
    });
  });
});
```

- One `describe` per service.
- One nested `describe` per method tested.
- One `it` per scenario (happy path + edge cases).

---

## Fakes

Fakes build domain model objects with sensible defaults. Named `XxxFakes`, one per feature, in the same folder as the domain files.

```typescript
// tournament.fakes.ts
export class TournamentFakes {

  static aTournament(overrides?: Partial<Tournament>): Tournament {
    return {
      id: 'tournament-id-1',
      name: 'Tournoi FVJC 2025',
      sport: Sport.PETANQUE,
      numberOfFields: 4,
      minPlayersPerTeam: 2,
      maxPlayersPerTeam: 4,
      date: new Date('2025-08-15'),
      status: TournamentStatus.DRAFT,
      ...overrides,
    };
  }

  static aDraftTournament(): Tournament {
    return TournamentFakes.aTournament({ status: TournamentStatus.DRAFT });
  }

  static anInProgressTournament(): Tournament {
    return TournamentFakes.aTournament({ status: TournamentStatus.IN_PROGRESS });
  }

  static aList(count: number, overrides?: Partial<Tournament>): Tournament[] {
    return Array.from({ length: count }, (_, i) =>
      TournamentFakes.aTournament({ id: `tournament-id-${i + 1}`, name: `Tournoi ${i + 1}`, ...overrides })
    );
  }
}
```

- `aTournament(overrides?)` — base factory with optional field overrides via spread.
- Named variants (`aDraftTournament`, `anInProgressTournament`) for common scenarios.
- `aList(count)` — generates N distinct instances with incremented IDs and names.
- Default values are realistic, valid domain objects.
- Fakes are in `src/app/` source tree (not a separate test folder) since Angular colocates spec files.

---

## Test Method Naming

```
should [expected outcome] when [condition]
```

Examples:
- `should map tournaments to select options`
- `should return empty array when no tournaments`
- `should return true when tournament status is DRAFT`

---

## Rules Summary

| Rule | Detail |
|------|--------|
| Test scope | Domain service static methods only |
| Test runner | Jest |
| Structure | `setup` → `call` → `assert` (3 parts, with comments) |
| No mocking | Static pure functions need no mocks |
| No TestBed | No Angular setup — plain Jest |
| Fakes | Class named `XxxFakes` colocated with domain files |
| Fake base method | `aXxx(overrides?)` with spread for overrides |
| `describe` grouping | One per service, one nested per method |
| Test naming | `should [outcome] when [condition]` |
| Assertions | Jest `expect` |
