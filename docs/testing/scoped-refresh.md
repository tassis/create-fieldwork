# Dimension/Frequency-Scoped Refresh — 2026-11-07

This records the scoped-refresh **logic revision**. A later texture-only candidate retained all 58 Java-class bytes; see [texture evidence](placeholder-textures.md). The historical765e hash below identifies this logic build, not `0.1.0` or a later artifact.

The change reduces additional Fieldwork refresh work while retaining Sable/Aeronautics movement/projection, synchronous transition timing and upstream update algorithms.

## Implementation

- Immutable `RefreshScope` selects dimension → native frequency sets, with all-in-that-dimension fallback. Refresh touches selected Create buckets and confirmed same-frequency Simulated receivers. An explicit full-server API remains for intentional full refresh/test setup.
- Transactions collect old scopes **before** first touch/registry.begin, add destination scopes before writes and final scopes after reconstruction, and retain old/new key/dimension unions through repair. Collection scans involved endpoint indexes, not craft blocks to discover frequencies.
- Controller tick reconciles its level, movement its UUID across dimensions, rebind its entry. Published snapshots/empty selections are reused rather than rebuilt on every unchanged tick.
- Marker changes include related controllers' old dimensions/keys via confirmed/proposed/live tracking/last-private/lectern CraftKeys, not player WORLD coordinates. Cross-dimension old entries are withdrawn/restored immediately without adding unrelated keys.
- Candidate access uses the existing one-shot targeted permit; private provenance restrictions read the same fenced candidate. Ordinary/null-view callbacks remain PENDING.
- Reentrant updater/explicit refresh work carries actual level/key and merges into the next synchronous pass. The existing 8-pass bound and failure barriers remain.
- READY `1→2`/`2→1` mutations skip receiver recomputation only with another actual marker witness, consistent index/block and no active/relevant barrier. The set is adjusted temporarily and reconciled against actual blocks in `finally`; stale onRemove/onLoad cannot overwrite newer block facts. First/last markers retain barriers/ledger. No-op/throw/nested replacement are tested.
- Load, affected unregistered sources or unknown frequency fall back to involved dimensions, not every server dimension.

## Performance/behavior limits

1. **Pure WORLD position updates retain upstream semantics.** Pinned `ManualFrequencyEntry.updatePosition` changes position/TTL without a network update (`.audit/bytecode/controller-create.txt:213–223`). This revision adds no WORLD-only cold refresh or general range-notification fix. Restricted moves refresh their key; scope/key changes refresh both sides.
2. **Simulated index traversal is still linear.** Selected-dimension position indexes are filtered by frequency; no permanent frequency reverse index was added. Known unrelated keys do not invoke `updateSignal`; unknown/incomplete behavior is conservative. Zero updater calls does not mean zero index traversal.
3. Scope collection still scans involved endpoint tables/controller metadata. Non-boundary markers update registry/revision and copy position sets; they are not zero-cost.
4. Dense same-frequency networks may still approach receivers × endpoints work. Unknown-frequency WORLD publication may defer other Low conditions; that policy is unchanged.
5. **No MSPT/time benchmark or improvement percentage is claimed.** Evidence below measures actual updater calls and behavior regression.

## Reproduction and results

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=refresh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=lifecycle
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=packed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=reload -PacceptanceKeepWorld
```

Final `.audit/scoped-refresh-release-*.log`: JUnit **85** (75 policy +7 state +3 selection, zero failures), network **148**, refresh **69**, lifecycle **23**, packed **24**, independent-JVM seed/reload **24/25**. Saves/state close are checked separately; counts are not added into one release matrix.

Refresh-phase counters observe main's targeted cold refresh and do not change authorization:

- Explicit full-refresh negative control hits frequencyB and Nether, proving counters detect the previous broad work scope.
- A's first/last marker: same-dimension B gets zero Create/Simulated updates; unrelated Nether gets zero total cold calls. Legal7/public15 still switch immediately.
- `1→2`/`2→1`: zero cold receiver updates. Failed/no-op additions/removals restore actual indexes.
- Nested last-marker removal still enters a synchronous boundary; stale removal after same-position replacement cannot erase the new marker.
- Old keys remain selected after endpoint disappearance/frequency change; new keys and old/new dimensions join the union.
- A callback adds another frequency; a subsequent pass actually updates it without expanding unrelated dimensions.
- Pending fallback expands only involved dimensions; controller-only frequencies and live-tracking-only provenance are included.
- Old controller entries in another dimension are withdrawn/restored on first/last marker changes. Ordinary callbacks still see PENDING during candidate restoration.

Network additionally tests restricted movement updates only its frequency and pure WORLD movement retains position/timeout with zero added cold calls. Fault/counter mixins live only in the `src/tools/acceptance/` tooling source set, never the shipping JAR.

Read-only review passed before-touch collection, fast-path rollback, old/new scopes and cross-dimension reverse candidate restrictions. A proposed WORLD-position change was withdrawn after checking upstream semantics; linear Simulated filtering is explicitly retained.

## Historical artifact and installer

- Version/file: `0.1.0-dev`, `create_fieldwork-neoforge-1.21.1-0.1.0-dev.jar`.
- SHA-256: `765e89e884d8118369825b084ae4e3acfd6048266e39b4ab86e0947dca012266`; build/install matched and upstream hashes matched the lock (`.audit/scoped-refresh-artifacts.log`).
- Installer ready `01:53:52Z`; status had no tokens and WORLD ACTIVE normalization passed. Console stop `01:54:46Z` saved all dimensions; exited/exit0/OOMfalse.
- `.audit/scoped-refresh-archive.log`: main RefreshScope present, test drivers/faults/counters absent.
- `.audit/bytecode/scoped-network-installed.txt`: two Sable bridges/main wrappers, one upstream operation each, aggregate slot4 retained; entry now receives Level for reentrant-scope recording.
- The player's earlier single-player report predates this revision; no later player retest is implied. See the [player guide](player-test-guide.md).
