# Redstone Link Isolator — Production Acceptance Record

## Status and evidence boundary

Recorded through 2026-11-11. Production main implements server-owned state/registry, the actual isolator/BE and recipe, Create/Simulated filters, lifecycle/mutation barriers, per-receiver scoped refresh, two-tier controller routing with event-driven reconciliation, fixed-WORLD train conditions and source-publication cache handling.

The current optional-integration revision makes Create the only required gameplay
dependency. A packaged Create-only server containing only NeoForge, Create and
Fieldwork reached `Done`, then console-stopped with complete dimension saves. Its
mod list contained no Sable, Aeronautics or Simulated. With an integration marker,
the same packaged server logged the prominent backup/data-loss warning, reached
`Done`, then console-stopped with complete dimension saves and exit code 0.
Fieldwork's own stable registry identities remain inert, but recovery of content
owned by missing optional mods is not claimed. Packaged Create-only client/offhand
interaction and real asymmetric multiplayer handshakes remain pending.

**Full v0.2 worktree recheck before the latest client hand-swap polish (metadata
still0.1.0, no publication):** JUnit106,
network174, refresh69, movement74, lifecycle23, packed24, schematic347,
binding177, binding-reset57, binding-faults134, controller-index43,
controller-work15 and independent-JVM seed24/reload25 plus
binding-seed30/binding-reload27 all passed. Runtime verifiers
checked complete saves and state close. Current clean-build SHA-256:
`c69b8bb59e098f96027a8d3a8050d7be77cbaa9812d2d88183e382733340886c`.
The same installed JAR reached `Done (0.692s)` at04:59:45Z, reported no withheld
transactions at05:00:09Z, then console-stopped with complete dimension and Sable
sub-level saves at05:00:09Z and exit0. Latest client controller retest remains
pending. See [integrated server evidence](v02-server-binding.md#final-integrated-recheck--2026-11-09)
for times, reproduction and limits. This does not replace historical hashes below
or establish every player check of later client builds. Final integration review
and detailed [client matrix](schematic-client-v02.md#player-matrix--scoped-feedback-and-remaining-checks)
remain partial; the later Iris/ghost follow-up has a scoped owner-reported pass.

The 2026-11-11 right-hand→offhand transform-preservation change then passed a
clean build and schematic348 with complete save/state close. The follow-up forces
the boundary sync even when Create's periodic-sync cooldown is zero. Its JAR
SHA-256 is
`366cf41a1a8b5601bccd83c9553b18df5d16b891215088eb3e51ad9dccd862f8`.
That focused rerun does not claim the full matrix or a player hand-swap retest for
this later hash.

This recheck followed a behavior-preserving feature-package reorganization.
`FieldworkServerState` remains the logical-server composition root; controller
validation/index/input/reconciliation/safety work, transition capture and
postconditions, and Create/Simulated refresh passes are package-private
collaborators behind the existing façades. Strict mixin count/cardinality stayed
25 common plus six client entries. Dedicated-server schematic acceptance proves
common mixin loading and client-array server safety, not client transformation.

The final lectern correction keeps Create's completed native lectern assignment
when the subsequent exact wireless refresh throws a `RuntimeException` or
`Error`, and retains that refresh scope for retry. The binding phase injects both failures,
checks that the native lectern identity remains coherent and that retry withdraws
the actual private output to0. Failure inside Create's native assignment still
rolls identity/channel/pending state back and rethrows.

The latest follow-up also removes Simulated indexes on chunk unload, clears player
session state on logout, compacts revoked loaded-BE grants without deleting global
revocation history, memoizes client comparability per tick and removes redundant
controller/binding snapshots. Direct acceptance covers each lifecycle/capacity
change. This is not an FPS/MSPT or measured speedup claim.

Normal container removal keeps the old craft/position/controller footprint under
barrier through withdrawal and publication. Detached serialized markers are not
validated against unstable live plot block states; active UUID absence is the
removal postcondition. Incomplete removal or exceptions retain quarantine. This
does not expand D01 into recovery of already-lost serialized content.

**Player-test candidate preparation/delivery is complete; full client/release acceptance is not.** Main acceptance excludes `fieldwork_probe`. The `src/tools/acceptance/` source set supplies fixtures, faults and counters, not scope/registry answers. Historical train95 and other prototype counts are not evidence for this implementation.

The logic revision's full regression recorded JUnit85, network148, refresh69, lifecycle23, packed24 and independent-JVM seed24/reload25, with complete save/state close checked per phase. Its historical `0.1.0-dev` hash is `765e89e884d8118369825b084ae4e3acfd6048266e39b4ab86e0947dca012266`; see [scoped refresh](scoped-refresh.md).

The subsequent texture-only candidate (`c6536ce872e8acf97b1253f6304c09c7a1fa7b305ef1afb9912077477381ab4a`) retained identical bytes for all 58 Java classes and repeated clean build/JUnit85/network148/installer-save checks. See [texture integration](placeholder-textures.md). Those hashes do **not** identify `0.1.0` after version preparation. That preparation passed clean build, 85 JUnit tests and installed texture/model checks; it did not newly rerun every historical runtime phase.

The later local0.1.0 worktree build adds receiver-limited private-controller position updates. It newly passed JUnit85, network148, refresh69, movement81, lifecycle23, packed24 and independent-JVM seed24/reload25, each with complete save/state close. Its installer also reached ready and console-save/stop exit0. Hash `a7769b40ca8a7f1bee15a6c21929f724e1748eb009e614ab34d312c6408e0000` identifies this build, not the earlier tag's artifact; see [controller movement evidence](controller-movement-refresh.md). No new player rendering/multiplayer/performance claim is made.

## Player observations: single-player, 2026-11-07

These reports preceded the scoped-refresh revision. The installed hash/controller type was not independently confirmed, and no agent-executed log/screenshot evidence accompanied them:

- Two craft built from the same blueprint/frequencies lifted together before isolators; after placement, the owner reported independent operation.
- With an existing WORLD lever-powered source held at15, removing the isolator immediately made the craft take off. This supports restoration of existing WORLD input without toggling the lever again.
- Follow-up tests were reported as looking correct: replacing the isolator blocked existing WORLD15; save/exit/reentry retained isolation and internal control; with two markers, removing one kept isolation and removing the last restored WORLD input.

These support the observed single-player core flow. They do not establish every controller path, dedicated-server restart, multiplayer, explosion handling, performance, or a player retest of later hashes.

## Dedicated creative-tab follow-up: 2026-11-07

The earlier release exposed the isolator through the vanilla Redstone tab but did not register a dedicated Fieldwork tab. The follow-up registers `create_fieldwork:main`, titled **Create: Fieldwork** in both language resources, with the isolator icon and item. The vanilla Redstone entry remains available.

`./scripts/gradle-docker.sh clean build` passed with 85 JUnit tests. The main-only `network` phase passed **152 assertions** with complete saving/state close, including four new checks: actual creative-tab registry identity, icon, visible contents for a non-operator and creative-search contents. Contents are built through the real NeoForge tab-build event, not a test-only item list. Scratch evidence: `run-fieldwork-acceptance/result-network.json` and `state-closed.txt`.

This follow-up is a local build until separately committed/published; the earlier hosted `v0.1.0` artifact does not include it. Actual client tab layout, icon rendering and localized title display still need visual confirmation.

## Pinned environment

Minecraft 1.21.1 / NeoForge 21.1.257 / Create 6.0.10 / Sable 2.0.5 / Aeronautics bundled 1.3.2 / Java 21. Every Gradle verification/extraction checks the three external mod hashes against [`smoke/artifacts.lock`](../../smoke/artifacts.lock).

## Reproduction: main-only automation

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=refresh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=movement
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=lifecycle
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=packed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reset
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-faults
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-index
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-work
# Consecutive independent JVMs; do not run a world-resetting phase between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=reload -PacceptanceKeepWorld
# Consecutive independent JVMs; do not run a world-resetting phase between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reload -PacceptanceKeepWorld
```

Scratch directory: `run-fieldwork-acceptance/`, separate from smoke. Runs reset `acceptance-world` unless explicitly preserved; reload refuses without `-PacceptanceKeepWorld`. Drivers are absent from shipping JARs/ordinary runs. The verifier requires matching phase/status, complete saving and `state-closed.txt`; exit code alone is insufficient. Results are also saved as `result-<phase>.json`; scratch/logs can be overwritten.

| Layer/phase | Recorded result | Evidence and limits |
|---|---|---|
| JUnit | 106, zero failures/errors/skips | `build/test-results/test/`; pure policy/state/index/client helpers; not game behavior |
| core | 8 assertions | `.audit/registry-main-acceptance-first.log`; real MinecraftServer holder, stop/close, research excluded |
| network | 174 assertions, saved/closed | `run-fieldwork-acceptance/result-network.json`; integrated network, train, recipe and controller boundaries |
| refresh | 69 assertions, saved/closed | `.audit/scoped-refresh-release-counters.log`; unrelated bucket/dimension zero updates, fast-path faults/nesting, cross-dimension provenance, candidate/reentry/fallback |
| lifecycle | 23 assertions, saved/closed | `.audit/scoped-refresh-release-lifecycle.log`; real holding load, duplicate load, observer fault, partial deletion, retry, heatmap split, disassembly |
| packed | 24 assertions, saved/closed | `.audit/scoped-refresh-release-packed.log`; real serializer with synthetic keepPacked flags, unregistered WORLD/private windows; not a claim about natural packed-state frequency |
| seed/reload | 24/25 assertions, each saved/closed | `.audit/scoped-refresh-release-{seed,reload}.log`; two craft, each ≥3 plot chunks, two markers, saved UUIDs, startup reconstruction; after ticks A internal15/B internal0/WORLD0 |
| movement / controller index / controller work | 74 / 43 / 15, saved/closed | zero movement/teleport Fieldwork work, targeted indexes/retries, 200-post safety cadence and native TTL removal |
| binding / reset / faults | 177 / 57 / 134, saved/closed | ordinary/managed routing, real result extraction, named quarantine/repair |
| binding seed/reload | 30/27, each saved/closed | consecutive independent JVMs; force-load, revocation tombstone, unloaded reset and authority reconstruction |
| schematic | 348, saved/closed | native headless schematic regressions including pre-swap transform preservation; not a GUI/player rendering pass |

An earlier integration review found a P1 post-publication barrier-release ordering issue. It was fixed and the follow-up gate passed. The remediation regression is preserved in `.audit/main-phase2-remediation-{build,network,lifecycle,packed,seed,reload}.log`: JUnit82 and phase136/23/24/24/25, each checked independently. Do not combine counts across revisions/phases into a single acceptance claim.

## Network phase coverage

- Actual main markers/assembly: internal15, legal external7 versus forbidden15, WORLD→PRIVATE denial, marker0↔1/2→1 and synchronous Create/Simulated refresh before return.
- Per-receiver range: receiver8 gets in-range7, receiver20 may receive its own in-range15. Cold refresh cannot spread another receiver's aggregate. Hot wrappers call Sable's upstream operation once.
- One-shot candidate permission at the precise updater entry; ordinary resolver callbacks/reentrant network calls remain behind barriers. Callback-overwritten outputs require a bounded additional refresh pass.
- BEGIN receiver failure precedes marker writes and retains a named FAILED/PENDING token. Failed A does not block unrelated B or known unrelated frequencies. Named repair validates marker intent and saves before release.
- Failure after candidate publication but before barrier release retains craft/all affected-position indexes. Visual failure is separate: committed PRIVATE remains, WORLD still gets legal7, cold retry restores ACTIVE.
- Real ServerPlayer/vanilla listener with only outbound transport stubbed: accepted plot, rotation-only, too-fast rejected WORLD movement, airborne gap, teleport-ack/private rejected proposal, actual supported WORLD departure, rotated craft, Nether round trip, disconnect and same-UUID new session.
- Real controller packet/ManualFrequencyEntry: world range coordinates retained; handheld/lectern same-craft control; WORLD operator does not turn private lectern input public; release/30-tick timeout; final-marker removal restores upstream disconnect semantics; fresh session input rebinds and refreshes reused entries.
- Fixed-WORLD train adapter: PRIVATE-only high/low, PENDING-only, WORLD-positive precedence, scope/session cache, root expansion, last-member early return without power-version change, cross-dimension/no-range, other global caller unchanged and Low waiting for an unregistered WORLD source.
- Real recipe matching/missing-ingredient rejection/output1, unlock advancement loading, loot1, tool tags, BE registration, WORLD ACTIVE normalization and no ordinary redstone output.

### Fixture/integration failures retained for context

- Initial Simulated15 expectation failed because the source was behind the directional receiver. Moving both sources to the correct side fixed the fixture; direction checks were retained. A server-owned Simulated position index avoids assuming every receiver is in a Create bucket.
- An early callback fixture used a custom non-LinkBehaviour as its own anchor; upstream does not update its own field. A real WORLD source became the nested anchor, observing another receiver.
- The private lectern's WORLD operator initially exceeded Create's approximately1.8-block reach. The fixture moved to real support beside the projected lectern.
- A same-UUID new session reused an endpoint without refresh because upstream `updatePosition` does not recompute. Main now retains the old published scope until post-position reconciliation; the regression passes.

## Historical installer artifacts

### Initial production candidate: 2026-11-06

- File: `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0-dev.jar`.
- SHA-256: `aa67eebec9c0f5c3ab1819cf9540250650314a2a8e60021608ef254eb8824077`, matching `.smoke/server/mods/create_fieldwork.jar` in that run. Only metadata's development-candidate wording changed before final verification; behavior matched remediation regression.
- Real NeoForge installer, Java21 JRE, lock-verified external mods; `mods/` held only those three + Fieldwork. No research/acceptance/fault/JUnit classes in the JAR.
- `18:25:20Z` ready: `.audit/candidate-final-launch.log`; no observed required-mixin or recipe/advancement loading failure.
- Console `fieldwork status` showed no tokens. A real isolator placed with ACTIVE=true normalized to false in WORLD and printed `FIELDWORK_FINAL_WORLD_INACTIVE_OK`; test block removed afterward.
- `18:26:04Z` console `stop`: all dimensions saved, exited/exit0/OOMfalse, `.audit/candidate-final-console-stop.log`.
- First non-TTY attach did not deliver input; PTY attach succeeded. SIGTERM was not used as save evidence.
- Archive checked main classes, strict mixins, assets/lang/models/data/recipe/loot/tags/advancement and absence of test drivers. [Mixin audit](main-mixin-audit.md) records installed transformations.
- Raw hash/inspect: `.audit/candidate-final-artifacts.log`; archive listing: `.audit/candidate-final-archive.log`. These are separate from launch logs.
- Research profile was changed to exclude main; baseline71 passed with `mainImplementationLoaded=false`. Rebuilding after that run-configuration change retained the main JAR hash.

The delivery review passed for “ready for player testing,” not full release acceptance. Missing raw hash/inspect evidence noted during review was subsequently saved in the files above. Later scoped/texture installer evidence has its own hashes and pages.

## Required boundaries

- **D02:** private train consumers deferred; every train condition reads public WORLD only, without Train ownership resolution.
- **D01:** stronger recovery of upstream-lost marker/data is deferred; normal registry state is not persisted. Runtime uncertainty still withholds authorization.
- A detached serialized craft is not checked through live plot block state. Its old footprint remains barred and removal publishes only after the active UUID is absent; this is lifecycle validation, not D01 data recovery.
- **OUTPUT_UNCONFIRMED:** callback failures are not circuit rollback. Some receivers may already have observed authorized results; do not claim all outputs are zero.
- `/fieldwork status` and `/fieldwork repair <token>` require permission2. Repair verifies original marker intent/unwritten baseline and saves; it refuses without proof, rather than force-clearing barriers.
- Active loads/incomplete publications with unknown frequency are conservatively pending. Confirmed WORLD-positive can still complete High. Queries do not scan craft blocks; enumeration is cold-path work.
- **Liveness limit:** long-lived unregistered WORLD transmitters of unknown frequency can defer other Low queries until registration/removal/unload. Known-frequency failure isolation is tested; precise indexing for unknown frequencies is not claimed.
- Arbitrary third-party endpoints with false `isAlive=true`, wrong coordinates or perpetually unstable callbacks are not verified.

## Outstanding player acceptance

1. Creative entry, English/Traditional Chinese names/tooltips, item/block rendering and gold/red ACTIVE synchronization.
2. Real workbench `OBO/ELE/BPB` crafting, obtaining a Link to unlock, correct-tool mining/drop.
3. World/same-craft/two-craft isolation in both directions with actual frequency/range boundaries.
4. Multiple markers, replacement, actual explosions, final removal/replacement while strong external power remains present.
5. Real handheld/lectern keys/GUI, boarding/departure, rotation/held/release/timeout, logout/rejoin, dimension changes and multiplayer/codec behavior.
6. Actual train-schedule High/Low behavior with public sources; private positives remain invisible.
7. Real craft disassembly/reassembly/split, unload/return and dedicated-server console-save/restart, observing stale power and ACTIVE.

Headless fixtures do not complete these rows. Use the [player guide](player-test-guide.md) to record results.
