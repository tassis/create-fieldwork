# Redstone Link Isolator — Implementation Plan and History

Execution state: [checklist](../checklists/2026-11-06-redstone-link-isolator.yaml). Canonical architecture: [V1 design](../../docs/design/redstone-link-isolator-v1.md). Product behavior: [plan.md](../../plan.md).

## Current outcome and decisions

Production main has reached **player-test delivery**. Core/holder, network/lifecycle/controller, fixed-WORLD train integration, content, installer and three review gates passed their recorded scopes. Task6/7 full client/release acceptance remains incomplete. Use the [player guide](../../docs/testing/player-test-guide.md) and [production evidence](../../docs/testing/redstone-link-isolator-acceptance.md); this plan is not a new acceptance result.

- **D01:** upstream data-loss recovery/journal optional/deferred. Fieldwork still owns normal registry/initialization/refresh correctness.
- **D02:** all train conditions observe public WORLD only; private train consumers/ownership research deferred. Visibility/high-low/pending/cache remain in scope.
- **D03:** ordinary shaped3×3 `OBO/ELE/BPB`, output1.

The owner authorized starting main after these simplifications. Earlier “no main yet,” private-Train prerequisite, old class names and outstanding static-research statements are historical, not current next actions. Train95 unsafe controls and unaccepted train-player remain evidence, not passes.

## Goal, risks and strategy

**Goal:** same-craft wireless communication with symmetric boundary isolation, preserving Create/Sable frequency/range/power.

**Remaining risk:** client/GUI/multiplayer, real explosions, complete lifecycle matrix and performance need player validation. Unknown-frequency publication can conservatively defer unrelated Low conditions; no MSPT improvement is claimed.

**Strategy:** pin artifacts, prove narrow hooks in a separate research mod, implement pure policy/server state, then real block/lifecycle/refresh/controller/train integration and player content. Never replace the complete handler or intercept ordinary redstone. Verification layers are JUnit, exact-stack transformation/runtime tests, packaged installer and explicit player checks; unexecuted cases remain unverified.

## Delivery slices: 2026-11-07

### Stable private-controller position refresh (completed local slice)

`task-controller-receivers` is owner-authorized: no binding, permanent index or tick delay. `RefreshScope.Selection` adds per-frequency optional private CraftKey receiver sets; absence means ALL, a present empty set NONE. Union distinguishes unselected keys from selected/unrestricted keys; ALL dominates, private sets union. `WirelessRefresh` filters updater invocation only, preserving source loops, Sable projection, targeted permits, bookkeeping and reentrant widening.

The controller shortcut requires old/new actual and published scopes equal the same PRIVATE(A), unchanged structural provenance/session/lectern identity/key/level, active input before/after and only position changed. Restricted source/session/availability changes still refresh full old/new networks even at the same position. WORLD position-only behavior remains upstream.

Acceptance adds a real-listener A/B/WORLD same-frequency fixture, per-position Create/Simulated counters, A range entry/exit, native Simulated raw result, source/session/liveness fallback, selection algebra and nested private/ALL requests. Code and review passed; `controller-receivers-verified-*.log` records JUnit85, network148/refresh69/movement81/lifecycle23/packed24/seed24-reload25 with saves/close. Finish current-version installer/hash delivery without changing the concurrently prepared0.1.0 version/tag or overwriting documentation history. Counts are not MSPT claims; frequency/source and Simulated-index scans remain.

Delivery is now complete: installer ready/console save/stop exit0, local0.1.0 artifact hash `a7769b40ca8a7f1bee15a6c21929f724e1748eb009e614ab34d312c6408e0000`. See [movement evidence](../../docs/testing/controller-movement-refresh.md). This later uncommitted worktree build is distinct from the existing tag/Release; no publication action was performed. Deferred player/client checks remain outside this slice.

### Tag CI

`task-ci-tag` added `.github/workflows/build.yml`: branches/PR/manual build artifacts; matching `v*` tags build then create Release/JAR, hyphenated versions prerelease. Java21/Gradle9.2.1, SHA-pinned actions, readonly build/write-enabled release, NeoForge filename aligned with build.gradle. Actionlint, actual version-script positive/negative fixtures, nonpublishing release-argv checks and local clean test build passed. A missing reference Gradle pin was replaced with a verified official v4 commit. See [CI evidence](../../docs/testing/github-actions.md). Local validation never proves hosted execution.

### Texture integration

`task-texture-template` exported seven16×16 face PNGs, unfolded/zoom previews, model templates, README and ZIP. Original generated placeholders, not copied upstream art. Default generation protects hand edits.

`task-texture-integration` installed those PNGs into main; base cube references each face, active inherits/overrides up. Exporter has explicit install and readonly main checks; model JSON is source-managed. All58 Java-class bytes matched the prior logic build; PNG/model, clean build/JUnit85/network148 and installer-save checks passed. Client rendering remains unverified. [Texture evidence](../../docs/testing/placeholder-textures.md) identifies its dev-candidate hash.

### Dimension/frequency selection

