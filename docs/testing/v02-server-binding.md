# v0.2 server binding implementation evidence

Initial server gate: 2026-11-07. Final integrated server/headless recheck: 2026-11-09.
Local worktree, version not bumped or published. This supersedes no released
artifact and does not establish client/GUI acceptance.

## Final integrated recheck — 2026-11-09

The current worktree passed JUnit **106**, shipping isolation, binding **177**,
binding-reset **57**, binding-faults **134**, controller-index **43**,
controller-work **15**, and consecutive independent-JVM binding-seed/reload
**30/27**. Ordinary regressions also passed: network **174**, refresh **69**,
movement **74**, lifecycle **23**, packed **24**, schematic **347**, and
seed/reload **24/25**. Every runtime verifier checked complete saves and state
close.

The corrected contract preserves native crouch-air channel editing. One bound
controller alone in 2x2/3x3 crafting unbinds only after actual result extraction,
preserving channels/name. Accepted airborne source positions and native physical
lectern lifecycle/input are covered. Details and the remaining client matrix are
in [controller player fixes](controller-player-fixes.md).

The five added binding assertions cover post-assignment `RuntimeException` and
`Error`: Create's successful native lectern assignment remains authoritative, the new
identity/pending state stays coherent and the exact failed refresh scope is
retained until retry withdraws the stale private output. Native assignment
failure itself continues to roll back and rethrow.

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
c69b8bb59e098f96027a8d3a8050d7be77cbaa9812d2d88183e382733340886c
```

`setup` rebuilt and installed this exact hash. The packaged server reached
`Done (0.692s)` at04:59:45Z; native console status returned withheld transactions
`[]` at05:00:09Z; native console `stop` saved all dimensions and Sable sub-levels
at05:00:09Z; the container exited0. This is packaged dedicated-server evidence,
not owner client acceptance. Older installer evidence below is historical.

### Lifecycle and allocation follow-up

The current candidate removes stale Simulated receiver positions on chunk unload,
clears player membership/controller session state on logout, and compacts revoked
grant records from loaded smart isolators while retaining the global persistent
revocation tombstones. Client schematic comparison now memoizes craft-bound
comparability within a tick. Controller reconciliation and binding rebuild avoid
redundant outer snapshots while retaining the endpoint-level defensive snapshots
needed during mutation. The global failed-transaction reset guard remains unchanged.
These are correctness/capacity/allocation changes; no FPS, MSPT or speedup is claimed.

Container removal retains the complete pre-removal craft/position/controller
footprint and barrier. A detached craft preserves markers in Sable's serialized
or holding data, so live plot block state is not a stable marker postcondition;
production instead requires that the active container no longer contains the
UUID before publishing removal. Exceptions or incomplete removal remain
fail-closed. This does not claim D01 recovery of already-lost serialized data.

## Historical integrated recheck — 2026-11-08

All rows below were rerun against the integrated production worktree, after the
client implementation/headless gate. These are separate evidence layers and
assertion counts, not a summed acceptance score.

| Check / phase | Passed | Report time (UTC) |
|---|---:|---|
| JUnit, explicitly `test --rerun-tasks` | 95; zero failures/errors/skips | 01:28:37 |
| network | 162 | 01:13:59 |
| refresh | 69 | 01:14:44 |
| movement | 81 | 01:15:21 |
| lifecycle | 23 | 01:17:10 |
| packed | 24 | 01:17:47 |
| binding | 106 | 01:18:34 |
| binding-faults | 95 | 01:19:15 |
| schematic (native headless, not GUI) | 329 | 01:19:57 |
| seed / independent-JVM reload | 24 / 25 | 01:20:58 / 01:22:01 |
| binding-seed / independent-JVM binding-reload | 29 / 26 | 01:22:41 / 01:23:18 |

Each runtime invocation used `verifyFieldworkAcceptance`, which required matching
phase/pass status, complete dimension saves and the server-state-close marker.
Reports are `run-fieldwork-acceptance/result-<phase>.json`. Both persistence pairs
used the same world within their pair and separate JVMs. The first original
reload launch was interrupted before a new game report; it was resumed with
`-PacceptanceKeepWorld` against the unchanged seed, not counted as a pass itself.
Fault/control exceptions are intentional assertions of quarantine/recovery;
they do not represent an assertion failure or prove arbitrary upstream recovery.

Clean builds and shipping isolation passed. The post-test cleanup removed only
unused art alternatives and their generator paths, not production resources or
Java. A further clean build retained the exact same JAR SHA-256:

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
307aca2dfcacc66c8eb2d1026729dcb48a8220976b1d57e05e64216711afbcdd
```

### Packaged installer smoke

`./scripts/smoke-server.sh setup` installed that JAR into the existing disposable
NeoForge smoke server with the lock-checked pinned stack. Existing `eula=true`
was preserved, not newly set by automation. No acceptance/research driver was
installed. This reused the prepared installer libraries rather than auditing
every library download again.

- Ready: `2026-11-08T01:27:51.479Z`, `Done (0.830s)!`.
- Native console `fieldwork status`: `01:28:53.498Z`, withheld transactions `[]`.
- Native console `stop`: complete `All dimensions are saved` at `01:28:53.719Z`.
- Container exited normally: exit **0**, OOM **false**, empty state error.
- Installed `.smoke/server/mods/create_fieldwork.jar` SHA-256 equals the build
  hash above. Current smoke log: `.smoke/server/logs/latest.log`.

