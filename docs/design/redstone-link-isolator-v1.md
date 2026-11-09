# Redstone Link Isolator V1 — Technical Design

Design established 2026-11-06; implementation and verification updated through 2026-11-09.

**Status:** production main is implemented and available as `0.1.0` for player testing. Headless and installer evidence exists; client and full release acceptance are incomplete. This document describes the intended architecture and guarantees. The [acceptance record](../testing/redstone-link-isolator-acceptance.md) defines what has actually been tested.

The [product contract](behavior-contract.md) remains authoritative for behavior.
The [project architecture policy](project-architecture-policy.md) defines mandatory
ownership, dependency, integration, and documentation boundaries.

## 1. Approved decisions

| Decision | V1 contract |
|---|---|
| A: isolation | Same isolated SubLevel can communicate internally; world/other craft are blocked in both directions |
| Membership | Compare logical server, dimension and SubLevel UUID; never infer membership from a craft AABB |
| Controllers | Ordinary handheld is always WORLD; ordinary private control requires a lectern physically on that craft; managed controllers use their loaded smart binding target |
| Upstream behavior | Preserve Create frequency/range/power and Sable projection |
| Uncertain transitions | PENDING denies authorization; null tracking does not prove WORLD membership |
| D01 | Strong recovery of upstream-lost blocks/data and an exceptional journal are optional/deferred |
| D02 | Every train condition is a public WORLD observer; private train consumers are deferred |
| D03 | Ordinary shaped 3×3 recipe, `OBO/ELE/BPB`, output one |

D01 and D02 do not remove responsibility for Fieldwork's own registry,
initialization, refresh correctness, or train source visibility/cache behavior.

## 2. Supported scope

Included:

- Create Redstone Link transmitter/receiver blocks.
- Handheld Linked Controller and Lectern Controller standard-network input.
- Directional and Modulating Linked Receivers in the pinned Aeronautics bundled/Simulated artifact.
- Train-schedule Redstone Link global conditions through a separate D02 adapter.
- One isolator feature family with ordinary and smart blocks/BEs, position-set
  handling for multiple isolators, assembly/disassembly/split,
  load/unload/restart, and ordinary block mutations.
- Bounded client-side offhand schematic comparison, outlines and native-preview
  masking. These hints are never server ownership or authorization inputs.
- Protocol-compatible endpoints with correct location/liveness and audited source semantics; this is not blanket support for arbitrary custom wireless implementations.

Excluded:

- Vanilla redstone, Redstone Contacts, Flight Control direct proxies, seats and unrelated controls.
- ComputerCraft/other wireless systems, gateways, frequency lists, and unmanaged
  or ad-hoc controller boundary exceptions. The server-owned smart-isolator
  binding route defined in section 6 is explicitly supported.
- General Sable block/BE transfer repair, whole-batch rollback, disk/OS power-loss recovery and the deferred D01 journal.
- Private train-consumer membership, carriage cross-scope ownership, offline carrier provenance and anchor-UUID recovery.

### D02: public WORLD observer

`RedstoneLinkCondition.tickCompletion(Level,Train,CompoundTag)` calls a global `hasAnyLoadedPower(Couple)` query. Upstream does not use the Train argument, receiver distance or pair hooks, and caches through `LastChecked`/`globalPowerVersion`. It requires its own adapter.

All train conditions observe same-frequency public WORLD sources across loaded dimensions without range checks. This is an intentional consumer contract, not a fallback after failed ownership resolution. A train aboard a craft still cannot read that craft's private network.

| Confirmed WORLD positive | Potentially visible unresolved source | High | Low/inverted |
|---|---|---|---|
| Yes | Either | Complete | Wait |
| No | Yes | Wait | Wait |
| No | No | Wait | Complete |

PRIVATE sources are invisible. PENDING must not become false and accidentally complete Low. Only this condition/call site is adapted; other global callers retain upstream behavior.

A server-owned source-visibility epoch and session stamp invalidate cache on scope-only changes and pending begin/expansion/release. PENDING does not commit a new checked version; a new server session invalidates old stamps. Unregistered-source load windows and last-member removal require independent tests. No Train/Carriage membership index is part of V1.

