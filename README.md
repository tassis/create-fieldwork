# Create: Fieldwork

A focused Create/Aeronautics addon for **Minecraft 1.21.1 / NeoForge**.

The **Redstone Link Isolator** gives an assembled Sable craft a private Create wireless-redstone network. Links on the same craft can communicate, while matching frequencies in the world or on another craft cannot cross the boundary. Create's frequency, range and power rules, including Sable's projected distances, still apply.

## Status

The current version is **0.1.0**. The isolator, recipe, controller integration, lifecycle handling and scoped refresh are implemented. Main-only headless tests and installer smoke tests have been recorded; client rendering, GUI interaction, multiplayer and the full explosion/lifecycle matrix still need player validation. Use a new or backed-up world for testing.

The current **local worktree candidate** also includes the Smart Redstone Link
Isolator and opt-in offhand schematic assistance. Smart isolators authorize
controllers to operate their private structure from outside. Crouch-use air keeps
Create's channel menu; crafting a bound controller by itself only unbinds it,
preserving channels/name. These changes are not automatically in the existing
Release despite the shared version string. See the [controller player-fix record](docs/testing/controller-player-fixes.md)
and [schematic client record](docs/testing/schematic-client-v02.md).

The latest local worktree build additionally limits stable private-controller position updates to that craft's receivers, without changing range/projection or delaying transitions. Same-frequency other-craft/WORLD receiver calls are excluded on this path; source/session changes still use full withdrawal. This local optimization is not automatically present in the existing tag/Release. See [controller movement evidence](docs/testing/controller-movement-refresh.md) and the [player guide](docs/testing/player-test-guide.md) for its exact hash. Updater counts are not MSPT benchmarks.

Train-schedule Redstone Link conditions observe **public WORLD sources only**, across loaded dimensions and without a range limit. They never read a craft's private network, including when the train is aboard that craft. Ordinary redstone, Redstone Contacts, Flight Control's direct proxy and unrelated wireless systems are outside the isolator's scope.

## Requirements

Install the same stack on the client and server:

| Component | Version |
|---|---|
| Java | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.257 |
| Create | 6.0.10 |
| Sable | 2.0.5 |
| Create Aeronautics bundled | 1.3.2 |

Use the bundled Aeronautics artifact; do not install duplicate copies of its nested mods. Download URLs and dependency checksums are pinned in [`smoke/artifacts.lock`](smoke/artifacts.lock). Other versions have not been verified.

## Using the isolator

1. Place an isolator on a connected structure and assemble it as a Sable/Aeronautics craft.
2. The isolator becomes active. Standard wireless links on that craft remain usable with their original frequencies and range.
3. Removing the **last** isolator restores public network behavior. Multiple isolators provide the same craft-wide isolation.

An isolator in the ordinary world has no isolation effect and emits no redstone power. Its gold inactive top changes to red when active.

In creative mode, find it in the **Create: Fieldwork** tab, identified by the isolator icon. It is also available in the vanilla Redstone tab and creative search.

Craft one in a normal 3×3 crafting grid:

```text
O B O
E L E
B P B
```

**O**: Obsidian · **B**: Andesite Alloy · **E**: Electron Tube · **L**: Redstone Link · **P**: Precision Mechanism. Obtaining a Redstone Link unlocks the recipe. Mine with a diamond-tier or better pickaxe for the drop. The local smart variant is a shapeless upgrade: one ordinary isolator plus three Brass Sheets.

See the [player testing guide](docs/testing/player-test-guide.md) for installation, controller behavior, test scenarios and reporting instructions.

## Building

With Docker Engine, Docker Compose v2+ and a POSIX shell, run as a non-root user:

```sh
./scripts/gradle-docker.sh build
```

The container supplies Java 21 and Gradle 9.2.1 and uses your host UID/GID. Output:

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
```

For a clean build and checks:

```sh
./scripts/gradle-docker.sh clean build
```

There is no Gradle Wrapper. Desktop development with Java 21 and Gradle 9.2.1 can use `gradle --no-daemon build` and `gradle runClient`.

## Development and verification

- [Developer guide](AGENTS.md): Docker, caches, development runs, installer smoke server and engineering rules.
- [Documentation index](docs/README.md): design, verification and historical research.
- [Behavior contract](docs/design/behavior-contract.md), [V1 technical design](docs/design/redstone-link-isolator-v1.md), and [architecture policy](docs/design/project-architecture-policy.md).
- [Main-only acceptance evidence](docs/testing/redstone-link-isolator-acceptance.md): tested behavior and remaining checks.
- [CI and tag releases](docs/testing/github-actions.md): build artifacts and automated GitHub Releases.

Current phase counts, artifact hashes and installer evidence are maintained in the
[production acceptance record](docs/testing/redstone-link-isolator-acceptance.md).

`verifyScopeProbe` runs historical prototypes **without production main**. Use `verifyFieldworkAcceptance` to verify the actual implementation.

## License

Copyright (C) 2026 Tassis.

Create: Fieldwork is licensed under the **GNU General Public License v3.0 only** (`GPL-3.0-only`). You may redistribute and modify it under those terms. It is provided without warranty; see [LICENSE](LICENSE) for the full text.
