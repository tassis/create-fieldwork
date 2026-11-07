# Controller Scope — Requirements and Design History

Established 2026-11-06. Production controller integration now exists; the [V1 design](redstone-link-isolator-v1.md#6-controllers-and-membership) defines current semantics and the [acceptance record](../testing/redstone-link-isolator-acceptance.md) records tested coverage.

## Requirement

Players aboard a craft must be able to use Create controllers to operate that craft's standard wireless network. Same-private-craft communication remains allowed; WORLD/other craft are denied in both directions. Preserve upstream frequency, range and power. Block-only probes cannot establish controller coverage.

## Findings that shaped the design

- Audit handheld and lectern routing separately. Both ultimately create `ManualFrequencyEntry` with the **player's world position**, not plot/lectern coordinates.
- Scope provenance must therefore be separate from range coordinates. Handheld uses confirmed player membership; lectern uses the validated source block.
- Use existing Sable tracking/block ownership APIs and compare logical server, dimension and UUID. A shared Minecraft Level alone cannot identify one craft.
- Never scan craft blocks, grant membership from AABB overlap, rewrite controller `getLocation`, or accept a client-reported scope UUID.
- Held-entry reuse updates position/TTL without upstream network recomputation. Scope/device/session changes need reconciliation and old/new boundary refresh.
- Null current tracking is not proof of departure. Sable mutates tracking before vanilla acceptance; rejected movement can clear it while the player stays put.
- PENDING conservatively handles unresolved/rejected private transitions. Last UUID can justify denial, not grant membership. Accepted ordinary-world position plus actual server-computed support can release protected history; accepted plot evidence restores craft control.

The earliest proposal restricted PENDING to disappearance of a prior private association. Later movement tests showed that an unaccepted private proposal must also pause an initially WORLD source. The current design includes that correction.

## Verification matrix

- Aboard private A: same-craft output, no WORLD/B output.
- WORLD/B same-frequency input cannot control private A.
- Marker-free craft retain upstream behavior.
- While held: departure, craft switch, rotation, dimension change, disconnect and new session retract/rebind old sources correctly.
- Handheld and lectern each retain player-world range semantics.
- Release and the upstream 30-tick timeout remain functional.
- Server-handler tests are distinct from real client UI, key and codec acceptance.

## Evidence trail

The [endpoint audit](../research/2026-11-06-controller-endpoints.md) recorded controller40, including the unsafe null→WORLD observation. The [movement continuation](../research/2026-11-06-movement-and-recovery.md) used real ServerPlayer/vanilla listeners for motion37: accepted/rejected input, airborne uncertainty, grounded departure, disconnect, rotation and cross-dimension behavior.

Those are non-shipping prototypes. Current main has server/session-owned implementation and separate acceptance tests. Uncertain airborne transitions may temporarily pause control; this does not authorize a gateway exception. Real client sequencing and complex support shapes remain player acceptance work.
