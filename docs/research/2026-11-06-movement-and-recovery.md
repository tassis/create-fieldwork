# Accepted Movement, Transaction Isolation and Failure Restart — 2026-11-06

**Historical research in `src/testmod/`.** Production main was not functional at this stage; it was integrated later. Use [production acceptance](../testing/redstone-link-isolator-acceptance.md) for current evidence and the [V1 design](../design/redstone-link-isolator-v1.md) for current guarantees.

**Decision update:** the owner ultimately approved D01 as optional/deferred. Strong upstream data-loss recovery and an exceptional journal are no longer V1 prerequisites. This supersedes the earlier temporary preference for a journal; the stronger recovery discussion below is retained as history. Fieldwork remains responsible for its own registry/refresh correctness. D02 later became fixed-WORLD train observation.

## Summary

Two uncertainties became reproducible findings:

1. Controller ownership must distinguish speculative Sable tracking from accepted vanilla movement. Normal authorization compares instance/UUID; unresolved transitions are PENDING, not null→WORLD.
2. Root transactions can fail/repair independently, but process-memory quarantine cannot preserve isolation intent across restart after every marker has physically disappeared. The restart control reproduced this loss of information.

These are prototype findings, not client/UI, ordinary-block-event, I/O-failure or power-loss acceptance.

## 1. Why FakePlayer was insufficient

Pinned NeoForge21.1.257 sources show FakePlayer installs FakePlayerNetHandler (`FakePlayer.java:97–101`), whose handleMovePlayer is empty (around269); teleport/send also have no-op behavior. Calling that listener cannot prove vanilla/Sable acceptance.

The revised fixture uses a real ServerPlayer and ServerGamePacketListenerImpl, adds the player to a real ServerLevel and submits packet objects on the server thread. Only outbound transport is a test sink. Movement handling, collision, teleport state and acceptance checks remain real. This is not codec/login-handshake/client validation.

Extracted sources: `.audit/vanilla/{FakePlayer,ServerGamePacketListenerImpl,Entity}.java`.

## 2. Tracking changes before acceptance

Sable client LocalPlayerMixin preSendPosition inverse-projects WORLD to plot coordinates while tracked, then restores WORLD position after sending. Untracked input sends WORLD coordinates.

At server handler HEAD, Sable resolves packet x/z, mutates current tracking, projects valid plot positions and clears tracking for ordinary WORLD or unowned reserved plots. Vanilla can still reject afterward:

- moved-too-quickly: source lines900–907, teleport back/return.
- collision/moved-wrongly:940–980, accept or teleport back.
- pending teleport acknowledgment: movement ignored even though Sable HEAD already changed tracking.

**Observed negative control:** a too-large WORLD move was rejected; real player position stayed unchanged but current tracking became null. The old controller prototype emitted WORLD15. Conversely, an ignored plot proposal during pending acknowledgment set tracking without accepted movement and could not legitimately authorize PRIVATE.

## 3. Acceptance evidence and PENDING candidate

ProbeMovementMixin uses WrapMethod to retain the raw packet's server-resolved plot UUID/WORLD origin before projection. It records success only after the single vanilla `lastGoodZ` PUTFIELD, after successful position/chunk/support/statistics updates. Full descriptor/single-match are pinned; upgrades need reaudit.

Rejected/ignored input does not overwrite last accepted evidence. An unaccepted private proposal cannot grant membership. The input context is server player/device provenance, with no new client scope UUID.

Candidate rules:

- Valid normal membership compares SubLevel instance/UUID and retains world range coordinates.
- While processing potentially changed private ownership or an unresolved rejected proposal, withhold authorization.
- Loss of protected private association becomes PENDING, with no timer-based WORLD release.
- Accepted plot movement can restore craft control.
- Conservative WORLD release needs accepted WORLD position and vanilla-computed ordinary-world support (`Entity.mainSupportingBlockPos`), not getOnPos fallback or packet onGround alone.
- Support checks are movement-acceptance cold work, not craft-AABB membership authorization or a query-time block scan.
- READY(empty) clears private restrictions and restores upstream behavior. The original candidate left ordinary WORLD actors without private history unaffected; current design also protects unaccepted private proposals from WORLD.
- Connection close retracts old private input before entity removal; upstream30-tick timeout remains.

Uncertain airborne/departure gaps can pause control. Evidence is server-accepted state, not a prediction of unreceived client actions.

