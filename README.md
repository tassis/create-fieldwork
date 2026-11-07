# Create: Fieldwork

A focused Create/Aeronautics addon for **Minecraft 1.21.1 / NeoForge**.

The **Redstone Link Isolator** gives an assembled Sable craft a private Create wireless-redstone network. Links on the same craft can communicate, while matching frequencies in the world or on another craft cannot cross the boundary. Create's frequency, range and power rules, including Sable's projected distances, still apply.

## Status

The current version is **0.1.0**. The isolator, recipe, controller integration, lifecycle handling and scoped refresh are implemented. Main-only headless tests and installer smoke tests have been recorded; client rendering, GUI interaction, multiplayer and the full explosion/lifecycle matrix still need player validation. Use a new or backed-up world for testing.

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

Craft one in a normal 3×3 crafting grid:

```text
O B O
E L E
B P B
```

**O**: Obsidian · **B**: Brass Sheet · **E**: Electron Tube · **L**: Redstone Link · **P**: Precision Mechanism. Obtaining a Redstone Link unlocks the recipe. Mine with a diamond-tier or better pickaxe for the drop.

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
python3 scripts/export-isolator-textures.py --check-installed
```

There is no Gradle Wrapper. Desktop development with Java 21 and Gradle 9.2.1 can use `gradle --no-daemon build` and `gradle runClient`.

## Development and verification

- [Developer guide](AGENTS.md): Docker, caches, development runs, installer smoke server and engineering rules.
- [Documentation index](docs/README.md): design, verification and historical research.
- [Behavior contract](plan.md) and [V1 technical design](docs/design/redstone-link-isolator-v1.md).
- [Main-only acceptance evidence](docs/testing/redstone-link-isolator-acceptance.md): tested behavior and remaining checks.
- [Texture editing](art/redstone_link_isolator/README.md): seven editable PNGs and the active-top model.
- [CI and tag releases](docs/testing/github-actions.md): build artifacts and automated GitHub Releases.

The scoped-refresh revision recorded 85 JUnit tests, network 148, refresh 69, lifecycle 23, packed 24 and independent-JVM seed/reload 24/25 assertions. Each phase has separate save/close checks; these counts are not a completed release matrix. The subsequent texture-only candidate repeated build/JUnit/network and installer checks. The `0.1.0` preparation also passed clean build, 85 JUnit tests and texture/model validation. Earlier `0.1.0-dev` hashes identify historical candidates, not the versioned `0.1.0` artifact.

`verifyScopeProbe` runs historical prototypes **without production main**. Use `verifyFieldworkAcceptance` to verify the actual implementation.

## License

Metadata currently declares **All Rights Reserved**. The owner has not selected a final license.
