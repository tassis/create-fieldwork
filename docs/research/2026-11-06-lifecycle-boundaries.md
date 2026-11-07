# SubLevel Lifecycle and Publication Boundaries — 2026-11-06

**Historical exact-binary audit.** These were initially candidate boundaries, later exercised by the [prototype suite](../testing/scope-hook-probe.md) and production [acceptance](../testing/redstone-link-isolator-acceptance.md). Main is now implemented. D01 later deferred strong upstream data-loss recovery/journal; it does not defer normal registry/publication correctness.

## Inputs and interpretation

Locked release artifacts and Java21 `javap -p -c -s -v` were compared with the prior ModDev transformed ServerLevel:

```text
Sable2.0.5
c8710f85bc780bbf523e6726ceaaf76dd0b8c61479db497b382c6bb34317e7ab
Nested Simulated1.3.2
fdf9d250996a084b52fced3a5b0089e879a5cac05dc7bcf295ac1f4ebe5e2bff
Prior transformed ServerLevel.class
40389a13e56ea18d8ad49ee5f3df47fbdf269f8398e406b296d5373b40b57f35
```

Offsets below are method bytecode offsets, not source lines; untransformed offsets do not transfer to transformed methods. Decorated handler names identify an export, not stable injection APIs. The initial static pass added no feature/registry hooks or dynamic LifecycleTraceAssertions. Subsequent tests supersede those stage-local missing-work statements.

## 1. ContainerReady precedes field publication/load

`dev.ryanhcode.sable.mixin.plot.container.ServerLevelMixin.sable$createPlotContainer()`:

```text
construct container
→ Sable.defaultSubLevelContainerInitializer(level,container)
→ onSubLevelContainerReady(level,container)
→ return container
```

Default initialization installs physics/tracking/observers/block properties; it is not `ServerSubLevelContainer.initialize()`.

Prior transformed constructor: offset44 calls createPlotContainer,47 stores the container field; ready emits inside the creator at76, before that assignment. Constructor-tail handler at645 later initializes storage. **Use the container passed by the event**; do not immediately reacquire it through Level/helper or assume the rest of Level is constructed.

`dev.ryanhcode.sable.mixin.plot.ServerLevelMixin.sable$init` injects at constructor TAIL, default priority, loads occupancy/ticket data, then calls initialize at original44. Initialize builds holding state and loads force-loaded craft.

ContainerReady installs observers only; onAdded establishes INITIALIZING, not a complete marker set.

## 2. Serializer UUID/content order

`dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelSerializer`:

```text
fromData(CompoundTag)SubLevelData: reads saved UUID
fullyLoad(ServerLevel,SubLevelData)ServerSubLevel:
  read plot/pose
  → allocateSubLevel(saved UUID,plot_x,plot_z,pose) [151]
  → ServerLevelPlot.load(plotTag)                 [190]
  → validate plot bounds
  → update pose/physics/velocity/name/userdata/bounds
  → return SubLevel                              [440]
```

Descriptor:

```text
(Lnet/minecraft/server/level/ServerLevel;
 Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;)
Ldev/ryanhcode/sable/sublevel/ServerSubLevel;
```

Allocation publishes plot/UUID indexes before onAdded and before block content load. UUID can come from the real serializer; do not invent it from BE NBT. Fieldwork remains INITIALIZING until full reconstruction.

### Plot load is not atomic

`ServerLevelPlot.load(CompoundTag)V` processes chunks sequentially:

- keepPacked BEs may retain only NBT.
- Per chunk: registerAllBlockEntitiesAfterLevelLoad at938, startTickingChunk945, platform postLoad955, chunk-loaded notification965, then next chunk.
- After chunks, block-state iteration updates heatmap/mass/floating blocks/bounds/physics; return1664.

Neither a BE onLoad nor first chunk notification proves full content restored. Reconstruct from block identity over complete restored chunks, not BE-instance counts.

Successful **non-null fullyLoad return** is the completion candidate: plot.load return is followed by bounds/physics validation. Invalid pose/allocation/bounds can return null; invalid bounds removes the craft. The exception table covers allocation's IllegalArgumentException139–159, not the whole load. Null/throw cannot success-commit; partial load stays closed until valid teardown/reconstruction. HEAD/RETURN counters are not finally; use exception-safe wrappers.

## 3. Holding load/unload

`SubLevelHoldingChunkMap.snatchAndLoad(GlobalSavedSubLevelPointer,UUID)V` obtains a holding chunk, removes primary/dependency in-memory holding entries through snatch, marks dirty, then loads entries via fullyLoad and removes allHoldingSubLevels references.

Being snatched/reference-removed does not prove restored content. This observation alone does not prove durable data loss, and the audit did not change upstream storage.

`moveToUnloaded(ServerSubLevel,ChunkPos)V` serializes the dependency chain, creates/stores HoldingSubLevel, then removes with UNLOADED. Observer removal occurs before index clearing. Both REMOVED/UNLOADED clear addon state without treating in-flight craft as READY(empty).

## 4. Assembly: afterMove is before source clear

`SubLevelAssemblyHelper.assembleBlocks(ServerLevel,BlockPos,Iterable,BoundingBox3ic)ServerSubLevel`:

```text
allocateNewSubLevel[201]
→ plot chunks/transform
→ moveOtherStuff[254–261]
→ moveBlocks[262–268]
→ mass/physics/pose
→ moveTrackingPoints[477–485]
→ return[486]
```

`moveBlocks(ServerLevel,AssemblyTransform,Iterable)V`:

1. Per block: beforeMove, save source BE, destination state/NBT, afterMove[604], state-change notification.
2. Destination notification pass662–807.
3. Source clearing807–919.
4. Block updates, return981.

afterMove can see source still present and incomplete destinations. Notifications can reenter game logic before source clearing; a single thread does not eliminate observable windows. Per-block/notification/source-clear exceptions may be caught/logged while the method continues. Normal return can represent partial transfer.

Wrap the outer batch; check actual source/destination identities and marker-transfer ledger. Input positions or “no throw” alone cannot establish successful transfer.

## 5. Disassembly: Simulated outer boundary

`dev.simulated_team.simulated.util.SimAssemblyHelper.disassembleSubLevel(Level,SubLevel,BlockPos,BlockPos,Rotation,boolean)V`:

- Collect loaded plot non-air blocks83–287.
- Handle Create contraptions287–300.
- Move/transform entities301–689.
- If blocks exist, kick entities and call Sable moveBlocks715.
- Move tracking points727; return730.

Use the whole outer method, not marker afterMove or inner moveBlocks alone. It does not directly removeSubLevel, so source container removal is not guaranteed by return. Inner swallowed failures still require ledger/actual-state validation.

World destinations need per-position transfer protection before registration/query, **not all WORLD marked INITIALIZING**. This motivated the tested O(1) affected-position overlay.

## 6. Split: child completion is not batch completion

`SubLevelHeatMapManager.split()V` groups connected components, runs SplitListener.addBlocks403–448, assembleBlocks471, invalid-child destroy/remove500–531, repeats and returns534.

SplitListener expands positions before assembly; splitComplete describes heatmap partition state, not all child transfers. Parent/children share an outer transaction; child assembly cannot publish/refresh the whole split early. Verify nested depth, invalid-child cleanup, reentry and actual parent/child content. Child normal return can still hide partial transfer.

## 7. Aeronautics balloon hooks

Nested Aeronautics injects:

- Balloon SubLevelAssemblyHelperMixin: needsBitSet HEAD may force true for an intersecting balloon; moveOtherStuff TAIL finds adjacent balloon controllers and calls setAssembling(transform).
- Balloon SimAssemblyHelperMixin: disassembleSubLevel's second getPlot call, ordinal1, marks balloon assembling before loaded-chunk collection.

Both use default priority. No observed deferred writes after outer return or replacement of moveBlocks' position iterable. These pre-transfer side effects did not invalidate the candidates, but final supported targets must still be checked in transformed methods.

## 8. Endpoint publication/stale power

- SmartBlockEntity has deferred first-tick initialization and late behavior attachment; this does not prove every third-party endpoint waits for a tick.
- LinkBehaviour.initialize directly adds to the network, and addToNetwork immediately updates it: registration can query scope synchronously.
- Unload removes from network and may recompute.
- setReceivedStrength is affected by newPosition; Create sets it for listening anchors. Writing a number directly to an arbitrary listener may not trigger its callback.
- isAlive checks chunk unload, removal, loaded location and exact BE instance at the position.
- Create cleans dead entries; Simulated's direct consumer does not.

These findings required tests for dead/custom entries, exact plot reuse/new UUID, old/new BE coexistence, and ownership removal without WORLD fallback.

## 9. Dynamic verification questions and later outcomes

| Boundary | Required observation |
|---|---|
| Construction/force-load | Event container usable before field; onAdded before first chunk; commit only successful fullyLoad |
| Multi-chunk/packed | Queries remain closed during each callback; block-based complete reconstruction |
| Assembly/disassembly | Query before/after move, notifications/source clear; no partial public exposure |
| Multi-child split | No early parent/child publication |
| Per-block/null/throw failure | No READY(empty) fallback; verify actual states and retain failed barrier |
| Registration/removal/reuse | Same-tick reentry and stale direct-consumer safety |
| Scope0↔1 | Correct synchronous final refresh, retain legal weak sources/range |

Later suite71, restart5/9, multi200/207, edges19 and recovery19 provided prototype evidence. [Movement/recovery research](2026-11-06-movement-and-recovery.md) records per-root repair and the expected-unsafe lost-marker restart5/7. Production has separate tests; these numbers do not certify actual isolator events or the full release matrix by themselves.

## 10. Packaged baseline smoke observations

After explicit owner EULA agreement, digest-pinned/non-root/localhost TCP+UDP setup reached ready at `10:25:42`, `Done (1.087s)!`; a second launch at `10:32:26`, `Done (0.702s)!`. It was an empty baseline world, not a craft-reload test.

Runtime JRE21.0.13+9-LTS, Mixin0.8.7/MixinExtras0.5.5; mod versions matched lock. Upstream refmap, JetBrains annotation and LanServerPinger warnings remained; not a warning-free launch claim.

First Docker stop exited143 without complete-save evidence: not accepted saving. Second TTY console `stop` logged Stopping server, Saving players/worlds, three dimensions Saving sub-levels, then All dimensions are saved. Inspect: exited/exit0/OOMfalse, no running smoke service. Build passed but tests were NO-SOURCE at this stage.

A pipe to docker attach failed with `the input device is not a TTY`; PTY attachment delivered stop. No new remote console interface was added.

Local evidence: `.audit/bytecode/*`, `.audit/production-ready-launch.log`, `.audit/production-ready-and-stop.log`, `.audit/production-second-launch.log`, `.audit/production-console-stop-pty.log`. This summary preserves conclusions, not raw logs in version control.
