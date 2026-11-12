# Create: Fieldwork — Behavior Contract

> Implementation status: version `0.1.0` contains the production implementation
> and is available for player testing. See [installation and testing](../testing/player-test-guide.md)
> and [main-only acceptance](../testing/redstone-link-isolator-acceptance.md).
> Client and full release acceptance are incomplete; this document specifies
> requirements, not a claim that every matrix row has passed.

## Purpose

Create: Fieldwork is a focused Create addon with optional Aeronautics integration.
Its first block, the **Redstone Link Isolator**, gives an assembled
Sable/Aeronautics craft a private Create wireless-redstone network.

The design exists to prevent Redstone Links aboard a craft from accidentally interacting with matching frequencies in the parent world or another craft, while preserving communication between links aboard the same craft.

This document defines product behavior. The [V1 technical design](redstone-link-isolator-v1.md)
defines the current implementation model and exact integration boundaries.

## Product Identity

- Display name: **Create: Fieldwork**
- Mod ID: `create_fieldwork`
- First block: **Redstone Link Isolator**
- Block ID: `create_fieldwork:redstone_link_isolator`
- Traditional Chinese localization: maintained in `src/main/resources/assets/create_fieldwork/lang/zh_tw.json`
- Current release line: `0.1.0`
- Reserved name only: **Redstone Link Gateway**, for a possible separately designed cross-boundary feature

## Reference Baseline

The supported V1 stack is pinned to:

- Minecraft `1.21.1`
- NeoForge `21.1.257`
- Create `6.0.10`
- Sable `2.0.5`
- Create Aeronautics `1.3.2`

Compatibility with other releases is not claimed. Dependency ranges remain exact;
any upgrade requires a new artifact and transformed-bytecode audit.

### Dependency modes

- Create is the only required gameplay dependency.
- Sable, Aeronautics and Simulated are one optional integration bundle. The
  isolator integration activates only when all three exact supported versions are
  present.
- In Create-only mode, offhand schematic assistance remains available. Fieldwork
  keeps its block/item/BE registry identities dormant for decoding safety, but
  exposes no isolator recipe, loot, creative entry, command or behavior.
- Client and server exchange the integration-active state during configuration and
  reject either mismatch before player join.
- An integrated world records a durable marker. If the bundle is later absent,
  Fieldwork warns prominently but does not prevent the player from opening or
  saving the world. Players should back up first because content owned by missing
  optional mods may be lost; Fieldwork does not claim to recover that data.

## Version 1 Scope

Version 1 affects only Create's standard Redstone Link network.

Included:

- Create Redstone Link transmitter/receiver pairing
- Other endpoints that deliberately participate in the same standard Create Redstone Link network protocol
- Create train-schedule Redstone Link global conditions (D02: public WORLD sources only; private-network consumers aboard craft are deferred)
- One craft-wide isolation block
- Multiple-isolator lifecycle handling
- Assembly, disassembly, load, unload, break, replacement, explosion, split, and reassembly handling

Excluded:

- Vanilla redstone dust, repeaters, comparators, torches, and neighbor power
- Create Redstone Contacts
- Flight Control's direct redstone proxy behavior
- Control seats and ordinary Aeronautics controls
- ComputerCraft or other wireless-redstone mods
- Frequency whitelist or blacklist UI
- Directional input/output settings
- A cross-boundary gateway
- Generic upstream block/block-entity data recovery, automatic rollback, and a crash-recovery journal (D01: optional safeguards, deferred beyond the V1 core)

The addon must not intercept broad methods such as `Level#getSignal`. Isolation is applied at the audited standard-network pair/consumer paths and, for D02, the train condition's global source-visibility query. The global train query must be handled separately rather than assumed covered by a pair filter.

## Behavior Contract

The isolator has no effect while it is an ordinary world block. It becomes active only when contained by an assembled Sable SubLevel.

| Sender | Receiver | Result |
|---|---|---|
| World | World | Preserve upstream behavior |
| Non-isolated craft | World | Preserve current Create/Sable behavior |
| Non-isolated craft A | Non-isolated craft B | Preserve current Create/Sable behavior |
| Isolated craft | Same SubLevel | Allow |
| Isolated craft | Parent world | Deny in both directions |
| Isolated craft A | Craft B | Deny if either endpoint belongs to an isolated craft |

Additional invariants:

