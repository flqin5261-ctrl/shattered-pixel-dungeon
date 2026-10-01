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


## Assist 0.9.0 — Kenney RPG Urban Pack

Assist 0.9.0 adds a small, curated subset of **RPG Urban Pack 1.0** by
Kenney for modern/industrial Infinite World districts.

- License: **Creative Commons Zero (CC0 1.0)**
- Official page: https://kenney.nl/assets/rpg-urban-pack
- Tile size: 16×16
- The official pack contains 480+ files/tiles.

For reproducible CI builds, the packed atlas is fetched from public mirror
`AndrewDanyliuk/2d-rpg`, pinned to commit:

`67b203b6f365a97b34615cfdef2ed6e9a3bd55f3`

Verified Git blob:

- `assets/RPG Urban Pack/Tilemap/tilemap_packed.png`
- `66bf1156a23e2436473f96cf2240c301e4309faf`

Curated packed-atlas indices used by Assist:
- 164 curved street lamp
- 165 straight street lamp
- 166 red utility/hydrant fixture
- 168 blue utility fixture
- 221 / 222 road-work barriers
- 223 small street sign
- 250 freestanding signboard
- 254 refuse/debris bags
- 270 city bench
- 272 bollard/post
- 279 / 280 refuse bins

The source art remains unmodified. Assist uses these as sparse physical scenery
for office, industrial, suburban, city, and pool-hall districts.

## Assist 0.9.4 — purpose-built Backrooms material atlas

Assist 0.9.4 adds `environment/custom_tiles/assist_backrooms_materials.png`, a small
16×16-tile atlas created specifically for this private Assist build. It is not
third-party artwork. The atlas supplies distinct yellow carpet/walls, white pool
tile, blue water, concrete, hotel, cave, field, city, pastel, arcade, beach and
pink-house materials plus a small set of matching props. Its main purpose is to
prevent Backrooms districts from falling back to unrelated dungeon colors (most
notably red Poolrooms water) and to reduce repeated use of the same medieval props.

