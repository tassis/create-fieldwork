# Production V1 Mixin Audit

Pinned artifacts/hashes: [`smoke/artifacts.lock`](../../smoke/artifacts.lock).
Configurations: `src/main/resources/create_fieldwork.base.mixins.json` and
`create_fieldwork.integration.mixins.json`, both
`required=true/defaultRequire=1`. The integration config is metadata-gated by
Sable, Aeronautics and Simulated. This audit was recorded against `0.1.0-dev`
candidates on the pinned stack; it is not a cross-version compatibility claim.

The 2026-11-07 scoped revision added no main injection points. Update entry now receives actual ServerLevel to record reentrant scope; controller/mutation internals use selection and PRIVATE non-boundary fast paths. Inspection found two Sable bridges/main wrappers, one Operation each, with aggregate slot4 retained. The durable findings are recorded here; local `.audit/` bytecode dumps are optional raw evidence. See [scoped regression/counters](scoped-refresh.md).

## Targets and evidence

Method signatures below use short class names; source contains actual descriptors/targets and strict counts. WrapMethod targets unique methods or full descriptors; each Inject/WrapOperation uses explicit require/allow.

| Feature-qualified mixin | Target/match | Purpose/evidence |
|---|---|---|
| `platform.MinecraftServerMixin` | `MinecraftServer.stopServer()V` RETURN1; instance accessor | Phase close; no process-global server map |
| `wireless.RedstoneLinkNetworkMixin` | `updateNetworkOf(LevelAccessor,IRedstoneLinkable)V` WrapMethod; `withinRange(IRedstoneLinkable,IRedstoneLinkable)Z` ordinal0/1, each1 | Original operation once then scope; targeted cold refresh prevents aggregate leakage; range7/15 and rotation |
| Same | update RETURN1, `@Local(ordinal=0) int power` | Custom non-LinkBehaviour target callback; installed aggregate slot4; callback/fault cases |
| `wireless.SimulatedReceiverMixin` | `AbstractLinkedReceiverBlockEntity.updateSignal()V` WrapMethod; first `getTransmittedStrength()I`, ordinal0, require1/allow1 | Deny before loop's second read; retain direction/raw/power; 7/15 and synchronous withdrawal |
| `wireless.TrainConditionMixin` | `tickCompletion(Level,Train,CompoundTag)Z` WrapMethod; its `hasAnyLoadedPower(Couple)Z` INVOKE require1/allow1 | WORLD visibility/cache/PENDING; other callers unaffected |
| `wireless.NetworkRegistrationMixin` | `addToNetwork`, `removeFromNetwork` and `onUnloadWorld(LevelAccessor)` WrapMethod, one exact method each | Capture source, epoch/publication, unload cleanup and last-member handling |
| `wireless.LinkPublicationMixin` | `RedstoneLinkBlockEntity.read(CompoundTag,HolderLookup.Provider,boolean)V` TAIL require1/allow1 | Unregistered source indexing; reconstruction before Level attachment |
| `isolator.AssemblyLifecycleMixin` | `assembleBlocks` and `moveBlocks`, one exact WrapMethod each | Outer completion, pre-write source/destination barriers and marker ledger |
| `isolator.LoadLifecycleMixin` | `fullyLoad(ServerLevel,SubLevelData)ServerSubLevel`, one WrapMethod | Initialization and success/null/failure cleanup |
| `isolator.ContainerLifecycleMixin` | `allocateSubLevel` RETURN require1/allow1; `removeSubLevel` WrapMethod | UUID INITIALIZING and teardown |
| `isolator.SplitLifecycleMixin` | `SubLevelHeatMapManager.split()V`, one WrapMethod | Real heatmap bridge removal/children and identity reconstruction |
| `isolator.DisassemblyLifecycleMixin` | `SimAssemblyHelper.disassembleSubLevel(...)V`, one WrapMethod | WORLD destination protection and inactive returned markers |
| `isolator.MarkerMutationMixin` | `LevelChunk.setBlockState(BlockPos,BlockState,boolean)BlockState`, one WrapMethod | Identity/publication transaction; BEGIN fault aborts pre-write |
| `controller.routing.ManualControllerEntryMixin` | `LinkedControllerServerHandler$ManualFrequencyEntry` interface/unique fields; no replacement | Provenance/session/published-scope metadata |
| `controller.routing.ControllerPacketMixin` | `handleItem`'s `receivePressed(...)V` INVOKE require1/allow1 | Validated server packet context for handheld/lectern input |
| `controller.routing.ControllerHandlerMixin` | `receivePressed`'s `Collection.add(Object)Z` and `ManualFrequencyEntry.updatePosition(BlockPos)V`, each INVOKE require1/allow1 | Managed native-add recovery and accepted position/source reconciliation; no tick injection |
| `controller.session.PlayerInventoryMixin` | Eight exact inventory mutators at RETURN; cardinalities 1 except `removeItemNoUpdate` require2/allow2 | Reconcile controller ownership after committed inventory changes |
| `controller.session.PlayerHandMixin` | `LivingEntity.setItemInHand` RETURN require1/allow1 | Reconcile committed hand changes |
| `controller.session.PlayerGameModeMixin` | `ServerPlayer.setGameMode`, one WrapMethod | Withdraw/reconcile across game-mode authority changes |
| `controller.session.PlayerActionMixin` | `handleSetCarriedItem` RETURN1 and `handlePlayerAction` RETURN6 | Selected-slot and drop packet writes that bypass inventory mutators |
| `controller.session.PlayerDimensionMixin` | `ServerPlayer.changeDimension`, one WrapMethod | Withdraw/reconcile across dimension changes |
| `controller.lectern.LecternControllerIdentityMixin` | `setController` WrapMethod; `startUsing`, `stopUsing`, `createLinkedController`, `dropController`, `write`, `writeSafe`, `read`, each require1/allow1 | Device/reset identity persistence and lectern endpoint reconciliation |
| `controller.lectern.LecternRemovalMixin` | `BlockEntity.setRemoved` RETURN require1/allow1 | Remove/reconcile lectern endpoint ownership |
| `controller.lectern.ControllerInteractionMixin` | `LinkedControllerItem.onItemUseFirst` and `use`, HEAD cancellable require1/allow1 | Server-authorized reset/binding interaction priority |
| `controller.reset.ControllerResetCraftingMixin` | `ResultSlot.checkTakeAchievements` HEAD require1/allow1 | Commit pending controller reset before result becomes ordinary |
| `schematic.StructureTemplateAccessor` | `StructureTemplate.palettes` accessor | Exact template palette access; no injection replacement |
| `schematic.client.SchematicHandlerAccessor` | Accessors for `activeHotbarSlot` and `syncCooldown` getter/setter | Narrow client state access |
| `schematic.client.SchematicTransformationAccessor` | Accessors for rotation, current/previous position and target | Narrow transformation state access |
| `schematic.client.SchematicHandlerMixin` | Four hooks require1/allow1 plus renderer INVOKE require3/allow3 | Offhand selection/sync/capture/input and three mirrored ghost passes |
| `schematic.client.SchematicRendererMixin` | `drawLayer` block-state INVOKE and `render` BE-helper INVOKE, each require1/allow1 | Hidden-ghost model and block-entity submission masks |
| `schematic.client.SchematicOffhandOverlayMixin` | `SchematicHotbarSlotOverlay.renderOn` HEAD cancellable require1/allow1 | Suppress native selected-slot overlay for offhand assistance |
| `schematic.client.ClientBlockChangesMixin` | `LevelChunk.setBlockState` RETURN require1 | Coalesce native schematic cache rebuild after client block changes |