1. Redstone Link frequencies, range, power, item data, and UI remain unchanged.
2. Zero isolators produces behavior equivalent to the unmodified Create/Sable stack.
3. One or more isolators makes the entire containing SubLevel private.
4. Isolation ends only when the last isolator leaves that SubLevel.
5. Internal communication is based on SubLevel identity, not distance from the isolator block.
6. Network evaluation never scans the craft's blocks.
7. Normal isolation registry state is reconstructed from isolator block identity
   and does not require separate permanent global data. This does not prohibit
   separate persistence for managed-controller revocation generations.

Controller tier contract:

1. An ordinary handheld Linked Controller is a WORLD source regardless of
   whether its player stands in WORLD, craft A, or craft B. It cannot address a
   private craft network merely from player membership.
2. An ordinary controller installed in a lectern follows the lectern's physical
   scope. A lectern on isolated craft A can address PRIVATE(A); a WORLD lectern
   remains WORLD.
3. A controller granted by a smart isolator resolves the server-owned binding
   target from hand or lectern and may operate that target from WORLD/A/B within
   the same dimension and native range. Invalid, unavailable, conflicted or
   unacknowledged managed state is PENDING, never WORLD fallback.
4. Fieldwork reconciles exact affected controller endpoints from events and keeps
   a low-frequency complete safety sweep. Create's native TTL loop is unchanged;
   Fieldwork must not add a second all-entry reconciliation every tick.

Schematic assistance contract:

1. Fieldwork assistance applies only when a valid Create schematic is selected in
   the offhand and no valid main-hand schematic takes precedence. Main-hand Create
   preview, tools and HUD remain native.
2. Comparison is client-side guidance only. It never supplies craft ownership,
   server authorization, placement permission or chunk loading.
3. Loaded ordinary-world positions may be classified as matching, wrong block or
   a supported construction-state mismatch. Unsupported properties, block-entity
   data, expected air and uncertain/excluded positions remain neutral rather than
   being guessed.
4. Correctly placed ordinary ghost blocks may be hidden from the native preview;
   stale, ambiguous, unsupported or over-budget results must restore the ghost.
5. Comparison, caching, outlines, masking and metadata capture are bounded. No
   measured performance improvement is claimed.
6. In active offhand assistance, a settled projected block under the crosshair may
   show its localized block name and corresponding item icon below the crosshair.
   A block without an item shows only its name. This is client-only guidance,
   limited to discovered, eligible cached cells within 64 blocks; hidden ghosts
   and cells occluded by real block outline geometry do not receive a label.
   Selection uses whole-cell bounds, not exact model shapes, and adds no material
   counts or construction-state text.

D01: restoring upstream-lost blocks/data and preserving their former isolation
intent across crashes are optional safeguards, not V1 prerequisites. Fieldwork
remains responsible for its own registry, initialization and refresh correctness.
Unknown/incomplete reconstruction must not be treated as an empty set; after
trustworthy reconstruction, actual absence of all isolators means WORLD. A
deferred journal is not implemented or claimed as verified.

D02: V1 treats the train-schedule Redstone Link condition as a public WORLD
observer regardless of train location. It observes same-frequency WORLD sources
across loaded dimensions without adding range checks; PRIVATE sources are
invisible. A known WORLD positive completes High and waits for Low; without one,
potentially visible PENDING sources defer both polarities; confirmed absence
completes Low. Train-on-craft private consumption is deferred and unsupported.
This is an explicit consumer contract: V1 does not derive Train/Carriage
membership. Source visibility, high/low, pending load windows, and
source-scope/session cache invalidation remain required.

## Core Model: Wireless Network Scope

Each Create wireless endpoint receives an execution-time scope:

```text
WORLD
PRIVATE(<SubLevel identity>)
PENDING
```

Resolution:

```text
Endpoint outside a Sable SubLevel
→ WORLD

Endpoint inside a SubLevel with zero isolators
→ WORLD

Endpoint inside a SubLevel with one or more isolators
→ PRIVATE(SubLevel identity)

Endpoint or source whose authoritative scope is unresolved
→ PENDING
```

Pair policy:

```text
Either endpoint/source is PENDING
→ deny temporarily

Both endpoints are WORLD
→ preserve Create/Sable result

Both endpoints are private and have the same SubLevel identity
→ allow

Exactly one endpoint is private
→ deny

Both endpoints are private but have different SubLevel identities
→ deny
```

Illustrative pseudocode:

