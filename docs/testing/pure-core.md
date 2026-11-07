# First Production Pure-Core Slice — Historical Record

Date:2026-11-06. This records `task-3-policy`, before the registry, real isolator and main hooks existed. Production subsequently reached player-test delivery; see [current acceptance](redstone-link-isolator-acceptance.md). Statements about an empty mixin/no isolation below describe this milestone only.

## Scope

After D02 was simplified to fixed public WORLD train observers, the first main slice added:

- `isolation/CraftKey.java`: dimension + SubLevel UUID within an enclosing logical-server context; no plot coordinates/live craft reference.
- `wireless/Scope.java`: WORLD, PRIVATE(CraftKey), PENDING. Later resolver distinguishes known-craft initialization (PRIVATE) from unresolved provenance/transfer (PENDING).
- `wireless/PairPolicy.java`: pure symmetric authorization ANDed with upstream result; no world reads, scans or registry mutation.

At this point main had no registry, isolator, controller or functional isolation hooks. The fixed-WORLD D02 adapter was not yet implemented; prototype train95 did not verify its new contract.

## Recorded commands/results

1. `./scripts/gradle-docker.sh test build`: success in34s; actual JUnit XML recorded75 tests, zero skipped/failures/errors, replacing the former NO-SOURCE state. `.audit/core-policy-first-build.log`.
2. `./scripts/gradle-docker.sh clean build`: success and all three mod hashes checked. Some compile/test outputs came from cache; not claimed as a second fresh JUnit execution. `.audit/core-policy-clean-build.log`.
3. `./scripts/gradle-docker.sh verifyScopeProbe`: research suite71 and complete save at `16:53:49Z`. `.audit/core-policy-suite-regression.log`. Intentional duplicate-plot load errors were negative fixtures, not main isolation acceptance.
4. Archive inspection of `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0-dev.jar`: core classes present, research/test classes absent; main strict mixin still empty.

Local `.audit/` and `build/` can be overwritten; source/tests and this summary preserve reproduction/results.

## Coverage and limits

- Six scope samples × six × two upstream outcomes =72 truth-table cases, each checked symmetrically. Includes equal independently constructed PRIVATE values, different UUID and same UUID in different dimensions.
- Three additional tests: value equality/hash; missing dimension/UUID/craft rejected; null scopes rejected even when upstream is false.
- PENDING/PENDING denies; same PRIVATE cannot override upstream range/frequency rejection.
- Pure policy does not prove an upstream Operation is called exactly once; hook integration tests establish that separately.
- CraftKey intentionally omits server instance; enclosing server-owned state enforces that boundary. Dual-state separation was subsequent work.
- Null is a programming error/fail-fast input. The runtime resolver must normalize uncertainty to PENDING rather than pass null into policy.

The following registry/holder slice added nonempty-vs-unknown initialization, immutable/idempotent position sets, teardown and server/session separation, followed by main hooks/lifecycle/refresh. The production acceptance page records those later milestones; this historical next-step list is not unfinished current implementation.