These 25 common and six client entries match the 31 classes listed across the two
strict production configs. No `Level#getSignal` interception, player-move
authority hook or replacement of the full network handler is used.

### Controller target descriptors

The pinned Create6.0.10 and Minecraft1.21.1 controller/session seams use these
exact JVM descriptors. Cardinalities remain those in the table above.

| Owner | Method descriptor |
|---|---|
| `LinkedControllerInputPacket` | `handleItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V` |
| `LinkedControllerServerHandler` | `receivePressed(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Ljava/util/UUID;Ljava/util/List;Z)V` |
| `Collection` invocation in `receivePressed` | `add(Ljava/lang/Object;)Z` |
| `ManualFrequencyEntry` invocation in `receivePressed` | `updatePosition(Lnet/minecraft/core/BlockPos;)V` |
| `Inventory` | `setItem(ILnet/minecraft/world/item/ItemStack;)V`; `removeItem(II)Lnet/minecraft/world/item/ItemStack;`; `removeItem(Lnet/minecraft/world/item/ItemStack;)V`; `removeItemNoUpdate(I)Lnet/minecraft/world/item/ItemStack;`; `dropAll()V`; `clearContent()V`; `replaceWith(Lnet/minecraft/world/entity/player/Inventory;)V`; `setChanged()V` |
| `LivingEntity` | `setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V` |
| `ServerPlayer` | `setGameMode(Lnet/minecraft/world/level/GameType;)Z`; `changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;` |
| `ServerGamePacketListenerImpl` | `handleSetCarriedItem(Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;)V`; `handlePlayerAction(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V` |
| `LecternControllerBlockEntity` | `setController(Lnet/minecraft/world/item/ItemStack;)V`; `startUsing(Lnet/minecraft/world/entity/player/Player;)V`; `stopUsing(Lnet/minecraft/world/entity/player/Player;)V`; `createLinkedController()Lnet/minecraft/world/item/ItemStack;`; `dropController(Lnet/minecraft/world/level/block/state/BlockState;)V`; `write(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V`; `writeSafe(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V`; `read(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V` |
| `BlockEntity` | `setRemoved()V` |
| `LinkedControllerItem` | `onItemUseFirst(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;`; `use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;` |
| `ResultSlot` | `checkTakeAchievements(Lnet/minecraft/world/item/ItemStack;)V` |