Console commands were sent through Docker's attached stdin, not Docker stop or
SIGTERM. This is packaged dedicated-server startup/status/save evidence, **not a
real client connection, HUD/interaction, multiplayer or full gameplay pass**.
The [schematic client matrix](schematic-client-v02.md#player-matrix--scoped-feedback-and-remaining-checks)
records a later scoped owner Iris/ghost pass and remaining detailed checks.
Final candidate integration documentation/review is separate
from this completed server recheck.

## Implemented in production main

Ordinary isolator retains its registered ID/name and uses3 andesite alloy in its
existing shaped recipe. Smart isolator has its own brass-rim resources, marker BE,
creative/loot/tool data and ordinary1+brass sheets3 shapeless upgrade. Both marker
types participate in private scopes, reconstruction, visuals and transactions.

Typed controller UUID component contains no destination. Smart BEs own stamped
grant generations; overworld SavedData is veto-only and server-owned, including
escaped-reference thread/close guards. Managed queries use loaded current BE
witnesses, READY private state and barriers; no craft scans occur in pair queries.

Native item use-first handles binding (with native mayBuild parity). Native
crouch-air use remains Create's channel editor. Explicit self-craft unbind vetoes
old generations, withdraws old sources, clears the extracted result's identity
while preserving channels/name, and requires release/fresh input before ordinary operation.
Passive loss never clears identity or falls back WORLD.

Main/offhand and lectern sources validate actual identity/program/active user,
session/liveness/movement acknowledgement, same dimension and native WORLD-distance
range. Device/provenance changes use old/new full frequency refresh; stable private
position changes retain receiver filtering. Native first-entry and release/tick
bookkeeping cover Simulated updates and dead-entry re-registration.

Grant and marker candidates share the exact permitted final view. Binding
transactions capture old endpoint networks and loaded private targets before
their first withdrawal. All setup, including pre-frame callbacks, enters named
quarantine on failure; failure publication is guarded and withdrawal retried under
barriers. Native force-loaded constructor-time plots retain initialization barriers
until actual server-level registration; saved veto storage can use its constructing
own overworld context rather than dereferencing an absent server.overworld.

## Initial server gate checks (2026-11-07; separate counts)

| Check | Result |
|---|---|
| Clean build / JUnit |91 tests, zero failures/errors/skips; shipping isolation passed |
| Main binding |106 assertions |
| Binding-specific faults / named repair |95 assertions |
| Main binding seed / independent JVM reload |29 /26 assertions, fresh consecutive runs |
| Original network / refresh / movement |162 /69 /81 assertions |
| Original lifecycle / packed |23 /24 assertions |
| Original independent seed / reload |24 /25 assertions |

Every phase verifier checked its phase/result, complete dimension save and main
server-state close. Last original regression pair finished18:59Z. Fault-phase
exception/quarantine logs are expected controls, not unhandled runtime failures.
Oracle gate1 review accepted the server safety remediation; advisor did not
independently replay runtime. No claim of client sync/menu rendering follows.

New phase commands, using the normal wrapper:

```sh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-faults
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reload -PacceptanceKeepWorld
```

Seed/reload must be consecutive. Scratch reports live in
`run-fieldwork-acceptance/`; keep summaries rather than treating ignored logs as
distributed evidence. Production acceptance excludes the historical bindingmod and
testmod. Acceptance supplies nine fault/counter mixins, no scope/registry answers;
none ship or load in ordinary client/server runs.

## Important fixture distinctions and limitations

- Native Sable moveToUnloaded unloads loading-dependency chains, which can include
  same-frequency ships. The offline-A/B test restores B from its real holding
  record while asserting A remains unloaded, not by creating replacement UUIDs.
- The persistent test first saves a real serialization pointer, then uses native
  snatchAndLoad. Direct record load is not substituted for pointer cleanup. A
  fresh seed/reload pair—not the failed startup/duplicate-record experiments—is
  the evidence above.
- Initial grant setup in the persistence fixture uses production service/storage
  methods; revocation uses real item air-use with all entries actually unloaded.
  Full binding/routing comes from the separate native-interaction binding phase.
- Directional Simulated receivers have valid independent native supports and
  axis-aligned sources; attenuation/support destruction are not isolation bugs.
- Fake transport does not negotiate NeoForge advanced_open_screen. A non-air
  branch test catches only that exact expected native UI-payload refusal. This
  proves the air guard did not consume the native menu branch, **not GUI sync**.
- No universal creative/NBT duplicate detection: ambiguous copy hooks are deferred
  by owner-approved bounded scope; normal form changes retain ID, revoked IDs do
  not revive, and different-instance copied grant stamps deny.
- Smart BE lists are bounded to4096 records; veto history is retained, with no
  unsafe GC or crash-atomic journal claim. Persistent corruption disables managed
  devices rather than inventing empty-ready authority.
- Receiver code that persistently throws can prevent complete physical output
  withdrawal; scope remains quarantined with named repair and explicit diagnostics.
  Successful one-shot fault retries above do not prove arbitrary upstream recovery.
- Client visuals, real inventory/menu/key interactions, multiplayer/explosion and
  performance measurements remain separate acceptance work. No MSPT claim.
