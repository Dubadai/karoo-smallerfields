"""Inline the Karoo frame, icons, fonts and karoo-device.js into mockup.html.
Run: python3 build.py  (from this folder)
"""
import base64, json, pathlib
here = pathlib.Path(__file__).parent
assets = here.parents[3] / 'design-assets'          # Karoo/design-assets
def data_url(p, mime='image/png'): return f'data:{mime};base64,' + base64.b64encode(p.read_bytes()).decode()
icons = {p.stem: data_url(p) for p in sorted((assets / 'tools/field-gallery/icons').glob('*.png'))}
fonts = assets / 'fonts'
html = (here / 'mockup.src.html').read_text()
html = html.replace('/*__KAROO_DEVICE_JS__*/', (assets / 'karoo-device.js').read_text())
html = html.replace('__FRAME__', data_url(assets / 'karoo-3-frame.png'))
html = html.replace('"__ICONS__"', json.dumps(icons))
for key, f, mime in [('REGULAR', 'IBMPlexSans-Regular.ttf', 'font/ttf'), ('MEDIUM', 'IBMPlexSans-Medium.ttf', 'font/ttf'),
                     ('SEMIBOLD', 'IBMPlexSans-SemiBold.ttf', 'font/ttf'), ('COND', 'IBMPlexSansCondensed-Medium.otf', 'font/otf')]:
    html = html.replace(f'__PLEX_{key}__', data_url(fonts / f, mime))
(here / 'mockup.html').write_text(html)
print('wrote', here / 'mockup.html')