```java
boolean mayCommunicate(Endpoint sender, Endpoint receiver) {
    Scope a = scopeOf(sender);
    Scope b = scopeOf(receiver);

    if (a == Scope.PENDING || b == Scope.PENDING) {
        return false;
    }

    if (a == Scope.WORLD && b == Scope.WORLD) {
        return true;
    }

    return a instanceof Scope.Private privateA
        && b instanceof Scope.Private privateB
        && privateA.craft().equals(privateB.craft());
}
```

This predicate supplements upstream frequency/range checks; it does not replace them.

## SubLevel Isolation Registry

Redstone Link evaluation must perform only constant-time ownership and registry lookups. It must never search a craft for an isolator during a network update.

Required model shape:

```text
MinecraftServer
└─ SubLevel identity
   ├─ lifecycle state
   └─ set of isolator positions
```

Lifecycle state:

```text
UNINITIALIZED
INITIALIZING
READY(isolator positions)
```

Requirements:

- Scope the registry to one logical server; never use one process-global static map across server instances.
- Store stable SubLevel identities and block positions, not strong references to unloaded SubLevel objects.
- Build the initial position set atomically during assembly/load before making endpoints queryable.
- Register and unregister isolators on placement, replacement, player break, explosion, split/reassembly, unload, disassembly, and server stop.
- Make duplicate lifecycle events idempotent by tracking positions rather than incrementing an unchecked integer.
- Clear registry data when its SubLevel is destroyed or unloaded.
- Reconstruct missing state from isolator block identity, never from visual block-state flags.
- During container removal, preserve the old craft/position/controller footprint
  and barrier. Detached serialized markers are not validated through unstable live
  plot block state; publish removal only after the active container no longer
  contains the UUID. Incomplete removal remains fail-closed. This does not add
  D01 recovery of already-lost serialized data.

Fail-closed lifecycle behavior:

- An uninitialized or initializing SubLevel must never silently join the shared world network.
- If a Redstone Link query occurs before reconstruction completes, temporarily resolve endpoints in that SubLevel as `PRIVATE(SubLevel identity)`.
- This may briefly isolate a no-isolator craft during a lifecycle race, but it cannot accidentally activate external machinery.

## Redstone Link Integration Boundary

Sable intentionally projects endpoints inside a SubLevel into parent-world coordinates before Create performs wireless range comparisons. Create: Fieldwork must preserve that transformation.

The implementation must:

1. Identify the exact method where Create evaluates a transmitter/receiver pair or aggregates eligible transmitters for a receiver.
2. Confirm how Sable transforms endpoint positions in that method.
3. Add one additional pair-eligibility predicate after ownership can be resolved.
4. Preserve Sable's projected range calculation and Create's frequency/power aggregation.
5. Avoid replacing or redirecting the complete network update.
6. Use strict mixin application (`require = 1` or equivalent) so dependency changes fail visibly.
7. Record the target descriptor, Sable injection type, priority, slice, ordinal, and transformed control flow for each supported dependency set.

A descriptor-only unit test is insufficient. A launch-level smoke test must apply both Sable's and Create: Fieldwork's mixin configurations and prove:

- Sable's projected distance still controls range.
- The Fieldwork scope predicate executes.
- Internal same-craft links remain eligible.
- Cross-boundary links are rejected.

If no narrow composable hook exists, stop and redesign rather than replacing the complete Redstone Link network handler.

## Redstone Link Isolator Block

The block has no redstone output or ordinary-world side effects.

Required properties:

- Block entity used for SubLevel lifecycle registration
- Derived boolean block-state property: `ACTIVE`
- `ACTIVE=false` in the ordinary world
- `ACTIVE=true` only when the block belongs to a `READY` assembled SubLevel with at least one registered isolator

The visual state is never authoritative:

- Registry reconstruction uses block identity and position.
- Persisted `ACTIVE` values do not create isolation.
- Assembly/load normalizes all isolator visual states from the rebuilt registry.
- Disassembly normalizes returned world blocks to inactive.
- Visual updates occur only when relevant lifecycle/count transitions happen, never during Redstone Link queries.

Tooltip intent:

> While aboard an assembled craft, Redstone Links can communicate only with links on the same craft.

The corresponding Traditional Chinese tooltip is maintained in the `zh_tw.json` localization resource.

The tooltip states that ordinary redstone and non-Create wireless systems are unaffected.

## Recipe Contract

D03 uses ordinary shaped 3×3 crafting, pattern `OBO / ELE / BPB`, output one.
The smart variant is a shapeless upgrade from one ordinary isolator plus three
Brass Sheets. Exact ingredients, unlock behavior and balance rationale are defined
in the [D03 recipe document](2026-11-06-isolator-recipe.md). Actual client
crafting/unlock interaction remains part of player acceptance.

