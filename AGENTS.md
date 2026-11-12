# Create: Fieldwork — Developer Guide

## Project status and authority

- Mod ID: `create_fieldwork`; Java package: `dev.tassis.createfieldwork`.
- [`docs/design/behavior-contract.md`](docs/design/behavior-contract.md) is the behavior contract. Current technical design: [`docs/design/redstone-link-isolator-v1.md`](docs/design/redstone-link-isolator-v1.md).
- Production main implements the isolator/BE, server-owned registry/holder, narrow Create/Simulated hooks, lifecycle barriers, per-receiver refresh, session-aware event-driven controllers and the fixed-WORLD train adapter. Strict mixins are active.
- **D01:** upstream block/data-loss recovery and an exceptional journal are optional/deferred. Fieldwork's registry, initialization and refresh correctness remain required.
- **D02:** every train condition observes public WORLD sources only. Private train consumers and Train ownership research are deferred. Source visibility, high/low, PENDING and cache/session behavior remain required.
- **D03:** ordinary shaped 3×3 crafting, `OBO/ELE/BPB`, output one. Implemented and headlessly tested; client crafting/unlock interaction remains part of player acceptance.
- Version `0.1.0` is available for player testing. Client/GUI/multiplayer/explosion acceptance is incomplete. Use the [production acceptance record](docs/testing/redstone-link-isolator-acceptance.md), not historical prototype pass counts.
- Normal container removal retains the old footprint/barrier and publishes only
  after active UUID absence. Detached serialized markers are not validated through
  live plot block state; this does not expand deferred D01 data recovery.
- The project uses GNU GPL v3.0 only (`GPL-3.0-only`); see [LICENSE](LICENSE). Keep main and test-driver metadata consistent and include the license text in the packaged JAR.

## Pinned toolchain

| Component | Version |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.257 |
| Java / Gradle / ModDev | 21 / 9.2.1 / 2.0.141 |
| Create / Sable / Aeronautics bundled | 6.0.10 / 2.0.5 / 1.3.2 |

Metadata requires exact Create and makes exact Sable/Aeronautics/Simulated one
optional integration bundle. Gradle compares the three audited mod JARs with
`smoke/artifacts.lock` on each verification/extraction.
`gradle/verification-metadata.xml` pins other build inputs; it is not an
independent audit of every third-party source/supply chain. Do not install
duplicate nested mods from Aeronautics bundled.

## Docker builds (default)

The host needs Docker Engine, Docker Compose v2+ and a POSIX shell, not Java/Gradle. Run from the root as a normal user; first-time dependency downloads require network access.

```sh
# Defaults to build
./scripts/gradle-docker.sh
# Recompile and run checks, including JUnit
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh check
./scripts/gradle-docker.sh --version
./scripts/gradle-docker.sh tasks --all
```

Output: `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar`; `gradle.properties` defines the version. Absolute-path invocation works from other directories. Arguments pass to Gradle. Each invocation uses a disposable `--rm` container; no `docker compose up` is needed.

### UID/GID and caches

- The wrapper derives `LOCAL_UID`/`LOCAL_GID` from `id -u`/`id -g`, including non-1000 accounts. Do not use `sudo`: UID 0 is rejected. Configure host Docker access first.
- The checkout mounts at `/workspace`; output, `.gradle/`, run directories and caches remain host-user-owned.
- `HOME=/workspace/.docker/home`; `GRADLE_USER_HOME=/workspace/.docker/gradle`. The host creates both before startup to avoid root-owned initialization.
- `.docker/` and `.gradle/` are ignored. `clean` removes output, not download caches. Stop project containers before removing the project's `.docker/gradle/` to reset it.
- Direct Compose calls require UID/GID. After running the wrapper once:

```sh
LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)" \
  docker compose run --rm --entrypoint sh build
```

Check `id`, `java -version`, `gradle --version` inside. A numeric UID need not have an `/etc/passwd` entry.

## Development runs

```sh
./scripts/gradle-docker.sh runServer
```

This is ModDev, not an installer/EULA-gate test. The observed development launch did not require `eula.txt`. Use console `stop` to exit. The build service publishes no ports; use smoke for connections and packaged-JAR checks.

`runClient` uses `run-client/`. Default Docker has no display/GPU forwarding. On a desktop with Java21/Gradle9.2.1 use `gradle runClient` or import the project in an IDE. There is no Gradle Wrapper; a host build uses `gradle --no-daemon build`.

## Packaged-JAR smoke server

