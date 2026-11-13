# Schematic Scribing Table — Visual Asset Contract

## Status and scope

The owner approved a single-block, sloped drafting desk for the planned 0.3.0
schematic material-list workstation, then requested a stronger Create workshop
character. This document fixes the selected visual assets, not a functional
block contract or release declaration. The project version is unchanged.

The owner accepted the open-frame desk silhouette and independently authored
local art. After reporting successful item spawning and reviewing the appearance, the owner selected a lighter
16×16 wood-core pattern, retained the iron legs, chose golden-sheet fittings
instead of brass, and accepted one simple dark square nail per corner plate.
This selected finish is **C-01**. Preview approval and reported item spawning
do not establish the full client-rendering or gameplay acceptance matrix.

The original visual-only milestone did not implement gameplay. The current
worktree now integrates these assets into the functional workstation described by
the [gameplay contract](schematic-scribing-table.md), including registration,
inventory, menu, recipe and collision shapes. Visual tests alone do not establish
those behaviors. No kinetic requirement is introduced by the hand-feed model;
Ponder scenes remain separate future work.

## Approved silhouette and materials

- An inset timber desktop surrounded by a dark metal perimeter, tilted backward
  at the north edge so the south/front edge is lower.
- Four dark metal legs, lower stretchers and gray cast feet/joints. The central
  volume below the desktop stays genuinely empty; there is no full-cube shell or
  cabinet hidden by texture transparency.
- Golden-sheet corner plates with one flat dark square nail each, roller bearing blocks and a small right-side hand-feed
  crank. Machinery remains compact and has a plausible paper-handling purpose;
  no cannon barrel, chimney, large decorative gear or shaft connection is added.
- A blue schematic sheet on the left and a material clipboard on the right when
  their corresponding visual input is present.
- Original low-resolution pixel art for all eight materials. The lighter wood
  core uses warm pixel clusters and broad irregular grain, not outlined knots or
  regular floor-plank joints. Golden fittings have no miniature white-glinted
  studs. Every material is supplied under Fieldwork's own resource namespace.

The static blueprint line drawing and clipboard marks are illustrative texture
art, **not** a preview of a player's schematic or calculated material results.

## Bundled-texture permission boundary

After approving B-02, the owner requested local texture ownership. The initial
proposal to bundle Create's sprite was not applied; the owner chose independently
authored local artwork instead.