## 3. Scope invariants

“Same craft” means one instance identified by dimension + SubLevel UUID within one logical server, not a name, object reference or reusable plot coordinate.

| Sender/receiver | Policy |
|---|---|
| WORLD ↔ WORLD | Preserve upstream result |
| Same PRIVATE craft | Allow scope; upstream frequency/range must still pass |
| PRIVATE ↔ WORLD | Deny |
| PRIVATE A ↔ PRIVATE B | Deny |
| Any PENDING/unavailable source | Temporarily deny |

- READY with zero isolators is WORLD; one or more is PRIVATE.
- A known SubLevel not fully reconstructed is conservatively PRIVATE(CraftKey), never READY(empty).
- A transfer overlay can deny individual positions in the ordinary world without disabling all WORLD traffic.
- Pair queries read snapshots and perform ownership/map lookups only: no block scan, registry repair or ACTIVE update.
- Invalid private provenance never falls back to WORLD. Zero-isolator compatibility is not an excuse to introduce general upstream wireless fixes.

## 4. Server-owned state

Production Java is under `src/main/java/dev/tassis/createfieldwork/`:

```text
MinecraftServer instance
└─ FieldworkServerState
   ├─ IsolationRegistry       CraftKey → lifecycle + immutable marker positions
   ├─ TransitionCoordinator   token → affected craft/positions + runtime state
   ├─ ControllerSources       server provenance and published entry scope
   ├─ PlayerSessions          player UUID/session → validated controller input provenance and lifecycle
   ├─ WirelessRefresh         dirty dimension/frequency work and update frames
   └─ SourcePublications      source publication/visibility state
```

| Component | Responsibility |
|---|---|
| `platform/FieldworkServerState`, `FieldworkStateAccess`, `mixin/platform/MinecraftServerMixin` | Server-instance holder, thread checks, cleanup |
| `isolator/state/CraftKey`, `IsolationRegistry` | Craft identity, initialization, atomic marker-set publication and teardown |
| `wireless/scope/Scope`, `PairPolicy`, `ScopeResolver` | WORLD/PRIVATE/PENDING and pure ownership/provenance policy |
| `wireless/train/TrainConditionQuery`, `wireless/publication/SourcePublications` | D02 visibility, epoch/session and publication windows |
| `isolator/lifecycle/TransitionCoordinator`, `TransitionCapture`, `TransitionPostconditions` | Batch/barrier ordering, immutable pre-mutation capture and ledger validation |
| `wireless/refresh/WirelessRefresh`, `CreateReceiverRefreshPass`, `SimulatedReceiverRefreshPass` | Scoped recomputation, cleanup, update frames and one-shot permissions |
| `controller/routing/ControllerSources` and package-private validation/index/reconciliation collaborators | Server input provenance, endpoint indexes and retained refresh obligations |
| `controller/session/PlayerSessions` | Server-generated player session identity and transition handling |
| `isolator/ordinary`, `isolator/smart`, `isolator/binding` | Actual blocks/BEs, smart binding authority and derived visuals |
| `platform/registry/FieldworkContent` | Block, item, BE type and creative entry registration |

All mutations run on the server thread; queries use immutable snapshots. Avoid long-lived strong references to SubLevels/players: store CraftKey, UUID and server-generated session identity. Endpoint metadata is a server-owned handle. Nested source contexts are restored with `try/finally`, not prototype static state/WeakHashMaps.

Logout, dimension changes, level unload and server stop clean the relevant state. A new login with the same UUID cannot inherit an old entry. Context mismatch never silently grants WORLD. Clients do not own an authoritative registry. Failure to resolve the pinned stack's logical-server context must be diagnosed/rejected rather than silently allowed.

## 5. Registry reconstruction

```text
UNINITIALIZED → INITIALIZING → READY(immutable positions)
                      └────→ FAILED_CLOSED until trustworthy reconstruction
```

FAILED_CLOSED describes authorization, not a guarantee that every receiver writer has successfully reached zero; see failure handling below.

