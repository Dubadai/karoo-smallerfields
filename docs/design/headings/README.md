# Headings that match the Karoo's (mockup)

Feedback: Medium numbers are good; headings should match the Karoo's own tiles when side by side.

- Now: labels 26 px (24 stacked), icons at 75 % (70 %): smaller than the Karoo's.
- Match the Karoo: labels and icons as big and placed as the Karoo's own header for a tile of the
  same height (28 px font under 150 px tall, else 31.875, icon full size; the field gallery's
  measured sizes, same as the Karoo's own on device). The app's lap icon is one glyph.

Shown next to the Karoo's own fields: map page row (141 px), quarter page (168 px), stacked, and a
ride page. Files: `mockup.src.html` + `build.py` → `mockup.html`; `01-overview.png`. Decision: pending.

Built 2026-11-08: headers 1:1 with the Karoo's own field header rules (playbook "Writing data
fields": capitals 20 / 23 px, top 21 px at 142 px tall to 26 px from 168 px, text 9 px from the
right, icon in a 25 px box 12 px from the left on the baseline), sized by each half's height.
- Checked on the Karoo next to its AVG SPEED on the map page row: same capital height, baseline and
  margins (the device picture shows the map, so it isn't public).

## Stacked: header on the left (mockup)

Feedback: in stacked, put icon and label on the left so their size stays the Karoo's, and make the
numbers bigger. Headers sized by the whole tile (identical to the Karoo's header next to it),
the number fills the right side. A: header top left. B: header centred on the number.
Long labels (KM/H) leave less room for the number. `02-stacked-left.png`. Decision: pending.
