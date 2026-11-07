# Documentation

## Start here

| Document | Purpose |
|---|---|
| [Project README](../README.md) | Features, requirements, recipe, build quick start |
| [Player guide](testing/player-test-guide.md) | Installation, testing, bug reports |
| [Developer guide](../AGENTS.md) | Build/run workflow, smoke, engineering rules |
| [Behavior contract](../plan.md) | Scope, invariants, required matrix |
| [V1 technical design](design/redstone-link-isolator-v1.md) | Architecture, lifecycle protocol, decisions |

## Design

- [Controller requirements](design/2026-11-06-controller-scope.md)
- [D03 recipe and cost comparison](design/2026-11-06-isolator-recipe.md)

**Decisions:** D01 defers stronger upstream data-loss recovery; D02 makes every train condition a public WORLD observer and defers private train consumers; D03 uses ordinary shaped 3×3 crafting. See the technical design for full boundaries.

## Development and verification

- [Production acceptance](testing/redstone-link-isolator-acceptance.md): main-only results, installer evidence, player observations, pending checks.
- [Scoped refresh](testing/scoped-refresh.md): selection, counters, performance limits.
- [Private-controller movement](testing/controller-movement-refresh.md): same-frequency receiver selection, range/session/reentry controls and the later local0.1.0 worktree build.
- [Production mixin audit](testing/main-mixin-audit.md): exact hooks and transformed bytecode.
- [Texture integration](testing/placeholder-textures.md) and [art editing](../art/redstone_link_isolator/README.md).
- [GitHub Actions and tag releases](testing/github-actions.md).
- [First pure-core slice](testing/pure-core.md): historical policy milestone.

## Historical research

Dated documents preserve non-shipping prototype findings. Read status notes before interpreting old requirements, missing-feature statements or pass counts.

1. [Dependencies and wireless paths](research/2026-11-06-dependency-audit.md)
2. [Lifecycle/publication boundaries](research/2026-11-06-lifecycle-boundaries.md)
3. [Controller provenance](research/2026-11-06-controller-endpoints.md)
4. [Accepted movement and recovery](research/2026-11-06-movement-and-recovery.md)
5. [Train conditions and counterexamples](research/2026-11-06-train-global-condition.md)
6. [Headless prototype runs](testing/scope-hook-probe.md)

Internal planning history/task state lives under `.ramblings/`; it does not replace production acceptance evidence.

## Evidence conventions

- `verifyFieldworkAcceptance` loads main; `verifyScopeProbe` deliberately excludes it.
- Historical `0.1.0-dev` hashes identify specific builds, not `0.1.0` checksums.
- JUnit, headless phases, installer startup and player reports are separate evidence layers; do not total assertions into a completed release matrix.
- Expected-unsafe controls prove reproduced hazards, not safety guarantees.
- `.audit/`, build reports and scratch worlds are ignored, local and replaceable. Summaries preserve results/reproduction commands; references do not imply raw logs are distributed.
