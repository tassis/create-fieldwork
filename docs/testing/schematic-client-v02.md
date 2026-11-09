# v0.2 schematic comparison — implementation evidence and player checks

Status: **implementation/headless gate passed; owner reports current Iris/ghost
follow-up OK; detailed client acceptance remains partial**.
The owner chose to perform GUI testing. No graphics-container dependencies were
added. This is a local working-tree candidate, not a released v0.2 artifact;
metadata remains `0.1.0`. The current product boundary is defined by the
[behavior contract](../design/behavior-contract.md).

## Latest scope clarification: offhand assistance only

The owner explicitly wants ordinary main-hand blueprint use to remain free of
Fieldwork assistance. Earlier candidates limited opacity/masking to offhand but
still ran comparison and extra HUD for main hand. The current candidate also
gates template capture, comparison discovery/refresh, craft-bound snapshots,
red/orange outlines and extra HUD on the exact active offhand selection. Leaving
that mode clears prior assistance once; idle/main-hand ticks do not repeatedly
clear the cache or do comparison world reads.

Controller-priority dispatch against the offhand blueprint and early sync flush
before inventory actions are likewise restricted to offhand mode. Controller and
native lectern right-clicks yield to vanilla without a synthetic second use. Normal native
controller interaction/server binding is a separate feature. Main-hand Create
preview/tool/HUD continues; both valid hands still select main and disable this
auxiliary mode. Hand selection/sync support remains installed, so this is not a
claim that no main-hand method is ever intercepted.

Build/JUnit101, shipping isolation and main-only schematic344 passed on this
scope-only candidate; phase report `2026-11-08T03:23:15Z`, complete save/close
`03:23:22Z`. Blend/mask implementation is unchanged from the owner-passed Iris
candidate below, but this new main/offhand mode switch still needs a client check.

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
1b37b3142488bf20b9598b27c6a12f6b0dd6d461c2c22d87711c9718aa54d053
```

A later tooltip-only copy update rebuilt the same JAR path with SHA-256
`4cacab82982de34d536be50c8de2f5afa563b775f0e53583778725aeba2b3e79`.
It removes the train tooltip and shortens both block descriptions to their
features; the Traditional Chinese localization uses the owner-approved wording
for “physical structure.”
Build/JUnit101/shipping isolation passed at `04:37Z`; no schematic or
client runtime check was newly executed for that copy-only update.

## Current follow-up: Iris and hiding completed ghosts (2026-11-08)

The owner reports that the first alpha candidate flickers and loses transparency
with **Iris**, then requests hiding correctly placed ghost blocks. Iris version,
shader pack/version and exact flickering surfaces have not yet been established.
The previous terrain-translucent approach is therefore **not accepted with Iris**.

Current candidate changes:

- Offhand ordinary ghost layers use native `entityTranslucent(block atlas)`
  rather than terrain `translucent()`. The native SBB writer was checked to emit
  overlay, light and normal attributes required by the entity vertex format.
  The owner subsequently reports this follow-up OK; the agent has not independently
  reproduced the Iris visual result.
- A source-coordinate mask hides only fresh, loaded, in-range ordinary-world
  `MATCH` candidates. Missing, wrong, unsupported, stale, ambiguous or excluded
  positions keep the preview. The mask is built from the existing bounded cache;
  rendering performs no additional actual-world block-state queries.
- The original native renderer is retained. Its per-block virtual-state read
  skips masked blocks during mesh rebuild, and its existing BE walk skips masked
  entities without shifting native indices/error bits. Both mirrored virtual
  variants are mapped back to original source coordinates with native size-1
  offsets. Actual world blocks/items and schematic data are not removed.
- New hiding is coalesced every20 ticks (about1 second after comparison discovers
  the match). Invalidated matches are restored on the next comparison tick;
  restores can rebuild sooner. Animation, selection/world/resource resets and
  uncertainty clear the mask. Main-hand rendering receives an empty mask and the
  original buffer.
- Because Create rebuilds a whole native mesh, hiding is limited to templates
  with **bounding volume ≤32,768 cells** (including air). At most4096 source
  positions are masked. Candidate-ring eviction disables hiding until cache reset
  to avoid repeated full rebuild churn. The HUD reports volume/cache limits;
  comparison outlines continue for larger/partial schematics. These are
  engineering guards, not measured frame-time claims.

New evidence: clean build, shipping isolation and **JUnit101**, zero
failures/errors/skips; `schematic` **344** at `02:54:21Z`, full saving/state close
`02:54:27Z`. New checks cover native mirrored source mapping, BE ordinal/enablement
preservation, mask coalescing/restoration, overflow/packed-coordinate rejection
and exact native hook counts. Read-only review accepted the structural/headless
remediation. After the current candidate below was supplied, the owner confirmed
that the Iris/opacity and completed-ghost follow-up worked. Record this as a
player-reported pass for that tested setup, not an agent-executed visual check or
every reset/mirror/shader combination. No new full server suite or installer run
is claimed for this client follow-up.

A final render-time guard also checks the current world/player, settled animation,
anchor/rotation/mirror before using a tick snapshot, covering input changes between
ticks without reading world blocks. Required build/JUnit101/shipping isolation
were rerun at `02:58Z`; the native344 phase above predates only these client guards.

Owner-reported Iris candidate (before the scope-only change above):

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
f0f3a49f3b5d6dcf73411d30d27fc5f55d395c8d498dbf44ed3e02d1cc3ce809
```

