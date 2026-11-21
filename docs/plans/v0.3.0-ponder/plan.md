# Version 0.3.0 Ponder Introduction Maintenance Plan

**Goal:** Deliver a shared isolator introduction with variant-specific lectern/binding chapters and the owner's Print-only scribing table.
**Current Risk:** Scene playback cannot prove live Sable ownership or server-side print/binding behavior. Automated development-client checks now pass; packaged-JAR and player entry/reading/scale acceptance remain separate gates.
**Approach:** Use Create's provided Ponder APIs for two already-assembled airships with visible rotating propellers. The owner removed table stories and Settings and selected concise action/result captions. The initial task sequence below is historical where it conflicts with that revision; the checklist, current feature contracts and acceptance record carry current scope/outcomes.
**Verification Strategy:** Validate actual client registration/compilation/playback/rendering in Create-only and integrated modes, plus dedicated-server/archive and relevant gameplay regression checks. Human checks cover tooltip entry, text readability and actual operations. Executed commands/results are in the production acceptance record; commands below alone are not pass evidence.

---

**Execution State:** `docs/plans/v0.3.0-ponder/checklist.yaml`

## Inputs and branch boundary

- Scope/brief: `docs/design/v030-release-scope.md`.
- Reference findings: `docs/research/2026-11-14-ponder-reference.md`.
- Starting branch/commit: `v0.3.0-dev` at `cf4909207e9e3c3aa14a68e702b15e1ba7bfb2b1`.
- Parent mission: none assigned. This continues the existing workstation checklist's
  Ponder-planning task, not a new workstation gameplay rewrite.
- Follow `docs/design/behavior-contract.md`, `docs/design/project-architecture-policy.md`,
  `docs/design/code-organization-policy.md`, `docs/design/controller-binding-v02.md`,
  `docs/design/schematic-scribing-table.md` and `docs/design/schematic-scribing-table-ui.md`.
- Keep the bridge/network-group proposal on `v0.4.0-dev`. Do not teach future
  group membership or change current controller/train policies for a scene.
- The owner subsequently requested rebasing this branch onto main. That rebase
  completed at `e056094` above `main` at `3cf9d01`, incorporating the dependency
  load-range metadata and guidance without changing build pins. Default version
  and publication remain separate decisions.

## Proposed coverage

The current scope has three chapters:

| Scene ID | Associated items | Teaching sequence |
|---|---|---|
| `isolator_basics` | Ordinary + smart isolators, integrated only | Shared two-ship contrast, isolator placement and ordinary handheld behavior |
| `isolator_lectern` | Ordinary isolator, integrated only | Start with blue isolated, then operate its lectern |
| `smart_controller_binding` | Smart isolator, integrated only | Start with blue isolated, then bind and operate aboard/outside |

Ordinary teaching lists basics, lectern, then the existing smart-binding chapter;
smart teaching lists basics then binding.
The table has no stories. Create-only mode has no Fieldwork stories.
The owner authorized this revised layout. Player acceptance
of final pacing/captions and item-key interaction remains separate.

### Owner-selected handheld versus lectern demonstration

The owner explicitly requested this sequence for `isolator_basics`:

1. Identify the assembled isolated craft and its onboard receiver/actuator.
2. Use an ordinary unbound Linked Controller with matching frequencies while
   within native range. Its handheld signal remains WORLD and cannot operate the
   private receiver, even if the operator stands aboard the craft.
3. Install that same controller, with unchanged frequencies, in a lectern physically
   on the same isolated craft. Operate the lectern and show the onboard receiver
   responding through the lectern's physical private scope.
4. State that a lectern in WORLD does not acquire access to the private craft.
   Contrast this ordinary onboard-lectern route with the smart block's separate
   managed handheld binding tutorial.

The owner's later visual feedback supersedes step4's external-lectern scene and
the earlier last-isolator-removal ending: omit those digressions. Add supported
internal/external lamp receivers, a separate lever transmitter, native displayed
matching channels, public two-lamp operation before assembly and private one-lamp
operation after assembly. Smart teaching shares that introduction and then shows
binding plus inside/outside handheld success without a lectern. The current
`docs/design/ponder-teaching.md` defines the revised exact sequence.

Do not silently grant the controller a managed device ID during the comparison.
Keep native range/frequency conditions valid on both sides so the demonstrated
change is source provenance, not a channel or distance change.

## Task 1: Lock storyboards against current behavior

**Historical initial task sequence:** Tasks1–6 preserve the first implementation
plan. The owner subsequently removed table Ponder and Settings, changed the
isolator experiment to two assembled airships and selected situational subtitles.
Do not recreate the removed features from old file lists below. Tasks ponder-07
and ponder-08 in the checklist track the current revision; current feature
contracts take precedence over these historical steps.

