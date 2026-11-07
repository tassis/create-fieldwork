# Headless scope hook probe

**Historical prototype record.** Production main is now implemented and has [separate acceptance evidence](redstone-link-isolator-acceptance.md). The dated attempts below preserve research-stage states, including statements that main had no functional hooks at the time; they are not current missing-feature claims.

The [movement/recovery continuation](../research/2026-11-06-movement-and-recovery.md) records `controller-motion` using the real vanilla listener (37 assertions), independent transaction recovery (19), and the **expected-unsafe** lost-marker seed/reload (5/7). D01 ultimately deferred the stronger recovery/journal guarantee. D02 subsequently fixed all train consumers to public WORLD observation; private-train research is deferred. Negative controls remain hazards reproduced, not safety acceptance.

## Purpose and isolation

`src/testmod/` is a non-shipping probe mod. Normal client/server runs explicitly exclude it; the normal JAR contains only `src/main`.
The test oracle (`ProbeScope`) is deliberately not the production registry. Changing its UUID scope flags proves hook behavior, not isolator lifecycle completeness.

Run (Docker Java 21, no client):

```sh
./scripts/gradle-docker.sh verifyScopeProbe
```

This starts an actual NeoForge dedicated server with the three hash-verified release JARs, executes assertions, writes `run-scope-probe/scope-probe-result.json`, and stops the server. The verifier fails on missing JSON or failed assertions. `runScopeProbeServer` alone starts the runner but **does not itself turn an assertion failure into a Gradle failure**; use `verifyScopeProbe`.

The scratch `probe-world/` is deleted before each default run. `-PprobeKeepWorld` preserves it for explicit restart experiments; ordinary scope tests assume a clean scratch world. The production `.smoke/server` is untouched. Mixin exports are in `run-scope-probe/.mixin.out/class/`.

## Recorded attempts

- **2026-11-06 10:49:33 UTC — failed fixture**, before any assertion: directly placing a brand-new Create BE in receiver state left deferred LinkBehaviour absent. The harness wrote `status=failed`; verifyScopeProbe rejected it. Fixed fixture to initialize as transmitter then perform the real receiver-mode state transition. This is not evidence of a Fieldwork hook defect.
- **2026-11-06 10:51:28 UTC — 12 assertions passed**, Create hook calls `82`; Simulated path not yet exercised. Used real Create RedstoneLink BEs, real Sable `assembleBlocks`, actual plot ownership, storage/world distance contrast, private scope on/off and a 4096-block pose translation. Includes stale receiver recalculation on both sides via endpoint snapshots.
- The transformed handler contains exactly two calls into the probe wrapper. Its Operation bridges to Sable's redirected projected comparison; the wrapper performs one original call and ANDs the scope result. Both strict call-site counts succeeded on MixinExtras 0.5.5.

## Evidence limits

### Later accepted/rejected runs

- **10:54:17 UTC — 35 assertions passed**, Create hook calls `770`, Simulated hook calls `172`. Added actual redstone-block input, both Simulated receivers, modulation raw-value preservation, back-facing rejection, bidirectional scope and two craft IDs, legal strength 7 versus forbidden strength 15, and scope restoration.
- **11:01:22 UTC — assertion JSON passed, but run NOT accepted**: stopping in the same tick as disassembly left a removed physics body in the save path (`Body has been removed`). The old verifier missed this. It now additionally requires `All dimensions are saved` and rejects `Exception stopping the server`. The fixture now uses the real holding unload/load path and processes ordinary container removals before stopping. This does not prove arbitrary same-tick production shutdown safe.
- **11:04:27 UTC — 59 assertions and clean shutdown passed**: actual holding unload/load, UUID restoration, per-chunk observation, actual heat-map split and Simulated disassembly.
- **11:06:38 UTC — 71 assertions and clean shutdown passed**: Create hook calls `3335`, Simulated hook calls `814`, `lifecycleViolations=[]`. Added same-private-scope range/frequency rejection; marker count/idempotence/0↔1; duplicate-load null path; injected load exception after chunk registration and retained-holding retry; negative control without position quarantine.

### Candidate transaction actually exercised

The prototype wraps whole `assembleBlocks`, `moveBlocks`, `fullyLoad`, `disassembleSubLevel` and heat-map `split` methods using try/finally-aware `@WrapMethod`. Nested child completion does not commit an outer transaction. Source/destination positions are held in an O(1) quarantine set; unrelated WORLD positions remain shared. Successful outer completion reconstructs actual block identity, publishes scopes, clears quarantine, and synchronously refreshes before returning. Null/throw does not success-commit.

