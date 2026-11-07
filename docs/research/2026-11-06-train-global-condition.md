# D02 — Train Redstone Link Global Conditions

**Current approved scope:** every V1 train condition is a public WORLD observer. Private train consumers aboard craft and Train/Carriage ownership research are deferred. Source visibility, cross-dimension/no-range behavior, high/low, PENDING and epoch/session cache remain required and have separate [production acceptance](../testing/redstone-link-isolator-acceptance.md).

This page preserves the **2026-11-06 non-shipping research**, including a superseded private-consumer requirement, train95 unsafe controls and an unaccepted train-player fixture. Those results do not verify the later fixed-WORLD implementation.

## 1. Decision history

The owner first required this query to be included rather than excluded. A later research-stage decision asked for trains aboard private craft to read that craft's private network. That prompted Train ownership/anchor research. Following counterexamples and player-scenario investigation, the owner explicitly simplified V1 to fixed WORLD observers and authorized main implementation.

The final decision removes private consumer membership as a prerequisite, not source protection for the whole condition. A fixed-WORLD observer is an intentional contract, never an unknown-ownership fallback.

## 2. Exact upstream behavior

Class: `com.simibubi.create.content.trains.schedule.condition.RedstoneLinkCondition`.

```text
tickCompletion(
  Lnet/minecraft/world/level/Level;
  Lcom/simibubi/create/content/trains/entity/Train;
  Lnet/minecraft/nbt/CompoundTag;
)Z
```

`.audit/bytecode/create-consumers.txt:103–139`:

1. Read context LastChecked (default−1).
2. Read handler globalPowerVersion.
3. If equal, return false at39, **not current power state**.
4. Otherwise write LastChecked, then call hasAnyLoadedPower(Couple) at56.
5. Return `hasPower != lowActivation()`.

Train argument is unused. The global method scans same-frequency buckets across registered loaded levels, asks only positive strength, and checks neither range nor isAlive. Create pair/Simulated hooks do not cover it.

## 3. Visibility and cache requirements

Current fixed-WORLD semantics:

| Confirmed WORLD positive | Potentially visible pending source | High | Low |
|---|---|---|---|
| Yes | Either | Complete | Wait |
| No | Yes | Wait | Wait |
| No | No | Wait | Complete |

PRIVATE-only is absent to this consumer. PENDING must not become absence and complete Low prematurely. No receiver distance, Train anchor or nearby-block scan is added.

Narrow integration wraps this condition's cache/pending handling and its single global-power call, leaving other callers unchanged. Use the passed Level's logical server and loaded dimensions; no cross-server static visibility. Each source uses constant-time scope lookup; the global query still enumerates its same-frequency sources, like upstream.

Scope-only changes may not have listeners or increment globalPowerVersion. Track a server-owned source epoch/session alongside Create's version; pending results do not commit a decisive checked stamp. Begin/expansion/publication/release/source invalidity must invalidate appropriately, and server restart invalidates old session stamps.

Endpoint registration can lag chunk/packed-BE loading. An empty current bucket does not prove absence; cold publication state must account for known or unknown frequency without query-time block scanning. A long-lived unknown-frequency WORLD source can conservatively defer unrelated Low conditions; this liveness boundary is documented in production acceptance.

Upstream removeFromNetwork may return early after the last member without incrementing power version (`create-wireless.txt:97–124`). The condition-local epoch needs a test for that boundary; do not silently fix all global callers.

### Superseded private-consumer proposal

Research would have resolved Train scope: WORLD reads WORLD; PRIVATE(A) reads only PRIVATE(A), no added range; unresolved/mixed consumer scope makes both polarities wait even with WORLD-positive. Confirmed consumers used the same positive/pending/absent tri-state table within their own scope.

It needed trustworthy whole-Train/carriage ownership across loaded/unloaded/dimensional sides, not first carriage, AABB or caller Level. The prototype derived this on each query; production would have required a cold-path server-owned index and expected carrier UUID. Those requirements are deferred with private consumers, not implicitly claimed solved.

