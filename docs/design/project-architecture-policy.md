# Project Architecture Policy

This document defines the mandatory architecture boundaries for Create: Fieldwork.
It is normative for production code, tests, integration hooks, and durable project
documentation. The [behavior contract](behavior-contract.md) defines product
requirements; the [V1 technical design](redstone-link-isolator-v1.md) defines the
current implementation model; this policy defines how changes must preserve those
requirements.

Authority precedence is: behavior contract, V1 technical design, mandatory
policies, then supporting feature contracts. Dated research and design history
provide context only and cannot override current requirements.

## Architectural principles

1. **Server authority:** all isolation, binding, publication, and controller
   authorization decisions are derived from server-owned state. Client reports
   are input requests, never authority.
2. **Logical-server ownership:** mutable runtime state belongs to one
   `MinecraftServer` through `FieldworkServerState`. Process-wide mutable server
   maps are prohibited.
3. **Fail closed:** unknown, initializing, conflicted, or failed state must not be
   interpreted as WORLD or as an empty private network.
4. **Narrow integration:** mixins adapt audited upstream seams. Broad interception,
   replacement network handlers, and unrelated vanilla redstone hooks are
   prohibited.
5. **Preserve upstream semantics:** Fieldwork supplements Create and Sable scope
   decisions without replacing frequency, range, strength, projection, timeout,
   or native lifecycle behavior.
6. **Constant-time queries:** hot scope and pair checks use indexed ownership and
   registry state. They must never scan craft blocks.

## Production package ownership

| Package | Architectural responsibility |
|---|---|
| `platform` | Bootstrap, registration, commands, events, and logical-server lifetime |
| `isolator` | Blocks, block entities, binding authority, craft state, and lifecycle transactions |
| `controller` | Endpoint provenance, routing, sessions, lecterns, and reset behavior |
| `wireless` | Scope policy, receiver refresh, source publication, and train visibility |
| `schematic` | Comparison, masks, inputs, and client-only rendering assistance |
| `mixin` | Feature-grouped adapters for exact upstream injection points |

Packages follow feature ownership rather than generic technical categories. New
`util`, `common`, or `misc` packages are not allowed. Detailed class-shape and
formatting rules are in the [code organization policy](code-organization-policy.md).

## State and dependency boundaries

- `FieldworkServerState` is the composition root and owns registry, transition,
  binding, controller, refresh, publication, train, and visual services.
- Isolation state and lifecycle code must not depend on Minecraft client classes.
- Binding authority is separate from controller endpoint routing. A managed
  controller stores a device identity, not a trusted target craft identity.
- Receiver refresh and source publication remain separate responsibilities.
- The isolation registry stores stable craft keys and positions, not long-lived
  loaded-craft references.
- Schematic client code may consume synchronized or local presentation state but
  must not decide server authorization.
- Non-shipping executable tooling lives under `src/tools/`: production acceptance
  fixtures under `acceptance`, and the historical binding experiment under
  `binding-probe`. Research and acceptance tools remain separate source sets and
  must not be packaged in the shipping JAR.

## Lifecycle and publication ordering

Lifecycle mutations must preserve the transaction protocol described by the V1
design:

1. capture the affected craft, position, controller, and network footprint before
   upstream mutation;
2. install craft and position barriers;
3. withdraw or stage affected output while barriers remain active;
4. perform the upstream mutation and validate ledger/postconditions;
5. run final Create receiver refresh before Simulated receiver refresh;
6. publish or forget registry state;
7. reclassify source publications and commit visibility;
8. release barriers only after every throwable authoritative stage succeeds.

Failure retains a named quarantine and retry footprint. Visual synchronization is
post-commit and must not turn a committed authoritative transition into rollback.

## Integration and mixin policy

- `create_fieldwork.mixins.json` remains strict with `required=true` and
  `defaultRequire=1`.
- Each mixin class targets one upstream class or one coherent injection seam.
- Package movement and injection-logic changes must be reviewed and verified as
  separate changes.
- Exact descriptors, ordinals, slices, and operation counts must be recorded in
  the [mixin audit](../testing/main-mixin-audit.md).
- Client mixins must remain in the client array; dedicated-server startup proves
  server-side safety but does not prove client runtime behavior.

## Documentation policy

- Durable design, architecture, decision, and engineering-policy documents live
  under `docs/` and are written in English.
- `docs/README.md` is the documentation authority map. Production documents must
  not depend on ignored planning artifacts or local scratch logs.
- `.ramblings/`, `.slim/`, `.audit/`, build outputs, and run directories are local
  workflow or evidence artifacts, not product authority.
- Historical research and old test counts must be clearly labeled and must not be
  presented as current acceptance.
- Current runtime evidence belongs in the production acceptance record; README and
  developer guidance should link to it instead of duplicating a second ledger.
- Superseded plans must be removed or explicitly fenced as history; imperative
  backlog wording must not remain in canonical documents after implementation.

## Change gates

- Documentation-only changes require link checks, English-content checks, and
  `git diff --check`.
- Production code changes require `./scripts/gradle-docker.sh build`; package,
  mixin, generated-resource, or version changes require `clean build`.
- Changes to lifecycle, binding, persistence, or server-state composition require
  the relevant production acceptance phases and persistence pairs.
- Shipping archive isolation, license inclusion, and strict mixin configuration
  must remain verified.
- Client, GUI, multiplayer, and real-explosion acceptance must be stated as pending
  until executed by an appropriate client/player environment.

## Exceptions

An exception to this policy must be explicit in the durable design document,
narrowly scoped, and supported by verification evidence. Convenience, shorter
diffs, or compatibility with ignored workflow artifacts are not sufficient
reasons to violate an architecture boundary.
