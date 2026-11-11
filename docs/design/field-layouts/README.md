# The field as a layout (mock, 2026-11-10)

On a map page with a route, the Karoo makes the top row shorter without telling the app (141 → 123
px on our test Karoo). Today SmallerFields draws one picture for the reported size and the Karoo
stretches it to the tile, so everything gets squashed in height, the header included
(`../map-squash/`). The proposal builds the field the way the Karoo builds its own: a layout of real
views. The header (icon, label) sits at KarooType's sizes and places, and the number is a text view in
the Karoo's font that auto-sizes to the room left. The Karoo lays it out at the height the tile really has.

All pictures are rendered in the field gallery with the app's real code (Android 12L, the Karoo's
fonts), at 2×.

- `01-map-row-native-today-layout-light.png`: the shortened map row (123 px). Columns: the Karoo's
  own AVG SPEED tile from a ride screenshot, SmallerFields today (squashed), the layout. The layout's
  header matches the native one: caps 20 px at 20 px from the top. Today's is 18 px.
- `02-normal-row-today-layout-light.png`: a normal small tile (141 px): today and the layout, side by side.
- `03-quarter-half-today-layout-light.png`: quarter and half tiles: today, layout, today, layout.
- `04-map-row-today-layout-dark.png`: the map row in dark mode: today and the layout.

Differences at normal size (otherwise pixel-close):
- The thickening of small numbers ("half way") isn't possible with a text view: small numbers come out
  in the Karoo's own weight, slightly thinner than today.
- Short stacked halves (a row split in two): numbers a little smaller than today.
- The Karoo's own number on the short map row sits lower (5 px from the bottom, not 12); ours keeps
  12. One screenshot so far: check on the Karoo before tuning.

Code: `FieldViews.kt` (layouts `field_side`, `field_stacked`, `field_half_*`; glyph drawables from
KarooType.Glyph). Not wired into the field yet: it still draws the picture until this is OK'd.

## Checked on our test Karoo (2026-11-10, build 0.1.0-55ca87c, BETA profile)

- `05-device-map-row-live-light.png`: the BETA map page with a route loaded and live values from
  FakeWatts. Field 1 (3S | HR) next to the Karoo's AVG SPEED: caps 20 px at y 83–102, native 84–103;
  the digits' ink ends on the same line as native (y 190). This row was full height (141 px), so it isn't
  the shortened case.
- `06-device-single-tile-live-light.png`: Field 1 switched to Single tiles in Settings, back on the
  ride page: redrawn, soft heart-rate fill with its rounded corner.
- `07-device-small-tile-light.png`: a small tile next to native SPEED: caps 23 px at the same y (567–589).
- Live for 48 s (FakeWatts Realistic): the number changed 39 times (native SPEED 31); the digits
  were 52 px tall in every frame, with no flicker, no blank frames and no size jumps.
- Settings: added Field 3, deleted it, changed Field 1's tiles and back: every change drew on the
  ride page. logcat: no "Class not allowed", inflation or RemoteViews errors.
- Not checked on the Karoo: the shortened row itself (on the ride it came with the lap drawer open;
  the gallery shows it), and dark mode (gallery only).
