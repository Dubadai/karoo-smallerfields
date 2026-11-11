# Proposal: field layouts in karoo-common

Status: proposal from SmallerFields, after its check on our test Karoo. Karoo ALL changes karoo-common
once it's agreed.

## Why

A field drawn as one bitmap with `fitXY` gets squashed whenever the Karoo gives the tile less room than
it reported, e.g. the top row of a map page with a drawer open (141 → 123 px). Its header then no longer
matches the native one. A layout of real views is laid out by the Karoo at the real size, like its own
fields.

## What to share

1. **Glyph drawables:** `glyph_<name>.xml`, made from `KarooType.Glyph` by a small script: a 38-unit
   viewport with a white fill, tinted with `setInt(id, "setColorFilter", colour)`.
2. **`FieldHeader`:** fills an icon ImageView and a label TextView for a tile `w × h` from KarooType's
   rules:
   - caps size: `headerCapsPx`; caps top: `headerCapsTop`;
   - icon box `caps × 25/23`, drawn at `size = box × 38/30` with margins `x − 4u`, `baseline − 34u`;
   - label top margin `capsTop − (ascent − capHeight)`, right margin `HEADER_RIGHT_INSET_PX`;
   - the label shrinks to the room left.
   It uses `RemoteViews.setViewLayoutMargin / Width / Height` (API 31; the Karoo is 32), with px units.
3. **`valueBox`:** the value TextView's margins, so its number lands where the Karoo puts its own at
   the reported size, and shrinks with the tile:
   - a tile: line area `tileH − 35`, baseline 14 px up. Bottom margin `14 − descent × size`, top margin
     `35 − bottom` (it can be negative).
   - a short half: the digits' ink fills the area under the header.
   The TextView: `fontFamily="relative"`, `letterSpacing=-0.04`, `includeFontPadding=false`, uniform
   autosize with 1 px steps, `gravity="bottom|end"`.
4. **`steadyText(value, widest)`:** the value with transparent "8"s in front (a ForegroundColorSpan
   survives RemoteViews), so autosize keeps one size as digits change.
5. **`emptyText`:** "No Sensor" as a TextView: label font at 25 px caps, its bottom 39 px up, shrunk to
   the width.
6. **Fills and cards:** zone backgrounds as an ImageView with a rounded shape and `setColorFilter`
   (no background tint needed). Card shapes have the tile's 14 px radius and 1 px #7D7D7D border.

## Pitfalls found

- Only FrameLayout, LinearLayout, TextView and ImageView are used (the Karoo refuses `View` / `Space`).
  A divider is an ImageView.
- Nested halves: `RemoteViews.addView(R.id.pair, half)`, so each half has its own ids.
- Thickening small numbers (stroke) isn't possible in a TextView: they come out in the Karoo's own weight.
- Fonts: `android:fontFamily="@font/…"` works for the app's own fonts, `"relative"` for the Karoo's.
- Before Android 12 keep the bitmap as the fallback (not a Karoo 3).
