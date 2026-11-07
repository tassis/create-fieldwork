# Create: Fieldwork — Redstone Link Isolator Design Plan

> Implementation status: version `0.1.0` contains the production implementation and is available for player testing. See [installation and testing](docs/testing/player-test-guide.md) and [main-only acceptance](docs/testing/redstone-link-isolator-acceptance.md). Earlier `0.1.0-dev` hashes remain historical build identifiers. Client and full release acceptance are incomplete; this document specifies requirements, not a claim that every matrix row has passed.

## Purpose

Create: Fieldwork is a focused Create/Aeronautics addon. Its first block, the **Redstone Link Isolator**, gives an assembled Sable/Aeronautics craft a private Create wireless-redstone network.

The design exists to prevent Redstone Links aboard a craft from accidentally interacting with matching frequencies in the parent world or another craft, while preserving communication between links aboard the same craft.

This document is intentionally repository-independent. Exact source paths, build layout, mappings, and mixin descriptors must be resolved against the dependency versions selected by the implementing project.

## Product Identity

- Display name: **Create: Fieldwork**
- Suggested mod ID: `create_fieldwork`
- First block: **Redstone Link Isolator**
- Suggested block ID: `create_fieldwork:redstone_link_isolator`
- Traditional Chinese localization: maintained in `src/main/resources/assets/create_fieldwork/lang/zh_tw.json`
- Suggested first release: `0.1.0`
- Possible future block: **Redstone Link Gateway**, reserved for controlled cross-boundary communication

## Reference Baseline

The original design research used this stack:

- Minecraft `1.21.1`
- NeoForge `21.1.257`
- Create `6.0.10`
- Sable `2.0.5`
- Create Aeronautics `1.3.2`

These versions are a research baseline, not a claim of compatibility with other releases. The implementation should use exact dependency ranges and reject unsupported versions until their transformed bytecode has been reviewed.

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
7. Isolation state is reconstructible from isolator block identity and does not require separate permanent global data.

D01 is confirmed: restoring upstream-lost blocks/data and preserving their former isolation intent across crashes are optional safeguards, not V1 prerequisites. Fieldwork remains responsible for its own registry, initialization and refresh correctness. Unknown/incomplete reconstruction must not be treated as an empty set; after trustworthy reconstruction, actual absence of all isolators means WORLD. A deferred journal is not implemented or claimed as verified.

D02 scope revision approved by the owner: V1 treats the train-schedule Redstone Link condition as a public WORLD observer regardless of train location. It observes same-frequency WORLD sources across loaded dimensions without adding range checks; PRIVATE sources are invisible. A known WORLD positive completes High and waits for Low; without one, potentially visible PENDING sources defer both polarities; confirmed absence completes Low. Train-on-craft private consumption is deferred and not supported in V1. This is an explicit consumer contract, not a fallback from failed Train ownership resolution: V1 does not derive Train/Carriage membership. Source visibility, high/low, pending load windows, and source-scope/session cache invalidation remain required. This revision supersedes the earlier requirement for PRIVATE(A) trains to consume PRIVATE(A).

## Core Model: Wireless Network Scope

Each Create wireless endpoint receives an execution-time scope:

```text
WORLD
PRIVATE(<SubLevel identity>)
```

Resolution:

```text
Endpoint outside a Sable SubLevel
→ WORLD

Endpoint inside a SubLevel with zero isolators
→ WORLD

Endpoint inside a SubLevel with one or more isolators
→ PRIVATE(SubLevel identity)
```

Pair policy:

```text
Neither endpoint is private
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

    if (!a.isPrivate() && !b.isPrivate()) {
        return true;
    }

    return a.isPrivate()
        && b.isPrivate()
        && a.subLevelId().equals(b.subLevelId());
}
```

This predicate supplements upstream frequency/range checks; it does not replace them.

## SubLevel Isolation Registry

Redstone Link evaluation must perform only constant-time ownership and registry lookups. It must never search a craft for an isolator during a network update.

Recommended transient model:

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

Fail-closed lifecycle behavior:

- An uninitialized or initializing SubLevel must never silently join the shared world network.
- If a Redstone Link query occurs before reconstruction completes, temporarily resolve endpoints in that SubLevel as `PRIVATE(SubLevel identity)`.
- This may briefly isolate a no-isolator craft during a lifecycle race, but it cannot accidentally activate external machinery.

## Redstone Link Integration Boundary

Sable intentionally projects endpoints inside a SubLevel into parent-world coordinates before Create performs wireless range comparisons. Create: Fieldwork must preserve that transformation.

The implementation should:

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

The block should have no redstone output or ordinary-world side effects.

Recommended properties:

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

Suggested tooltip:

> While aboard an assembled craft, Redstone Links can communicate only with links on the same craft.

The corresponding Traditional Chinese tooltip is maintained in the `zh_tw.json` localization resource.

The tooltip should explicitly state that ordinary redstone and non-Create wireless systems are unaffected.

## Recipe Direction

The block is intended as a mid-game Create/Aeronautics control component, normally one per craft.

Suggested ingredients:

- One Create Redstone Link
- One Precision Mechanism
- Electron Tubes
- Brass components
- Obsidian as the visual/material language for isolation