**Why:** A visually convincing tutorial can otherwise teach a nonexistent operation.
**Risk:** Medium/high; static world isolators, ordinary controller authority and
material calculation are easy to misrepresent.

**Files:**
- Read: current scope/feature contracts listed above, `src/main/java/dev/tassis/createfieldwork/isolator/binding/BindingInteractions.java`, `src/main/java/dev/tassis/createfieldwork/schematic/table/ScribingTableMenu.java`.
- Create: `docs/design/ponder-teaching.md`.
- Modify: `docs/README.md` and this plan/checklist after scene selection.

**Steps:**
1. Confirm the proposed four storyboards or adjust grouping while covering all
   three items. Confirm that clipboard/book teaching means the existing native
   blueprint material checklist; clarify before adding a different recipe feature.
2. Write stage-by-stage goals, actual controls, important warnings and which state
   is illustrative. Use the actual configurable Ponder key in player guidance.
3. Distinguish ordinary-world props from an assembled-craft illustration. Show
   private control boundaries with section movement/overlays, not by granting real
   Sable authority inside the client teaching world.
   Include the approved handheld-denied/onboard-lectern-allowed comparison above,
   with the lectern's containing craft explicitly visible.
4. Keep the 21-loaded-tick print workflow and native medium/overwrite semantics.
   Use overlays/control hints for GUI operations; do not imply Ponder runs the
   actual server menu or print transaction.

**Verification:** Compare every caption/action against current contracts; check
English prose, links and `git diff --check`.
**Completion Criteria:** Approved coverage and no scene requires nonexistent
gameplay, private ordinary handheld control, or an unapproved output type.
**Re-entry / Idempotence:** Use the selected scene IDs as durable resource identities;
do not create conflicting parallel scene catalogs.

## Task 2: Register a client-only Fieldwork Ponder plugin

**Why:** Establish the actual item-hover entry through Ponder's plugin API.
**Risk:** Medium; dedicated-server loading and optional integration mode.

**Files:**
- Create: `src/main/java/dev/tassis/createfieldwork/integration/ponder/FieldworkPonderClientSetup.java`, `src/main/java/dev/tassis/createfieldwork/integration/ponder/FieldworkPonderPlugin.java`.
- Read: `src/main/java/dev/tassis/createfieldwork/platform/AeronauticsIntegration.java`, `src/main/java/dev/tassis/createfieldwork/platform/registry/FieldworkContent.java`, `build.gradle`.
- Modify: `docs/design/project-architecture-policy.md` to document the narrow
  presentation-only `integration.ponder` responsibility alongside existing integration adapters.

**Steps:**
1. Confirm exact Ponder 1.0.82 signatures from the audited extracted JAR, then
   register once with `PonderIndex.addPlugin` in enqueued client setup.
2. Use `PonderPlugin.registerScenes` and `forComponents` with Fieldwork's registry
   IDs. Register table stories in both modes and isolator stories only in active
   complete-bundle mode. Avoid direct Sable/Simulated class references.
3. Keep client entry annotation/distribution and plugin reachability isolated from
   dedicated-server bootstrap. Existing stable common block classes stay unchanged.
4. Use existing Create-provided compile/runtime libraries. Do not add duplicate
   Ponder/Flywheel jarJar artifacts or copy the reference's optional handbook system.

**Verification:** `./scripts/gradle-docker.sh build`; actual registration/mode tests
from Task 5 and dedicated-server startup from Task 6 are required before calling
this integration verified.
**Completion Criteria:** Exactly one plugin registration; expected item associations
by mode; no duplicate library packaging or server-side client initialization.
**Re-entry / Idempotence:** Inspect existing plugin/client setup before adding code;
avoid registering through both client setup and optional integration bootstrap.

## Task 3: Author original structures and localized teaching text

**Why:** Scene lookup needs real templates and complete text for each supported locale.
**Risk:** Medium; invalid block properties, missing keys and nondeterministic assets.

**Files:**
- Create: `scripts/generate-ponder-schematics.py`.
- Create: `src/main/resources/assets/create_fieldwork/ponder/tutorials.json`.
- Create: `src/main/resources/assets/create_fieldwork/ponder/isolator_basics.nbt`, `src/main/resources/assets/create_fieldwork/ponder/smart_controller_binding.nbt`, `src/main/resources/assets/create_fieldwork/ponder/scribing_clipboard.nbt`, `src/main/resources/assets/create_fieldwork/ponder/scribing_book.nbt`.
- Modify: `src/main/resources/assets/create_fieldwork/lang/en_us.json`, `src/main/resources/assets/create_fieldwork/lang/zh_tw.json` and the Task 2 plugin.
- If substantial reference code is reused, create a specific retained MIT notice
  under `src/main/resources/META-INF/licenses/` and include it in artifact checks.

