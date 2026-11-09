# Managed Controller Binding Contract

Status: implemented server/headless contract; client/GUI acceptance remains incomplete.
The [behavior contract](behavior-contract.md) is the product authority, and the
[V1 technical design](redstone-link-isolator-v1.md) defines the current two-tier
controller routing model.
Native crouch-air use edits channels. Standalone crafting unbinds a managed
controller while preserving channels and name.

## Identity and authority

- Ordinary controller: no Fieldwork device component. Handheld sources are always
  WORLD; a physical lectern resolves from its own WORLD/PRIVATE location.
- Managed controller: typed persistent UUID component, never a target UUID.
- Brass/smart BE owns an instance UUID stamp and device→grant-generation list.
- One smart isolator may grant multiple controller devices. A managed controller
  resolves to at most one craft; multiple grants for the same device on the same
  craft are redundant witnesses, while grants across different crafts conflict
  and deny.
- A server-owned Loaded authority indexes valid records only. Target resolution
  requires matching actual containing Sable UUID, READY private registry, loaded
  exact BE, no active/failed transaction and matching revocation generation.
- A per-server overworld SavedData ledger records issued/revoked generations.
  It supplies no target, never grants access and never converts managed to ordinary.
  Missing/corrupt entries deny. No static cross-server map or hot-path craft scan.
- Repeated same-ship bind may add a loaded entry; another ship requires explicit
  unbind. If the existing target is unavailable, do not guess. Conflicts deny.

## Mutations

1. Server valid crouch-controller interaction on smart block: resolve actual craft
   and READY state; reject incompatible existing binding, then issue/reuse device
   and record matching generation in this BE. Mark persistent records dirty.
2. Explicit unbind: one bound controller alone in the player2x2/table3x3 grid.
   Preserve channels/name and unrelated components. Preview is pure; retain old
   device UUID plus a denial-only pending marker in the result until actual
   server result-slot extraction commits revocation/old endpoint withdrawal.
   Then clear only the device/pending components, including quick-craft's actual
   inventory/cursor results. Ordinary controllers have no no-op recipe. Existing
   held press cannot turn into ordinary power; require release/fresh press.
3. Passive missing/unloaded/removed brass, target WORLD, UUID mismatch, conflict,
   death/disconnect/new session/dimension mismatch: managed scope PENDING, never
   ordinary fallback. Restored valid grant may revalidate only native-live input.
4. Revoked devices receive a new device ID when explicitly rebound; do not revive
   the old ID and accidentally reactivate other old copies.
5. Normal lectern set/create/read/write/writeSafe/drop/return preserves ID and
   frequency, plus any denial-only pending marker. Commit extra fields after
   native assignment succeeds; roll native program/device/pending back together
   on assignment failure. Native null is a no-op; EMPTY clears. Only identified copy/reset contexts may issue new
   managed-unbound IDs; ambiguous creative/NBT copying is unsupported, not hidden.

## Interaction entry points

Pinned Create6.0.10 LinkedControllerItem has `onItemUseFirst(ItemStack,UseOnContext)`
and `use(Level,Player,InteractionHand)`. Native crouch-use opens frequency UI.
Exact audit confirms `onItemUseFirst` invokes native `use` for unrecognized blocks,
and crouch-use consumes before `Block.useItemOn`. Bind therefore uses a narrow
HEAD injection into `onItemUseFirst`, only for smart blocks and crouching controller.
Native `use` is preserved for crouch-air frequency configuration. A narrow use/
use-first guard only retries already-pending crafted results; failure prevents
activation/placement. A ResultSlot hook handles real extraction, never preview.
Failed commits keep one unusable pending output and allow vanilla to consume one
ingredient, including shift-take, rather than throwing after output delivery and
duplicating an ingredient. Pending state survives lectern forms. Retrying is
refused while any named failed transaction remains; after repair, native use can
finish the pending item. No new item becomes ordinary before the full commit.

Active offhand schematic handling yields controller and native lectern right-
clicks to vanilla instead of consuming the tool action or sending synthetic use
packets. No client-supplied ID/target is authority. Actual client interaction is
still a separate acceptance step.

## Source and refresh contract

