# Assist third-party visual assets

## Kenney Tiny Dungeon / Tiny Town

Assist 0.5.8 uses a small subset of the 16×16 pixel-art atlases from:

- **Tiny Dungeon 1.0** — Kenney
- **Tiny Town 1.1** — Kenney
- License: **Creative Commons Zero (CC0 1.0)**
- Official pages:
  - https://kenney.nl/assets/tiny-dungeon
  - https://kenney.nl/assets/tiny-town

The upstream license files state that the content is free to use in personal,
educational and commercial projects and that attribution is not mandatory.

For reproducible builds, the Assist workflow fetches the packed PNG atlases
from public repository `indecenti/NucleoOs`, pinned to commit:

`e22e06e317be6c933b779ad7b055b6a6aeafa5e8`

Verified upstream Git blob IDs:

- Tiny Dungeon `Tilemap/tilemap_packed.png`:
  `f6e8b937c99fe45b1b75490cc050ee5929954259`
- Tiny Town `Tilemap/tilemap_packed.png`:
  `1655d1dfc918dbd450b192d95d8831b6f6155d89`

Used 16×16 sprites in 0.5.8:

- Tiny Town: bush (index 5), mushrooms (29), sign (83), barrel (107)
- Tiny Dungeon: crate (66)

Assist 0.5.8 used these sprites as sparse non-collision overlays.

Assist 0.5.9 / Generator V16 additionally arranges the same CC0 sprites into
non-interactive environment set-pieces. Bushes and mushroom clusters remain
visual-only, while selected signs, barrels and crates can occupy real solid
terrain cells via Terrain.CUSTOM_DECO. The art is still presentation-only:
there are no loot containers, attacks, pickups or scripted interactions tied to
these sprites.

V16 also mixes in Shattered Pixel Dungeon's own non-interactive solid statue
and region-decoration terrain for more scene variety. External Kenney sprites
never replace water, doors, merchants, items, creatures or gameplay objects.
