# Values in Relative, like the Karoo (before/after, field gallery)

The Karoo's own values are Hammerhead Relative Mono 12 (`relative`), letterSpacing −0.04,
auto-sized to fill the space under the header (Karoo ALL: design-assets/karoo-type, KarooType.kt).
Before: Plex Medium, our own sizing. After: KarooType (Relative from the Karoo, never bundled).

- Side by side: bigger and fuller, closer to the Karoo's own numbers (half page much bigger).
- Stacked: smaller, because the space under the header in a half-height field is short and the
  Karoo's auto-size fits the whole line height. Needs a decision before building.

`01-before-after.png`. Decision: pending.

Round 2 (fbd07a9, on our Karoo): Relative everywhere; side by side the Karoo's auto-size; stacked
sized by digit height. `02-map-row-after.png` (map page on the Karoo), `03-next-to-karoo.png`
(before/after on the Karoo, and quarter / stacked next to the Karoo's own LAP AVG POWER from a ride shot).

Round 3: thicken smaller numbers so every size reads as well (Relative has one weight). Strokes
thickened by size, capped at 3.5 % of the size, with the digit gaps kept. `04-weight-options.png`:
as is / A half way / B even (capped), next to the Karoo's own quarter tile. Decision: pending.

Picked: **A** (half way). Fixes: stacked halves under 100 px tall (small / row tiles) put icon and
label on the left of one line and the number fills the half's height; half-page headers are the
Karoo's own size (they only looked small when shrunk on the board). `05-fixes-true-scale.png`.

Calibrated KarooType (workspace 93a9014): side-by-side values use valueSize(valueW, valueLineH),
baseline 14 px above the bottom, hyphens. Stacked quarter halves (81 px) fall under the short-half
layout. `06-calibrated.png` (true scale, gallery natives in Relative).

Steady size (2026-11-09): picked. Numbers sized by the widest normal value per field (power/HR/cadence 3 digits, speed/distance 88.8, lap 88:88, ride 8:88:88, ascent 4 digits). `13-steady-size.png`.
