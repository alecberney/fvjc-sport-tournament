# UC-01 — Create Tournament

## Summary

The admin creates a new tournament by providing its basic parameters. The tournament is saved with status `DRAFT` and the admin is redirected to the tournament main page.

---

## Actor

Admin (single user type, no authentication required for now)

---

## Preconditions

None.

---

## Main Flow

1. Admin opens the create tournament form.
2. Admin fills in the tournament parameters (name, sport, number of fields, min/max players per team, date).
3. Admin submits the form.
4. System validates the input (see Validation Rules).
5. System creates the tournament with status `DRAFT`.
6. Admin is redirected to the tournament main page (UC-02).

---

## Tournament Parameters

| Field                | Type   | Required | Constraints                                      |
|----------------------|--------|----------|--------------------------------------------------|
| `name`               | String | Yes      | Max 250 characters                               |
| `sport`              | Enum   | Yes      | `PETANQUE`, `VOLLEY`, `LUTTE`, `TIR_A_LA_CORDE`, `FOOTBALL` |
| `numberOfFields`     | Int    | Yes      | Min 1, max 500                                   |
| `minPlayersPerTeam`  | Int    | Yes      | Min 1                                            |
| `maxPlayersPerTeam`  | Int    | Yes      | Must be >= `minPlayersPerTeam`                   |
| `date`               | Date   | Yes      | Today or future (no past dates)                  |

---

## Validation Rules

| Rule | Error message |
|------|---------------|
| `name` is blank | "Le nom est obligatoire" |
| `name` exceeds 250 characters | "Le nom ne peut pas dépasser 250 caractères" |
| `sport` is null | "Le sport est obligatoire" |
| `sport` is not a valid enum value | "Le sport sélectionné n'est pas valide" |
| `numberOfFields` is null | "Le nombre de terrains est obligatoire" |
| `numberOfFields` < 1 | "Le nombre de terrains doit être d'au moins 1" |
| `numberOfFields` > 500 | "Le nombre de terrains ne peut pas dépasser 500" |
| `minPlayersPerTeam` is null | "Le nombre minimum de joueurs est obligatoire" |
| `minPlayersPerTeam` < 1 | "Le nombre minimum de joueurs doit être d'au moins 1" |
| `maxPlayersPerTeam` is null | "Le nombre maximum de joueurs est obligatoire" |
| `maxPlayersPerTeam` < `minPlayersPerTeam` | "Le nombre maximum de joueurs doit être supérieur ou égal au minimum" |
| `date` is null | "La date est obligatoire" |
| `date` is in the past | "La date doit être aujourd'hui ou dans le futur" |

---

## Tournament Status Lifecycle

```
DRAFT → IN_PROGRESS
```

- `DRAFT`: tournament created, teams can be registered, parameters and groups can be updated, schedule can be generated and regenerated
- `IN_PROGRESS`: admin explicitly started the tournament (UC-05) — match results can now be entered

---

## API Contract

### Request

```
POST /api/tournaments
```

```json
{
  "name": "Tournoi FVJC 2025",
  "sport": "PETANQUE",
  "numberOfFields": 4,
  "minPlayersPerTeam": 2,
  "maxPlayersPerTeam": 4,
  "date": "2025-08-15"
}
```

### Response — Success `201 Created`

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Tournoi FVJC 2025",
  "sport": "PETANQUE",
  "numberOfFields": 4,
  "minPlayersPerTeam": 2,
  "maxPlayersPerTeam": 4,
  "date": "2025-08-15",
  "status": "DRAFT"
}
```

### Response — Validation Error `400 Bad Request`

```json
{
  "errors": [
    { "field": "name", "message": "Le nom est obligatoire" },
    { "field": "date", "message": "La date doit être aujourd'hui ou dans le futur" }
  ]
}
```

---

## Domain Impact

- Creates a new `Tournament` aggregate with a generated `TournamentId`.
- Persisted via `TournamentStore.save()`.
- No domain events raised at this stage.
