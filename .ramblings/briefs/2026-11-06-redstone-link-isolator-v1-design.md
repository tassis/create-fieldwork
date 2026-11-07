# Redstone Link Isolator V1 Design Brief

Historical design brief, established2026-11-06. Main implementation subsequently reached player-test delivery. Current architecture: [V1 design](../../docs/design/redstone-link-isolator-v1.md); observed results: [acceptance](../../docs/testing/redstone-link-isolator-acceptance.md).

## Goal and context

The original brief consolidated exact-artifact research/headless prototypes into a reviewable design before main implementation. The owner later authorized simplified implementation under D01/D02/D03.

- Confirmed A: same isolated Sable instance communicates internally; WORLD/other craft cannot cross the boundary.
- Compare server/dimension/SubLevel UUID through existing current/block ownership APIs, not craft scans/AABB authorization.
- Handheld controllers aboard a craft must control it. Both upstream controller routes use player WORLD coordinates, so provenance and range must be separated.
- Prototype hooks, accepted movement/PENDING, multi-chunk/restart and transaction isolation have research evidence; prototype pass counts do not establish production acceptance.
- Pinned stack: MC1.21.1, NeoForge21.1.257, Create6.0.10, Sable2.0.5, Aeronautics1.3.2.

## Approved direction

- Shared WORLD/PRIVATE/PENDING policy and resolver, with fixed endpoints and controller provenance resolved separately.
- Narrow filters at both Create comparisons and Simulated's independent path; preserve upstream Operation/projection/frequency/range/power and controller getLocation.
- Logical-server-owned registry, immutable snapshots, affected-only lifecycle overlays, withdrawal before destructive writes and final receiver refresh.
- Server/session controller provenance and accepted movement evidence; unresolved/rejected private transitions remain PENDING.
- One isolator with derived ACTIVE and layered content/runtime/client acceptance.
- **D01:** stronger recovery of upstream-lost blocks/data and exceptional journal optional/deferred. Normal Fieldwork initialization/registry/refresh remain required.
- **D02:** every Train is a public WORLD global/no-range observer. Source visibility/high-low/pending/epoch/session remain required. Do not build TrainMembershipIndex for V1; private train consumers are deferred.
- **D03:** normal shaped3×3 `OBO/ELE/BPB`, output1; retain initial ingredients and Precision Mechanism's mid-game gate.

The earlier private-train-consumer/Train-ownership prerequisite and “do not start main” statements were superseded by explicit owner authorization. Ordinary craft links/controllers remain supported. `plan.md` is the behavior contract; unapproved scope changes must not be hidden.

## Constraints and alternatives

Queries perform constant-time ownership/map lookup; reconstruction/endpoint enumeration belongs to cold paths. Avoid long-lived craft/player references and cross-session reuse. Do not ship research oracles/static maps/global recovery coordinators. Receiver callbacks/mechanical side effects are not whole-batch rollbackable.

Rejected alternatives:

- Disable all onboard wireless: violates A.
- Replace handler/compete with Redirect: breaks composability.
- Change controller location to plot coordinates: changes range quantization; use provenance metadata.
- Null immediately WORLD: rejected movement reproduced external leakage; use PENDING.
- Make general upstream recovery a mandatory V1 feature: deferred by D01.
- Add nine Mechanical Crafters: manufacturing gate already exists in Precision Mechanism; ordinary shaped crafting is sufficient initially.

## Follow-through

Implementation proceeded through pure policy/JUnit, server registry/holder, main hooks/lifecycle/refresh, controller/train adapters and player content. Source visibility/cache required independent main tests rather than old private-train probes. Client visuals/GUI, full player matrix and final license remain outstanding; see the checklist without treating this brief as completion evidence.
