# UC-05 — Start Tournament & Enter Results

## Summary

The admin starts the tournament (transitioning it to `IN_PROGRESS`), which unlocks the results entry page. For each match in the schedule, the admin enters two integer scores. The system records the result and immediately recomputes the group ranking. Results can be corrected after entry.

---

## Actor

Admin

---

## Tournament Status Lifecycle (updated)

```
DRAFT → IN_PROGRESS
```

- The results page is not accessible while the tournament is in `DRAFT`.
- The "Start Tournament" button is disabled until the tournament is in `DRAFT` with a generated schedule.
- Once `IN_PROGRESS`, the tournament cannot go back to `DRAFT`.

---

## UC-05.1 — Start Tournament

### Summary

The admin clicks "Start Tournament" and confirms. The tournament transitions from `DRAFT` to `IN_PROGRESS`.

### Preconditions

- Tournament is in `DRAFT` status.
- A schedule has been generated (at least one round exists).

### Main Flow

1. Admin clicks "Start Tournament".
2. System shows a confirmation dialog.
3. Admin confirms.
4. Tournament status transitions to `IN_PROGRESS`.
5. Admin is on the results entry page.

### Validation Rules

| Rule | Error message |
|------|---------------|
| Tournament not in `DRAFT` | "Le tournoi est déjà démarré" |
| No schedule exists | "Impossible de démarrer le tournoi sans calendrier généré" |

### API Contract

```
POST /api/tournaments/{tournamentId}/start
```

No request body.

**Response — `200 OK`**

```json
{
  "id": "uuid",
  "name": "Tournoi FVJC 2025",
  "status": "IN_PROGRESS"
}
```

**Response — Validation Error `409 Conflict`**

```json
{
  "error": "Le tournoi est déjà démarré"
}
```

---

## UC-05.2 — Enter or Update Match Result

### Summary

The admin enters or corrects the score for a match. The system validates the scores, saves the result, and immediately recomputes the ranking for the group that match belongs to.

### Preconditions

- Tournament is in `IN_PROGRESS` status.
- Match exists and belongs to the tournament.

### Main Flow

1. Admin navigates to the results entry page, which displays the current round.
2. Admin can navigate to the previous or next round using back/forward arrows.
3. Admin enters the two scores for a match in the current round.
4. Admin submits.
5. System validates and saves the result.
6. System recomputes the group ranking.
7. Admin sees the updated result and ranking inline.

### Result Format

A result is always two non-negative integers:

| Field    | Type | Constraints     |
|----------|------|-----------------|
| `score1` | Int  | Min 0, max 500  |
| `score2` | Int  | Min 0, max 500  |

Draws are valid (e.g. 3-3).

### Ranking Points

| Outcome      | Points awarded |
|--------------|----------------|
| Win          | 2              |
| Draw         | 1 (each team)  |
| Defeat       | 0              |

### Validation Rules

| Rule | Error message |
|------|---------------|
| Tournament not `IN_PROGRESS` | "Les résultats ne peuvent être saisis que pour un tournoi en cours" |
| `score1` is null | "Le score de l'équipe 1 est obligatoire" |
| `score1` < 0 | "Le score ne peut pas être négatif" |
| `score1` > 500 | "Le score ne peut pas dépasser 500" |
| `score2` is null | "Le score de l'équipe 2 est obligatoire" |
| `score2` < 0 | "Le score ne peut pas être négatif" |
| `score2` > 500 | "Le score ne peut pas dépasser 500" |
| Match not found | "Match introuvable" |

### API Contract

Enter or update a result (same endpoint for both):

```
PUT /api/tournaments/{tournamentId}/matches/{matchId}/result
```

```json
{
  "score1": 3,
  "score2": 1
}
```

**Response — `200 OK`**

```json
{
  "match": {
    "id": "uuid-m1",
    "field": 1,
    "groupId": "uuid-group-a",
    "groupName": "A",
    "team1": { "id": "uuid-t1", "name": "Les Aigles" },
    "team2": { "id": "uuid-t2", "name": "Les Faucons" },
    "result": {
      "score1": 3,
      "score2": 1
    }
  },
  "ranking": {
    "groupId": "uuid-group-a",
    "groupName": "A",
    "entries": [
      {
        "rank": 1,
        "team": { "id": "uuid-t1", "name": "Les Aigles" },
        "played": 1,
        "wins": 1,
        "draws": 0,
        "defeats": 0,
        "goalsFor": 3,
        "goalsAgainst": 1,
        "goalDifference": 2,
        "points": 2
      },
      {
        "rank": 2,
        "team": { "id": "uuid-t2", "name": "Les Faucons" },
        "played": 1,
        "wins": 0,
        "draws": 0,
        "defeats": 1,
        "goalsFor": 1,
        "goalsAgainst": 3,
        "goalDifference": -2,
        "points": 0
      }
    ]
  }
}
```

**Response — Validation Error `400 Bad Request`**

```json
{
  "errors": [
    { "field": "score1", "message": "Le score ne peut pas dépasser 500" }
  ]
}
```

**Response — Tournament not IN_PROGRESS `409 Conflict`**

---

## UC-05.3 — Get Round

Returns the matches for a specific round, used for the round-by-round navigation. The response includes navigation metadata so the UI can enable/disable the back and forward arrows.

### API Contract

```
GET /api/tournaments/{tournamentId}/schedule/rounds/{roundNumber}
```

**Response — `200 OK`**

```json
{
  "round": {
    "id": "uuid-r2",
    "number": 2,
    "startTime": "2025-08-15T09:25:00",
    "isFirst": false,
    "isLast": false,
    "matches": [
      {
        "id": "uuid-m3",
        "field": 1,
        "groupName": "A",
        "team1": { "id": "uuid-t1", "name": "Les Aigles" },
        "team2": { "id": "uuid-t3", "name": "Les Loups" },
        "result": null
      },
      {
        "id": "uuid-m4",
        "field": 2,
        "groupName": "B",
        "team1": { "id": "uuid-t5", "name": "Les Faucons" },
        "team2": { "id": "uuid-t6", "name": "Les Aigles Noirs" },
        "result": { "score1": 2, "score2": 2 }
      }
    ]
  }
}
```

**Response — Not Found `404`** — round number does not exist for this tournament.

---

## UC-05.4 — Get Group Ranking

Returns the current ranking for a group, computed from all entered results so far.

### Ranking Computation

For each team in the group:
- `played` = number of matches with a result entered
- `wins`, `draws`, `defeats` = computed from results
- `goalsFor` / `goalsAgainst` = sum of scores across all played matches
- `goalDifference` = goalsFor - goalsAgainst
- `points` = (wins × 2) + (draws × 1)

**Ordering:** points DESC → goalDifference DESC → goalsFor DESC. Fully tied teams share the same rank.

Teams with no results yet appear at the bottom with all values at 0.

### API Contract

```
GET /api/tournaments/{tournamentId}/groups/{groupId}/ranking
```


**Response — `200 OK`** — same `ranking` structure as UC-05.2.

---

## Domain Impact

- `Match.result` is a value object `MatchResult(score1, score2)` — null until entered.
- Ranking is **not persisted** — it is computed on the fly from all `Match` results in the group.
- `PUT /matches/{id}/result` overwrites the existing result if one was already entered (no separate create/update endpoints).
- Entering or updating a result triggers a ranking recomputation for that group (returned inline in the response).
