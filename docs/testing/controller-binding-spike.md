# Controller identity and block-owned authorization experiment

**2026-11-07: feasibility established within the limits below, not a v0.2
release or production acceptance.** No production Java/resources were changed
for this experiment. Existing unrelated worktree changes were retained.

## What was implemented

`src/tools/binding-probe/` is a separate non-shipping experimental tool. It loads production main,
but neither `fieldwork_acceptance` nor the historical `fieldwork_probe`.
Normal client/server and main acceptance do not load it.

- A test authorization block/BE owns a set of controller device IDs and the
  actual containing Sable instance UUID stamp. This is not a finished brass
  isolator: the fixture separately retains a real ordinary isolator.
- Controller stacks contain only an experimental device UUID in custom data,
  not a target ship UUID. Successful server-side authorization creates or
  reuses that ID. Production should choose a versioned component/schema.
- A server-owned, fixture-driven reverse index reconstructs ID → target lookup
  from valid loaded block records. Missing managed IDs resolve `BLOCKED`, not
  `ORDINARY`; conflicting valid targets also resolve `BLOCKED`. These are
  candidate experiment policies, not newly approved product decisions.
- Two strict test-only Create mixins preserve ID and observe input. They do
  not modify production scope answers, reroute input, or bypass native checks.

## Executed evidence

Pinned Minecraft1.21.1 / NeoForge21.1.257 / Create6.0.10 / Sable2.0.5 /
Aeronautics bundled1.3.2; no dependency/version changes.

| Check | Result |
|---|---|
| Final `clean build` | Passed; JUnit **85**, zero failures/errors/skips (75 policy + 7 server + 3 refresh tests) |
| Experimental seed | **41** assertions passed; final save completed 15:10:57Z |
| Experimental reload, independent JVM, same world | **39** assertions passed; final save completed 15:11:30Z |
| Normal main-only network/controller regression, experiment absent | **152** assertions passed; repeated after final fixes, complete save and state close at 15:13:53Z |
| Shipping archive isolation | `verifyShippingIsolation` passed during final clean build |

Counts are separate evidence layers, not one combined acceptance total.
The network152 count includes pre-existing worktree fixture changes; this
experiment did not add four tests to production network acceptance.

The seed/reload verifier checks report kind, phase, exact count, every assertion,
complete dimension saves, and main server-state close. Reload also requires a
matching successful seed report and a different JVM start timestamp. Its lookup
uses the UUIDs returned by real Sable assembly, not replacement fixtures.

### Native Create paths actually exercised

1. **Negative control:** disable the experimental patch for one fresh lectern.
   Native `setController/getController` preserves programmed frequencies but
   loses the arbitrary device ID. This confirms why an explicit adapter is needed.
2. With the patch, set/get retains ID and frequencies. Native
   `saveWithFullMetadata` → `BlockEntity.loadStatic` produces a **fresh** Create
   lectern BE with the same identity. An independent JVM reload then confirms
   actual world persistence, not just an in-memory NBT round trip.
3. `writeSafe` carries the ID; safe-template placement itself was not exercised.
4. Real `LinkedControllerInputPacket.handle` observes the same ID for handheld
   and lectern-selected stacks. Input before `tryStartUsing` is still rejected.
   A validated active operator can send lectern input.
5. Handheld backend entries are released and expired before lectern input;
   the fixture asserts they are empty, then requires a fresh native pressed15
   entry. A leftover handheld map entry cannot satisfy this assertion.
6. Native `swapControllers` returns the same ID/frequencies to the empty hand.
   Native `dropController` produces an actual matching ItemEntity. Both paths
   clear the old ID from the emptied lectern; the test restores a controller
   afterward for the saved-world reload.

### Block authority and UUID checks

- Two same-frequency controllers obtain different IDs, both persisted in A's
  block-owned list. Reauthorizing the same stack retains its ID.
- After reload, the new reverse index initially denies managed input until it
  is explicitly rebuilt from the loaded block record.
- Copy A's saved authorization record into a BE physically contained in real
  craft B. B has a different actual Sable UUID; the old stamp is rejected.
  Removing A's index record leaves the device blocked, despite that copy.
- Explicitly granting the same ID on both A and B yields a conflict and denial,
  not whichever ship loaded last. Removing the conflict restores A.
- Physically remove A's authorization block while retaining its ordinary
  isolator. Main still reports A as PRIVATE; explicit candidate-index removal
  blocks the managed controller. Restoring the saved block can rebuild A's entry.
  **This tests the candidate invalidation model, not automatic source withdrawal.**

### Reload setup correction

Initial runs with only vanilla `setChunkForced` did **not** reload the two saved
UUIDs; increasing the fixture to a three-chunk plot did not fix that. Those
reload failures are not passes and do not establish ID data loss.

Audited Sable `ServerSubLevelContainer.initialize/loadForceLoadedSubLevels`
loads saved Sable tickets/pointers; vanilla forced chunks do not create these
tickets. Holding-chunk loading is a separate route. The final fixture installs
real persisted Sable `COMMAND_FORCED` tickets through `addForceLoadTicket`.
Both UUIDs then load without reconstruction in the next JVM. Thus the result
proves persistence **when the target is actually loaded under this explicit
fixture**, not universal automatic loading of arbitrary un-ticketed ships.

## Reproduction

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyBindingProbe -PbindingPhase=seed
./scripts/gradle-docker.sh verifyBindingProbe -PbindingPhase=reload -PbindingKeepWorld
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyShippingIsolation
```

Keep seed/reload consecutive and do not reset their world between JVMs. The
dedicated scratch directory is `run-binding-probe/`, with `binding-world/`,
`seed.json`, `result-seed.json`, `result-reload.json`, `state-closed.txt` and logs.
It is ignored/local, not a distributed player world. Do not load it without the
experimental mod. The final checked JAR remains version0.1.0 and contains no
binding prototype classes/config/metadata; there is no new playable binding JAR.

## Not established / next production work

- **No remote PRIVATE signal routing:** `BindingIndex.resolve` is not connected
  to main's controller scopes. The native pressed15 backend observation is not
  proof that an authorized ship receiver was powered from outside.
- Automatic index lifecycle publication/removal, registry readiness/barriers,
  unload/split/move/reassembly/repair, last-brass source withdrawal and receiver
  refresh are not implemented. Fixture calls explicitly publish/remove records;
  the cached index would be stale without those calls. Never ship it as-is.
- Movement-only refresh shortcuts must eventually account for device/mode/
  authorization changes. Distance, release/timeout, session/disconnect and
  multi-player/ownership policy need new production-level evidence.
- Authorization UI/permissions, recipes, real brass/andesite assets, data
  migration/malformed-tag handling, rollback on exceptional upstream mutation,
  creative/NBT ID-copy policy, cross-dimension behavior and client sync are absent.
- The persistent lectern is a WORLD fixture; a lectern carrying an assigned ID
  through actual Sable assembly/movement/packing is not proved by this run.
  Drop fallback is tested, not every destruction/explosion/loot route.
- UUID rejection uses a real different craft plus a copied block record, not a
  complete schematic-copy or disassembly/reassembly operation.
- No offhand blueprint/color-outline code or client/GUI verification was added.

The experiment supported the later narrowly scoped production design. Current
decisions are recorded in the [controller binding contract](../design/controller-binding-v02.md);
this historical experiment does not override that contract or authorize publication.

Read-only integration review passed for this experimental scope. Its backend
contamination and seed-report validation findings were corrected and rerun;
the reviewer did not independently replay the runtime evidence.