Capture device identity on the actual validated Create input stack. Every managed
query revalidates session, current hand/lectern identity, active user, target state
and native alive/range. Authorized A may differ from player WORLD/B membership;
do not project player getLocation into plot space. Frequency and range stay native.
Managed sources require a live server-owned source and use the server player's
current physical position for native range, not a confirmed actor craft membership.
Movement and same-dimension teleport/ack are not routing events and perform no
Fieldwork controller reconciliation; the next native controller packet observes
the current server position. Player craft membership never upgrades
an ordinary handheld source: it remains WORLD aboard A or B. An ordinary
controller reaches PRIVATE(A) only while installed in a validated lectern
physically on A. A managed controller in hand or lectern resolves only its loaded
binding target, independent of operator/lectern craft, and never falls back WORLD.
Identity/mode/revision/session/target changes withdraw old pressed contributions,
even at identical frequency/position/private scope. Only fully stable position
updates may use the private-receiver shortcut. D02 train observers remain WORLD.

Loaded grants and marker candidates must have a coherent final transaction view
for the exact permitted updater; regular callbacks remain blocked. All throwable
publication precedes barrier release. Last smart entry removal refreshes managed
sources even if an ordinary marker keeps the craft private. Chunk unload/load,
Sable transactions and packed BE load drive index changes automatically.

## Pinned event-hook constraints

The exact NeoForge-compiled Minecraft 1.21.1 and Create 6.0.10 bytecode establish
these durable constraints:

- Create `LinkedControllerServerHandler.tick(LevelAccessor)` decrements each
  `ManualFrequencyEntry`, calls `isAlive`, then invokes the single
  `RedstoneLinkNetworkHandler.removeFromNetwork` call before iterator removal.
  Create's native per-entry TTL loop remains; Fieldwork's extra full reconcile is
  the only loop targeted for removal.
- Teleport/ack writers are not controller-routing seams. Same-dimension teleport,
  ACK and ordinary movement intentionally schedule zero Fieldwork controller work;
  logout, session replacement and dimension travel still withdraw affected endpoints.
- `Inventory#setChanged()V` only increments `timesChanged`. Crucially,
  `setItem`, both remove variants, `removeItemNoUpdate`, `dropAll`, `clearContent`
  and `replaceWith` mutate lists without necessarily calling `setChanged`.
  Therefore `setChanged` alone is not a complete managed-item invalidation hook.
  Exact inventory mutators plus packet-level selected-slot and hand-swap seams
  must be covered and tested; do not add a player-wide tick scan.
- `ServerGamePacketListenerImpl#handleSetCarriedItem` writes
  `Inventory.selected` directly at offset77. `handlePlayerAction` swaps hands via
  two `ServerPlayer#setItemInHand` calls and drops through `ServerPlayer#drop`.
- `LecternControllerBlockEntity.tick` calls private `stopUsing(Player)` when its
  user disappears, leaves native range, or ceases native lectern use. Strict
  start/stop hooks are required in addition to `setController`.
- `RedstoneLinkNetworkHandler#onUnloadWorld(LevelAccessor)` clears native level
  state directly rather than calling `removeFromNetwork` per endpoint; it is a
  separate level-index cleanup seam.

The durable [production mixin audit](../testing/main-mixin-audit.md#targets-and-evidence)
records the controller event targets, descriptors, and strict cardinalities.
Local bytecode dumps under `.audit/` are optional reconstruction evidence, not
architecture authority. Any pinned artifact difference is a stop condition, not
permission to weaken mixin cardinality.

Container removal captures and withdraws the old craft/position/device footprint
before upstream mutation. Once detached, marker blocks live in Sable serialized or
holding data, so live plot block state is not a valid postcondition. Publication
requires active-container UUID absence; incomplete removal retains the barrier.

## Content and acceptance boundaries

- Smart display name: Smart Redstone Link Isolator; internal ID:
  `brass_redstone_link_isolator`. The exact ordinary and smart recipes are defined
  by the [recipe contract](2026-11-06-isolator-recipe.md).
- Selected gray/brass rim textures only; original full top textures retained.
- Main-only tests must observe actual Create/Simulated receiver outputs; direct
  ledger tests are not interaction or routing acceptance. Native lectern and
  independent JVM persistence are separately exercised. Faults retain barriers.
- Exclude all experimental mixins from ordinary/main acceptance/shipping.
- True client interaction and client schematic work have separate gates; no GUI
  or complete copy protection claim follows from a headless build.

Current phase counts, persistence results, artifact hashes and installer evidence
are maintained only in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md).