The no-quarantine negative control observed still-powered world-source contribution during transfer, followed by correct private behavior after commit. This supports the proposed **conservative transaction policy**, not a claim that upstream is defective: the source still exists as a world block during part of assembly.

Disassembly observations checked that in-flight destination positions were denied, unrelated WORLD/WORLD stayed permitted, and actual marker blocks appeared in ordinary world coordinates after completion.

**Prototype limits:** marker = vanilla obsidian, not a production isolator/BE. Marker mutation cases explicitly invoke the coordinator; production place/break/explosion wiring is untested. The test coordinator rescans all loaded craft markers at root commits; production must scope state per logical server and limit reconstruction to affected craft. Its static test oracle must not become the production registry.

### Still required before the complete scheme is settled

- Multi-chunk/packed-BE startup reconstruction and marker-free craft publication; the restart fixture below covers one single-chunk private craft only.
- Intermediate multi-tick transitions/asynchronous physics; the restart fixture covers normal tick progression and final observations, not assertions on every tick or transition. The original 71-assertion suite remains synchronous.
- Per-block movement failure, stale/custom non-BlockEntity endpoints and plot-coordinate reuse: current fault injection covers whole-load null/throw, not every lifecycle surface.
- Production registry/isolator event wiring and client visuals/synchronization are not implemented or accepted.

The normal JAR was inspected and contains no testmod classes or mixins. Raw run logs remain under `.audit/scope-probe-*.log`; the latest scratch JSON is overwritten on the next run, so this document preserves accepted and rejected attempts.

First scope suite executes synchronously on the server thread to isolate comparison semantics from physics motion. Calling the real assembly API is not yet a test of player interaction, multi-tick steady-state propagation or split/reload ordering. The fixture uses real block/BEs and a controlled scope oracle, not a production isolator. Subsequent lifecycle probes must independently establish reconstruction and transaction publication.

Dependency verification: the three mod hashes are independently compared with `smoke/artifacts.lock` on every check/extraction. `gradle/verification-metadata.xml` also records other Gradle/NeoForge transitive inputs; those generated entries are reproducibility pins, not a claim that all third-party source or supply chains were independently audited.

## Genuine JVM restart fixture — 2026-11-06 continuation

Run sequentially (two independent Gradle/server launches):

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=seed
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=reload -PprobeKeepWorld
```

Do not run the default suite between these commands: it resets the scratch world. `seed` also starts from a fresh scratch world by default; `reload` must preserve it. Phase identity is checked by the verifier so a report from another mode is not accepted.

`RestartProbe` seeds a real assembled craft containing redstone-block input, transmitter, internal receiver and one obsidian marker, plus an external world receiver. It uses a **vanilla forced world chunk** so the saved craft can load without a connected player. The server advances from fixture creation at tick 20 to final observations at tick 60, then saves and exits. The second JVM reads the same disk world and UUID, observes automatic Sable load, and repeats actual signal observations after tick progression.

Accepted evidence:

- Seed: **5 assertions passed**, complete save at `11:15:24 UTC`; JVM start `1793963698790`.
- Reload: **9 assertions passed**, complete save at `11:15:58 UTC`; JVM start `1793963733865`. An assertion explicitly requires the JVM start times to differ.
- Saved/restored UUID: `0c6529bf-9be4-48da-8c10-f6e6a42d4a46`.
- Reload trace contains `begin:load → loaded-chunk → end:load`, successful at depth 1. Startup reconstruction populated scope **before** the tick-based assertion runner; the mod no longer resets that reconstructed map at ServerStartedEvent.
- After normal server ticks and physics, the actual source remains 15, internal reception is 15, external reception is 0; no hand-set replacement craft/UUID is used in reload.
- Both runs pass the existing shutdown/save guard. Reload executes the Create hook 308 times; this fixture does not exercise Simulated receivers (already covered by the separate scope suite).

This adds **14 assertions across two JVMs**, not a claim that the default suite now contains 85 assertions. The 71-assertion suite was not rerun after adding the phase dispatcher in this continuation; seed/reload both compiled and passed. Raw logs: `.audit/restart-seed.log`, `.audit/restart-reload.log`; fixture identity: `run-scope-probe/restart-fixture.json`.

Remaining limits: one craft, one loaded plot chunk, forced-chunk loading, no packed BE, no marker-free craft, no per-tick assertion during a live 0↔1 transition. These are the next cases rather than evidence already obtained.

## Multi-chunk continuation — work in progress

- After the phase dispatcher changes, the default suite was rerun successfully: **71 assertions**, `.audit/scope-regression-after-restart.log`.
- Added `seed-multi` / `reload-multi` fixtures: two crafts on different frequencies, at least three plot chunks each, one with a marker and one without. The fixture samples ordinary tick outputs and performs explicit coordinator-driven marker transitions at ticks 40/50/60/65/70/75.
- In `reload-multi`, the test-only load wrapper marks saved BE tags `keepPacked=true` before the real Sable loader. Counters verify input tags, pending BE positions and per-chunk INITIALIZING state. This is synthetic packed input to exercise the real branch, not a claim ordinary saving always produces packed tags.
- **11:34:35 UTC — seed-multi failed and is NOT accepted.** Both three-chunk size assertions and marker-free identity passed; tick 26 private external=0 and internal=15 passed. The subsequent shared-world receiver read threw NPE because the receiver no longer existed.
- Fixture review found the external receivers had been placed directly over spine blocks included in assembly. Their support moved away; the shared receiver's normal updates exposed the invalid setup. External receiver positions were moved perpendicular to the spine (`origin + (0,0,8)`), with their support outside the moved positions. **This correction has not yet been rerun.**
- The combined shell command stopped after failed seed, so `reload-multi` has **not run**. Packed-BE and marker-free startup behavior remain unverified.

Next commands, after resuming the limited goal:

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=seed-multi
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=reload-multi -PprobeKeepWorld
./scripts/gradle-docker.sh verifyScopeProbe
```