`task-refresh-scope` completed review, JUnit85, network148/refresh69/lifecycle23/packed24/seed24-reload25 and installer saving. [Scoped-refresh evidence](../../docs/testing/scoped-refresh.md) preserves artifacts/limits.

Implementation contract:

1. Immutable RefreshScope dimension→frequency sets/all-in-dimension fallback. Keep targeted receivers, one-shot permits, callback isolation and8-pass bound; reentrant calls contribute actual level/key to subsequent synchronous work.
2. Collect old scopes **before touch/registry.begin**, destinations before writes, final new scopes after reconstruction. Keep union through repair; scan endpoint indexes in involved dimensions, not craft blocks to discover frequencies. Unknown load/publication falls back only involved dimensions. Related controllers use confirmed/proposed/live tracking/last-private/lectern provenance, not WORLD coordinates.
3. Controller reconciliation is per-level tick, per-player transition and per-entry rebind. Restricted position updates select their own key; membership acceptance semantics unchanged.
4. PRIVATE non-boundary mutation fast path requires READY, no active/relevant barrier, consistent block/index and another real marker witness. Temporarily update set, finally reconcile actual state, preserve nested FAILED. First/last marker retains barrier/ledger. Test no-op/throw/nested replacement, not only successful2→1.
5. Driver counters establish unrelated Create/Simulated frequency/dimension zero updates, old/new union, controller provenance, conservative fallback, reentrant work and non-boundary zero cold updates. Counters/fault switches never ship. Dense same-frequency cost and selected-dimension linear Simulated filtering remain; no timing benchmark.

## Ordered implementation tasks and gates

### Task1: dependency/tool pinning — completed

Modify build.gradle/gradle.properties/metadata and add Gradle verification metadata. Pin Modrinth Create `LNytGWDc:UjX6dr61`, Sable `T9PomCSv:U678xqle`, bundled Aeronautics `oWaK0Q19:44pLdPGg`; verify against lock, not unchecked first-download hashes. Exact required ranges include Create6.0.10/Sable2.0.5/Aeronautics+Simulated1.3.2.

Extract verified nested compile-only libraries if API references require them; runtime installs only the outer bundled JAR. JUnit Jupiter5.11.4/platform launcher and ModDev unitTest testedMod provide actual game-class tests. Align MixinExtras0.5.5 without a duplicate runtime.

Gate: `./scripts/gradle-docker.sh clean build`, matching hashes/metadata and no duplicate mods; formal build no longer depends on `.audit/stack.init.gradle`.

### Task2: narrow hook feasibility — completed research

Use a non-shipping source set/run at `run-scope-probe/`. Main acceptance later excludes it; research now excludes main in turn.

- Create WrapOperation at both full withinRange INVOKEs in updateNetworkOf, original Operation exactly once then scope AND. Original proposal require/allow2 overall; production uses ordinal-specific1 matches.
- Simulated updateSignal's first strength read ordinal0, strict1: deny→0, preserve upstream loop/projection/direction/modulation.
- Prefer direct operands over unstable local-slot capture. If composition fails, independently evaluate another narrow hook; do not stack redundant candidates or replace handler.

Gate: `verifyScopeProbe` (not runner exit alone), transformed exports showing both Sable comparisons retained and actual range/strength/Simulated observations. Match drift, duplicate Operation or lost projection stops integration. No test oracle/counter/force-scope switch in shipping JAR.

### Task2-controller: provenance and movement — completed research

Audit exact handheld/lectern packet routing, location, entry key/lifetime and Sable tracking producers. At minimum aboard-craft input must control same craft. Real server packet handler tests remain distinct from client input/UI.

Controller40 established provenance feasibility but includes unsafe null fallback. Motion37 with real ServerPlayer/vanilla listener established accepted/rejected input, PENDING, support-based departure, teleport waiting, disconnect, rotation and Nether round trip. Use server identities/session state in main, never prototype static player references. See [controller](../../docs/research/2026-11-06-controller-endpoints.md) and [movement](../../docs/research/2026-11-06-movement-and-recovery.md).

### Task2-train: private ownership — cancelled as V1 prerequisite

Train95 includes cold WORLD-anchor hint loss, plot reuse/new-UUID misauthorization and same-root expansion cache skip (without wrong completion evidence). Natural train-player had no accepted private station result/full save. Do not call these safe. D02 now defers this branch; source visibility/cache moved to task5-train.

### Task3-policy and Task3: pure core/server state — completed

Actual files under `src/main/java/dev/tassis/createfieldwork/`: isolation/CraftKey, IsolationRegistry; wireless/Scope, PairPolicy, ScopeResolver; server/FieldworkServerState, FieldworkStateAccess; mixin/MinecraftServerMixin. Earlier proposed WirelessScope/ServerIsolationState names were superseded.

- WORLD/PRIVATE/PENDING truth table, same-value identity, dimension distinction, upstream range cannot be overridden; null is a programming error, runtime uncertainty normalized PENDING.
- Known missing registry is PRIVATE, never READY(empty); FAILED is PENDING. Immutable/idempotent marker sets, no separate normal persistence.
- One state per server instance, server-thread mutation, session separation and close cleanup; no cross-server static map/long-lived craft references.
- Resolver performs audited O(1) ownership/map lookup without reconstruction.