Smoke uses the real NeoForge installer, Java21 JRE and `.smoke/server/`, separate from `run-server/`. Images are digest-pinned; installer/mod downloads are SHA-256-checked against `smoke/artifacts.lock`. Setup supplies download tools.

```sh
# Clean build, verify/download dependencies, install NeoForge, copy current JAR
./scripts/smoke-server.sh setup
# Read https://aka.ms/MinecraftEULA and, only if you agree, manually set:
# .smoke/server/eula.txt -> eula=true
./scripts/smoke-server.sh start
./scripts/smoke-server.sh logs
./scripts/smoke-server.sh status
# Attach, then enter stop to save and shut down
./scripts/smoke-server.sh console
```

- EULA agreement is never automatic. `start` refuses without it. A successful start command is not readiness: check `Done (...)!`, errors and behavior.
- TCP **and UDP** 25565 bind to `127.0.0.1` only; Sable uses UDP. Resolve conflicts first. Clients need the same pinned stack.
- Defaults: online mode, creative, peaceful, flat world; 3 GiB heap, 5 GiB container memory, 4 CPUs, 1024 PIDs, 60-second stop grace period; non-root UID/GID, dropped capabilities, `no-new-privileges`.
- Detach with `Ctrl-P`, `Ctrl-Q`, not `Ctrl-C`.
- For important worlds use **console `stop`** and confirm `All dimensions are saved`. The observed `smoke-server.sh stop` SIGTERM path exited 143 without complete-save evidence. Container termination does not prove saving.
- Setup refuses while running. After source changes shut down normally, then `setup`/`start` to install the new JAR.
- Setup preserves worlds, config, EULA, `server.properties`, `fieldwork_jvm_args.txt`; templates copy only when absent. Extra/old JARs cause refusal and must be moved manually, not silently deleted.
- `.smoke/downloads/` caches verified inputs. Installer libraries are not fully reverified each setup. For suspected corruption, stop, back up the world and use a fresh `.smoke/server/`.
- Mixin exports: `.smoke/server/.mixin.out/class/`; filters: `fieldwork_jvm_args.txt`. Exports prove transformation, not behavior.
- `.smoke/` and `.audit/` are ignored. Preserve conclusions/reproduction paths in `docs/`, not only local logs.

Create-only packaged smoke uses an independent server directory and TCP port:

```sh
./scripts/base-smoke-server.sh setup
# Read the EULA and, only if you agree, set .smoke/base-server/eula.txt to eula=true
./scripts/base-smoke-server.sh start
./scripts/base-smoke-server.sh logs
./scripts/base-smoke-server.sh console
```

## Verification layers

### Production main-only acceptance

```sh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=refresh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=movement
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=lifecycle
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=packed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reset
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-faults
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-index
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-work
# Consecutive independent JVMs; do not reset the world between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=reload -PacceptanceKeepWorld
# Consecutive independent JVMs; do not reset the world between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reload -PacceptanceKeepWorld
```

`src/tools/acceptance/` is the production acceptance tool source set. It supplies
fixtures and fault/work-counter mixins, but no scope/registry answers, and is absent
from shipping JARs and ordinary runs. Scratch: `run-fieldwork-acceptance/`. The
verifier checks phase/results, complete saves and state close.

Current phase counts, artifact hashes and installer evidence live only in the
[production acceptance record](docs/testing/redstone-link-isolator-acceptance.md).
Performance boundaries are documented in [scoped refresh](docs/testing/scoped-refresh.md)
and [controller movement](docs/testing/controller-movement-refresh.md); do not claim
measured speedups or remove Simulated bookkeeping/source iteration.

Edit game PNGs directly in
`src/main/resources/assets/create_fieldwork/textures/block/redstone_link_isolator/`.
See [texture integration history](docs/testing/placeholder-textures.md).

### Historical research probes

### Non-shipping v0.2 identity experiment

`src/tools/binding-probe/` is a separate **experimental binding probe tool**, not
production acceptance:
`verifyBindingProbe -PbindingPhase=seed`, then `verifyBindingProbe -PbindingPhase=reload -PbindingKeepWorld`.
It uses `run-binding-probe/` and real persisted Sable force-load tickets. It
proves device-ID/lectern persistence and a fixture-driven UUID-stamped block
authorization model, **not** remote private-network routing or automatic source
withdrawal. Normal runs/main acceptance exclude it; `check` verifies shipping
archive isolation. See [binding experiment](docs/testing/controller-binding-spike.md).

### Historical scope probes

```sh
./scripts/gradle-docker.sh verifyScopeProbe
```

