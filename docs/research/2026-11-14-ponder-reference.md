# Ponder Introduction Reference Investigation

## Evidence boundary

Inspected on 2026-11-14 for the owner-selected
[0.3.0 scope](../design/v030-release-scope.md). Fieldwork branch:
`v0.3.0-dev`, commit `cf4909207e9e3c3aa14a68e702b15e1ba7bfb2b1`.
No Ponder implementation, source generation, dependency change or runtime/client
verification was performed by this investigation.
Subsequent owner-authorized implementation is recorded in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md#ponder-teaching-development-slice--2026-11-14);
the findings below preserve the pre-implementation reference audit.

The supplied reference is the local checkout
`/home/tassis/workdir/repos/draconic-insight`. Its HEAD is
`3957928e7bcd7320a98fa83feda7b25a57cb1381`, but the inspected Ponder files and
related documentation/tooling include untracked and modified work. Findings refer
to the current local files, not a reproducible committed release at that HEAD.
The reference checkout was not modified or built.

## Reference implementation

Paths in this section are relative to the reference checkout.

| File | Finding |
|---|---|
| `src/main/java/dev/tassis/draconicinsight/integration/client/TutorialClientSetup.java` | Client-only mod entry point; enqueued client setup registers the plugin after a Ponder presence check |
| `src/main/java/dev/tassis/draconicinsight/integration/ponder/DraconicPonderPlugin.java` | `PonderPlugin`, `PonderIndex.addPlugin`, item associations through `forComponents`, registered storyboards and shared localized text |
| `scripts/generate_ponder_schematics.py` | Python-standard-library deterministic compressed vanilla structure NBT, palette/properties and scene geometry catalog |
| `src/main/resources/assets/draconic_insight/ponder/` | Structure templates and a catalog containing scene/item/geometry/caption-key references |
| `src/tools/tutorial-smoke/java/dev/tassis/draconicinsight/smoke/PonderSmoke.java` | Actual registered scene compilation, schematic loading, palette/property validity and English text checks |
| `src/tools/tutorial-smoke/java/dev/tassis/draconicinsight/smoke/TutorialVisualSmoke.java` | Separate integrated-client rendering/screenshots; stronger than scene compilation alone |
| `scripts/verify-tutorials-docker.sh` | Xvfb/software-rendered client launcher requiring a fresh verifier report |
| `build.gradle` | Separate non-shipping smoke source set, explicit ordinary-run isolation, packaged-distribution checks and client/server lanes |

The plugin reads English defaults from the language JSON rather than duplicating
prose in the scene catalog. `registerSharedText` supplies caption defaults and
scene titles use the same source. Model facing that is stored in a BE must be
initialized explicitly. Scene scale/offset are based on structure size.

Reference file SHA-256 values at inspection:

| File shorthand | SHA-256 |
|---|---|
| `TutorialClientSetup.java` | `d2022236ca3826988de6691ddb6e9209fb5151a3e1956fb8cac8e052b83b34d1` |
| `DraconicPonderPlugin.java` | `145988ea2413a56b52d8d532c0bf12a1ff25e544eb6cae5a744d3a50b81933a4` |
| `generate_ponder_schematics.py` | `93ec602f3d1869822b74c41a0bc57b2166a4cce311c89a2cac3f2ce8d54058ff` |
| `PonderSmoke.java` | `4b19fc10ea4a64269079ad9f27baa5a072fe3bde7d52e2d08e9a17b4dc261c7c` |
| `verify-tutorials-docker.sh` | `67800a3936c07e75fc53844d799cad4a0fc0bb74a166b18458df6e55b1c4e9bb` |

## Adaptation to Fieldwork

### Reuse the existing Create-provided libraries

The reference targets Ponder `1.0.82+mc1.21.1` and Flywheel `1.0.6`, embedding
them because it can operate without Create. Fieldwork requires Create. Its existing
`build.gradle` extracts audited nested JARs for compilation and includes those
libraries on test runtime, without installing duplicate nested mods.

The current extracted compile libraries contain
`ponder-neoforge-1.0.82+mc1.21.1.jar` and
`flywheel-neoforge-1.21.1-1.0.6.jar`. These generated/cache files are inspection
evidence, not editable source. The future plugin should initially use those exact
APIs and keep the current dependency/verification pins. No extra jarJar packaging,
optional Patchouli system, Draconic Evolution dependency or standalone
Ponder/Flywheel installation is needed for the Fieldwork design.

### Keep presentation client-only and integration-aware

Use client setup to register one Fieldwork plugin. It can register the scribing
table's stories in Create-only mode and add the isolator stories only when
`AeronauticsIntegration.active()` is true. Stable registry identities exist even
in inactive mode, so testing item registration alone does not establish correct
tutorial availability.

Avoid direct Sable/Simulated API references in the plugin, structure palette and
scene playback. Use Fieldwork's stable blocks and Create/vanilla props to illustrate
craft sections. Actual server authority and physical assembly remain unchanged.
Client-only code must not be referenced by dedicated-server bootstrap classes.

### Author teaching material for the actual features

Three block subjects can share four concise stories, splitting table clipboard
and book operation. The smart item may reuse the ordinary isolation introduction
before offering its own binding story. Current runtime contracts define all
captions and examples; bridge groups and future Loconautics support are not taught.

The table accepts book, written book and Create clipboard; paper and writable book
are not accepted. It preserves the schematic, generates native material
requirements after 21 active loaded ticks and does not build the target. These
details were checked against `docs/design/schematic-scribing-table.md` and
`src/tools/acceptance/java/dev/tassis/createfieldwork/acceptance/ScribingTableAcceptance.java`.

The smart story should show crouch-controller interaction on a loaded assembled
craft's smart block, channel configuration, bounded remote use, and explicit
standalone-crafting unbind. Binding is not an action available on an ordinary
world smart block, nor is standing aboard a craft sufficient to bind an ordinary
controller. `isolator/binding/BindingInteractions.java` and the existing managed
binding contract define these constraints.

### Verification is layered

1. Resource coherence: exact item/scene associations, nonempty templates, valid
   registered block states/properties, complete English/Traditional Chinese keys.
2. Actual client initialization: plugin is registered once and expected mode-specific
   scene associations compile and load. A catalog count alone is insufficient.
3. Actual playback/rendering: inspect important stages, keyframes, item/control
   hints, redstone indications, clipping, scene bounds and readable text.
4. Dedicated-server distribution: client/Ponder scene classes do not initialize on
   the server; existing base/integrated gameplay and archive isolation remain valid.
5. Player acceptance: item tooltip entry, configurable Ponder key, replay, language
   and GUI scale; carry out the table/controller operations in a real world.

Reference smoke results are not Fieldwork results. Scene compilation does not
verify runtime network isolation, binding, print transactions or complete playback.
Fieldwork's default Docker image has no display stack. If adding a dedicated
Xvfb test profile, adapt it to the project's non-root UID/GID, isolated scratch and
cache conventions instead of copying the reference wrapper unchanged.

The reference uses an MIT license. If substantial source is copied, retain its
notice in the appropriate packaged third-party notice; otherwise implement the
small API integration independently. Author original Fieldwork scenes and prose.