## First offhand opacity candidate (2026-11-08; Iris issue reported)

The owner reported that the prior candidate worked, then requested a slightly
transparent offhand ghost to distinguish missing / wrong block / wrong supported
orientation / correct construction. This is a general player report, not a
per-row completion of the matrix below. The owner subsequently reported the Iris
issue described above.

That first **offhand** candidate submitted ordinary chunk-layer ghost vertices
at alpha166/255 (approximately65% opacity) into the native blended translucent
layer. Main-hand selection passes the original buffer through. All three native
normal/mirrored renderer branches use the scoped adapter. The native cached mesh,
global shader color, real-world blocks, red/orange outlines and HUD are not tinted.
No preview rebuild is required just to change hands.

Custom block-entity render types with their own textures/shaders retain native
rendering; they are not remapped to the block atlas. Their special/animated parts
may consequently remain opaque. This adjustment does not claim uniform alpha for
every mod-specific renderer. Check ordinary blocks, leaves/glass, mirrors and any
special parts used by the player's schematic.

New verification: `./scripts/gradle-docker.sh build` passed **JUnit98**, zero
failures/errors/skips, including vertex attributes/RGB preservation, packed/default
vertex methods and alpha multiplication, followed by unmodified native emission.
An ASM read of the pinned Create bytecode confirms the exact three hook calls
without loading the client. `schematic` **329** passed again at `02:31:29Z`, complete
save/state close at `02:31:35Z`; dedicated-server class isolation passed. These do
not establish actual client mixin application or blended visual quality.

