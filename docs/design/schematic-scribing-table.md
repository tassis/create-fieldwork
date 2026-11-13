# Schematic Scribing Table — Gameplay Contract

## Status and authority

This supporting contract defines the workstation in the current development
worktree for the planned 0.3.0 feature set. It does not declare a release, change
the project's default version, or replace the behavior/architecture policies.
The [visual contract](schematic-scribing-table-visuals.md) records the accepted
open-frame C-01 finish, iron legs and original local textures. Current verification evidence
belongs in the [production acceptance record](../testing/redstone-link-isolator-acceptance.md#schematic-scribing-table-development-slice--2026-11-12).

Production now registers the block, item, block entity and menu under
`create_fieldwork:schematic_scribing_table`. The feature uses Minecraft, NeoForge
and Create APIs only; it does not require the optional aircraft integration. It
appears in vanilla Functional Blocks and, when that optional tab is registered,
the Fieldwork tab. Create-only client and latest packaged-server testing remain
separate from the integrated development-server evidence.

Ponder scenes are not implemented by this slice.

## Player operation

1. Deploy a Create schematic in the world and choose its location, rotation and
   mirror. The table evaluates that item in the table's current dimension.
2. Open the table, using an empty hand if an active schematic tool intercepts the
   click. Insert the schematic in the larger left blueprint area.
3. Insert a book, written book or Create clipboard in the upper-right input. Paper and
   writable books are not accepted, matching the cannon's media choices.
4. Open Settings to choose a replacement mode and whether existing block entities may be replaced
   **in the material calculation**. These options never authorize actual building.
5. Press Print. Server-owned progress fills downward, then the native checklist
   appears in the lower-right output. One job takes 21 active loaded server ticks
   (about 1.05 seconds at 20 TPS), matching the cannon-like paper-writing cadence.

No medium is consumed at start. One medium is consumed/replaced on successful
completion. A clipboard's notes or a
written book's old contents are overwritten; the Print tooltip warns about this.
The schematic is neither consumed nor changed. No gunpowder, kinetic connection
or building-material consumption is required. The table never places/removes
target blocks or spawns schematic entities.

The schematic anchor must be strictly less than 256 blocks from the table, using
Create's native distance convention. The whole target need not fit within that
radius. Missing deployment/anchor, invalid media, missing/empty/unreadable files,
and an occupied output do not consume a medium.

## Material semantics

`SchematicPrinter.loadSchematic(..., true)` supplies native anchor, rotation,
mirror, block state and block entity transformation. A new printer is loaded for
each job's completion: the same server filename may have been replaced or deleted
without changing the schematic item's components. Start performs only cheap
structural validation. No file load/world scan occurs on menu opening, GUI
refresh, idle ticks or progress ticks 1–20; the completion tick performs the native
load and scan against current file, target world and adjacent availability.

The table calls native `markAllBlockRequirements` and `markAllEntityRequirements`,
then uses `MaterialChecklist.createWrittenBook` or `createWrittenClipboard`.
Output formatting, tool-damage conversion, entity inclusion and their upstream
limitations remain Create 6.0.10 semantics, not an independent materials mapper.
An already matching world block normally needs no material; a replacement or
missing block counts only if the native printer and chosen predicate accept it.
The schematic's total block contents are therefore not the same as the remaining
placement requirements.

### Replacement policy

Defaults are mode **2**, with existing block entities protected.

| Mode | Native calculation policy |
|---|---|
| 0 | Non-air target into existing non-redstone-conducting blocks only |
| 1 | Non-air target; normal cubes may replace solid blocks, other targets require non-conducting existing blocks |
| 2 | Non-air target regardless of existing solidness |
| 3 | Every target, subject to the other native exclusions |

The native printer still handles matching states, unloaded positions, world
border, unbreakable blocks, paired halves and mergeable block entities. The
adapted predicate preserves structure void, upper-half, bed-head, piston-head
and middle-belt exclusions and the existing-BE option. The cannon-specific
two-block firing exclusion is intentionally omitted: a non-building workstation
has no firing clearance to reserve. An existing table is still protected by the
ordinary existing-BE option unless that option is enabled.

The small policy adaptation retains Create's MIT code notice in
`META-INF/licenses/create-scribing-policy-MIT.txt`. Create's reserved-rights
texture/GUI assets are not copied; all table textures and drawn screen panels are
Fieldwork resources.

### Adjacent availability and incomplete results

As with the cannon, the six immediately adjacent loaded block entities are
queried for item-handler capabilities on their table-facing sides. A simulated
one-item extraction determines eligibility; the full eligible stack is reported
to `MaterialChecklist.collect`. Nothing is extracted. This availability controls
the native completed/missing presentation; it does not change which world
positions still need placement. The player's inventory and distant storage are
not included.

Handler objects are deduplicated with the same `LinkedHashSet` equality behavior
as Create. Distinct capability wrappers over shared storage can still represent
the same underlying supply more than once; no stronger global inventory-identity
or storage-network deduplication is claimed.

Unloaded targets produce a **partial** checklist with Create's native warning
and a partial status in the menu. They are never assumed empty or force-loaded.
World-border/native unsupported requirements retain upstream skip behavior; a
printed list is not proof that every schematic element is buildable.

## Inventory, lifecycle and server authority

Three block-entity slots persist: schematic, medium and output. Replacement
settings persist too. Closing the menu leaves the items in the table. Normal
removal releases all contents through the vanilla container-drop path, while the
loot table supplies the table item subject to ordinary tool/explosion rules.
Success/error UI status is transient, not persisted as authoritative success.
Active work is saved as exact input snapshots, replacement options and elapsed
loaded ticks together with the inventory/settings. Decode validates types,
elapsed 0–20, item/count/components, settings and output vacancy before resuming;
malformed or mismatched work is discarded without consumption. Unloaded time
does not advance progress. Removal cancels before dropping the unconsumed items.

Horizontal facing and two derived input-presence properties select the 16 native
blockstate variants. Clipboard visibility follows a clipboard in the medium or
output slot, not a plain book. Visual state changes do not replace the BE or drop
items. The collision/selection shape retains open space under the desk and uses
a stepped thin envelope for the sloped desktop; actual in-game feel is a player
check, not established by resource geometry alone.

Printing and settings use vanilla container-button packets. The server rejects
unknown buttons, spectators, a stale/different active menu, and an invalid/distant
table. The client only presents synchronized slots/data; it sends no material
totals, file contents or trusted craft identities.

The job captures both exact input stacks and replacement options at start and
revalidates them plus empty output on each active tick. Normal inventory/settings
changes cancel without consumption; an identical notification does not. The live
Container API still exposes mutable stacks: unnotified transient changes restored
before observation cannot be detected, so no stronger mutation-history guarantee
is claimed.

At tick 21 the entire native output is generated before consuming a medium. Job
object identity is checked first after external hooks, then exact inputs/options,
BE ownership and output vacancy are revalidated. A callback that cancels/restores
inputs cannot revive that job or replace its cancellation with success. Nested
start/tick calls are suppressed while ticking/generating. Commit publishes one
output and shrinks one backing medium with no intervening external callback,
clears the job/status and then marks the complete inventory changed.

Runtime generation failure consumes nothing. An Error cancels/rethrows; that
cancellation is durable only after a successful save. These guarantees cover
normal in-memory transactions and clean save/reload, not a cross-chunk crash
journal or atomic durability with other inventories after a process failure.

## Cost and engineering limits

The completion tick synchronously loads/parses the server file, constructs the native
virtual schematic, scans its positions/entities and visits adjacent item slots.
Work is linear in template positions and adjacent slots, not constant-time.
Create's file parsing/accounting and upload constraints are inherited. This
feature adds no demonstrated stress/large-template or aggregate CPU guarantee.

An active job rejects repeat starts without resetting progress. There is no
separate cooldown after cancellation/completion: each successful start must
receive 21 active server ticks before expensive work, and canceled work can retry
from zero. This is not incremental parsing or amortized scanning; the final tick
can still be long. Multiple tables remain possible. No aggregate abuse resistance,
measured MSPT improvement or global work budget is claimed.

## Implemented UI design

The owner approved an iron-frame, wood-backed drafting-desk UI with paper panels
and a lighter blue schematic sheet using the model texture's color family. The
larger blueprint area is on the left; the narrower right card puts medium above,
server-owned progress in the middle and output below. Only input slots have faint
runtime item guides; no Media/Output labels or output guide are shown. Caption
clearance and normal inventory spacing are retained. This design is implemented;
the [UI contract](schematic-scribing-table-ui.md) records the shared coordinates,
settings foldout, original text-free background and real-client evidence limits.

The selected buttons use light paper-label faces, a simple dark outline and thin
inset horizontal rules above/below the text, rather than prominent beveled edges.
The owner approved this final composition and button refinement. Actual client
usability remains a player check rather than a headless-test claim.

Production keeps native container-slot/ItemStack rendering,
including component-driven icons and resource packs. Text must be rendered at
runtime through Minecraft's client font and translated Components, never baked
into the background texture. Player language and font providers determine actual
glyphs; GUI scale determines physical display size. Layout measures runtime
text widths and uses native component-aware splitting, shadow-free button text and
full status/settings tooltips rather than the mockup's English bitmap-letter widths.

## Recipe and client acceptance

The selected recipe outputs one table: ` B /PAP/I I`, where B is a book, P a golden
sheet, A an andesite casing and I an iron ingot. Obtaining a casing unlocks the
recipe-book entry. The desk is pickaxe-mineable. This is the initial engineering
recipe structure with the owner's selected golden sheets replacing the former
two brass sheets; the superseded brass inputs no longer match.

Headless acceptance exercises native server menu opening/button handling with a
NeoForge mock connection for outbound payload support. It is not a real client
handshake or screen test. Still pending: actual client baking, inventory icon,
localized layout, slot alignment, tooltip/readability, button use, shift-click,
crafting/unlock UI, collision feel, resource-pack override behavior, real
multiplayer interaction, explosions and Ponder teaching.
