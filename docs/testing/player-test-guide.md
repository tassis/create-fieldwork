# Player Testing Guide

Current version: **0.1.0**. Client/GUI, multiplayer, real explosions and the complete lifecycle matrix still need validation. Prefer a new or backed-up world. The project is licensed under GNU GPL v3.0 only (`GPL-3.0-only`); see [LICENSE](../../LICENSE).

## Obtain and identify the build

- Local build: `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar`.
- Mod ID: `create_fieldwork`.
- Block/item ID: `create_fieldwork:redstone_link_isolator`.
- CI uploads the JAR as an artifact; a successful matching tag workflow attaches it to the GitHub Release. See [CI/release documentation](github-actions.md).

Record the hash of the exact JAR you install. **Fully close the game/server before replacing a mod JAR**; leaving a world does not reload mod code.

### Current local controller/schematic candidate

Use the current hash below; the controller player-fix record preserves an older
historical candidate. Client and server must both use this same candidate.

Current SHA-256: `c69b8bb59e098f96027a8d3a8050d7be77cbaa9812d2d88183e382733340886c`.

This exact build also passed packaged dedicated-server startup/status/save with
exit0. That does not replace the client interaction checks below.

- Main-hand controller, crouch-use the smart isolator to bind.
- Crouch-use air opens the native channel menu and keeps the binding.
- Craft one bound controller alone in2x2 or3x3 to unbind it; channels and custom
  name stay unchanged. An ordinary unbound controller has no unbind recipe.
- Right-click activation and native lectern use take priority over an active
  offhand blueprint. Test with the blueprint present and absent.
- Verify outside control on ground and while flying/leaving the structure, then
  put the controller in a lectern on that structure and check its real output.
- Fresh input is required after binding/unbinding. Passive unavailability keeps
  the device managed/blocked, never silently turns it into a public controller.

Those actual client interactions remain player-retest items; headless native
menu/packet/receiver tests do not establish every GUI/input path. Offhand visual
assistance and its limits are documented [separately](schematic-client-v02.md).

### Historical local controller optimization

An earlier private-controller movement optimization used local0.1.0 SHA-256
`a7769b40ca8a7f1bee15a6c21929f724e1748eb009e614ab34d312c6408e0000`.
It limited stable private position updates to that craft's receivers while
preserving range and full transition handling. It is historical, not the current
JAR above; see [movement refresh evidence](controller-movement-refresh.md).

This is a **local worktree build after version preparation**, not proof that the existing `v0.1.0` tag or GitHub Release includes these uncommitted changes. No tag/Release was replaced. Use the hash to identify the installed build, not the shared version string alone.

### Historical candidates

These hashes belong to specific **0.1.0-dev** candidates, not the versioned `0.1.0` build:

| Candidate | SHA-256 |
|---|---|
| Initial installer candidate | `aa67eebec9c0f5c3ab1819cf9540250650314a2a8e60021608ef254eb8824077` |
| Scoped-refresh logic revision | `765e89e884d8118369825b084ae4e3acfd6048266e39b4ab86e0947dca012266` |
| Subsequent six-face texture revision | `c6536ce872e8acf97b1253f6304c09c7a1fa7b305ef1afb9912077477381ab4a` |

The texture revision retained identical bytes for all 58 Java classes from the scoped-refresh revision; its build/installed hashes matched. See [texture evidence](placeholder-textures.md) and [logic regression](scoped-refresh.md). Renaming/versioning or rebuilding can change the artifact hash, so do not identify a build by filename alone.

## Installation

Install the same pinned versions on both sides:

| Component | Version/file |
|---|---|
| Java | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.257 |
| Create | 6.0.10, `create-1.21.1-6.0.10.jar` |
| Sable | 2.0.5, `sable-neoforge-1.21.1-2.0.5.jar` |
| Aeronautics bundled | 1.3.2, `create-aeronautics-bundled-1.21.1-1.3.2.jar` |
| Fieldwork | 0.1.0, the selected build above |

External download URLs/hashes are in [`smoke/artifacts.lock`](../../smoke/artifacts.lock). Use Aeronautics **bundled** without installing duplicate nested mods such as Simulated. Metadata intentionally pins dependencies; other versions are unverified.

Place Fieldwork in the instance's `mods/` directory and move out the old Fieldwork JAR to avoid duplicate mod IDs. Do not install `fieldwork_probe` or `fieldwork_acceptance`; these development drivers are not part of the shipping JAR.

### Repository smoke server

