# Controller routing and event-driven refresh (updated 2026-11-09)

This revision reduces Fieldwork's additional work while using a controller on a moving craft. It adds no binding UI, permanent index or tick delay, and does not change Sable projection or native distance/strength calculations.

## Stable position-only path

A controller update selects only receivers in `PRIVATE(A)` when all these observations agree:

- old actual, new actual, old published and post-reconciliation published scope are the same `Private(A)`;
- player UUID, server session, dimension and handheld/lectern provenance are unchanged;
- lectern position/craft and its weak BE referent match by identity where applicable;
- native frequency/level match and the input is alive before and after;
- only the original world-space position changed, and reconciliation did not already refresh.

Sources in Create's same-frequency bucket are still evaluated by upstream code. Fieldwork only filters **which receiver updater calls it initiates**; it does not filter away source iteration, replace aggregation, change `getLocation`, or assume rigid motion preserves rounded world-coordinate distances.

Scope/key transitions, PENDING, source/session/liveness changes use the full old/new frequency scope. Restricted provenance changes refresh even when position is unchanged. Pure WORLD position-only handling remains upstream behavior; unchanged private input also causes no extra receiver refresh.

## Selection and reentry

`RefreshScope.Selection.receiverFilters` optionally maps a selected frequency to private `CraftKey` receiver sets. No map entry means all receivers; an explicitly empty set means none. A dimension-wide fallback selects all frequencies/receivers.

Union rules distinguish an operand that did not select a key from one that selected it unrestricted. Unrestricted work dominates only shared selected keys; private sets union; disjoint keys retain their own filters. Selection maps/sets are defensively copied.

Filters gate invocation, not Simulated discovery or stale cleanup. An unknown-frequency Simulated entry is eligible if any selected network is unrestricted or its private scope occurs in the restricted-filter union. WORLD/PENDING does not match a private set. This unknown-key rule has algebra coverage; there is no new behavior-null Simulated runtime fixture.

Nested ordinary updater calls request unrestricted network work; explicit full requests also widen the next synchronous pass. Private A+B requests union their receiver sets without adding WORLD. Existing one-shot candidate permits and non-inheriting immediate callback frames remain unchanged.

## Evidence

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=refresh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=movement
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=lifecycle
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=packed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=reload -PacceptanceKeepWorld
```

The final integrated rerun records clean build/JUnit **103**, network **174**,
refresh **69**, movement **74**, lifecycle **23**, packed **24**,
controller-index **43**, controller-work **15**, and independent-JVM
seed/reload **24/25**. Each runtime verifier checked complete saving and
server-state closure. Counts belong to separate phases, not a summed score.

Movement and same-dimension teleport/ack now perform zero Fieldwork controller
reconciliation. Ordinary handheld input remains WORLD; the next native packet
uses the server player's current position. Endpoint/device/lectern/lifecycle
events drain targeted dirty/retry work, while one complete diagnostic safety
sweep runs every 200 server-post ticks. The index/work phases prove no ordinary
per-Create-tick Fieldwork full scan, 199 sweep-free posts, exactly one sweep on
post 200, native-only adoption, index-only purge, targeted heartbeat, and exact
native TTL removal.

The movement driver uses a real ServerPlayer/vanilla listener, actual controller packets and two Sable craft plus WORLD receivers on the **same frequency**:

- A's Create/Simulated receivers are refreshed; B's and WORLD's per-position targeted cold calls are zero.
- B retains legal9 and WORLD legal7, including Simulated outputs, rather than being zeroed.
- A immediately receives15 when moving into range and loses stale15 when moving out. Native Simulated direction/raw recomputation agrees.
- A full-frequency negative control exercises all groups. The small fixture observed **3 private-path updater calls versus 9 full-frequency calls**. The assertion requires private work below baseline plus exact outsider-zero checks; it does not hardcode a universal3:9 ratio.
- Unchanged input has zero cold calls. Same-position handheld→lectern, same-position same-UUID replacement session, and dead→alive input all use full-frequency fallback.
- Native-key selection algebra covers ALL/NONE/private/disjoint/unknown/copy cases. Runtime callbacks test private A+B replay and unrestricted ALL widening.

The first range fixture used an exactly3-block boundary; Sable's floating projection made that an unsuitable far-point assertion. The accepted fixture uses a clear2-block threshold with near/far positions, preserving rather than modifying upstream comparisons.

Readonly integration review found no blocker. Remaining work is still linear in the selected frequency bucket and existing Simulated position index, plus upstream source scanning per target receiver. Native Simulated per-tick work remains upstream and is not counted as Fieldwork cold refresh. **No timing/MSPT or FPS improvement percentage is established.**

## Local artifact, not a tag publication

This remains a **local worktree build**, not evidence that the existing tag or
hosted Release contains these changes. No commit, tag modification, push or
release was performed.

- JAR: `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar`.
- SHA-256: `1ca147fb51c416f1d28b0173cfee1e572d468942bef11042b675df1648913e26`.
- Build/install hashes match; shipping isolation excludes acceptance/research drivers and the archive includes `LICENSE`.
- Packaged installer reached `Done (0.741s)` at **02:59:47Z**; console status reported no withheld transactions and console `stop` saved all dimensions at **03:01:51Z**; the container exited 0.
- Client/GUI/multiplayer and real-load performance remain unverified. Fully close the game/server before replacing JARs; use the [player guide](player-test-guide.md).
