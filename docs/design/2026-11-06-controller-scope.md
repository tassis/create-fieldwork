# Controller Scope — Superseded Design History

Established 2026-11-06. This document records investigation history and does not
define current behavior. The [behavior contract](behavior-contract.md) and
[V1 design](redstone-link-isolator-v1.md#6-controllers-and-membership) supersede
it; the [acceptance record](../testing/redstone-link-isolator-acceptance.md)
records tested coverage.

## Original requirement and approved replacement

The original requirement sought controller operation by players aboard a craft.
Research showed that player-membership authority was unsafe and poorly aligned
with Create's actual controller paths. The approved implementation instead uses:

- ordinary handheld controllers as WORLD sources;
- ordinary lecterns according to their physical WORLD/PRIVATE location; and
- managed controllers according to a loaded server-owned smart-isolator grant.

All routes preserve upstream frequency, range and power. Invalid managed state is
PENDING and never falls back to WORLD.

## Findings that shaped the design

- Audit handheld and lectern routing separately. Both ultimately create `ManualFrequencyEntry` with the **player's world position**, not plot/lectern coordinates.
- Scope provenance must therefore be separate from range coordinates. The
  superseded proposal used confirmed player membership for handheld scope; the
  approved model keeps ordinary handheld scope WORLD and uses smart grants for
  remote private control. Lecterns use the validated source block.
- Use existing Sable tracking/block ownership APIs and compare logical server, dimension and UUID. A shared Minecraft Level alone cannot identify one craft.
- Never scan craft blocks, grant membership from AABB overlap, rewrite controller `getLocation`, or accept a client-reported scope UUID.
- Held-entry reuse updates position/TTL without upstream network recomputation. Scope/device/session changes need reconciliation and old/new boundary refresh.
- Null tracking is not proof of WORLD membership. Historical movement probes
  demonstrated this hazard; production does not use movement proposals as
  controller authority.

The earlier movement-based PENDING model is superseded by server-owned source
sessions, physical lectern provenance and managed-device grants.

## Historical verification questions

- Aboard private A: the original player-membership proposal expected same-craft
  output; current ordinary handheld behavior is WORLD instead.
- WORLD/B same-frequency input cannot control private A.
- Marker-free craft retain upstream behavior.
- Managed source, session, inventory, dimension, disconnect and grant changes
  retract/rebind old contributions correctly.
- Handheld and lectern each retain player-world range semantics.
- Release and the upstream 30-tick timeout remain functional.
- Server-handler tests are distinct from real client UI, key and codec acceptance.

## Evidence trail

The [endpoint audit](../research/2026-11-06-controller-endpoints.md) recorded controller40, including the unsafe null→WORLD observation. The [movement continuation](../research/2026-11-06-movement-and-recovery.md) used real ServerPlayer/vanilla listeners for motion37: accepted/rejected input, airborne uncertainty, grounded departure, disconnect, rotation and cross-dimension behavior.

Those are non-shipping prototypes. Current production does not use airborne or
movement transitions as controller authority or routing events; ordinary movement
and same-dimension teleport/ack schedule no Fieldwork controller work. Current
main has server/session-owned implementation and separate acceptance tests. Real
client sequencing remains player acceptance work.