Updated working-tree JAR (same path, metadata0.1.0):

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
603530de8d96e517b92858b854238b67b52a0323b801c77b03160f2744965784
```

The full server/installer recheck documented below belongs to the earlier
`307aca…` candidate; it was not rerun in full for this client-only adjustment.

## Implemented boundary

- A valid Create schematic in the main hand wins. Otherwise the actual offhand
  stack, inventory slot **40**, supplies selection, deployment, HUD and native
  synchronization. Same filename does not identify a deployment: components,
  slot, player, world, anchor, rotation and mirror participate in invalidation.
- The comparison reuses the template Create has already loaded for its ghost.
  It does not parse another file or place another full virtual structure.
  Native `calculateRelativePosition`, then the anchor once, determines positions;
  native state mirror/rotation is applied once. Render interpolation is not a
  target grid. Rotation/mirror/translation animation clears comparison outlines
  until the preview settles.
- Wrong block type is **red**; supported construction-state differences are
  **orange**. Facing, horizontal facing, axis, horizontal axis, half, double-block
  half and slab type are supported. Power, light, waterlogging, open/enabled/active
  states are ignored. Other properties remain unverified, not full-state errors.
  Block-entity NBT is not compared. Actual air keeps Create's waiting ghost;
  expected air does not detect excess world blocks. Fluids/replacement semantics
  and arbitrary mod-specific construction states are not a comprehensive validator.
- Only loaded ordinary-world positions are compared. Reserved Sable plot
  coordinates and projected craft bounds are excluded. These bounds are **hint
  exclusions, never server authorization or ownership answers**.
- Each tick captures at most **256 crafts** from native metadata, not craft
  blocks. It projects local plot bounds through the current pose instead of
  trusting potentially stale cached global bounds. Moving poses use a conservative
  rotation-safe swept envelope between previous/current poses. This can suppress
  hints near a ship even where there is no actual ship block. Unknown/invalid
  metadata or more than 256 crafts pauses comparison, rather than assuming WORLD.
- There are no comparison-specific server packets or chunk force loads. Native
  Create placement packets remain responsible for syncing deployment changes.
  Expected sync echoes have a bounded 32-entry slot-specific history.
- Crouch/controller right-click is dispatched through the native interaction
  path before the offhand schematic consumes it. Whether native input, HUD and
  other mods coexist correctly still needs the player's real-client check below.

## Engineering limits, not measured performance claims

| Work | Hard limit |
|---|---:|
| Template entries considered per tick | 128 |
| Dirty + periodic candidate revisits per tick | 128 combined |
| Actual comparison block-state reads per tick | 256 combined |
| Cached candidate positions | 4096 |
| Submitted error outlines per tick | 128 |
| Player distance | 64 blocks |
| Result age before hiding stale errors | 40 ticks |
| Supported template | one palette, ≤1,000,000 entries, each extent ≤512 |
| Craft metadata bounds captured per tick | 256; above this comparison pauses |

Discovery is round-robin, and an over-capacity candidate set uses a fixed ring.
The partial HUD reports cached candidates / template entries, **not completed
validation coverage**; template entries include air and excluded targets. A large
or mostly distant/air schematic can take many ticks to revisit relevant entries.
Dirty positions take refresh priority. Outliner draws a bounded cached snapshot;
render/HUD handlers do not scan blocks. No FPS/MSPT improvement has been measured.

## Newly executed evidence (2026-11-08 UTC)

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic
```

- Clean build passed; JUnit **95**, zero failures/errors/skips, including four
  comparison budget/selection tests. Shipping isolation passed: research,
  acceptance and binding-prototype driver/config/metadata absent from the JAR.
- Main-only `schematic` phase **329 assertions**, report timestamp
  `2026-11-08T00:46:38.417617486Z`; all dimensions saved `00:46:44Z` and verifier
  checked state close. Raw reports: `run-fieldwork-acceptance/result.json` and
  `result-schematic.json`; JUnit: `build/test-results/test/` (local ignored output).
- A non-symmetric 3×2×4 native template uses stairs, observer, log and slab.
  All three mirrors × four rotations × two translated anchor/pivot cases compare
  positions, block types and supported orientation against native world placement.
  Multi-palette input is neutral, not guessed.
- Actual native `SchematicSyncPacket` handling exercises slot 40 while preserving
  a distinct same-filename main-hand deployment. Echo recognition, one-time
  consumption, other-slot rejection, hand selection and bounded history are tested.
- Actual Sable assembly exercises reserved plots, fresh projected occupancy,
  stationary bounds, nonzero rotation point, translation, 90° rotation and
  nonuniform scale. Eight projected corners at five interpolated poses are loaded
  deliberately by the fixture and remain neutral. A separate loaded ordinary
  position stays eligible; nonfinite pose metadata pauses comparison. Fixture
  chunk loading is not used by production comparison.
- Production `ComparisonCycle` is tested with dirty backlogs 0/63/128/4096,
  combined callback/read caps, queue remainder, outline permits and per-tick reset.
  The 4096 cache cap is structural (fixed array and one-to-one slot map).
- Read-only gate2 review passed after these tests. Exact API/bytecode inspection
  supports strict client mixin descriptors, but **no real client launch, client
  mixin transformation, screenshot or GUI success is claimed** by this evidence.
- A subsequent world-unload cleanup clears the snapshot's retained world
  reference. The required `./scripts/gradle-docker.sh build` was rerun successfully
  at `00:50Z`, including JUnit and shipping isolation. This client cleanup does
  not constitute an additional native schematic or GUI run.

The earlier cached-global-bound query failed the projected-craft test. It was
replaced by the bounded metadata snapshot; those failed attempts are not passes.