**Steps:**
1. Generate small original vanilla-structure NBT templates with stable palette and
   block ordering, fixed compressed timestamps and no dependency/source-tree writes.
   Use the Python standard library; no package installation is needed.
2. Put only geometry/item associations and caption identifiers in the catalog.
   Keep English defaults and Traditional Chinese prose in language JSON, using
   `create_fieldwork.ponder.<scene>.header` and shared caption keys.
3. Match real Fieldwork/Create/vanilla block states, facing, link roles and table
   visual state. No physical craft UUIDs or Sable BE data are part of a template.
4. Generate illustrative completed checklist items if needed only for visual props;
   do not represent them as proof of a production print transaction.
5. Ensure regeneration is idempotent and does not rewrite language files.

**Verification:** After implementation run
`python3 scripts/generate-ponder-schematics.py`, compare generated bytes across
two runs, and load/validate palette properties in the Task 5 client verifier.
Check both locale keys and packaged resource paths; run `clean build` after assets.
**Completion Criteria:** All selected scene IDs resolve to nonempty, original,
deterministic templates and complete supported-locale captions.
**Re-entry / Idempotence:** Rebuild checked-in assets from the generator; never fix
scene source by patching generated build outputs or copying reference structures.

## Task 4: Implement storyboards and review visual teaching behavior

**Why:** Static structures alone do not explain workflow or isolation boundaries.
**Risk:** Medium/high; staged animations can contradict actual interactions.

**Files:**
- Create: `src/main/java/dev/tassis/createfieldwork/integration/ponder/IsolatorScenes.java`, `src/main/java/dev/tassis/createfieldwork/integration/ponder/ScribingTableScenes.java`.
- Modify: Task 2 plugin, Task 3 catalog/locales and selected NBT source definitions.

**Steps:**
1. Use Ponder's native selections, independent sections, controls/item hints,
   text/keyframes and camera to teach each selected stage. Inspect exact API
   signatures before using a Create-specific builder extension.
2. Represent craft versus WORLD with clearly separated sections and labels.
   Network/range outcomes shown by scene-authored lamp states are explanatory;
   never call server registries, `BindingInteractions` or assembly helpers.
   Show the ordinary unbound controller's handheld failure before moving that same
   item to the isolated craft's lectern. Preserve its frequency hints and valid
   range; label the lectern's physical private scope as the reason control works.
3. Show smart binding and native channel setup as distinct actions. Explain
   witness-unavailable behavior and unbinding without suggesting automatic retarget.
4. For table scenes show schematic location/rotation, media, Print/progress and
   output in actual vertical order; warn about overwritten notes and partial lists.
5. Scale and offset each scene to its geometry. Keep caption density readable and
   expose useful pause/replay/keyframe points rather than one crowded scene.

**Verification:** Compile every registered storyboard through Task 5; inspect real
rendered early/mid/final stages in English and Traditional Chinese. Compare controls
with actual item/table UI. Manual client playback is the fallback if automated
rendering is unavailable; mark that evidence pending rather than inferring success.
**Completion Criteria:** Every subject teaches a complete usable operation, and
simulated teaching props are not mistaken for live gameplay verification.
**Re-entry / Idempotence:** Fix source scene geometry/captions; do not add gameplay
exceptions solely to make a teaching world behave like a live Sable craft.

## Task 5: Add an isolated client registration/playback verifier

**Why:** Build success alone misses plugin discovery, template lookup and client crashes.
**Risk:** Medium; a driver accidentally shipped or loaded in ordinary runs is unacceptable.

**Files:**
- Create: `src/tools/ponder-smoke/java/dev/tassis/createfieldwork/pondersmoke/PonderSmoke.java`, `src/tools/ponder-smoke/java/dev/tassis/createfieldwork/pondersmoke/PonderVisualSmoke.java`.
- Create: `src/tools/ponder-smoke/resources/META-INF/neoforge.mods.toml`.
- Modify: `build.gradle` for an isolated source set/run/verifier and shipping exclusion.
- If a Docker client lane is approved, create `scripts/verify-ponder-docker.sh`,
  `scripts/Dockerfile.ponder-smoke`; modify `compose.yaml` only if needed for a
  dedicated non-default verification service.

