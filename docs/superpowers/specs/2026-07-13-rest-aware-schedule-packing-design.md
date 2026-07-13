# Rest-Aware Schedule Packing — Design

**Date:** 2026-07-13
**Status:** Approved (pending spec review)
**Area:** Backend — `schedule` domain (`ScheduleService.packRounds`)

## Problem

During a real tournament, the generated schedule produced unfair rest patterns:
some teams played two matches in consecutive rounds (back-to-back, no rest),
while other teams waited 2 or 4 rounds idle and then also played several rounds
in a row.

The current `packRounds` algorithm only guarantees that **no team plays twice
within the same round**. It has no notion of *rest between consecutive rounds*.
Matches are prioritized purely by remaining-degree (teams with the most games
left go first), so nothing prevents a team from being scheduled in round `r`
and again in round `r + 1`.

## Goal

Spread each team's matches as evenly as possible across rounds — avoid
back-to-back play and long idle stretches — **while still filling every field
in every round whenever the constraints allow it**.

## Hard Constraint

**Fill fields always.** Filling every field in every round is a hard rule. A
field is never intentionally left empty. The number of rounds stays minimal
(we still pack as many parallel matches as the "no team twice per round" rule
allows). Rest fairness is optimized *only* among the choices that preserve full
fields. When the only remaining eligible matches involve a team that just
played, we still take them to keep the field busy.

## Approach (chosen: A — Rest-Aware Greedy)

A drop-in replacement for the match-selection ordering inside `packRounds`.
Instead of sorting remaining matches purely by remaining-degree, we make the
selection **rest-aware**.

Alternatives considered and rejected:
- **B. Two-phase pack-then-balance** (pack maximally, then local-search swap
  matches between rounds to minimize a fairness cost). More optimal, but much
  more complex and harder to unit-test.
- **C. ILP / constraint solver.** Overkill; adds a dependency.

## Detailed Design

Modify `packRounds` (and its helper ordering) as follows:

1. **Track last-played round per team.** Maintain a
   `Map<TeamId, Integer> lastPlayedRound`, holding the index of the most recent
   round each team appeared in (`-1` / absent = never played yet). `roundIndex`
   is the 0-based index of the round currently being filled.

2. **Score each remaining match by rest.** A team's *rest* = `roundIndex -
   lastPlayedRound[team]` (large = rested a long time / never played). A match's
   priority is driven by the **minimum rest across its two teams**, so the more
   "starved" team drives selection and gets scheduled sooner.

3. **Penalize back-to-back.** A match where *either* team played in the
   immediately previous round (`lastPlayedRound[team] == roundIndex - 1`) is
   pushed to the back of the ordering — it is only picked to fill a field that
   would otherwise sit empty.

4. **Tie-break by remaining-degree.** Among matches with equal rest standing,
   keep the current criterion (higher combined remaining-degree first). This
   preserves the minimal round count — teams with more games left still get
   scheduled first.

5. **Greedy field fill (unchanged).** Iterate the ordered matches, adding a
   match to the round only if neither team is already used this round, until all
   fields are filled or no eligible match remains.

6. **Update `lastPlayedRound`.** After a round is built, set
   `lastPlayedRound[team] = roundIndex` for every team that played in it.

### Ordering key (conceptual)

Sort remaining matches ascending by a composite key so the best match sorts
first:

- **Primary:** back-to-back flag (false before true) — non-back-to-back first.
- **Secondary:** minimum rest across the two teams, descending — most-rested
  first.
- **Tertiary:** combined remaining-degree, descending — busiest teams first
  (preserves minimal round count).

## Comments Requirement

Per user request, the implementation must include explanatory comments in the
code making the rest-aware selection logic clear — what each ordering criterion
means and why (min-rest drives selection, back-to-back penalty, degree
tie-break preserves round count).

## Testing

Unit tests on `ScheduleService.generate` (existing test style, Mockito +
JUnit 5). Existing tests must still pass unchanged:
- Field-fill and round-count assertions (minimal rounds preserved).
- No team twice per round.

New test(s):
- A scenario where the old algorithm produced a back-to-back, asserting the new
  schedule gives the affected team at least one round of rest between its
  matches when the field-fill constraint permits it — i.e. assert that no team
  plays in two consecutive rounds unless unavoidable given full fields.

## Out of Scope

- No change to round-robin match generation (`generateGroupMatches`).
- No change to round timing, persistence, or the API/DTO layer.
- No change to the "fill fields always" hard constraint.
