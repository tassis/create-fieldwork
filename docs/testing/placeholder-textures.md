# Six-Face Placeholder Texture Integration — 2026-11-07

## Current selected resources (2026-11-08)

Production now uses the [selected gray/brass rim pair](../../art/isolator_variants/SELECTED.md)
for ordinary and smart isolators. Their 14 face PNGs are all model-referenced;
the inner 12×12 pixels and both complete top states retain the original design.

At the owner's request the unused instrument, casing and wood-rim drafts, previews
and ZIPs were removed. Original art snapshots remain references only; production
reads the PNGs under `src/main/resources`. The shipping JAR SHA-256 remained
unchanged during the recorded selection; see
[current server evidence](v02-server-binding.md). Real client display remains pending.

## Historical original-texture integration

The owner approved installing the original generated art from `art/redstone_link_isolator/` into main. This historical slice changed visual assets only, not Java, recipe, blockstate or isolation behavior. Its hash identifies a `0.1.0-dev` candidate, not `0.1.0` after version preparation.

## Editing and model layout

The game reads `src/main/resources/assets/create_fieldwork/textures/block/redstone_link_isolator/`:

- `north.png`, `south.png`, `east.png`, `west.png`, `up.png`, `down.png`: independent 16×16 faces.
- `up_active.png`: active 16×16 top.
- Inactive model: `minecraft:block/cube`, individual face references, north particle texture.
- Active model: inherits inactive and overrides only `up`; item model and active blockstate variants remain unchanged.

Edit main PNGs directly, rebuild and replace the JAR after fully closing the
game/server. Art and ZIP files preserve historical template/unfolded-preview
snapshots and do not synchronize with production resources.

## Recorded verification

- The recorded resource check verified all seven main PNG dimensions/pixel data
  and six-face/ACTIVE model references.
- Clean build/JUnit85 and main-only network148 passed with complete save/state close: `.audit/placeholder-textures-{build,network}.log`.
- Build versus prior installed scoped-refresh JAR: **all 58 Java class paths and bytes identical**, `.audit/placeholder-textures-class-check.log`.
- Archive included seven PNGs/new models and excluded drivers/faults/counters: `.audit/placeholder-textures-archive.log`.
- Installer ready `03:59:16Z`; status had no tokens; console stop `04:00:07Z` saved all dimensions; exited/exit0/OOMfalse. Logs: `.audit/placeholder-textures-smoke-{setup,launch,stop}.log`.
- Build/installed SHA-256 matched: `c6536ce872e8acf97b1253f6304c09c7a1fa7b305ef1afb9912077477381ab4a`; raw evidence `.audit/placeholder-textures-artifacts.log`.

**Client model baking/actual rendering was not accepted in this pass.** PNG/reference checks, JAR inclusion and server readiness do not establish in-game display. Art is original generated placeholder work, not copied vanilla/dependency images.
