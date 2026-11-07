# Production V1 Mixin Audit

Pinned artifacts/hashes: [`smoke/artifacts.lock`](../../smoke/artifacts.lock). Configuration: `src/main/resources/create_fieldwork.mixins.json`, `required=true/defaultRequire=1`. This audit was recorded against `0.1.0-dev` candidates on the pinned stack; it is not a cross-version compatibility claim.

The 2026-11-07 scoped revision added no main injection points. Update entry now receives actual ServerLevel to record reentrant scope; controller/mutation internals use selection and PRIVATE non-boundary fast paths. New installed export: `.audit/bytecode/scoped-network-installed.txt`; two Sable bridges/main wrappers, one Operation each, aggregate slot 4 retained. See [scoped regression/counters](scoped-refresh.md).

## Targets and evidence

Method signatures below use short class names; source contains actual descriptors/targets and strict counts. WrapMethod targets unique methods or full descriptors; each Inject/WrapOperation uses explicit require/allow.

| Main mixin | Target/match | Purpose/evidence |
|---|---|---|
| MinecraftServerMixin | `MinecraftServer.stopServer()V` RETURN1; instance accessor | core8 and phase close; no process-global server map |
| RedstoneLinkNetworkMixin | `updateNetworkOf(LevelAccessor,IRedstoneLinkable)V` WrapMethod; `withinRange(IRedstoneLinkable,IRedstoneLinkable)Z` ordinal0/1, each1 | Original operation once then scope; targeted cold refresh prevents aggregate leakage; range7/15 and rotation |
| Same | update RETURN1, `@Local(ordinal=0) int power` | Custom non-LinkBehaviour target callback; installed aggregate slot4; callback/fault cases |
| SimulatedReceiverMixin | `AbstractLinkedReceiverBlockEntity.updateSignal()V` WrapMethod; first `getTransmittedStrength()I`, ordinal0,1 | Deny before loop's second read; retain direction/raw/power;7/15 and synchronous withdrawal |
| AssemblyLifecycleMixin | `assembleBlocks(ServerLevel,BlockPos,Iterable,BoundingBox3ic)ServerSubLevel`; `moveBlocks(ServerLevel,AssemblyTransform,Iterable)V`, WrapMethod | Outer completion, pre-write source/destination barriers, marker ledger; network/lifecycle/restart |
| LoadLifecycleMixin | `fullyLoad(ServerLevel,SubLevelData)ServerSubLevel`, WrapMethod | Initialization, success/null/failure cleanup; duplicate/observer-fault/retry, packed/restart |
| ContainerLifecycleMixin | `allocateSubLevel(UUID,int,int,Pose3d)SubLevel` RETURN1; `removeSubLevel(int,int,SubLevelRemovalReason)V` WrapMethod | UUID INITIALIZING and teardown |
| SplitLifecycleMixin | `SubLevelHeatMapManager.split()V` WrapMethod, final `subLevel` shadow | Real heatmap bridge removal/children; identity reconstruction |
| DisassemblyLifecycleMixin | `SimAssemblyHelper.disassembleSubLevel(Level,SubLevel,BlockPos,BlockPos,Rotation,boolean)V` WrapMethod | Actual disassembly, WORLD destination protection, inactive returned markers |
| MarkerMutationMixin | `LevelChunk.setBlockState(BlockPos,BlockState,boolean)BlockState` WrapMethod | Isolator identity changes/supported Link transmitter publication; ACTIVE-only changes do not open transactions; BEGIN fault aborts pre-write |
| TrainConditionMixin | `tickCompletion(Level,Train,CompoundTag)Z` WrapMethod; its single `hasAnyLoadedPower(Couple)Z` INVOKE1 | WORLD visibility/cache/pending; network/packed; other callers unaffected |
| NetworkRegistrationMixin | `addToNetwork(LevelAccessor,IRedstoneLinkable)V` and `removeFromNetwork(...)V` WrapMethod | Capture source, epoch/publication; last-member early-return cache test |
| ManualControllerEntryMixin | `LinkedControllerServerHandler$ManualFrequencyEntry` interface/unique fields; no replacement | Provenance/session/published-scope metadata; location retained; session rebind |
| ControllerPacketMixin | handleItem's single `receivePressed(LevelAccessor,BlockPos,UUID,List,boolean)V` INVOKE1 | Validated server packet source context; real handheld/lectern handlers |
| ControllerHandlerMixin | receivePressed's `ManualFrequencyEntry.updatePosition(BlockPos)V` INVOKE1 (`Coerce` private class); tick HEAD1 | Reconcile after position, restricted range/held/release/timeout/new session |
| PlayerMovementMixin | handleMovePlayer WrapMethod; after `lastGoodZ:D` PUTFIELD Inject1; readonly awaitingPositionFromClient accessor | Raw versus accepted movement; rotation/rejection/airborne/support/ack/dimensions; no client scope protocol |
| LinkPublicationMixin | `RedstoneLinkBlockEntity.read(CompoundTag,HolderLookup.Provider,boolean)V` TAIL1 | Unregistered source indexing; cold chunk/full reconstruction covers packed reads before Level attachment |

No `Level#getSignal` interception or replacement of the full network handler is used.

## Installed transformation

The real installer export at `.smoke/server/.mixin.out/class/com/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler.class` was inspected with Docker Java21 `javap -p -c`. Summary in `.audit/bytecode/main-network-installed.txt`:

- Lines200–242 retain Sable `projectComparisons` and both logicalPose projections.
- Lines323–414 contain main source/receiver wrappers with one `Operation.call` each.
- Lines523–578 bridges still invoke Sable's redirect.
- Original loops enter main ordinal0/1 wrappers at lines638/712.
- RETURN lines719–727 pass original aggregate slot4 to customReceiver handling.

Line numbers identify that export only. Bytecode proves composition; dynamic main-only assertions establish scope/range/callback/lifecycle effects.

## Driver isolation

`acceptancemod` has publication, visual and marker-write faults plus two counters (five mixins). Driver-only flags never supply authorization/registry answers or ship. Marker-write injection supplies no-op/throw/nested inputs; counters observe work without changing behavior.

Historical `testmod` runs without main. Use `verifyFieldworkAcceptance`, not prototype passes, for production evidence.
