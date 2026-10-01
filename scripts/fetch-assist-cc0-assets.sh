#!/usr/bin/env bash
set -euo pipefail

UPSTREAM_REPO="indecenti/NucleoOs"
UPSTREAM_COMMIT="e22e06e317be6c933b779ad7b055b6a6aeafa5e8"
BASE="https://raw.githubusercontent.com/${UPSTREAM_REPO}/${UPSTREAM_COMMIT}/assets/coop-rpg"
DEST="core/src/main/assets/environment/custom_tiles"

mkdir -p "$DEST"

fetch_and_verify_git_blob() {
  local url="$1"
  local out="$2"
  local expected_blob="$3"

  curl -L --fail --retry 3 --retry-delay 2 -o "$out" "$url"

  local size actual
  size="$(wc -c < "$out" | tr -d ' ')"
  actual="$(
    { printf 'blob %s\0' "$size"; cat "$out"; } |
      sha1sum | awk '{print $1}'
  )"

  if [[ "$actual" != "$expected_blob" ]]; then
    echo "Git blob verification failed for $out" >&2
    echo "expected: $expected_blob" >&2
    echo "actual:   $actual" >&2
    exit 1
  fi
}

fetch_and_verify_git_blob \
  "$BASE/tiny-dungeon/Tilemap/tilemap_packed.png" \
  "$DEST/assist_kenney_tiny_dungeon.png" \
  "f6e8b937c99fe45b1b75490cc050ee5929954259"

fetch_and_verify_git_blob \
  "$BASE/tiny-town/Tilemap/tilemap_packed.png" \
  "$DEST/assist_kenney_tiny_town.png" \
  "1655d1dfc918dbd450b192d95d8831b6f6155d89"

# Kenney RPG Urban Pack 1.0 (CC0), mirrored verbatim in a public repository.
# Keep the commit and Git blob pinned so Assist builds remain reproducible.
URBAN_REPO="AndrewDanyliuk/2d-rpg"
URBAN_COMMIT="67b203b6f365a97b34615cfdef2ed6e9a3bd55f3"
URBAN_BASE="https://raw.githubusercontent.com/${URBAN_REPO}/${URBAN_COMMIT}/assets/RPG%20Urban%20Pack"

fetch_and_verify_git_blob \
  "$URBAN_BASE/Tilemap/tilemap_packed.png" \
  "$DEST/assist_kenney_rpg_urban.png" \
  "66bf1156a23e2436473f96cf2240c301e4309faf"

echo "Verified Kenney Tiny Dungeon/Tiny Town/RPG Urban CC0 atlases from pinned upstream commits."