- Store immutable BlockPos/packed positions. Duplicate registration/removal is idempotent; counts cannot go negative.
- Rebuild from isolator **block identity**, not instantiated BEs or saved ACTIVE flags. Packed BE tags may exist before BE instances.
- Publish one complete set after all relevant loaded plot chunks are restored.
- Placement/replacement/break/explosion update explicit mutation paths; do not wait for an arbitrary BE tick.
- `0↔1` changes scope and refreshes. Non-boundary changes maintain set/visual consistency without changing privacy.
- Unload/destroy clears registry state without exposing still-live stale endpoints as READY(empty).
- Restart reconstructs saved block facts. Normal READY/count/private flags are not persisted separately.

## 6. Controllers and membership

### Fixed endpoints

Use the network's server Level and `Sable.HELPER.getContaining(level,storedPos)`. Valid craft resolve through the registry; genuinely ordinary-world endpoints resolve WORLD. A null owner during an unpublished plot/load window is not proof of ordinary-world membership; initialization/publication gates must cover it.

### Provenance and distance are separate

Both upstream controller paths use the **player's world BlockPos**. Preserve `getLocation()` and its range quantization:

- Ordinary handheld scope is always WORLD, even while its player stands on an
  isolated craft. Player membership is not private authorization.
- Ordinary lectern scope follows the validated source block, even if its operator
  stands in WORLD. A lectern physically on PRIVATE(A) is the ordinary route to A.
- Managed hand or lectern scope follows only its loaded smart binding target;
  player and lectern craft do not retarget it.
- Distance still uses upstream player coordinates and Sable projection of the other endpoints.
- Capture provenance after upstream held-item/active-user validation. Frequencies come from the server ItemStack; no client-reported craft UUID protocol is added.
- Attach metadata before `addToNetwork`. Refresh it on frequency-entry reuse/heartbeat and device changes, not only construction.

### Session and event reconciliation

Controller authority comes from server-validated item or lectern provenance and,
for managed devices, the loaded smart binding target. Player position or craft
membership never grants ordinary handheld private authorization. `PlayerSessions`
tracks validated controller-input sessions so disconnect, dimension change,
inventory/program changes, and replacement sessions revoke or reconcile only the
affected endpoints. Production does not hook vanilla movement acceptance or use
movement proposals as controller authority; historical movement-hook findings
remain research evidence only.

Inventory/program, lectern-user, binding, lifecycle, logout/session replacement
and dimension-travel events reconcile affected endpoints. Ordinary movement and
same-dimension teleport/ack schedule no Fieldwork controller work; the next native
controller packet observes the current server position. A fixed 200-server-tick
complete safety sweep is a backstop; no extra all-controller Fieldwork pass runs
every Create tick. Preserve upstream release and 30-tick timeout and do not impose
managed item/program rules on ordinary WORLD timeout behavior. Create's native TTL
loop is unchanged.

## 7. Narrow integration boundaries

Keep full descriptors, strict match counts and `required=true/defaultRequire=1`. Reaudit on dependency upgrades. The [mixin audit](../testing/main-mixin-audit.md) records actual production injection details.

| Target | Design |
|---|---|
| Create `updateNetworkOf(LevelAccessor,IRedstoneLinkable)V`, both `withinRange(...)Z` calls | WrapOperation each original call once, then AND scope policy; preserve Sable redirect |
| Simulated `updateSignal()V`, first `getTransmittedStrength()I` call | Denied source becomes 0; preserve direction/modulation/range |
| Train `tickCompletion(...)Z` and its single global-power call | Coordinate visibility, cache and PENDING only for that consumer |
| Controller `handleItem` → `receivePressed` | Keep upstream call; capture validated server source context |
| Controller reuse/events | Reconcile exact affected position, published scope and validity; low-frequency complete safety sweep only |
| Sable/Simulated outer lifecycle methods | Exception-safe narrow wrappers; retain upstream transfer algorithm |

Create cleans dead entries; Simulated's direct loop does not. Restricted/PENDING/overlay sources therefore need liveness checks to prevent dead cached endpoints being reauthorized after plot reuse. WORLD/WORLD preserves upstream behavior. Arbitrary third-party endpoints that falsely report `isAlive=true` are outside this guarantee.