## Future Gateway Boundary

The current block is an isolator, not a gateway. It creates a private scope but provides no path across that boundary.

The name **Redstone Link Gateway** is reserved only. No gateway is committed to
the roadmap. A future proposal would require its own behavior and security design
and could consider:

- Bridge selected frequencies between craft and world
- Support input/output/bidirectional modes
- Require explicit player configuration
- Remain the only controlled exception to isolation

No gateway behavior may be partially included in V1.

## Required Test Matrix

### Network behavior

1. World transmitter to world receiver remains unchanged.
2. A craft without an isolator communicates exactly as before.
3. Two links inside one isolated craft communicate.
4. Isolated craft to world is blocked.
5. World to isolated craft is blocked.
6. Isolated craft A to craft B is blocked.
7. Two non-isolated crafts retain existing Sable behavior.
8. Frequency, range, and power remain Create-authored.
9. Train Redstone Link global conditions obey the separately confirmed D02 visibility/cache semantics, including high/low, pending sources, cross-dimension/no-range behavior and scope-only changes without listeners.

### Registry lifecycle

1. Zero, one, and multiple isolators.
2. Isolator placed after assembly.
3. Isolator replaced, broken by a player, or destroyed by explosion.
4. Final isolator removed.
5. Assembly and disassembly.
6. SubLevel unload/reload.
7. Server restart.
8. Craft split and reassembly.
9. Duplicate lifecycle callbacks.
10. Redstone Link query during initialization.

### Visual state

1. Inactive in the ordinary world.
2. Active in a ready isolated SubLevel.
3. Multiple isolators show consistent derived state.
4. Disassembly restores inactive world blocks.
5. Restart reconstructs state from block identity.
6. Client receives the correct synchronized block state.

### Controller binding

1. Ordinary handheld controllers remain WORLD regardless of player craft.
2. Ordinary lecterns follow their physical scope.
3. Managed controllers resolve only a valid loaded smart-isolator grant.
4. Missing, unloaded, conflicted or revoked grants remain PENDING and never fall
   back to WORLD.
5. Multiple smart isolators and multiple devices preserve craft-wide scope;
   conflicting grants across crafts deny.
6. Unbind preserves native channels/name and requires a fresh press before an
   ordinary source can publish.

### Schematic assistance

1. Offhand-only assistance does not alter normal main-hand Create behavior.
2. Rotation, mirror and translation use Create's native template transform.
3. Wrong block and supported construction-state differences are distinguished;
   unsupported or uncertain cases stay neutral.
4. Reserved plots and projected craft bounds are excluded as hints, never used as
   server ownership answers.
5. Work, cache, outline and mask limits are enforced without force-loading chunks.
6. Client rendering, interaction and shader behavior remain separate player
   acceptance from headless comparison tests.

### Regression boundary

1. Redstone Contacts unchanged.
2. Redstone dust, repeaters, comparators, and torches unchanged.
3. Flight Control direct proxy behavior unchanged.
4. Control seats and unrelated Aeronautics controls unchanged.
5. No wireless query scans craft blocks.
6. No permanent global isolation registry is written; the managed-controller
   revocation ledger remains a separate denial-only mechanism.

## Completion Criteria

- One Redstone Link Isolator makes only the containing craft's standard Create wireless network private.
- Same-craft links remain functional.
- Cross-boundary communication is rejected symmetrically.
- Removing the last isolator restores upstream behavior.
- Registry lifecycle cannot leave stale isolation or accidental exposure.
- Sable's projected range behavior remains intact.
- No unrelated redstone or Aeronautics system changes.
- Redstone Link evaluation uses constant-time lookup and performs no block scan.
- Managed controllers never derive authority from client-provided craft identity.
- Schematic assistance remains bounded, client-only and non-authoritative.

## Dependency and Evidence References

Pinned artifact checksums live in [`smoke/artifacts.lock`](../../smoke/artifacts.lock).
Exact integration findings are preserved in the [dependency audit](../research/2026-11-06-dependency-audit.md),
[lifecycle audit](../research/2026-11-06-lifecycle-boundaries.md),
[controller audit](../research/2026-11-06-controller-endpoints.md) and
[production mixin audit](../testing/main-mixin-audit.md). Public upstream branches
are references only; compatibility claims require the pinned artifacts and their
transformed bytecode.