`src/testmod/` runs only in `scopeProbeServer`, **without production main**. The verifier requires `mainImplementationLoaded=false`. Main acceptance excludes research in turn; the shipping JAR excludes both drivers.

The probe writes `run-scope-probe/scope-probe-result.json` and phase reports, then shuts down. Use `verifyScopeProbe`, not just `runScopeProbeServer`: assertions and complete saves must be checked. Default runs reset `run-scope-probe/probe-world/`, never smoke. Reload requires `-PprobeKeepWorld`; keep seed/reload consecutive.

Historical modes: suite71, seed5/reload9, multi200/207, controller40, edges19, motion37, recovery19. These are prototype evidence:

- Controller40 includes an unsafe null→WORLD observation. Motion37 used the real vanilla listener to test accepted/rejected movement and PENDING; do not ship the old fallback.
- Sable can swallow marker-write failures. Normal return plus marker scans is insufficient; transaction barriers/ledgers must remain independent.
- Failure seed5/reload7 are **expected-unsafe controls**: complete marker loss removes memory quarantine across JVMs and external output becomes15. D01 defers stronger recovery; normal registry is not persisted.
- Train95 includes unsafe cold-read/plot-reuse controls; `train-player` has no accepted run. D02 defers private train consumers; current fixed-WORLD semantics have separate main tests.

See [probe history](docs/testing/scope-hook-probe.md) and the [research index](docs/README.md#historical-research).

## Source layout

| Path | Purpose |
|---|---|
| `build.gradle`, `settings.gradle`, `gradle.properties` | Toolchain, dependencies, runs, version |
| `src/main/java/` | Production Java |
| `src/main/resources/` | Mixins, assets, localization, data |
| `src/main/templates/META-INF/neoforge.mods.toml` | Metadata source |
| `src/test/` | JUnit |
| `src/tools/acceptance/` | Production-main acceptance tooling and fault/counter fixtures |
| `src/tools/binding-probe/` | Historical experimental binding persistence probe |
| `src/testmod/` | Historical scope research driver without production main |
| `build/generated/sources/modMetadata/` | Generated metadata; do not edit |
| `Dockerfile`, `compose.yaml`, `scripts/` | Containers, smoke, texture tools |

## Architecture and documentation policy

- All agents and contributors **must follow** the
  [project architecture policy](docs/design/project-architecture-policy.md) and
  [code organization policy](docs/design/code-organization-policy.md).
- Treat the behavior contract, V1 design, and architecture policies under
  `docs/design/` as durable authority. Ignored workflow artifacts, scratch logs,
  build outputs, and local run directories must not replace or override them.
- Keep durable design, architecture, decision, and engineering-policy documents
  under `docs/` and write their prose in English. Localization resources may
  contain their target language, but durable documentation should describe those
  values in English rather than embedding untranslated discussion.
- Update `docs/README.md` whenever a canonical design or policy document is added,
  moved, superseded, or removed. Production documentation must not link to
  `.ramblings/`, `.slim/`, `.audit/`, or other ignored local artifacts as authority.
- Architecture exceptions must be explicit in durable documentation, narrowly
  scoped, and supported by verification evidence.

CI uses native Java21/Gradle9.2.1 runners. Branch/PR/manual CI runs checks/build
without uploading artifacts or publishing. Releases are started only through the
manual Release workflow on `main`; its required `release_version` input overrides
the project default via `-Pmod_version`. After a successful clean build, Release
creates the matching `v<release_version>` tag and GitHub Release from the same
commit and artifact, without rewriting the default or creating a version commit.
Hyphenated versions are prereleases. See [CI and release](docs/testing/github-actions.md).

## Change and verification rules

- Keep strict mixins: `required=true`, `defaultRequire=1`; audit actual injections against exact artifacts.
- Pair queries use constant-time scope/registry lookup. Never scan craft blocks in queries or intercept broad methods such as `Level#getSignal`.
- State belongs to one logical server, never a cross-server static map. Initialization fails closed; unknown is not READY(empty).
- Preserve Sable projection and upstream frequency/range/power. Do not add controller gateway exceptions or trust client-reported craft IDs.
- Code changes require `./scripts/gradle-docker.sh build`; version/build/resource-generation changes require `clean build`. Add behavior-appropriate runtime checks. Compilation alone does not establish isolation or lifecycle correctness.
- Do not fix source problems by editing generated files in `build/`, `.gradle/`, `.docker/` or run directories.
- Documentation-only changes need link/content checks; do not present historical runtime results as newly executed tests.