## 4. Controller-motion results

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=controller-motion
```

`.audit/controller-motion-support-proof.log`: **30 assertions**, complete save:

- Real accepted plot boarding, same-craft15/WORLD0.
- Rejected WORLD/null-tracking negative control, then PENDING repair with WORLD0.
- Accepted plot restores control; accepted airborne WORLD does not grant WORLD.
- Accepted WORLD with real support produces craft0/WORLD15.
- Ignored private proposal during teleport acknowledgment grants no craft scope.
- Getter disappearance remains closed; READY(empty) restores upstream; returning marker does not guess WORLD.
- Closed connection retracts private input before player removal.

`.audit/controller-motion-embedded.log` extended to **37 assertions**, complete save: logical/physics rotation, actual `changeDimension` Nether round trip, old-dimension entry withdrawal and restored same-instance control. Dimension API returns Entity, requiring checked ServerPlayer conversion. Initial outbound sink lacked a Channel and failed NeoForge dimension-sync attribute access; an in-memory EmbeddedChannel fixed the fixture. No client connection or overridden dimension/movement validation.

An earlier attempt to force rejection by moving into a solid marker did not establish its premise and failed; the fixture switched to the real awaiting-teleport branch. It is not an accepted collision-rejection case or Sable defect.

Bare actor.move still did not establish tracking. Evidence comes from the real listener, not FakePlayer setters. The old controller40 mode retains its unsafe control and is not this corrected movement test. Main needed server-owned UUID/session/connection state and cleanup instead of static WeakHashMap/strong player references; that was subsequent implementation work.

## 5. Independent root transactions and repair

ProbeLifecycle was revised to give each root:

- Token, affected CraftKeys/positions, marker-transfer ledger and failure state.
- Position/craft→token indexes for constant-time quarantine lookup.
- Shared root for normal nested moves/split children, independent completion for unrelated roots.
- Reconstruction limited to affected craft; unrelated success cannot clear failure.
- Named repair validating every destination marker and removed source.
- A flush `saveEverything(...,flush=true,...)` before releasing that token. This is normal save ordering, not I/O/power-loss durability.

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=recovery
```

At `13:34:11Z`, **19 assertions** plus saving passed:

- A destination-marker failure retains a named token and cannot recover before repair.
- Unrelated B can commit READY/WORLD and emit15 while A remains closed.
- Repair releases A only.
- One lost marker among two cannot be hidden by the survivor; all ledger requirements must pass.
- Source-clear failure leaving duplicate source/destination markers refuses repair until source is removed.
- Final pending-token set is empty.

This validates prototype feasibility, not every overlapping/cross-dimension composite operation or missing callback in production.

## 6. Expected-unsafe cross-JVM marker-loss control

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=seed-failure
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=reload-failure -PprobeKeepWorld
```

**Passing this test means the hazard was reproduced.** JSON has `expectedUnsafeObservation=true`; verifier prints `EXPECTED UNSAFE ... not a safety acceptance`.

Seed5:

1. Destination marker write throws; Sable catches/continues.
2. Source is also cleared: neither position has a marker, but craft/source15 survive.
3. In-process token denies external output (0).
4. Save/stop without repairing markers or writing a recovery journal.

Reload7 (`13:35:58Z`): independent JVM automatically loads the same saved craft. No marker and no memory token remain. Identity-only reconstruction publishes WORLD; **external receiver actually gets15**.

Fixture JSON stores only UUID/coordinates to locate the craft, not quarantine reconstruction. It is not a hidden recovery journal. Runtime transfer ledgers cannot recover intent from entirely missing blocks after process restart.

## 7. Recovery-policy history

The original contract required block-based reconstruction without a permanent normal isolation database. Exceptional persistence could not be introduced silently.

The stronger proposed journal would record unfinished-transfer intent **before destructive writes**, including token, dimension, source/destination positions, related/provisional craft identity, expected marker identities and phase. Startup would load constant-time pending indexes before endpoint publication; corrupt records could not mean “none.” Normal READY/count stays derived from blocks; remove exceptional records only after verified repair/save. Interruption/write-order/I/O tests would be required: setDirty is not durability.

Alternatives considered:

- Prove crash-safe atomic transfer/rollback, including partial saves and rollback writes that themselves fail.
- Explicitly scope recovery to actual surviving block facts: after trustworthy reconstruction, complete physical marker loss means WORLD rather than preserving an impossible former intent.

The owner briefly selected a temporary journal, then explicitly approved **D01 optional/deferred** after discussing general upstream transfer risk. The latter defines V1. This is not proof of the stronger guarantee, and the negative control remains unsafe evidence. Fieldwork failures/incomplete scans must not be mislabeled physical loss or READY(empty).

## 8. Regression and local evidence

After per-root refactoring: suite71, controller40, edges19 and multi200/207 regressed successfully. The final earlier set included motion30/recovery19/suite71/failure5/7 and clean build; motion later extended to37. Accepted probe runs require complete saving/no shutdown failure. Failed constructor/channel fixture attempts are excluded.

At this historical stage the main JAR was still nonfunctional and excluded testmod. Current production status is recorded elsewhere. Local logs: `.audit/controller-motion-*.log`, `.audit/recovery-*.log`, `.audit/failure-*.log`; phase JSON: `run-scope-probe/scope-probe-result-<phase>.json`. These ignored files may be overwritten; this page retains conclusions, limits and commands.