### Still-pending adversarial endpoint/move probes

A read-only review identified a concrete risk to test, not an observed production bug: Simulated reads registered endpoints without calling `isAlive`, whereas Create's updater clears dead entries. Register a live custom `IRedstoneLinkable`, then mark it dead and query Simulated directly **before** any Create cleanup/refresh. Separately prove Create removes it. A plot-reuse case must demonstrate exact coordinate reuse plus a new UUID; do not silently skip the reuse precondition.

If reproduced in a restricted scope, assess a restricted-scope-only liveness/generation guard; do not globally change zero-isolator upstream behavior. For per-block move exceptions, distinguish swallowed exceptions inside moveBlocks catch regions from exceptions outside them: a successful outer return may still require reconstructing actual residual source/destination states. No new liveness or movement guard was implemented in this continuation.

## Latest accepted continuation: multi, controller and adversarial edges

The preceding work-in-progress notes are historical. The external-receiver support correction was rerun successfully. Latest accepted modes:

| Mode | Assertions | Evidence |
|---|---:|---|
| suite | 71 | `12:02:58 UTC`, `.audit/scope-regression-final.log` |
| seed-multi | 200 | `12:03:36 UTC`, `.audit/multi-seed-final.log` |
| reload-multi | 207 | `12:04:13 UTC`, `.audit/multi-reload-final.log` |
| controller | 40 | `.audit/controller-probe-movement.log` (earlier final run had 37) |
| edges | 19 | `12:02:24 UTC`, `.audit/edge-probe-final.log` |

Each accepted verifier run checks complete save and absence of shutdown failure. `clean build` also passed (`.audit/build-final-probe.log`); normal JAR archive still contains only the main entry, empty strict mixin configuration and metadata, not probe classes.

Reports are now also saved by phase as `run-scope-probe/scope-probe-result-<phase>.json`; latest report stays at the original path. Reload modes refuse to run without `-PprobeKeepWorld` before deleting any scratch data. Unknown phases are rejected. A default suite or another seed overwrites the scratch world, so always run each seed/reload pair consecutively.

### Multi-chunk, packed BE and marker-free startup

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=seed-multi
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=reload-multi -PprobeKeepWorld
```

Two real crafts have at least three loaded plot chunks each and distinct frequencies. A's marker is near the far end of its 36-block spine, while B has no marker. Seed and reload each observe ticks 26–90 without repeatedly calling refresh between commits: private/shared output, internal output and marker-free output are sampled each tick. Explicit coordinator mutations at ticks 40/50/60/65/70/75 cover 1→0→1→2→1→0→1 while normal physics advances.

Reload verified both UUIDs auto-load, B becomes READY/WORLD rather than remaining in initial private scope, four saved BE tags pass the synthetic keepPacked branch, pending BE positions are observed (`packedPositionsObserved=12` observations, not 12 distinct BEs), and six per-chunk callbacks occur before READY. `lifecycleViolations=[]`.

This proves the tested reconstruction/tick boundaries, not production isolator onPlace/onRemove wiring. The packed input is intentionally modified by the test loader; the world still comes from a genuine prior JVM save.

### Controller provenance: promising, not yet safe to integrate

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=controller
```

