# Field size: big stacked halves (mock, 2026-11-10)

After a ride on 2026-11-10: one of the fields was too big.

## Cause

The ride log shows the BETA page "SmallerFields": Field 1 (side by
side) and Field 2 (stacked) at 21 rows each. A page is 60 rows, so the Karoo stretches the last
field, Field 2, into the free 18 rows: about 478 × 467 px, each stacked half ~230 px tall.

A stacked half fits its **digits** to the room under the header (meant for halves with little
room). At 230 px that gives ~169 px digits, where a native Karoo field of the same size uses
~118 px and Field 1 above it ~113 px. Same rule in v0.1.0 (bitmap) and in the layout build.

## Proposal: cap at native size

A stacked half's number is never bigger than a native field of the half's size
(`KarooType.valueSize` with the native line area, baseline 14 px up). Where the digit fit is
smaller, it stays. In practice every full stacked half (≥ 100 px) then gets the native size.

| Picture | Left: today | Right: capped |
|---|---|---|
| 01 | BETA page as ridden (Field 2 stretched) | Field 2 like native |
| 02 | Stacked field on a half page (478 × 336), native fields below | same |
| 03 | 01 in dark | |

Rendered with the field's own layout code (FieldViews, `capStacked` switch) on the Karoo 3
emulator through the field gallery. Field 2's metrics in the mock: Speed / Lap time.

## Built (2026-11-10)

The cap is the rule in FieldViews and in the bitmap fallback (TileRenderer). Gallery redrawn: only
the stacked half-page pictures changed (separate-stacked-half, soft-zone-half and its sequence).
Not yet on a Karoo.