Gate: actual JUnit, not NO-SOURCE. Policy75 then state7; main-only core8 verifies real holder/close/research exclusion. Core gate passed. Later resolver/hooks verified separately; pure tests alone did not complete lifecycle.

### Task4: lifecycle boundaries — completed research

Exact artifacts trace ContainerReady before Level field assignment, onAdded before contents, serializer saved UUID and complete fullyLoad, assembleBlocks/moveBlocks, Simulated disassembly, outer heatmap split, unload/destroy and endpoint publication.

Use event container; initialize known craft conservatively. Rebuild from all restored block states, not BE counts/ACTIVE. afterMove precedes source clearing; swallowed writes invalidate “normal return means success.” Protect actual affected WORLD positions rather than all WORLD.

Gate: callback-time queries, nested children, null/throw/retry, packed/multi-chunk, real JVM restart, stale endpoints/plot reuse and source/destination failures. Prototype suite71/restart5-9/multi200-207/edges19/recovery19 establish their recorded scopes; marker was obsidian, not production wiring. Strong lost-marker restart recovery5-7 remains expected-unsafe/deferred under D01. See [lifecycle audit](../../docs/research/2026-11-06-lifecycle-boundaries.md).

### Task5: actual isolator/lifecycle/refresh — completed integration

Production files: content/IsolatorBlock/IsolatorBlockEntity/VisualUpdates, registry/FieldworkContent, isolation/TransitionCoordinator/IsolationRuntime and wireless/WirelessRefresh, plus audited mixins/controller state.

Final design supersedes the early simplistic “publish scope then refresh” proposal: install affected overlay and withdraw before destructive writes; build actual candidate; refresh under one-shot candidate view while ordinary callbacks remain pending; only then install snapshots/release own token. Visual updates follow and retry independently.

Per-receiver recomputation must preserve range, legitimate weak sources and LinkBehaviour callback/newPosition semantics. Refresh both old/new boundaries including Simulated; no next-tick delay. Bound reentry, retain failed token/output status, keep unrelated roots independent. Authoritative failure is not circuit rollback; persistent writer failure is OUTPUT_UNCONFIRMED.

Integration gate initially found a post-publication barrier-release P1; fixed and follow-up passed. Remediation regression: JUnit82/network136/lifecycle23/packed24/seed24-reload25, save/close separately. Later scoped revision extended results as above.

### Task5-train: fixed-WORLD adapter — completed integration

Only RedstoneLinkCondition's global call/cache is adapted. Never resolve Train anchors/tracking or change other global callers. Source epoch/session must cover begin/expansion/publication/release/last-member and delayed sources. Tests independently cover WORLD/PRIVATE/PENDING, high/low, WORLD-positive precedence, no-range/cross-dimension, scope-only/session cache, packed/unregistered windows and other callers. Client real train GUI remains task7.

### Task6-content / Task6: player content — implementation complete, client gate pending

Register one block/item/BE, creative entry, derived ACTIVE, original assets, English/Traditional Chinese tooltip, loot/tool tags and approved recipe. Use1.21.1 singular recipe/loot_table paths. D03 is ordinary shaped3×3 with existing ingredients/output1, unlock via Link. Main network tests real match/missing materials/output, loot/tool tags/advancement/BE/ACTIVE/no ordinary output.

Task6 remains pending for actual client models/translations/tooltips/crafting/unlock/drop/synchronization. Resource existence or server loading cannot substitute for rendering/GUI acceptance. License remains owner-selected.

### Task7-candidate / Task7: delivery versus full acceptance

Candidate installer delivery completed: real packaged JAR, exact hashes/dependencies, main mixin composition, ready, actual console marker normalization, console stop/full save/exit0/OOMfalse and installation/report guide. Initial/logic/texture candidates have distinct hashes; never relabel them as0.1.0 without a new check.

Full Task7 remains pending. Test product matrix with real clients: world/same craft/two craft, controllers, range/rotation, legal7/forbidden15, markers/break/replacement/explosion, load/unload/restart/split/reassembly, initialization and excluded-system regression. Record coordinates, assembly/frequency/range/input/output, sequence, time/log/screenshots for manual cases. Actual train High/Low is separate from pair coverage.

For packaged updates: console `stop` and confirm `All dimensions are saved`, setup, start, logs/Done, perform checks, console-save again. Docker stop exit143 without save evidence is not acceptance. Unknown/failed/unrun mandatory cases cannot be marked passed. Single-player reports support specified observations only; controller type/hash/multiplayer/performance are not inferred.

## Continuation rules

Read the checklist and current design/evidence before work. Follow remaining runnable work rather than restarting historical research gates. Record concrete outcomes in task evidence; advisory reviews are not external blockers. Client/player availability is not a reason to misreport the completed player-test delivery as full release acceptance. Documentation translation does not execute remaining implementation tasks or change their status.