Do not replace the network handler, add a competing ordinary Redirect, target decorated private Sable handler names, intercept `Level#getSignal` or introduce gateway exceptions.

## 8. Lifecycle and publication protocol

| Operation | Outer boundary |
|---|---|
| Load | Successful, non-null `SubLevelSerializer.fullyLoad` return |
| Assembly | `SubLevelAssemblyHelper.assembleBlocks` |
| Disassembly | Simulated `SimAssemblyHelper.disassembleSubLevel` |
| Split | Whole `SubLevelHeatMapManager.split`; child assembly must not commit early |

ContainerReady uses the **event's container** to install observers: it fires before the Level container field is assigned. onAdded establishes INITIALIZING only. afterMove is before source clearing and cannot commit a batch. Enumerate blocks only on explicit cold paths.

Container removal preserves the complete old craft/position/controller footprint
and barrier through withdrawal. A detached craft's marker blocks are represented
by Sable's serialized/holding data, so live plot block state is not a stable
postcondition. The active container must no longer contain the UUID before removal
can publish; incomplete removal or exceptions remain quarantined. This does not
claim recovery of already-lost serialized content under D01.

Each root owns a token, affected craft and source/destination positions. Normal nested moves share their root; unrelated roots can complete independently. Overlapping operations cannot skip existing barriers.

### Normal transition

1. Validate server context/affected sets; install overlays and mark affected craft INITIALIZING.
2. **Before the first destructive write or neighbor callback**, recompute/drain relevant networks under pending policy to retract old contributions. Preserve legal unrelated WORLD sources. If retraction fails, do not begin destructive writes.
3. Execute upstream work in nested context. Add newly known destination positions/UUIDs to the overlay before they are queryable.
4. After content transfer, build a trustworthy candidate from actual block identity on the cold path; do not publish it to ordinary queries yet.
5. Keep the token/barrier and refresh receivers using the controlled candidate view. Drain scoped work. Ordinary queries still see pending. Callbacks can observe candidate-authorized results receiver by receiver; this is **not** simultaneous circuit-side-effect commit.
6. Only after final refresh succeeds, install candidate snapshots and remove **this token's** overlay in a callback-free server-thread memory step. Other tokens remain.
7. Update derived ACTIVE visuals. Log/retry visual failures without reversing authoritative marker counts/privacy. Callback-induced authoritative changes enter their own transitions.

Never compute power under INITIALIZING(private) and then switch an empty craft to WORLD without final recomputation. Updating only onboard transmitters or zeroing every external receiver is also incorrect.

### Candidate-view permissions

A server-owned one-shot permit grants candidate access to one precisely targeted updater entry. It ignores only its own token's overlay; all other pending tokens still deny. Candidate results use the same pair policy and upstream range, never unconditional allow.

The permit is consumed into an update frame. Reentrant callbacks open their own
ordinary frames and cannot inherit candidate access. A global boolean or broadly
inherited ThreadLocal would be unsafe.

Required tests include: candidate refresh gets final values; callbacks cannot borrow permissions; refresh failure retains the token; failed BEGIN withdrawal prevents writes. No general redstone/mechanical rollback is promised.

### Partial refresh failure

Publication/barrier ordering does not make receiver callbacks rollbackable. A receiver updated before another throws may already have produced authorized side effects.

On partial failure:

1. Retain the token, revoke unused candidate permits and withhold stable READY authorization.
2. Make a bounded safe retraction/recompute pass under ordinary pending policy. Capture each receiver's result independently so the first error does not skip the rest.
3. Claim conservative output only for successfully reconciled receivers. Already-observed mechanical effects are not undone.
4. If a writer keeps throwing, record **authorization closed, OUTPUT_UNCONFIRMED**. Retain the barrier and identify failed targets for reconstruction/repair. Do not claim all signals are zero or mark that acceptance case passed.

The same rule applies to BEGIN: unresolved output prevents destructive writes. Normal supported setters must be proven free of forbidden residual power; these boundaries do not excuse Fieldwork bugs.

## 9. Scoped wireless refresh

