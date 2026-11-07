# Create Controller Endpoints and Scope — 2026-11-06

**Historical provenance research.** Controller40 includes an unsafe null→WORLD observation; it is not safety acceptance. Later [motion37](2026-11-06-movement-and-recovery.md) used the real vanilla listener and a PENDING candidate. Production now has separate server/session-owned implementation and [acceptance evidence](../testing/redstone-link-isolator-acceptance.md).

## 1. Actual routing in the pinned artifacts

Inputs are Create6.0.10/Sable2.0.5/Aeronautics1.3.2 from [`smoke/artifacts.lock`](../../smoke/artifacts.lock), not public-main assumptions. Create controller classes are under `com.simibubi.create.content.redstone.link.controller`:

```text
LinkedControllerPacketBase.handle(ServerPlayer)V
  lecternPos != null → resolve real LecternControllerBlockEntity → handleLectern
  otherwise → validate LINKED_CONTROLLER in server main/off hand → handleItem

LinkedControllerInputPacket.handleLectern(ServerPlayer,LecternControllerBlockEntity)V
  isUsedBy(player) → obtain BE controller stack → handleItem

LinkedControllerInputPacket.handleItem(ServerPlayer,ItemStack)V
  server player UUID/blockPosition + server stack frequencies
  → receivePressed(LevelAccessor,BlockPos,UUID,List,boolean)V
```

**Both paths pass player world BlockPos**, not lectern/plot position. handleItem reads blockPosition at offset12 and invokes receivePressed at72.

`LinkedControllerServerHandler$ManualFrequencyEntry` is package-private and implements IRedstoneLinkable:

- Constructor `(BlockPos,Couple<Frequency>)V` stores pos and counter30.
- getLocation returns pos; getTransmittedStrength is15 when alive, otherwise0.
- Handler map groups by level/player UUID, reusing entries by frequency key.
- A press on an existing entry only calls updatePosition and resets TTL30; it does not addToNetwork/updateNetworkOf.
- Release sets counter0; next handler tick removes entry/updates network.
- Tick decrements TTL; no refresh for30 ticks expires it.

Constructor-only source annotation is insufficient for held movement, device switching and scope changes.

## 2. Sable player coordinates/tracking

```text
EntityMovementExtension.sable$getTrackingSubLevel()SubLevel
EntityMovementExtension.sable$setTrackingSubLevel(SubLevel)V
EntityMovementExtension.sable$getLastTrackingSubLevelID()UUID
```

`Sable.HELPER.getTrackingSubLevel(entity)` is O(1) field access. Player position remains projected world position; plotPosition is separate. Passing player world BlockPos to plot ownership lookup cannot establish aboard-craft scope.

Setting current null does not clear last UUID. Collision/vertical displacement, vehicle inheritance and packets update current; last UUID is history, not positive membership authority.

Producer: `dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.player.ServerboundMovePlayerPacketMixin.sable$handle(ServerPlayer)V`:

- No-position packet returns.
- Valid plot x/z sets tracking and projects plot packet coordinates to WORLD.
- Reserved plot without craft replaces packet coordinates with current player position and clears tracking.
- Ordinary WORLD coordinates clear current.

The prototype adds no new client scope UUID protocol. Server field presence is not proof that vanilla accepted movement; the later motion audit established that distinction.

## 3. Non-shipping prototype

`src/testmod/.../test/ProbeController.java`, `ControllerEndpoint.java` and three probe mixins:

1. Wrap handleItem's receivePressed call; preserve upstream, capture validated server player/optional lectern source, restore context in finally.
2. Annotate the real ManualFrequencyEntry at constructor RETURN (before addToNetwork) and updatePosition HEAD.
3. Handheld candidate uses current tracking; lectern uses source-block ownership. Keep original getLocation/world range quantization.
4. Handler tick compares published/current source scope and synchronously recalculates both sides when changed.
5. Invalid former-private sources cannot automatically become WORLD; true READY(empty) clears private restrictions and restores upstream timeout semantics.

Static contexts/strong player references made the test convenient but are unsuitable for production. Main state must belong to a logical server and use player/device/session identities.

## 4. Recorded headless evidence

```sh
./scripts/gradle-docker.sh verifyScopeProbe -PprobePhase=controller
```

- First lectern active-user assertion failed because player eye-to-controller distance exceeded upstream `0.4 × block interaction range`; correcting feet height passed without bypassing validation.
- `12:01:51Z`:37 assertions and saving passed; network calls397, reconciliations5.
- Actual Sable movement-producer cases extended to **40**: `.audit/controller-probe-movement.log`. Valid plot sets A/projected coordinates; WORLD clears current. This is not complete vanilla acceptance.
- Location-only negative control: player tracks A but controller stores WORLD position; A gets0, WORLD15, proving the old resolver fails the aboard-craft requirement.
- Provenance candidate: A15, WORLD/B0; B switch, release, timeout, out-of-range, removed source and READY(empty) observations.
- Real lectern tryStartUsing/isUsedBy/packet.handle worked; non-active-user input created no entry. Range follows player, scope follows device.
- Bare FakePlayer.move did not naturally establish tracking (`naturalPlayerTrackingObserved=false`). Most ownership cases used controlled setters; no client UI/codec validation was performed.

## 5. Null-tracking defect and resulting gate

The original Source.owner treated current=null as WORLD. Its “leave ship” case directly set null and did not prove departure; jumps, collision or packet gaps could do the same. Clearing private history then can expose WORLD output.

The resulting candidate states:

```text
WORLD                       no protected private association
TRACKED(private key)        valid current/accepted provenance
PENDING(last confirmed key) unresolved departure; deny rather than grant WORLD
UNAVAILABLE                 offline/removed/dimension/session mismatch; retract
```

Release requires accepted server movement evidence or actual READY(empty). Last UUID can justify conservative denial, never arbitrary private authorization. Production needed real standing/jump/departure/rotation/dimension/disconnect tests before integration. The later motion37 continuation supplied server-side evidence and demonstrated rejected movement clearing tracking before acceptance.

The unsafe fallback was not shipped. Passing40 means the documented observations/negative controls ran, not that ownership safety was proven. Local evidence: `.audit/bytecode/controller-*.txt`, `.audit/controller-probe-*.log`, `run-scope-probe/scope-probe-result-<phase>.json`.
