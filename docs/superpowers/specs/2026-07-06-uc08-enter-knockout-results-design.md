# UC-08 — Enter Knockout Stage Results: Design

## Overview

UC-08 adds result entry for knockout bracket matches with automatic winner/loser advancement. It also extends the bracket generation (UC-07) to produce a TROISIEME_PLACE round and wire `loserNextMatchId` on DEMI_FINALE matches.

---

## Backend

### 1. Domain model — `BracketMatch`

Add one new field:

```java
BracketMatchId loserNextMatchId;  // null for all matches except DEMI_FINALE
```

### 2. DB migration

Add column to `bracket_matches`:

```sql
ALTER TABLE bracket_matches ADD COLUMN loser_next_match_id UUID;
```

### 3. Bracket generation — TROISIEME_PLACE round

When `totalRounds >= 2` (a DEMI_FINALE exists):
- Pre-generate a TROISIEME_PLACE match ID before the main loop.
- In the DEMI_FINALE loop iteration (`r == totalRounds - 2`, where `teamsInRound == 4`), set `loserNextMatchId` on each match to the TROISIEME_PLACE match ID.
- After the main loop, save a "Troisième place" round (number = `totalRounds + 1`) with one match (teams null, no `nextMatchId`, no `loserNextMatchId`).
- The loser slot in TROISIEME_PLACE mirrors `nextMatchTeamSlot`: match with slot 1 → loser goes to team1, match with slot 2 → loser goes to team2.

### 4. New domain request

```java
// bracket/domain/BracketMatchResultRequest.java
@Value @Builder
public class BracketMatchResultRequest {
    Integer score1;
    Integer score2;
}
```

### 5. Validator — `BracketValidator`

Add `validateBracketMatchResult(BracketMatchResultRequest request)`:

| Rule | Error |
|------|-------|
| score1 or score2 null | "Les deux scores sont obligatoires" |
| score1 < 0 or score2 < 0 | "Le score ne peut pas être négatif" |
| score1 > 500 or score2 > 500 | "Le score ne peut pas dépasser 500" |
| score1 == score2 | "Un match éliminatoire ne peut pas se terminer sur un match nul" |

### 6. Service method — `BracketService.enterResult`

```java
public BracketMatch enterResult(UUID tournamentId, UUID matchId, BracketMatchResultRequest request)
```

Steps:
1. `validateBracketMatchResult(request)`
2. Load match via `bracketMatchStore.findById(matchId)` → 404 if absent
3. Determine winner/loser from scores
4. Save match with `result` set
5. If `nextMatchId != null`: load next match, set team in slot `nextMatchTeamSlot`, save
6. If `loserNextMatchId != null`: load loser match, set loser team in slot `nextMatchTeamSlot`, save
7. Return the saved match

### 7. New endpoint

```
PUT /api/tournaments/{tournamentId}/bracket/matches/{matchId}/result
```

- Request body: `BracketMatchResultRequestDto { score1: @NotNull Integer, score2: @NotNull Integer }`
- Response: `200 OK` with `BracketMatchDto`

### 8. Tests — `BracketServiceTest`

New test cases:
- `enterResultWhenValidShouldAdvanceWinnerToNextMatch`
- `enterResultWhenDemiFinaleLoserShouldAdvanceToTroisiemePlace`
- `enterResultWhenDrawShouldThrowValidationException`
- `enterResultWhenScoreNegativeShouldThrowValidationException`
- `enterResultWhenScoreOverMaxShouldThrowValidationException`
- `enterResultWhenMatchNotFoundShouldThrowNotFoundException`
- `enterResultWhenFinaleShouldNotAdvanceAnyone` (terminal match)

---

## Frontend

### API layer

- `BracketMatchResultRequestDto { score1: number; score2: number }` added to `bracket.api.dto.ts`
- `bracketApiService.submitBracketMatchResult$(tournamentId, matchId, score1, score2)` — `PUT` call, returns `BracketMatchDto`
- Mapper: `toSubmitResultRequest(score1, score2): BracketMatchResultRequestDto`

### Domain layer

- New action: `EnterBracketMatchResult { tournamentId: string; matchId: string; score1: number; score2: number }`
- State `@Action(EnterBracketMatchResult)`: call API, on success dispatch `LoadBracket` to refresh the full bracket

### Display layer

The `tournament-bracket.page` gains result entry inline:
- `selectedMatchId: string | null` — tracks clicked match
- `score1: number | null`, `score2: number | null`
- `selectMatch(match)` — sets `selectedMatchId`, resets scores
- `submitResult()` — dispatches `EnterBracketMatchResult`, clears selection on success

**Template changes:**
- Match rows become clickable (`[class.selected]`, `(click)="selectMatch(match)"`)
- A detail panel appears alongside (or below) the bracket table when a match is selected, showing score inputs and a "Enregistrer" button — only rendered when both teams are non-null and result is null
- After successful submission, the bracket reloads and the selected match now shows its score

---

## Constraints

- No result re-entry guard: results can be overwritten (spec does not prohibit it).
- The loser slot in TROISIEME_PLACE uses the same `nextMatchTeamSlot` as the winner slot — no additional field needed.
- TROISIEME_PLACE and FINALE are terminal: no advancement, service simply saves the result.
