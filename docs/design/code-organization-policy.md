# Code Organization and Readability Policy

This policy governs production Java under `dev.tassis.createfieldwork`. Its goal
is to make behavior discoverable by feature while preserving Fieldwork's
server-owned authority, fail-closed transitions and strict mixin auditability.
The [project architecture policy](project-architecture-policy.md) remains the
higher-level authority for ownership and integration boundaries.

## Package direction

Top-level packages are vertical feature areas:

- `platform`: mod bootstrap, registration, commands and logical-server lifetime.
- `isolator`: ordinary/smart block behavior, binding, lifecycle transactions and
  authoritative craft state.
- `controller`: endpoint routing, player sessions, lectern integration and reset.
- `wireless`: scope policy, receiver refresh, source publication and train reads.
- `schematic`: comparison, masking and client rendering/input assistance.
- `mixin`: integration classes grouped by the feature they serve.

Technical categories such as `content`, `registry` or `server` must not become a
catch-all. Place a class with the feature whose invariant it protects. Do not add
generic `util`, `common` or `misc` packages; name the concrete responsibility.

## Required dependency boundaries

- `FieldworkServerState` remains the single logical-server composition root.
- Isolation registry/state never depends on Minecraft client code.
- Binding authority remains separate from controller endpoint routing.
- Hot pair/scope queries remain constant-time and never scan craft blocks.
- Receiver refresh and source publication remain separate responsibilities.
- Mixin classes adapt upstream methods only; domain decisions belong outside
  mixins whenever the exact injection ordering does not require otherwise.
- No process-wide mutable server state may be introduced during reorganization.

## File and class shape

- Prefer one authoritative concept per top-level class.
- Keep public façades stable while extracting package-private collaborators.
- A class above roughly 250 lines requires a responsibility review. Above 400
  lines, extraction is expected unless the file is an auditable adapter whose
  ordering would become less clear when split.
- Methods should normally express one phase. Extract validation, selection,
  mutation, rollback and publication into named methods instead of interleaving
  them.
- Small immutable records and enums may remain nested when they have no meaning
  outside their owner. Do not create one-file wrappers solely to reduce line
  counts.

## Formatting

- Use one statement per line and braces for multi-line control flow.
- Break long boolean expressions by semantic clause, not at arbitrary columns.
- Put blank lines between validation, state capture, mutation and publication.
- Do not compress `try`, `catch`, rollback or retry paths onto one line.
- Prefer early returns for failed preconditions; preserve explicit `finally`
  ordering where transaction cleanup is part of correctness.
- Comments explain invariants and non-obvious upstream behavior, not syntax.

## Merge and split rules

- Merge files only when they share authority, lifecycle and callers. Registry
  declarations may share a façade; unrelated registry values must not become a
  monolith merely to reduce file count.
- Never merge mixins that target different upstream classes or injection points.
- Never merge `FieldworkServerState` with transition, binding or wireless state.
- Never merge receiver refresh with source publication.
- Split large classes behind their existing façade before changing callers.

## Mixin layout

Mixin packages mirror feature ownership: `mixin.platform`, `mixin.isolator`,
`mixin.controller.*`, `mixin.wireless` and `mixin.schematic.client`.

Every relocation must update the mixin JSON fully-qualified relative name while
retaining `required=true` and `defaultRequire=1`. Relocation and injection logic
changes must occur in separate verified steps.

## Refactor verification

Pure organization work must preserve behavior and artifact boundaries:

1. Run `./scripts/gradle-docker.sh clean build` after package or mixin movement.
2. Verify shipping archive isolation and strict mixin configuration.
3. Run production acceptance phases selected by the moved boundary.
4. Run both persistence pairs after lifecycle, binding or state composition moves.
5. Run `git diff --check` and update durable architecture documentation.

Compilation alone does not establish that package movement preserved lifecycle,
routing or fail-closed behavior.