Initial recipe, set under the owner's D03 delegation after checking the pinned Create/Minecraft recipes: ordinary shaped 3×3 crafting, output one. Pattern `OBO / ELE / BPB`: two Obsidian, three Brass Sheets (`c:plates/brass`), two Electron Tubes, one Redstone Link, one Precision Mechanism. Unlock on obtaining a Redstone Link. The Precision Mechanism already supplies the mid-game sequenced-assembly gate; no extra Mechanical Crafting recipe is required. The recipe/item is now implemented and headlessly tested; actual client crafting/unlock interaction remains part of player acceptance.

## Future Gateway Boundary

The current block is an isolator, not a gateway. It creates a private scope but provides no path across that boundary.

The name **Redstone Link Gateway** is reserved for a possible later block that could:

- Bridge selected frequencies between craft and world
- Support input/output/bidirectional modes
- Require explicit player configuration
- Remain the only controlled exception to isolation

No gateway behavior should be partially included in the first release.

## Implementation Phases

### Phase 1: Exact Dependency Audit

- Obtain the exact Create, Sable, and Aeronautics artifacts targeted by the project.
- Verify artifact hashes and mappings.
- Inspect Create's wireless network handler and endpoint interfaces.
- Inspect Sable's transformed Redstone Link range logic and SubLevel ownership APIs.
- Select narrow composable hooks for the supported pair/direct-consumer paths and separately confirm the D02 global condition; stop/redesign any path that requires replacing the complete network handler.

### Phase 2: Standalone Addon Scaffold

- Create the Create: Fieldwork addon with exact required dependency ranges.
- Add an empty strict mixin configuration and launch it against the target stack.
- Keep the addon independent from any modpack-specific integration mod.

### Phase 3: Isolator and Registry

- Register the Redstone Link Isolator block and block entity.
- Implement server-scoped SubLevel lifecycle state and isolator-position sets.
- Cover every mutation/lifecycle surface and fail closed while initialization is incomplete.
- Test duplicate events, multiple isolators, teardown, and reconstruction.

### Phase 4: Wireless Scope Filter

- Implement the pure scope resolver and pair policy.
- Integrate strict, narrow filters at the audited Create/Sable pair boundary and Simulated consumer path, plus validated controller provenance.
- Specify and test the D02 train global-visibility adapter, both activation polarities, pending behavior and scope-aware cache invalidation; do not mark it excluded or infer it is covered by pair tests.
- Add static contract tests and launch-level mixin composition tests.

### Phase 5: Player-Facing Content

- Add model, derived active state, translations, tooltip, recipe, loot, and creative-tab entry.
- Keep the first release to one functional block.

### Phase 6: Runtime Acceptance

- Build one candidate against the exact target dependencies.
- Execute the full behavior and lifecycle matrix on a disposable server.
- Record exact artifact hashes and observed results.

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

### Regression boundary

1. Redstone Contacts unchanged.
2. Redstone dust, repeaters, comparators, and torches unchanged.
3. Flight Control direct proxy behavior unchanged.
4. Control seats and unrelated Aeronautics controls unchanged.
5. No wireless query scans craft blocks.
6. No permanent global isolation database is written.

## Completion Criteria

- One Redstone Link Isolator makes only the containing craft's standard Create wireless network private.
- Same-craft links remain functional.
- Cross-boundary communication is rejected symmetrically.
- Removing the last isolator restores upstream behavior.
- Registry lifecycle cannot leave stale isolation or accidental exposure.
- Sable's projected range behavior remains intact.
- No unrelated redstone or Aeronautics system changes.
- Redstone Link evaluation uses constant-time lookup and performs no block scan.

## Reference Material

### Create

- Source repository: <https://github.com/Creators-of-Create/Create>
- Relevant concepts/classes to locate in the target version:
  - `RedstoneLinkNetworkHandler`
  - `IRedstoneLinkable`
  - `RedstoneLinkBlockEntity`
  - Frequency/network membership and receiver-power aggregation

### Sable

- Source repository: <https://github.com/ryanhcode/sable>
- SubLevel model and parent-world relationship should be reviewed in the exact target version.
- Relevant NeoForge compatibility source:
  - `neoforge/.../mixin/compatibility/create/redstone_links/RedstoneLinkNetworkHandlerMixin.java`
  - `neoforge/.../mixinhelper/compatibility/create/redstone_contact/RedstoneContactBlockEntity.java`
- Raw reference for the public Redstone Link mixin path used during initial research:
  <https://raw.githubusercontent.com/ryanhcode/sable/main/neoforge/src/main/java/dev/ryanhcode/sable/neoforge/mixin/compatibility/create/redstone_links/RedstoneLinkNetworkHandlerMixin.java>

### Create Aeronautics / Simulated

- Public source references may differ from a distributed bundled artifact; compare exact bytecode before selecting targets.
- Simulated Project repository: <https://github.com/Creators-of-Aeronautics/Simulated-Project>
- Example linked-receiver implementation reviewed during initial research:
  `simulated/common/.../content/blocks/redstone/AbstractLinkedReceiverBlockEntity.java`
- Raw reference:
  <https://raw.githubusercontent.com/Creators-of-Aeronautics/Simulated-Project/main/simulated/common/src/main/java/dev/simulated_team/simulated/content/blocks/redstone/AbstractLinkedReceiverBlockEntity.java>

### Important Source Caveat

Public `main` branches are architectural references only. Before implementation, verify exact target artifacts, mappings, descriptors, and transformed mixin behavior. Do not claim compatibility from repository source alone.
