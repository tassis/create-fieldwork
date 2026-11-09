# Controller player-fix evidence

Status: historical automatic/server evidence from 2026-11-08; superseded by the
2026-11-09 integrated candidate in the [production acceptance record](redstone-link-isolator-acceptance.md).
Latest real-client interaction retest is pending. Neither candidate is a release
or version bump.

## Corrected behavior

- A managed controller targets its authorized physical structure from an accepted
  server position, including accepted airborne departure. The operator need not
  currently be a confirmed member of another structure. Rejected or unacknowledged
  movement remains denied; range, session, liveness and native frequencies remain
  Create-authored.
- A native lectern on a physical structure preserves the controller's channels,
  device ID and pending-denial marker through placement, client tag, save/load,
  safe export, drop and take-back. Assignment failure rolls all three back.
- Once native lectern assignment succeeds, a later Fieldwork refresh failure does
  not undo or interrupt that assignment call. The coherent native state remains
  installed and its exact refresh scope is retried; injected runtime/error cases
  both withdraw the stale private output on retry.
- Crouch-use in air remains Create's native channel UI and does not unbind.
- Crafting exactly one bound controller alone in the player 2x2 or table 3x3 grid
  unbinds it. The extracted result keeps channels, custom name and unrelated
  components. An ordinary controller has no no-op recipe.
- Reset preview is denial-only and pure. Actual result-slot extraction revokes the
  old generation and withdraws endpoints before identity clears. Failure leaves
  one pending result and one named repair token; it cannot become ordinary power.
- Active offhand schematic assistance yields handheld controller and native
  lectern right-clicks to vanilla. It does not synthesize a second use packet.

## Fresh automatic evidence

| Check | Result (UTC) |
|---|---|
| Build / JUnit / shipping isolation | 101 tests, zero failures; rerun at 07:49 |
| binding | 152 assertions, 07:49 |
| binding-reset | 57 assertions, 06:59:03 |
| binding-faults | 132 assertions, 06:59:45 |
| binding-seed / independent-JVM reload | 30 / 27, 07:00:26 / 07:01:04 |
| ordinary network / refresh / movement | 162 / 71 / 81 |
| lifecycle / packed / schematic | 23 / 24 / 347 |
| ordinary seed / independent-JVM reload | 24 / 25 |

Every runtime verifier required the expected phase/status, complete dimension
saves and server-state close. Fault-phase exceptions are intentional injections.
The persistence pair includes pending lectern marker/device/channel survival and
stale grant denial. Selected texture and model-reference checks also passed.

Historical artifact:

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
SHA-256 ca4b4c58e406e2f6165af60dcd96fea674e75e0fb113e6d568bd8103cd27355f
```

This hash identified both the clean automatic-test JAR and the installed packaged
smoke JAR. The server reached `Done` at08:00:27Z, status returned no withheld
transactions, console `stop` completed all dimension saves at08:03:11Z and the
container exited0. Run the player checks below against the current hash in the
[player guide](player-test-guide.md), not this historical artifact.

## Required player retest

1. Bind from the main hand, then operate the authorized structure while standing
   outside it and while flying/leaving it.
2. Place the bound controller through native survival lectern interaction on the
   physical structure and verify its real receiver output.
3. Crouch-use air and confirm Create's channel UI opens without losing binding.
4. Craft the bound controller alone using pickup and shift-click paths; confirm
   it becomes unbound while channels and custom name remain.
5. With an active offhand blueprint, repeat handheld activation, channel UI and
   lectern use; none may edit or consume the blueprint action.

Headless packet/menu branches do not prove GUI rendering, mouse dispatch, remote
multiplayer or every shader/mod interaction.
