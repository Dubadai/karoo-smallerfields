# First look (round 1)

BetterFields: two data fields in one Karoo tile, in the Karoo's own field style (icon top left,
capitals top right, number bottom right), smaller.

Shown (light | dark, quarter page, showcase numbers 280 W / 132 bpm / 28.4 / 4:21):

- A · Side by side, B · Stacked
- Zone colours: Number (native-safe) and Background (needs our own coloured shape: the karoo-ext
  SDK can't colour an extension's tile, the reason BigWatts dropped its Background in v0.1.1)
- Settings: Example card, Field 1, Field 2, Layout, Zone colours

Files: `mockup.src.html` + `build.py` → `mockup.html`; `01-round1-overview.png` (render).
Decision: pending.

## Round 2

Feedback on round 1: support both layouts (side by side and stacked); colour options as before
(black/white or coloured numbers, coloured background, or both); background either strong like the
Karoo or soft and tonal. Zone backgrounds wanted even though the Karoo SDK can't colour our tile.

Settings: Layout (Side by side / Stacked), Number (Black / white / Zone colour), Background
(None / Karoo / Soft). Every combination is shown as bare tiles, light and dark.

- HR: the Karoo's own HR zone colours (ride app climb1..7, from BetterIntervals). Power:
  BigWatts' palette, not yet measured against the Karoo's power field.
- Karoo + zone number: the number in a dark shade of its zone (weak contrast, shown as is).

Files: round 1 in `round1/`; `02-round2-overview.png` (render). Decision: pending.

### Karoo zone colours (2026-11-08)

Read from the Karoo ride app's code: power and HR zones use ONE colour table (`climb1…7`:
`#60EEB2 #00B988 #FFF500 #FB8C65 #FE581F #D60404 #B700A2`, same in light and dark, black text),
picked by how many zones the rider profile has (class `u8.d`, used by the element renderer
`t7.g.i` for both `getPowerZones` and `getHeartRateZones`): 7 zones (power) → all seven,
5 zones (HR) → c1 c2 c3 c4 c6. BigWatts' palette (grey, blue, green, yellow…) is not the Karoo's.
The mockup now uses the Karoo's table for both.

## Round 3

Feedback: no line in the middle; make the tile look like two small Karoo tiles (background,
rounding). Then: add "separate tiles or a single tile" as an option.

- **Tiles: Separate** (new default): our bitmap fills the field with the page grey and draws each
  half as its own card (radius 14, 1 px #7D7D7D border), 2 px gap between columns, 6 px between
  rows, like the Karoo's own tiles. The outer edges line up with the Karoo's tile.
- **Tiles: Single**: one tile, a thin line between the halves (round 2 look).
- Settings: Layout, Tiles, Number, Background.
- Open point: with data field boundaries off on the Karoo, the grey gap would not match.

`03-round3-overview.png` (render). Decision: pending.
- Single: the line now runs top to bottom (1 px, the Karoo's border grey #7D7D7D).
- Boundaries: the Karoo tells each field `ViewConfig.boundariesEnabled` (karoo-ext 1.1.2+;
  BetterIntervals uses it). On: Separate draws the two cards. Off: the Karoo draws no cards, so
  Separate draws none either (two fields, nothing between); Single keeps its line. The off page is
  drawn from the gallery's stock fields without cards, not yet checked on the device.
- `ViewConfig.alignment` (Left / Center / Right, the rider's field alignment) is there too.
- Short labels, icons carry the meaning: 3s power → `3S` (bolt), lap time → `LAP` (lap-clock icon),
  speed → its unit `KM/H` (or `MPH`), HR stays `HR`. Labels 26 px side by side / 24 px stacked,
  icons at 75 % / 70 % of the Karoo's (were 22 px, 60 %). Every field needs a short label in the build.
