# Field gallery

SmallerFields at every size it's made for (quarter, small, row, half), light and dark, drawn by
`TileRenderer` on the Karoo 3 emulator with the showcase values (3s power 280 W = zone 4 at
FTP 280, heart rate 132 bpm = zone 2, speed 28.4, lap time 4:21). Data field boundaries on.

Looks: `separate-side` (3S | HR), `separate-stacked`, `single-side` (KM/H | LAP),
`karoo-bg` (Karoo zone background), `mixed` (power on the Karoo background, HR zone number), `not-set-up`, `soft-zone` (stacked, soft background, zone numbers).

- `fields/`: the field alone · `screens/`: on a Karoo page · `artboard/` (not in git): in the
  Karoo 3 frame on Sky / Night

```bash
bash ../design-assets/tools/field-gallery/gallery.sh karoo-smallerfields
```
