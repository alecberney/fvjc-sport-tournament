# UC-08 — Enter Knockout Stage Results

## Summary

The admin enters the result for a played knockout match. The winner is automatically placed into the next match in the bracket. For DEMI_FINALE matches, the loser is additionally placed into the TROISIEME_PLACE match. Draws are not allowed in knockout.

---

## Actor

Admin

---

## Preconditions

- A bracket has been generated (at least one BracketRound with matches exists).
- The match exists and both teams are set (not null).

---

## Domain Concepts

### Result entry

When a result is entered on a `BracketMatch`:

1. **Winner** → placed into the `nextMatch` (identified by `nextMatchId`), in team slot `nextMatchTeamSlot` (1 → team1, 2 → team2).
2. **Loser of a DEMI_FINALE** → placed into the `loserNextMatch` (identified by `loserNextMatchId`), in the same slot as `nextMatchTeamSlot`.
3. **FINALE** and **TROISIEME_PLACE** matches have no `nextMatchId` — they are terminal, no advancement happens.

The TROISIEME_PLACE match behaves exactly like any other knockout match — no draws, winner determined by score.

---

## Use Cases

### UC-08.1 — Enter Knockout Match Result

#### Validation Rules

| Rule | Error message |
|------|---------------|
| Match not found | "Match introuvable" |
| `score1` or `score2` null | "Les deux scores sont obligatoires" |
| `score1` or `score2` < 0 | "Le score ne peut pas être négatif" |
| `score1` or `score2` > 500 | "Le score ne peut pas dépasser 500" |
| `score1` == `score2` | "Un match éliminatoire ne peut pas se terminer sur un match nul" |

#### API Contract

```
PUT /api/tournaments/{tournamentId}/bracket/matches/{matchId}/result
```

```json
{
  "score1": 3,
  "score2": 1
}
```

**Response — `200 OK`** — returns the updated `BracketMatchDto`:

```json
{
  "id": "uuid-km1",
  "field": 1,
  "team1": { "id": "uuid-t1", "name": "Les Aigles" },
  "team2": { "id": "uuid-t5", "name": "Les Loups" },
  "result": { "score1": 3, "score2": 1 }
}
```

**Response — `400 Bad Request`** — validation error.

**Response — `404 Not Found`** — match not found.

---

## Domain Impact

- `BracketMatch.result` is set to the entered scores.
- Winner's `TeamRef` is copied into slot `nextMatchTeamSlot` of the `nextMatch` (if `nextMatchId` is not null).
- For DEMI_FINALE: loser's `TeamRef` is copied into the same slot of the `loserNextMatch` (if `loserNextMatchId` is not null).
- The `loserNextMatchId` field is set on DEMI_FINALE matches during bracket generation (UC-07.1).
