# Map page: the header squashed (2026-11-10)

On a ride page with the map and a route, the Karoo makes the top row shorter without telling the
app. Here that was 123 px on screen, while the field drew for the size the Karoo reported (141 px, a
small tile). The field is one picture stretched to fit (`fitXY`), so all of it shrank by 0.87, but
only in height:
- the labels' capitals came out 17 px instead of 20, with their baseline 5 px above the native one;
- the widths stayed native (HR 31 px wide, "No Sensor" 99 px).
Measured on a ride screenshot from our test Karoo, 2026-11-10, 07:28.

`01-native-today-proposal-light.png`, at 3×, shows:
- left: the Karoo's own AVG SPEED tile;
- middle: SmallerFields today, the whole picture squashed;
- right: the proposal, where the header strip stays 1:1 (native size and baseline) and only the
  part under it shrinks to fit what's left.

The proposal draws the header and the rest as two pictures in the field's layout: the header with a
fixed height at the top, the rest filling the remaining height.
