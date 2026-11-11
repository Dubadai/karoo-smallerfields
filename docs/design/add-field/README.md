# Add field flow (round 1, mockup)

Feedback on the first build: the "Field 1 2 3 4" picker said nothing. Make it a flow instead:
add a field, give it your own name, choose its data, layout, tiles and colours; make as many as
you like.

Limit found in the SDK: the Karoo reads an app's field list once, from `extension_info.xml` in the
app, so an app can't add or rename fields later, and a field gets no id of its own on a page.
So BetterFields ships a pool of 10 fields (BetterFields 1–10); each field you make takes the next
free one, and the app tells you which to pick on the Karoo. Your name shows in the app only.

Screens: Your fields (empty, three fields) · Add field in 5 steps (Name, Data, Layout, Tiles,
Colours) · ready screen with where to put it · Edit (all on one page, Rename, Delete) · a pool
field with nothing set up on the Karoo ("Not set up yet").

Files: `mockup.src.html` + `build.py` → `mockup.html`; `01-flow-overview.png`. Decision: pending.

## Round 2

Feedback: no names. Each field is just its number; tell the rider to remember it, since that's
what they pick in the Karoo's list. Now: the list shows each field by its number (badge +
"BetterFields 1") and what it shows; Add field has 4 steps (Data, Layout, Tiles, Colours) with
the new number in the bar; after Save a big "This is BetterFields 4 / Remember the 4"; Edit has
no Rename; Delete frees the number. Decision: pending.

## Round 3

Feedback: the number showed up several times ("1" badge next to "BetterFields 1"). Now each
screen shows it once: the list is badge + what it shows; Add field says "New field" until the
end; the done card is the badge + "Remember this number" (and step 3 "Pick BetterFields 4");
Edit is badge + "Edit field". Decision: pending.

Empty state (round 3, after a shared reference picture): no page title; three made-up fields
drawn as tilted, stacked cards (number badge, what it shows, mini tile, arrow) so it reads as an
example, then "No fields yet" and one line on what a field is.

## Round 4

Feedback: write "Field 1" as the title, since that is what you pick later. The Karoo fields
become Field 1 to Field 10 (under DDB BetterFields), and the app uses the same names: list rows
"Field 1" + what it shows (no badge), empty-state cards, Edit title, "Field 4 / Remember the
name" after Save, "Pick Field 4", and FIELD 4 on a tile that is not set up. Decision: pending.

Empty state, 4 options (same words: "No fields yet / Press “Add field” to create your first
BetterField."; arrows removed from the example cards): A tilted example cards, B a small Karoo
page with a dashed + spot, C one big example tile marked EXAMPLE, D the list in outline with a +.
Decision: pending.
Picked (2026-11-08): **A · tilted example cards**.

Built 2026-11-08 (9544bd7) and installed on our Karoo (demo build).
- `device/01-empty-light.png`: Your fields, empty, on the Karoo.