**Steps:**
1. Adapt the reference's actual registered-entry traversal, `gatherSharedText`,
   `compileScene`, `loadSchematic` and registered block/property parsing.
2. Assert associations per item and unique scene IDs, not just total count. Require
   four unique scenes in integrated mode and two table scenes in Create-only mode
   if the proposed layout is selected. Verify every expected caption/title key.
3. Check script instructions/playback and representative rendered stages, not only
   resource loading. Add screenshots for clipped text, framing and important controls.
4. Require a fresh report containing loaded mode, source/artifact identity, scene
   IDs and failures. Reject missing/stale reports and wrong modes. Ensure ordinary
   runs/shipping load only production main, never the driver.
5. Add a Create-only run without Sable/Aeronautics/Simulated rather than merely
   overriding the activation boolean. Give each lane its own scratch directory.
6. Default project Docker has no display dependencies. For a new client image,
   preserve pinned images, non-root UID/GID and local cache conventions; adding
   Xvfb/software-rendering packages is a verification-environment change requiring
   explicit approval. Desktop Java21/Gradle9.2.1 is a manual fallback.

**Verification:** Proposed tasks after wiring:

```sh
./scripts/gradle-docker.sh clean build
./scripts/verify-ponder-docker.sh
./scripts/verify-ponder-docker.sh -PponderCreateOnly
./scripts/verify-ponder-docker.sh -PponderVisualSmoke
```

These wrapper/property names are now implemented. The wrapper reuses an existing
Xvfb/Mesa image instead of installing a new environment. A suitable desktop is the
fallback when no equivalent image exists; do not call a missing display a scene defect.

**Completion Criteria:** Both real dependency modes register the correct item
associations and compile/load scenes; driver isolation and fresh reports are checked.
**Re-entry / Idempotence:** Remove only the lane's previous report before each run;
keep generated screenshots as local evidence and record conclusions in docs.

## Task 6: Close gameplay, distribution and player acceptance

**Why:** New teaching content must remain consistent with working 0.3.0 gameplay.
**Risk:** Medium; client availability and actual workflow have separate limits.

**Files:**
- Modify: `docs/design/schematic-scribing-table.md` only after Ponder exists,
  `docs/design/ponder-teaching.md`, `docs/testing/player-test-guide.md`,
  `docs/testing/redstone-link-isolator-acceptance.md`, `docs/README.md`, `AGENTS.md`.
- Verify: `build.gradle`, `src/main/resources/create_fieldwork.base.mixins.json`,
  `src/main/resources/create_fieldwork.integration.mixins.json` and packaged JAR.
- Read/reuse: `src/tools/acceptance/java/dev/tassis/createfieldwork/acceptance/ScribingTableAcceptance.java`,
  `src/tools/acceptance/java/dev/tassis/createfieldwork/acceptance/ScribingMenuAcceptance.java`,
  `src/tools/acceptance/java/dev/tassis/createfieldwork/acceptance/BindingAcceptance.java`.

**Verification:**

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=scribing
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=scribing-reload -PacceptanceKeepWorld
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic
```

Keep the scribing pair consecutive. Require reports, complete saves and state close.
Headless network/binding phases prove gameplay regressions, not scene teaching.

Use the documented base/integrated packaged smoke setup/start/logs/console flow
after normal server shutdown and manually accepted EULA. Check dedicated-server
startup without loading client scene classes, strict mixins, GPL/retained notices,
scene resources and driver exclusion. This client-only integration does not itself
justify rerunning unrelated lifecycle suites unless runtime hooks are changed.

Manual player gates: hover each of the three items, use the configured Ponder key,
play/replay scenes, inspect en_us/zh_tw at different GUI scales, then reproduce
clipboard/book printing and smart binding/unbinding in a real world. Preserve the
existing pending multiplayer/explosion/table GUI acceptance until actually executed.
For the ordinary isolation example, reproduce handheld denial while aboard the
craft, success through an onboard lectern, and denial through a WORLD lectern with
the same unbound controller/frequencies and valid range. Scene lamp animation alone
does not establish this gameplay behavior.

**Completion Criteria:** All three subjects are accessible in their supported
dependency modes and teach current operations; current evidence and pending player
checks are recorded honestly. Release/version/tag actions remain separate.
**Re-entry / Idempotence:** Read the central acceptance record before claiming a
new pass; never reclassify old headless results as new client acceptance.

## Resumption

The owner authorized implementation after planning. Production stories and the
development-client verifier are implemented, and the automated regression slice
has passed. Read the checklist and central acceptance record for remaining
packaged/player gates rather than restarting completed work or treating historical
planning language as an instruction to implement the feature a second time.
