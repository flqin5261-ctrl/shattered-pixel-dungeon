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

These sprites are used only as sparse non-collision Infinite World decoration
overlays. They do not replace Shattered Pixel Dungeon's terrain, water, doors,
items, creatures or gameplay objects.

## Assist 0.5.9 expanded scenery usage

The same pinned CC0 atlases are reused; no additional external pack is introduced.

Tiny Town indices used:
- 4 green pine
- 5 round green tree
- 10 amber pine
- 11 round amber tree
- 17 fern
- 29 mushrooms
- 57 sealed wooden crate
- 59 fence post
- 71 wooden post
- 81 fence section
- 83 weathered sign
- 95 warning marker
- 105 rock
- 106 fallen log
- 107 barrel
- 130 empty tub
- 131 water trough

Tiny Dungeon indices used:
- 29 torch
- 42 rubble
- 63 bookshelf
- 64 stone cross
- 65 gravestone
- 66 old casket
- 72 table
- 73 stool
- 74 stone basin
- 75 cupboard
- 79 iron railing
- 120 cold firepit
- 122 weapon display

0.5.9 separates pass-through ground clutter from bulky physical scenery. In
Generator V16 worlds, bulky scenery is backed by the game's native
`Terrain.CUSTOM_DECO` solid terrain while the Kenney sprite remains a
`CustomTilemap` visual. These props remain non-interactive.