- Stable pair queries never enumerate endpoints. Enumeration belongs to lifecycle/scope/controller transitions.
- Use endpoint snapshots and frequency buckets in affected server levels, covering old/new sides and receivers outside the craft. Remove dead entries safely outside live iteration.
- Use audited upstream update adapters, including LinkBehaviour `newPosition`/callback semantics; do not reinvent aggregation or spread one receiver's result to another.
- Refresh Simulated direct consumers at the same boundary; “next tick” is insufficient.
- Merge dirty work, iterate snapshots and bound reentry. Failure to stabilize retains the affected barrier and output status, without freezing unrelated craft.

Immutable `RefreshScope` selections use dimension → frequency set or
all-in-that-dimension fallback. Collect old keys **before touch**, add
destination/final keys and retain the union through repair, including related
controller old dimensions. Unknown load/publication frequencies expand only
involved dimensions.

Ordinary callbacks cannot borrow candidate views; nested requests merge into subsequent synchronous passes (8-pass bound). Controller device/lectern/lifecycle events target indexed endpoints; ordinary movement and same-dimension teleport/ack schedule no Fieldwork controller work. Pure WORLD position notifications retain Create's existing semantics.

A READY PRIVATE→PRIVATE marker mutation can skip receiver recomputation only with a real second-marker witness, consistent index/block state and no active/relevant barrier. Temporarily update the set, then reconcile actual block identity in `finally`; do not overwrite nested failures. First/last marker changes still use synchronous barriers/ledger.

Simulated has no permanent frequency reverse index here: selected-dimension position indexes are still linearly filtered. Same-frequency dense networks may approach receivers × endpoints work. Counters demonstrate reduced updater calls, not an MSPT improvement percentage. See [scoped-refresh evidence](../testing/scoped-refresh.md).

Stable-private controller position updates use per-frequency receiver filters. Only
four matching old/new actual/published PRIVATE(A) observations, unchanged
structural source/session/lectern identity/key/level and alive input qualify.
Other transitions use unrestricted old/new networks even at unchanged positions.
Filter union distinguishes absent keys, ALL and NONE, and reentrant unrestricted
work widens later synchronous passes. Invocation filtering never grants source
visibility or skips discovery/cleanup. Sources, native range/strength and the
exact candidate permit remain upstream/fenced; see
[controller movement evidence](../testing/controller-movement-refresh.md).

## 10. Failure responsibility and D01

Fieldwork is responsible for missing registry entries, initialization gaps, duplicate callbacks, stale private contributions, incomplete reconstruction published as READY, and failed A contaminating unrelated B.

Context/overlay setup failure, incomplete enumeration, untrustworthy candidates, BEGIN retraction failure and final-refresh failure are Fieldwork/integration failures. Keep affected authorization closed and handle output as above. **Only** after trustworthy reconstruction and authoritative block-state inspection proves actual absence does D01's physical upstream-loss boundary apply. Normal return, an empty BE set or failed scan is not proof of physical loss.

D01 defers restoring blocks/data already lost by upstream and preserving the former intent of nonexistent isolators across crashes:

- Runtime uncertainty/reconstruction failure remains affected-only FAILED_CLOSED, with diagnostics and possibly unconfirmed output.
- A complete trustworthy reconstruction follows actual surviving blocks; if all isolators physically disappeared, zero means WORLD.
- Unrelated success cannot release failure. Explicit valid reconstruction/teardown may release it; timers never grant WORLD.
- Do not invent phantom markers from ACTIVE/history, auto-rollback general transfer, or persist normal registry state.
- Marker ledgers support diagnostics/consistency checks, not a permanent guarantee to restore original intent after upstream data loss.
- The lost-marker restart control remains expected-unsafe evidence outside the deferred stronger guarantee.

A future exceptional journal needs a separate design and interruption/I/O tests. Test JSON or `SavedData.setDirty` alone is not durability.

## 11. Block and player content

