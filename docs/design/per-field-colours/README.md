# Per-field colours and number weight (round 1, mockup)

Feedback: Number and Background were one setting for both fields; each field should have its
own (e.g. power with the Karoo zone background, heart rate with a zone-coloured number). Also:
the numbers look thin on the Karoo.

- Ride page with mixed colours (3s power Karoo background, HR zone number), light and dark.
- A · both fields on one page: a group per field (scrolls to the second).
- B · pick the field (3s power | Heart rate), its half gets a ring in the Example, then its
  Number and Background (fits one screen). Edit would get the same.
- Number weight: our Field 1 next to the Karoo's AVG SPEED (as on the map page), Regular (now,
  the Karoo's own weight) / Medium / SemiBold. Same weight as the Karoo's, but at half the size the
  strokes look thin.

Device: checked on the map page on 2026-11-08 (the picture shows the map, so it isn't public).
Files: `mockup.src.html` + `build.py` → `mockup.html`; `01-overview.png`. Decision: pending.

Picked (2026-11-08): **B** (pick a field, then its colours; also on Edit) and **Medium** numbers.

Built 2026-11-08 (964bbf2), demo on our Karoo.
- Field 1 with Medium numbers, checked on the map page (picture not public: it shows the map).

## Round 2

Feedback from the Karoo (964bbf2): changing a field's colours was hard, the page jumped up and
down and it was not clear which field was being edited.
- Fixed in 335ab5c (no design change): the page keeps its scroll position when a setting changes.
- Mock: the Example sticks to the top of Edit, the field being coloured has a ring and the other
  is faded; Number and Background on one line each. A: pick the field with two buttons (icon +
  name). B: tap the field in the Example ("Tap a field to change its colours"), section named
  "3s power colours". Decision: pending.
Picked (2026-11-08): **B**, tap the field in the sticky Example.

Round 2 B built (1b37def) with the hint above the Example and an Apple-blue outline; on our Karoo.
- `device/03-settings-list-light.png`: Your fields on the Karoo (dark mode at the time).