Integrated recheck on 2026-11-08: schematic329 passed again at `01:19:57Z`, with
complete saves at `01:20:13Z`. JUnit95 was explicitly executed again using
`./scripts/gradle-docker.sh test --rerun-tasks` at `01:28:37Z` (not merely a cached
test result). The same integrated JAR also passed packaged installer startup/save;
see [server recheck](v02-server-binding.md#integrated-recheck--2026-11-08). None of
these checks starts a GUI or changes the pending player matrix below.

## Player matrix — scoped feedback and remaining checks

Use a backed-up disposable world with the pinned stack. Remove the old Traveler
Echoes schematic patch first; Fieldwork is an independent replacement, not a
patch-order compatibility layer. Do not install duplicate nested Sable/Create
Simulated dependencies. Record the exact candidate hash used for your report.

| Check | Reproduction / expected observation | Status |
|---|---|---|
| Client startup | Normal shipping mod, no acceptance/research driver; no strict mixin failure | Pending |
| Main-hand priority | Different valid schematics in both hands; main preview/HUD wins | Pending |
| Assistance activation | Main-hand blueprint has no Fieldwork comparison frames/extra HUD; offhand blueprint enables assistance; switching back clears it. With valid blueprints in both hands, main wins and auxiliary mode stays off | Pending for latest scope-only candidate |
| Offhand support | Non-schematic main, valid offhand; deploy/rotate/mirror/translate and verify server receives that offhand deployment | Pending |
| Same filename, different state | Two copies with different deployment components; switch hands/slots and confirm no stale preview or wrong-slot overwrite | Pending |
| Swap/inventory sync | Change preview, immediately F-swap or open inventory; reopen both copies and check each saved state | Pending |
| Construction hints | Ordinary world: wrong type red; matching block/wrong facing, log axis, stair half and slab half orange; correct block has no extra frame | Pending |
| Native ghost | Missing block remains native ghost; no replacement of Create's preview | Pending |
| Offhand opacity | Ordinary ghost about65% opaque; real correct blocks stay solid, red/orange frames remain clear. Switch to main hand: native opacity immediately returns; test both mirrors and glass/leaves | Owner reports follow-up OK; detailed combinations not individually recorded |
| Correct ghost removal | Within hiding budgets: correctly placed supported blocks lose their ghosts after comparison/coalescing; delete/change them and verify ghosts return. Rotate/mirror/move/swap hands and ensure no wrong cells disappear | Owner reports follow-up OK; detailed reset/mirror cases not individually recorded |
| Hiding limits | Over32,768 bounding-volume cells or candidate-cache eviction: HUD reports masking disabled, normal preview/outlines remain | Pending |
| Iris | First opacity candidate reported flicker/lost alpha; the owner confirmed the current entity-translucent/correct-ghost candidate worked | Player-reported pass in tested setup; exact pack/version not recorded |
| Special renderers | Check animated/custom block-entity parts; they retain native shaders and may remain opaque | Pending |
| Dynamic/unknown state | Toggle lamp/power/waterlogging; no false orange. Unsupported states remain neutral | Pending |
| Animation | Rotate/mirror/move rapidly: no half-angle or displaced frames; hints resume only when settled | Pending |
| Target exclusions | Overlap a stationary/moving/rotated/scaled Sable ship, plot area or unloaded chunk: no comparison frames there; nearby ordinary cells still work | Pending |
| Invalidation | Replace/delete blocks, unload chunks, undeploy, drop/change hands, dimension change/disconnect and resource reload: no leftover outlines | Pending |
| Controller priority | Controller + offhand blueprint: crouch-right-click smart isolator binds; crouch-air opens native channel UI; self-craft unbind preserves channels/name; handheld and lectern use do not edit the blueprint | Pending for latest candidate; headless policy passed |
| Budget/HUD | Large/distant schematic: partial coverage is explicit and no claim of whole-blueprint validation; check responsiveness and HUD overlap | Pending |
| Multiplayer | Repeat slot sync and controller gesture with a real remote client/server | Pending |

Shader/render-mod compatibility, all other mod states and unrestricted ship-target
comparison are not established. See [player guide](player-test-guide.md) and
[server binding evidence](v02-server-binding.md) for the separate server checks.
