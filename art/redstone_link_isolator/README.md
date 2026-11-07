# Redstone Link Isolator — Texture Templates

Each face is an independent 16×16 PNG. `up_active.png` supplies the active top.

| File | Minecraft block-local face |
|---|---|
| `north.png` | North, −Z |
| `south.png` | South, +Z |
| `east.png` | East, +X |
| `west.png` | West, −X |
| `up.png` | Top, +Y, inactive |
| `down.png` | Bottom, −Y |
| `up_active.png` | Top, +Y, active |

There is no independent `FACING` property. Textures rotate with the block/craft's local axes. `cube_net.png` is a 64×48 unfolded snapshot; `preview.png` is its 8× nearest-neighbor preview:

```text
         up
 west   north   east   south
        down
```

Edit the seven face PNGs. The unfolded images are viewing snapshots, not a Minecraft packed atlas; edits do not update them automatically.

## Game resources

These original generated placeholders use black/purple sides, a gold inactive top and a red active top. They are not copied from Minecraft or dependency JARs. They are already installed in main:

```text
src/main/resources/assets/create_fieldwork/textures/block/redstone_link_isolator/
```

The simplest workflow edits main PNGs directly. This art folder and sibling ZIP preserve original templates/previews and do not synchronize automatically with main.

The inactive model uses `minecraft:block/cube` with separate face references. The active model inherits it and overrides only `up`. `model.inactive.json` and `model.active.json` correspond to main's `models/block/redstone_link_isolator.json` and `redstone_link_isolator_active.json`. Blockstate/item models need no change.

## Commands

From the project root:

```sh
# Validate art
python3 scripts/export-isolator-textures.py --check
# Validate main PNGs and model references
python3 scripts/export-isolator-textures.py --check-installed
# Explicitly overwrite main's seven PNGs with art originals
python3 scripts/export-isolator-textures.py --install --force
# Regenerate placeholders, discarding art edits
python3 scripts/export-isolator-textures.py --force
```

The exporter uses only Python's standard library. Default generation writes art only and refuses existing PNGs. Only `--install` writes main PNGs; existing files require `--force`. Model JSON is not rewritten. `--check-installed` is read-only and allows art/main to differ.

Rebuild and fully close the game/server before replacing the JAR; recheck its hash. The ZIP is a template bundle, not an installable resource pack. The exporter does not build/replace JARs. See [integration evidence](../../docs/testing/placeholder-textures.md) for packaging checks and pending client rendering.