The official [Create 6.0.10 license](https://github.com/Creators-of-Create/Create/blob/mc1.21.1-6.0.10/LICENSE.md),
at commit `ac0c444d9828da3453ae8cc65338e8de063286fb`, explicitly places files
under `src/main/resources/assets/` under **All Rights Reserved**, copyright
The Create Team / The Creators of Create. Its MIT grant covers other files,
not this asset directory. The
[andesite-casing PNG](https://github.com/Creators-of-Create/Create/blob/mc1.21.1-6.0.10/src/main/resources/assets/create/textures/block/andesite_casing.png)
falls within that boundary; no file-specific redistribution exception was found.

Do not infer a redistribution grant from Create's code license, Fieldwork's GPL
license, attribution, namespace changes, cropping, or the existence of a rendered
review preview. C-01 does not bundle or reference those upstream pixels. Its
wood-top recipe uses original numeric colors and pixel-cluster coordinates with
the selected softer palette, with no image inputs, palette extraction, tracing
or dependency JAR reads. The preview images are regenerated exclusively
from Fieldwork assets as well. The permission blocker is resolved by selecting
this independent alternative, not by assuming that permission was granted.

## Native model structure

Resource namespace: `create_fieldwork`; base model ID:
`block/schematic_scribing_table`.

| Model suffix | Visible inserts |
|---|---|
| none | Empty desk |
| `_schematic` | Left schematic sheet |
| `_clipboard` | Right clipboard |
| `_schematic_clipboard` | Both inserts |

All four models contain identical base desk geometry. The item model points to
the empty desk. Gameplay integration now supplies four horizontal orientations
and two input-presence booleans, yielding 16 registered blockstate variants.

All desktop parts use the same native element rotation: X axis, 22.5 degrees,
origin `[8, 13, 8]`, without rescaling. All transformed vertices, including the
roller and crank, remain within `[0, 16]` on every axis. Inserts sit slightly above
the timber plane to avoid coplanar faces. Open-frame elements retain all faces
without `cullface` assumptions about solid neighboring blocks.

Production resources:

- [Base block model](../../src/main/resources/assets/create_fieldwork/models/block/schematic_scribing_table.json)
- [Populated model](../../src/main/resources/assets/create_fieldwork/models/block/schematic_scribing_table_schematic_clipboard.json)
- [Item model](../../src/main/resources/assets/create_fieldwork/models/item/schematic_scribing_table.json)
- [Eight Fieldwork PNG textures](../../src/main/resources/assets/create_fieldwork/textures/block/schematic_scribing_table/)

The desktop's `wood_top` alias resolves to
`create_fieldwork:block/schematic_scribing_table/wood_top`. Its upper face uses
the full `[0, 0, 16, 16]` sprite UV range. The fitting alias is `gold`, resolving
to the local `gold.png`; obsolete `brass.png` and its alias are removed. Apart
from material names and authoring labels, geometry remains unchanged. The six
other PNGs, including the iron frame/legs, are byte-identical to the previous
production revision. The particle sprite remains Fieldwork iron.

Blueprint art uses a 32×32 PNG; wood-top and the six remaining materials use 16×16
PNGs. All are opaque RGB images, with explicit per-face UVs. Create-only casing
resource-pack replacements no longer change this desktop; packs may instead
override the Fieldwork texture explicitly.

## Editing and reproduction

Production reads the JSON and PNGs in `src/main/resources`, not the art directory.
They can be edited directly; the authoring scripts are not Gradle generators.

The [art directory](../../art/schematic_scribing_table/) includes two distinct
operations:

```sh
# Explicitly replace the model/texture assets with the original art recipe.
python3 art/schematic_scribing_table/build_assets.py
# Read current production JSON/PNG assets and refresh the inspection images.
python3 art/schematic_scribing_table/render_preview.py
```

Both tools use only Python's standard library. Running the authoring tool will
overwrite manual edits to this desk's assets; preview generation does not. The
models can also be imported as Minecraft Java block/item models in Blockbench.
The authoring tool removes the specific superseded `brass.png` when generating
the selected golden-sheet material.
Neither operation requires or reads a dependency JAR. The preview loader rejects
non-Fieldwork texture namespaces, so an upstream reference cannot silently return.

The [model preview](../../art/schematic_scribing_table/preview.png) is rendered
from the actual asset geometry, UVs and nearest-neighbor PNG samples. C-01
emphasizes the empty desk to expose its
wood surface and shows populated views alongside it. The
[texture sheet](../../art/schematic_scribing_table/texture-sheet.png) enlarges
the eight original Fieldwork PNGs. These are supporting art, not an independent
source of gameplay authority. The current images replace the earlier casing-based
review previews rather than including upstream pixels in the art delivery.

## Evidence boundary

`ScribingTableAssetsTest` checks parent/reference resolution, PNG format and
dimensions, legal shared element rotations, transformed bounds, nondegenerate
UVs, the local-only material references, full wood-top UV range and selected
plain gold nail pixels, input-state
differences, paper separation and sampled under-desk empty space. `clean build`
runs those resource tests with the existing project checks;
archive inspection establishes that the resources ship without the art tooling.

The software preview uses its own orthographic rasterizer and simple directional
shading. It does **not** execute Minecraft model baking, ambient occlusion,
inventory display transforms, resource-pack replacement, or in-world lighting.
Those client checks remain pending. Functional material-list evidence is recorded
separately under the gameplay contract and production acceptance record. Ponder
acceptance remains future work; passing visual checks establishes neither.

### Selected C-01 finish — 2026-11-12

The selected wood and gold PNGs were compared pixel-for-pixel with the accepted
drafts before delivery. All model coordinates, rotations, faces and UVs remain
the same, apart from the material reference/name change. Production Java and
packaged class bytes are unchanged; all six unaffected textures are identical.
The canonical previews now read the production C-01 files, not private overrides.

The recipe replaces two brass sheets with two golden sheets; the deployed-item
and material-list behavior is unchanged. Current build, recipe rejection control,
independent-JVM persistence results and artifact hash are recorded only in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md#schematic-scribing-table-development-slice--2026-11-12).
After delivery, the owner reported that the new materials passed acceptance.
That is a player-reported appearance result; broader lighting/orientation/
resource-pack and GUI acceptance is not implied. See the central acceptance
record for the evidence boundary.

### Historical B-03 visual-slice checks — 2026-11-12

- `./scripts/gradle-docker.sh clean build` passed 124 JUnit tests, including eight
  desk resource checks, with no failures, errors or skipped tests.
- Comparison against B-02's archive confirmed identical geometry, inserts and
  seven other PNGs. Only the wood-top reference/UV and new authored PNG changed.
- Shipping isolation and archive inspection passed: four block models, one item
  model and eight local PNGs match main resources. The license is retained;
  upstream casing textures, art tools and non-shipping drivers are not bundled.
- Asset authoring and actual-resource preview generation are byte-identical on
  repetition. A negative control confirms that the preview loader rejects the
  superseded Create texture identifier. No dependency JAR is needed by either
  authoring or preview generation.
- Empty/populated views and the texture sheet were visually inspected. Source
  review confirmed the numeric wood-art recipe and absence of upstream pixel
  inputs; this describes the generation path, not a legal originality guarantee.
  Local documentation links, English prose and whitespace checks passed.
- Client model baking, inventory transforms and in-world lighting remain
  unexecuted; the B-03 image is a software preview for owner review.

### Historical B-02 visual-slice checks — 2026-11-12

These results describe the superseded runtime casing reference, not current C-01
texture ownership or preview tooling.

- `./scripts/gradle-docker.sh clean build` passed 124 JUnit tests, including eight
  desk resource checks, with no failures, errors or skipped tests. The dependency
  sprite is resolved from Create's owning module in the NeoForge test runtime.
- Comparison against the prior B-01 archive confirmed unchanged geometry, inserts
  and seven other PNGs; only the desktop texture reference/UV and particle sprite
  changed. The obsolete authored wood-top PNG was removed.
- Shipping isolation and archive inspection passed: four block models, one item
  model and seven Fieldwork PNGs match main resources. No upstream casing PNG,
  stale custom top, art tooling or non-shipping drivers are bundled.
- The preview reads the casing sprite from the hash-checked pinned Create JAR,
  including its indexed PNG palette. Authoring and preview regeneration were
  byte-identical on repetition. Empty/populated views and the wood crop were
  visually inspected; documentation link/content/whitespace checks passed.
- This remains software/resource evidence, not Minecraft client model baking or
  gameplay acceptance. Resource-pack replacement behavior was not exercised.

### Historical B-01 visual-slice checks — 2026-11-12

These counts describe the initial custom-wood version, not the later revisions.

- `./scripts/gradle-docker.sh clean build` passed: 123 JUnit tests, including the
  seven new desk resource checks, with no failures, errors or skipped tests.
- Shipping isolation passed. Archive inspection matched all four block models,
  the item model and eight PNGs byte-for-byte with main resources, confirmed the
  license, and found no art scripts/previews or non-shipping test drivers.
- Explicit asset authoring and preview regeneration were each byte-identical on
  repetition. The populated and empty desk views were visually inspected.
- Local documentation links, English prose and whitespace checks passed.
- Actual client model baking, inventory display and in-world lighting were not
  executed. No functional workstation or Ponder acceptance is claimed.
