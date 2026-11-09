# Documentation

## Start here

| Document | Purpose |
|---|---|
| [Project README](../README.md) | Features, requirements, recipe, build quick start |
| [Player guide](testing/player-test-guide.md) | Installation, testing, bug reports |
| [Developer guide](../AGENTS.md) | Build/run workflow, smoke, engineering rules |
| [Behavior contract](design/behavior-contract.md) | Scope, invariants, required matrix |
| [V1 technical design](design/redstone-link-isolator-v1.md) | Architecture, lifecycle protocol, decisions |
| [Project architecture policy](design/project-architecture-policy.md) | Mandatory ownership, dependency, integration, and documentation boundaries |
| [Code organization policy](design/code-organization-policy.md) | Mandatory package, class-shape, formatting, and refactor rules |

## Design and architecture

Authority is ordered as follows: the behavior contract defines product behavior;
the V1 technical design defines the current implementation model; mandatory
policies constrain changes; supporting contracts refine one feature. Historical
notes never override those documents.

### Canonical authority

- [Behavior contract](design/behavior-contract.md)
- [V1 technical design](design/redstone-link-isolator-v1.md)
- [Project architecture policy](design/project-architecture-policy.md)
- [Code organization and readability policy](design/code-organization-policy.md)

### Supporting current contracts

- [Managed-controller binding contract](design/controller-binding-v02.md)
- [D03 recipe and cost rationale](design/2026-11-06-isolator-recipe.md)

### Superseded design history

- [Original controller-scope requirements](design/2026-11-06-controller-scope.md)
  records the investigation that preceded the current two-tier controller model.

**Decisions:** D01 defers stronger upstream data-loss recovery; D02 makes every train condition a public WORLD observer and defers private train consumers; D03 uses ordinary shaped 3×3 crafting. See the technical design for full boundaries.

## Development and verification

- [Production acceptance](testing/redstone-link-isolator-acceptance.md): main-only results, installer evidence, player observations, pending checks.
- [v0.2 server binding](testing/v02-server-binding.md): production managed-controller routing, persistence and fault evidence, separate from the experiment.
- [Controller player fixes](testing/controller-player-fixes.md): accepted airborne control, physical lectern checks, native channel UI and channel-preserving self-craft unbinding.
- [v0.2 schematic comparison](testing/schematic-client-v02.md): implementation/headless evidence, engineering limits and the pending owner client matrix.
- [Scoped refresh](testing/scoped-refresh.md): selection, counters, performance limits.
- [Private-controller movement](testing/controller-movement-refresh.md): same-frequency receiver selection, range/session/reentry controls and the later local0.1.0 worktree build.
- [Production mixin audit](testing/main-mixin-audit.md): exact hooks and transformed bytecode.
- [Texture integration](testing/placeholder-textures.md) and [art editing](../art/redstone_link_isolator/README.md).
- [GitHub Actions and tag releases](testing/github-actions.md).
- [First pure-core slice](testing/pure-core.md): historical policy milestone.
- [Controller binding experiment](testing/controller-binding-spike.md): non-shipping device-ID/lectern persistence and block-owned UUID-stamped authorization model; not remote-routing acceptance.

## Historical research

Dated documents preserve non-shipping prototype findings. Read status notes before interpreting old requirements, missing-feature statements or pass counts.

1. [Dependencies and wireless paths](research/2026-11-06-dependency-audit.md)
2. [Lifecycle/publication boundaries](research/2026-11-06-lifecycle-boundaries.md)
3. [Controller provenance](research/2026-11-06-controller-endpoints.md)
4. [Accepted movement and recovery](research/2026-11-06-movement-and-recovery.md)
5. [Train conditions and counterexamples](research/2026-11-06-train-global-condition.md)
6. [Headless prototype runs](testing/scope-hook-probe.md)

Ignored planning and local evidence artifacts are not documentation authority.
Durable design and policy documents live under `docs/` and use English prose.

## Evidence conventions

- `verifyFieldworkAcceptance` loads main; `verifyScopeProbe` deliberately excludes it.
- `verifyBindingProbe` loads main plus experimental persistence code; its results are not main-only acceptance.
- Historical `0.1.0-dev` hashes identify specific builds, not `0.1.0` checksums.
- JUnit, headless phases, installer startup and player reports are separate evidence layers; do not total assertions into a completed release matrix.
- Expected-unsafe controls prove reproduced hazards, not safety guarantees.
- `.audit/`, build reports and scratch worlds are ignored, local and replaceable. Summaries preserve results/reproduction commands; references do not imply raw logs are distributed.