The packet wrapper invokes the exact `receivePressed` descriptor above. The
handler wrappers invoke the exact `Collection.add` and `ManualFrequencyEntry`
descriptors above. Any descriptor or call-count change in the pinned artifacts is
a stop-and-reaudit condition.

## Client-only opacity follow-up (2026-11-08)

`schematic.client.SchematicHandlerMixin` wraps exactly the three
`SchematicRenderer.render(PoseStack,SuperRenderTypeBuffer)V` calls in the world
`SchematicHandler.render(PoseStack,SuperRenderTypeBuffer,Vec3)V` overload
(`require=3`, `allow=3`). These are normal and both mirrored ghost branches.
The active handler slot must be40 and the currently selected actual stack must
still be that handler's offhand stack. Otherwise the original buffer is passed.
The adapter remaps chunk layers to native translucent and multiplies emitted
vertex alpha; custom BE render types remain native. No full renderer replacement,
cached-mesh mutation or global shader-color state is introduced.

The exact pinned Create class resource is checked by ASM in JUnit without client
loading; new build/JUnit98 plus main schematic329 passed. This is descriptor/
call-site and vertex-data evidence, **not client transformation or visual alpha
acceptance**. See [current opacity boundary/player checks](schematic-client-v02.md#current-follow-up-iris-and-hiding-completed-ghosts-2026-11-08).

## Installed transformation

The real installer export of `RedstoneLinkNetworkHandler.class` was inspected with
Docker Java21 `javap -p -c`. The durable summary is:

- Lines200–242 retain Sable `projectComparisons` and both logicalPose projections.
- Lines323–414 contain main source/receiver wrappers with one `Operation.call` each.
- Lines523–578 bridges still invoke Sable's redirect.
- Original loops enter main ordinal0/1 wrappers at lines638/712.
- RETURN lines719–727 pass original aggregate slot4 to customReceiver handling.

Line numbers identify that export only. Bytecode proves composition; dynamic main-only assertions establish scope/range/callback/lifecycle effects.

## Driver isolation

The `src/tools/acceptance/` tool has publication, visual, marker-write, binding-ledger/grant/
withdrawal/refresh and lectern-assignment faults plus two counters. Driver-only
flags never supply authorization/registry answers or ship. Fault injections
supply no-op/throw/nested inputs; counters observe work without changing behavior.

Historical `testmod` runs without main. Use `verifyFieldworkAcceptance`, not prototype passes, for production evidence.

## Later client visibility and mode scope

The later `SchematicRendererMixin` adds strict1/1 hooks for the virtual
`SchematicLevel.getBlockState` call in `drawLayer` and native entity-helper call in
`render`. Confirmed source-coordinate mask entries skip model/BE submissions;
native entity ordinals/error bits and both mirror offsets are retained. Native
cache rebuilds are bounded/coalesced; mask coordinate and volume guards have
unit/native checks. The owner reported Iris/ghost candidate f0f3a49 OK.

The subsequent offhand-only assistance clarification gates comparison work,
template capture, extra HUD, controller-priority dispatch and early inventory-sync
flush with the active offhand selection. Main-hand render receives native buffer/
empty mask. Hooks remain installed for hand-selection/sync support; this is not
a zero-interception claim. That dated integrated recheck passed build/JUnit103,
schematic329 and native controller/lectern passthrough policy checks; see its scope
section in the client record for the corresponding artifact and pending mode check.