Uses real Create input packet.handle, real ManualFrequencyEntry objects, FakePlayer, actual lectern active-user checks and existing Sable API. It preserves world positions/range while adding test-only source metadata. The 40 assertions include negative controls and observations of the current prototype's null-tracking fallback; **passing them is not approval of that fallback**. Bare FakePlayer.move did not establish tracking. Three later assertions exercise the real Sable movement packet extension, but not vanilla's complete accepted movement lifecycle.

Full findings and the PENDING/private-to-WORLD gate are in the [controller audit](../research/2026-11-06-controller-endpoints.md). At this stage null tracking remained a material issue, not merely a missing UI test; later motion37 and production integration addressed accepted-movement handling. Null must never automatically grant WORLD.

### Adversarial edges: two original assumptions refuted

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=edges
```

1. A registered non-BE endpoint whose isAlive=false but cached strength=15 still affects Simulated's direct loop; Create's updater instead cleans it. A negative control reproduces the same after exact plot-coordinate reuse with a different UUID. The candidate guard checks liveness only for restricted/quarantined communication. WORLD/WORLD deliberately retains upstream behavior; the test verifies it adds no liveness call there. The fake models a protocol edge, not proof that shipped BEs normally leave stale entries.
2. Throwing at one marker destination write is swallowed by Sable; assembly returns normally, the marker never arrives, and identity-only reconstruction incorrectly publishes WORLD. A candidate pre-move marker transfer ledger checks destination presence and source removal. It withholds success and retains quarantine after this failure. An unrelated success cannot release that failure.

The candidate coordinator is still global/test-only: a failed transaction can prevent all later root commits. This is **not** an acceptable production recovery model. Production needs transaction-scoped quarantine/ledger/failure state, independent commits for unaffected craft, and explicit retry/rollback/reconstruction that releases only the repaired operation. Source-clear failures, multiple-marker partial failures and missing callbacks remain to be tested. A post-hoc marker scan and a successful method return are no longer considered sufficient commit evidence.

## New research modes (supersede the preceding global-coordinator limitation)

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=controller-motion
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=recovery
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=seed-failure
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=reload-failure -PprobeKeepWorld
```

The transaction prototype now has independent root tokens, affected-craft indexes, token-scoped position quarantine and named recovery. Normal nested moves/split children share their root; unrelated B can publish while failed A remains closed. Repair validates the whole marker ledger and requests a flush save before releasing only that token. Recovery19 covers partial two-marker loss and duplicate marker/source-clear failure, not just the original destination failure. It remains test-only and does not prove I/O-failure/crash-safe durability.

Controller-motion37 tests the actual ServerGamePacketListenerImpl acceptance branches with real ServerPlayer, a stubbed outbound transport/in-memory channel, and no client. A bare FakePlayer listener would not suffice. Confirmed normal same-instance lookup stays unchanged; uncertain/rejected transitions cannot publish WORLD. WORLD release after a private source requires an accepted position plus vanilla's computed ordinary-world support; client onGround alone is insufficient. READY(empty) restores ordinary upstream behavior. Temporary control pauses during uncertain airborne transitions are deliberate fail-closed behavior.

The lost-marker pair reproduced the contract gap: marker disappears at both source and destination, memory quarantine blocks power in seed, but after a real save/restart identity-only reconstruction publishes WORLD and the external receiver gets15. JSON explicitly says `expectedUnsafeObservation=true`. No recovery journal was introduced. The owner later approved D01 optional/deferred; normal registry persistence remains prohibited, and the stronger lost-data guarantee is not claimed.

## Research baseline isolation after production integration

`scopeProbeServer` now loads only research testmod and the pinned upstream stack, **not create_fieldwork main**. The verifier requires `mainImplementationLoaded=false` so two authorization implementations cannot mask each other. Suite71 still passed with complete saving after this separation. Use `verifyFieldworkAcceptance` and the [production acceptance record](redstone-link-isolator-acceptance.md) for the actual implementation.