## 4. Train prototype and accepted observations

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=train
```

Sources: `src/testmod/.../TrainProbe.java`, `ProbeTrain.java`, `mixin/ProbeTrainConditionMixin.java`. No main feature was introduced by this research.

The fixture creates real Train, Carriage, Bogey, TrackGraph, TravellingPoint and CarriageContraptionEntity, invokes real conditions and Train.write/read. Membership enumerates all carriages/dimensional sides using non-creating getDimensionalIfPresent, checks live bindings/anchors and treats mixed/missing data as PENDING. It is not natural station assembly/driving or independent-JVM Train reload.

Early setup failures: missing contraption bounds (`15:54:52Z`), anchors and uninitialized MountedStorageManager. Adding a real structure block/bounds/anchors and carriage.storage.initialize fixed fixtures. In inactive chunks getEntity(UUID) may be null; the prototype rejects nonnull mismatched instances while still checking liveness/Level/train ID/index/binding/anchors. That does not prove natural movement behavior.

- `16:08:15Z`:71 assertions, `.audit/train-probe-seventh.log`: WORLD/A/B, sameA, high/low, mixed consumer PENDING, multiple same-scope carriages, markers, offline **plot** anchors, real NBT round trip, epoch/session, source/consumer pending, range2 retaining global behavior, cross-dimension sources/missing portal anchors.
- `16:23:17Z`:88 assertions.
- `16:25:47Z`:95 assertions and full save, `.audit/train-probe-ownership-negative-final.log`. Includes **UNSAFE** controls below. Scratch: `run-scope-probe/scope-probe-result-train.json`.

## 5. Counterexamples and cache controls

**An unsafe assertion passing means a defect was reproduced, not fixed.**

1. **Cold projected-carrier provenance loss:** controlled Sable tracking makes a live WORLD-anchor carriage PRIVATE(A). After real unload, the warm object stays PENDING; Train.write/read creates a new carriage without WeakHashMap hint, incorrectly WORLD. WORLD7 completes cold High while warm waits. This proves an object-identity/cold-read gap, not a natural tracking producer or real-JVM restart.
2. **Plot reuse grants a replacement UUID:** save offline Train in A, remove A, serializer-load new UUID B into the exact plot and assert A is absent/current owner B. Cold old anchors adopt B and complete High on B-private15. Exact serializer-based replacement was demonstrated, not allocator timing/frequency in normal play.
3. **Root begin/PENDING cache control passed:** cache WORLD Low absence, begin a private-source root, observe epoch increase and requery/wait. Repeated PENDING queries do not become decisive cache. The suspected missing begin invalidation was disproven. Known WORLD-positive overriding other source pending follows the intended tri-state rule.
4. **Same-root expansion skips rescan:** cache decisive WORLD-positive under an unrelated root, then addDestination quarantines that source without changing power version/epoch. Same context skips the query; fresh context sees PENDING. Upstream cached return is false, so **no incorrect High/Low completion/leak was established by this case**. It proves stale in-root cache timing that needs expansion/publication/failure tests.

Private-consumer integration gaps at that point: server-verifiable expected carrier UUID/cold provenance; query-time carriage enumeration incompatible with production O(1) membership; complete epoch notifications; natural station/driving/carrier and independent-JVM reload; late publication/last-member/unrelated-failure isolation. Prototype static state/session/WeakHashMap/global-depth pending could not be moved into production.

Regression after the negative controls: suite71 complete save at `16:26:25Z`, `.audit/suite-after-train-negative.log`; intentional Plot already exists remained a load-negative fixture. Build passed in8s with hash verification (`.audit/build-after-train-negative.log`), but tests were NO-SOURCE at that stage. The research task was later cancelled as a V1 prerequisite, not marked safe.

## 6. Player-scenario reachability investigation (incomplete)

The owner requested natural-flow investigation rather than treating artificial negative-control counts as occurrence rates.

Static clues:

- Carriage.updateContraptionAnchors derives dimension rail coordinates from bogey/TravellingPoint/TrackNodeLocation (`train-Carriage-code.txt:851–1045`). No Sable rewrite to projected WORLD anchors was established. Natural rail graphs retaining plot coordinates might not satisfy the controlled cold-WORLD premise.
- Generic Sable EntityMixin can project setPos and establish tracking (`controller-tracking-producer.txt:595–648`), but natural CarriageContraptionEntity reaching that producer was not shown.
- allocateNewSubLevel uses getFirstEmptyPlot: first unoccupied bit in a fixed order (`player-SubLevelContainer.txt:207–244,321–340`). Plot reuse has a natural route, not a random UUID collision. However stale Train survival after ordinary craft/rail removal was unproven.
- Same-root cache control showed no incorrect completion or natural trigger frequency.

### Unaccepted train-player phase

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=train-player
```

PlayerTrainProbe uses real TrackBlock/natural rail graph, StationBlockEntity.assemble, bogey/controls/glue, without new Train or tracking setters. TargetTrack NBT substitutes placement UI; vanilla force-load substitutes nearby players, so even a pass would not establish client acceptance. JSON trainObservations records anchors/entity positions/tracking/membership.

Attempts:

- First/second WORLD assembly: nothing_attached; added real glue.
- Glued: no_controls; orienting controls toward assemblyDirection produced a WORLD Train.
- Controls/support/connected PRIVATE attempts later saw void_air. Only plot chunks were forced, not parent chunks; no upstream incompatibility was proven.
- Parent-loaded `16:40:44Z`: station block persisted but getStation()==null, failing the real graph-edge-point assertion. `.audit/train-player-parent-loaded.log`. Stop reached Saving worlds then120s timeout, **no complete-save evidence**; not accepted. Subsequent inspection found no project probe container running.

No accepted train-player result exists. WORLD assembly is partial fixture progress, not proof of private driving/unload/restart/natural tracking or occurrence rates.

| Gap | Reachability evidence | Conclusion |
|---|---|---|
| Cold projected-WORLD provenance | Controlled tracking only; natural anchors appear rail/plot-derived | High impact; normal trigger unknown, neither common nor rare established |
| Old plot anchors adopt replacement UUID | Natural allocator reuse; stale Train survival unproven | Cannot dismiss based on “unlikely plot reuse”; normal teardown needs investigation |
| Same-root expansion cache | Artificial transaction; no wrong completion | Timing gap, not demonstrated player leakage |

The historical next step was private station graph registration (TrackTargetingBehaviour/TrackPropagator/saved target), then natural movement/teardown/allocator/restart. Work stopped under a reported goal budget limit (configured200000, reported usage5337701); that tool state was not product policy. The later owner decision deferred this ownership branch. Source visibility/cache work continued in the separate fixed-WORLD main adapter.