- ID: `create_fieldwork:redstone_link_isolator`; block + BE, no GUI, frequency setting or redstone output.
- ACTIVE is derived: false in the ordinary world; true for a valid registered marker in a ready isolated craft. Normalize on load/disassembly; never use it as registry authority.
- Ordinary-world placement does not create a craft registry or periodic scan.
- Use original or legally reusable resources, not unlicensed assets copied from dependency JARs.
- English/Traditional Chinese names/tooltips, loot, item model and creative entry are implemented. State train WORLD-only behavior clearly.

Tooltip intent: aboard an assembled craft, standard links communicate only within that craft, retaining frequency/range limits; ordinary redstone and other wireless systems are unaffected.

**D03:** ordinary shaped 3×3, `OBO/ELE/BPB`, output one. Exact ingredients,
unlock behavior, smart upgrade and rationale are maintained in the
[recipe contract](2026-11-06-isolator-recipe.md). Real client crafting remains
part of player acceptance.

## 12. Client schematic assistance

- Assistance activates only for the selected offhand schematic when no valid
  main-hand schematic takes precedence. Main-hand Create behavior remains native.
- Comparison reuses Create's loaded template and native transform. It does not
  parse a second schematic, place a virtual structure or force-load chunks.
- Wrong block type and a bounded set of construction-state differences are hints.
  Unsupported properties, block-entity data, expected air, unloaded positions and
  uncertain/excluded craft regions remain neutral.
- Loaded craft metadata may suppress hints near reserved/projected regions, but it
  never supplies server ownership or authorization.
- Correctly placed ordinary ghost blocks may be masked through the native preview.
  Stale, ambiguous, unsupported or over-budget state restores the ghost.
- Tick work, cache size, outlines, masks and captured craft metadata are bounded.
  No FPS or MSPT improvement is claimed.

Exact limits, comparison properties and player checks are maintained in the
[schematic evidence record](../testing/schematic-client-v02.md).

## 13. Verification boundaries

The production implementation has reached player-test delivery; client/GUI,
multiplayer and complete visual acceptance remain incomplete. Current phase
counts, artifact hashes and installer observations live only in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md).

Verification layers:

1. JUnit loads main only and checks pure policy/state; research auto-shutdown drivers must not enter unit tests.
2. `src/tools/acceptance/` uses the real main registry/isolators. Its
   fault/counter fixtures do not supply authorization answers. Research
   scope/controller/lifecycle mixins are excluded.
3. Exported transformations establish Sable/main composition; dynamic assertions establish actual behavior.
4. Installer smoke uses the packaged JAR, not ModDev class output. A future driver JAR needs an explicit acceptance profile, not a bypass of ordinary smoke's foreign-JAR rejection.
5. Record exact hashes, input/output, saves and close; clients separately verify visuals, translation, ACTIVE and actual input flow.

Required scenarios include world/same-craft/two-craft traffic, both controller
paths, rotation/range, legal7/forbidden15, marker0↔1/multiple markers, ordinary
events, multi-chunk/packed startup, real JVM restart, zero Fieldwork work for
ordinary movement and same-dimension teleport/ack, next-packet position/range
observation, withdrawal on dimension travel/session replacement,
unrelated-failure isolation and excluded-system regression.

D02 independently requires high/low, PRIVATE-only, PENDING-only, WORLD+PENDING, cross-dimension/no-range, scope-only and server-session cache invalidation, root expansion, last-member removal and late publication. Old private-train research is not acceptance of the fixed-WORLD adapter.

## 14. References and version boundary

Pinned: Minecraft 1.21.1, NeoForge 21.1.257, Create 6.0.10, Sable 2.0.5, Aeronautics bundled/Simulated 1.3.2, Java 21. Observed Mixin 0.8.7 / MixinExtras 0.5.5 are part of the composition baseline; do not add duplicate runtimes or upgrade implicitly.

- [Exact artifact audit](../research/2026-11-06-dependency-audit.md)
- [Lifecycle audit](../research/2026-11-06-lifecycle-boundaries.md)
- [Controller endpoints](../research/2026-11-06-controller-endpoints.md)
- [Movement/recovery](../research/2026-11-06-movement-and-recovery.md)
- [Probe history](../testing/scope-hook-probe.md)
- [Production acceptance](../testing/redstone-link-isolator-acceptance.md)