For a fresh installation, use the [developer guide](../../AGENTS.md#packaged-jar-smoke-server). Setup rebuilds/installs the current JAR; it does not accept the EULA for you.

```sh
./scripts/smoke-server.sh setup
# Read https://aka.ms/MinecraftEULA; if you agree, set eula=true manually
# in .smoke/server/eula.txt
./scripts/smoke-server.sh start
./scripts/smoke-server.sh logs
# Wait for Done (...)! and check errors before connecting
```

On the same host, connect to `127.0.0.1:25565`. Both TCP and UDP bind only to localhost; another computer cannot connect through its own localhost. Remote access needs separate network configuration, including Sable's UDP traffic.

To save and stop:

```sh
./scripts/smoke-server.sh console
# Enter stop, then confirm All dimensions are saved
```

`smoke-server.sh stop` is a Docker termination command; its observed SIGTERM path did not establish complete saving. For important worlds use console `stop`. After source changes, stop normally, then `setup`/`start` and record the new hash. Historical installer success does not establish which JAR is currently installed in a reused local directory.

## Suggested first test pass

### 1. Item and recipe

- Find **Redstone Link Isolator** in the dedicated **Create: Fieldwork** creative tab (isolator icon), the vanilla Redstone tab or creative search. You can also use `/give @s create_fieldwork:redstone_link_isolator`.
- Check block/item models, English and Traditional Chinese names, and both tooltip sections. Original placeholder art has black/purple sides, a gold inactive top and a red ACTIVE top.
- The game reads the seven PNGs under `src/main/resources/assets/create_fieldwork/textures/block/redstone_link_isolator/`; ACTIVE changes only `up_active.png`.
- Craft one with the ordinary shaped 3×3 recipe:

```text
O B O
E L E
B P B
```

O = Obsidian, B = Andesite Alloy, E = Electron Tube, L = Redstone Link, P = Precision Mechanism. Obtaining a Link unlocks the recipe. In survival, use a diamond-tier or better pickaxe for the drop. Upgrade one ordinary isolator plus three Brass Sheets shapelessly to obtain one Smart Redstone Link Isolator.

### 2. Minimal isolation scene

1. Establish normal same-frequency Create transmitter/receiver behavior in the world; input15 should be received.
2. Put a transmitter, receiver and isolator on one continuously supported structure. Assemble through Sable/Aeronautics. An ordinary moving Create contraption is not necessarily a Sable craft. Disconnected parts can legitimately split into separate SubLevels.
3. The isolator should become ACTIVE. Same-craft reception remains15; WORLD/other-craft receivers must not receive that source. WORLD transmitters must not control the private craft either.
4. Test different frequencies and range boundaries: same craft does not mean unlimited range. Add a legal weaker public source and confirm forbidden private15 does not override it.
5. Add a second isolator. Removing one keeps privacy; removing the last restores public behavior. Replacing it should immediately retract the old cross-boundary contribution.

ACTIVE is a visual state, not a power source; the isolator emits no ordinary redstone.

### 3. Controllers and trains

- Use actual Linked Controller keys aboard a craft. Confirm same-craft control and no WORLD/other-craft leakage. Hold input through boarding/departure/rotation; test release and the approximately 30-tick upstream timeout.
- Test a Lectern Controller separately: its scope follows the source block's craft, not its WORLD operator. Range still follows upstream player-world coordinates.
- Test teleport, disconnect/rejoin/new session and dimension changes. Uncertain private transitions may pause control; they must not become public.
- **Every train condition observes public WORLD signals only**, with upstream global cross-dimension/no-range behavior. A private source must not complete High; PRIVATE-only permits Low. Potentially visible sources still loading/unregistered may defer both polarities. Trains aboard craft do not read private craft networks in V1.

### 4. Lifecycle

Test disassembly/reassembly/split, final-marker break/replacement/explosion, leaving and returning to loaded range, then console-save/restart. Observe both inside/outside receivers for stale power and ACTIVE for consistency with actual craft state.

## Known boundaries and reports

- Headless success does not replace client rendering, GUI/codec, multiplayer, real-explosion or performance testing.
- A long-lived unregistered WORLD transmitter with unknown frequency can conservatively defer unrelated Low conditions until registration/removal/unload. This is a liveness limitation, not private-signal leakage.
- Strong recovery after upstream block/data loss and a durable recovery journal are deferred. Receiver/circuit side effects are not rollbackable transactions.
- Failed operations retain named barriers and log `withheld transaction`. Permission-level2 operators can use `/fieldwork status` and `/fieldwork repair <token>`. Repair validates marker intent/baselines and saves before release; it cannot force-clear an invalid barrier or repair arbitrary data corruption.
- Visual synchronization failures retry without reversing committed privacy.

Include the exact JAR hash, full mod versions, `latest.log`, scene coordinates/assembly method, frequencies, input/output strengths, operation sequence and any SubLevel UUID/transaction token. Back up an anomalous world before experimenting; do not hide a failure by deleting registry state or repeatedly overwriting the world.

See [production acceptance](redstone-link-isolator-acceptance.md) for automated coverage and outstanding checks.
