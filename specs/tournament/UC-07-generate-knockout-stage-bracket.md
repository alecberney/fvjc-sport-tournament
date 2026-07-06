# UC-07 — Generate Knockout Stage Bracket

## Summary

Once all group stage results are entered, the admin triggers the knockout stage by specifying the starting round. The system computes how many teams qualify per group, seeds 1st-place finishers, and randomly draws 2nd-place (and lower) finishers against them. The admin can manually swap teams in the bracket before play begins. Knockout matches follow a standard single-elimination bracket, including a third-place match generated automatically.

---

## Actor

Admin

---

## Preconditions for starting knockout

- Tournament is in `IN_PROGRESS` status.
- All group stage matches have a result entered.

---

## Domain Concepts

### Knockout Stage

| Starting Round       | Teams needed |
|----------------------|-------------|
| 64ème de finale      | 128         |
| 32ème de finale      | 64          |
| Seizième de finale   | 32          |
| Huitième de finale   | 16          |
| Quart de finale      | 8           |
| Demi-finale          | 4           |
| Finale               | 2           |

### KnockoutMatch

| Field             | Type    | Description                                            |
|-------------------|---------|--------------------------------------------------------|
| `id`              | UUID    | —                                                      |
| `round`           | Enum    | `FINALE`, `TROISIEME_PLACE`, `DEMI_FINALE`, `QUART`, `HUITIEME`, etc. |
| `position`        | Int     | Match position within the round (1-based)              |
| `team1Id`         | UUID?   | Null if not yet determined (waiting for prior result)  |
| `team2Id`         | UUID?   | Null if not yet determined                             |
| `result`          | null    | Filled in when match is played                         |
| `nextMatchId`     | UUID?   | Match the **winner** advances to (null for `FINALE` and `TROISIEME_PLACE`) |
| `loserNextMatchId`| UUID?   | Match the **loser** advances to — set only for `DEMI_FINALE` matches, pointing to the `TROISIEME_PLACE` match |

---

## Qualifier Computation

Given `stageSize` (teams needed) and `numberOfGroups`:

```
baseQualifiers = floor(stageSize / numberOfGroups)
extraSpots     = stageSize % numberOfGroups
```

- All groups send `baseQualifiers` teams.
- `extraSpots` additional qualifier slots are filled by the best `extraSpots` teams ranked at position `baseQualifiers + 1` across all groups (compared by points → goalDifference → goalsFor).

**Example — 7 groups, huitième de finale (16 teams):**
```
baseQualifiers = floor(16 / 7) = 2
extraSpots     = 16 % 7 = 2
→ All 7 groups send top 2 (14 teams)
→ Best 2 third-place finishers across all groups fill the last 2 spots
```

**Example — 4 groups, quart de finale (8 teams):**
```
baseQualifiers = 2, extraSpots = 0
→ All 4 groups send top 2 (8 teams exactly)
```

---

## Seeding Algorithm

1. Collect all 1st-place finishers — these are **seeded** (placed in the top half of each bracket match).
2. Randomly assign each 2nd-place finisher to face a 1st-place finisher from a **different group**.
3. If extra qualifiers exist (3rd-place etc.), fill remaining bracket positions randomly, avoiding same-group pairings where possible.
4. Build the full bracket: winners advance to the `nextMatch` in the next round (positions connect in standard bracket order).
5. Create one `TROISIEME_PLACE` match with both team slots null — losers of both semi-finals are placed there as their results are entered.

---

## Use Cases

### UC-07.1 — Generate Knockout Bracket

#### Validation Rules

| Rule | Error message |
|------|---------------|
| Tournament not `IN_PROGRESS` | "Le tournoi doit être en cours pour démarrer les phases éliminatoires" |
| Any group match has no result | "Tous les matchs de la phase de groupes doivent être terminés" |
| `stage` is null | "La phase de départ est obligatoire" |
| `stage` is not a valid value | "La phase de départ n'est pas valide" |
| Not enough teams to fill stage | "Pas assez d'équipes qualifiées pour cette phase (besoin de X, disponibles Y)" |
| Bracket already exists | Existing bracket is deleted and replaced (allowed only if no knockout result entered) |

#### API Contract

```
POST /api/tournaments/{tournamentId}/bracket/generate
```

```json
{
  "stage": "HUITIEME"
}
```

**Response — `201 Created`**

```json
{
  "startingRound": "HUITIEME",
  "totalRounds": 4,
  "bracket": [
    {
      "round": "HUITIEME",
      "matches": [
        {
          "id": "uuid-km1",
          "position": 1,
          "team1": { "id": "uuid-t1", "name": "Les Aigles", "groupName": "A", "groupRank": 1 },
          "team2": { "id": "uuid-t5", "name": "Les Loups", "groupName": "C", "groupRank": 2 },
          "result": null,
          "nextMatchId": "uuid-km9"
        },
        {
          "id": "uuid-km2",
          "position": 2,
          "team1": { "id": "uuid-t3", "name": "Les Faucons", "groupName": "B", "groupRank": 1 },
          "team2": { "id": "uuid-t7", "name": "Les Tigres", "groupName": "D", "groupRank": 2 },
          "result": null,
          "nextMatchId": "uuid-km9"
        }
      ]
    },
    {
      "round": "QUART",
      "matches": [
        {
          "id": "uuid-km9",
          "position": 1,
          "team1": null,
          "team2": null,
          "result": null,
          "nextMatchId": "uuid-km13"
        }
      ]
    },
    {
      "round": "DEMI_FINALE",
      "matches": [...]
    },
    {
      "round": "DEMI_FINALE",
      "matches": [...]
    },
    {
      "round": "TROISIEME_PLACE",
      "matches": [
        {
          "id": "uuid-km-3p",
          "position": 1,
          "team1": null,
          "team2": null,
          "result": null,
          "nextMatchId": null,
          "loserNextMatchId": null
        }
      ]
    },
    {
      "round": "FINALE",
      "matches": [...]
    }
  ]
}
```

**Response — Validation Error `400 Bad Request`**

**Response — `409 Conflict`** if any knockout result exists.

---

### UC-07.2 — Get Knockout Bracket

```
GET /api/tournaments/{tournamentId}/bracket
```

**Response — `200 OK`** — same structure as UC-07.1 response.

**Response — `200 OK` (no bracket yet)**

```json
{ "bracket": [] }
```

---

### UC-07.3 — Swap Two Teams in Bracket

The admin swaps two teams in the first round of the bracket before any knockout match is played. Both teams swap their position and opponent.

#### Preconditions

- No knockout match result has been entered yet.

#### Validation Rules

| Rule | Error message |
|------|---------------|
| Either team not in bracket | "Équipe introuvable dans le tableau" |
| A knockout result already entered | "Impossible de modifier le tableau après la saisie d'un résultat" |

#### API Contract

```
POST /api/tournaments/{tournamentId}/bracket/swap
```

```json
{
  "teamId1": "uuid-t1",
  "teamId2": "uuid-t5"
}
```

**Response — `200 OK`** — returns the full updated bracket (same structure as UC-07.1).

---

## Domain Impact

- `BracketMatch` entries are linked by `nextMatchId` and `loserNextMatchId`.
- `TROISIEME_PLACE` and `FINALE` have no `nextMatchId` — they are terminal matches.
- The TROISIEME_PLACE round is created automatically during generation whenever totalRounds ≥ 2 (i.e., a DEMI_FINALE exists).
- Generating the bracket is destructive (deletes prior bracket) only if no result has been entered.
- Swapping is only allowed before the first knockout result is entered.
