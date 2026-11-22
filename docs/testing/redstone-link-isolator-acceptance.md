# Redstone Link Isolator — Production Acceptance Record

## Status and evidence boundary

Recorded through 2026-11-22. Production main implements server-owned state/registry, the actual isolator/BE and recipe, Create/Simulated filters, lifecycle/mutation barriers, per-receiver scoped refresh, two-tier controller routing with event-driven reconciliation, fixed-WORLD train conditions, source-publication cache handling, the development scribing-table slice and client Ponder teaching below.

### Direct Sable allocation compatibility — 2026-11-22

Fieldwork now gives a Sable SubLevel allocated outside the normal assembly/load
wrappers its own short transaction before the allocation returns to its caller.
This covers third-party plate-only craft allocation without targeting or depending
on the third-party mod. If allocation throws before returning, Fieldwork uses
Sable's normal removal path and releases a standalone barrier only after the UUID,
plot slot and occupancy bit are all absent without a cleanup exception. Nested
load/assembly failure and any cleanup callback failure remain quarantined; the
original exception is rethrown and a cleanup exception is attached as suppressed.

The production-main lifecycle phase passed **33 assertions** at
`2026-11-22T14:23:08Z`. It covers successful direct rollback, plot reuse, direct
allocation as READY/WORLD, later isolator insertion as PRIVATE, last-isolator
removal as WORLD, nested-load partial cleanup with retained outer quarantine and
post-removal fault fallback with no stranded Sable craft. The verifier confirmed
complete saves and server-state close. `./scripts/gradle-docker.sh build` then
passed in 24s, including JUnit and shipping-isolation checks; the acceptance-only
post-removal fault mixin is absent from the shipping artifact. This is headless
Sable lifecycle evidence, not a player test of Absolute Kinematics controls or
rendering.

The owner subsequently reported that the current candidate completed the intended
single-player path without crashing. The supplied client log records Absolute
Kinematics 1.0.1, Create 6.0.10, Sable 2.0.6, Aeronautics/Simulated 1.3.3,
NeoForge 21.1.257 and Fieldwork 0.1.0. While the owner held right-click to assemble
and disassemble continuously, Sable logged `Received a sub-level movement packet
for a non-existent sub-level` 15 times between 23:11:28 and 23:18:34. The final
seven form a 400 ms cadence during the rapid toggle. The log contains no
`Can't keep up`, Java exception, Rapier panic, unknown tracking-removal packet or
Fieldwork lifecycle error. The integrated server then completed all-dimension saves,
stopped normally and started again; no further instance appears through 23:22:13.

Sable 2.0.5 and 2.0.6 emit this exact line when a movement snapshot names a plot
not present in the client container; that handler logs and skips the entry. Sable
sends tracking creation/removal over TCP but uses UDP for movement snapshots when
connected. Rapid toggling can therefore let a movement snapshot arrive before its
TCP creation or after its TCP removal. Fieldwork does not handle these packets. The
observed bounded bursts and clean restart support a transient Sable packet-ordering
race rather than persisted Fieldwork or Sable state damage. Repetition after input
stops, or an unresponsive replacement craft, would instead match broader open
upstream Sable desynchronization reports
([#589](https://github.com/ryanhcode/sable/issues/589),
[#1582](https://github.com/ryanhcode/sable/issues/1582)) and remains a stop
condition. Neither report currently carries a maintainer diagnosis or resolution.
The exact installed Fieldwork artifact hash and operation count were not captured.
This does not yet establish multiplayer, dedicated-server or broader Sable-version
compatibility.

### Chunk lifecycle callback ordering — 2026-11-22

The lifecycle-hook follow-up found that off-thread chunk load/unload callbacks
could queue older index intent while a newer server-thread callback executed
immediately. Fieldwork now assigns generations in its server-owned state for each
dimension/chunk and lets only the latest callback mutate source-publication,
Simulated-receiver and smart-binding indexes. Closing the server state clears all
unclaimed generations.

`./scripts/gradle-docker.sh build` passed in 24 seconds with **151 JUnit tests**,
including three new ordering checks, and shipping isolation. The production-main
`lifecycle` phase then passed **33 assertions** at
`2026-11-22T14:54:34.970049865Z`; `binding` passed **177 assertions** at
`2026-11-22T14:55:11.625783025Z`. Both completed all-dimension saves and verified
server-state close. These server phases cover ordinary unload/reload, publication,
receiver and binding behavior. The delayed-callback ordering itself is deterministic
JUnit evidence; this run did not naturally induce an off-thread NeoForge chunk
callback race.

### Integration verification — 2026-11-21

The owner accepted the current candidate and authorized committing and merging
the 0.3.0 development work into `main`. A final Docker
`clean build -Pmod_version=0.3.0-dev` passed in 12 seconds, including 148 JUnit
tests (zero failures, errors or skips) and shipping isolation/license checks.
Gradle reused cached compilation and test results. Deterministic generation
checks passed for all three Ponder structures, and the smoke wrapper passed
shell syntax checking. The resulting JAR SHA-256 remains
`c917545d1a90c1f3e99ee41985c3ebd993ebc82dc8c54283eb2c067fa76a1ab3`.
The client/runtime evidence below remains from its recorded execution dates;
broader player acceptance and fresh packaged-JAR smoke remain pending.

## Two-airship teaching and Print-only table — 2026-11-14

### Centered balloon supports — 2026-11-15, 05:54–05:56Z

Both ships' fore/aft fence pillars now lie on each balloon's width centerline,
at x3/x11 and symmetric z4/z8 anchors. Each pillar runs continuously from the deck
to the balloon. The previous side columns are empty. Receiver links moved to the
west sides of their existing lamps, at `(2,3,7)` and `(10,3,7)`, preserving the same
iron-ingot/redstone pair and leaving the rear center pillars clear.

Fresh Docker `clean build -Pmod_version=0.3.0-dev` passed in33s with JUnit148 and
shipping checks. The actual Traditional Chinese client passed three full
scene/replays, exact compiled chapter order, entrance/rotor probes,21 captures and
the real Print-only table UI. Added geometry assertions check all centered fence
cells, removed columns, deck/balloon contact, supported receivers and frequencies.
Render review covered both ships in common and binding chapters.

Current candidate SHA-256:
`c917545d1a90c1f3e99ee41985c3ebd993ebc82dc8c54283eb2c067fa76a1ab3`.
This supersedes the earlier placement candidate. The change is teaching geometry;
no new gameplay server phase or packaged/player pass is claimed here.

### Ship-side lectern placement — 08:20–08:25Z

At the owner's correction, the lectern is now at `(1,3,5)`, directly beside the
isolator `(2,3,5)` along the ship side. Both previous lectern cells are empty.
The hint anchor is offset by `(-4,1,0)` and connected to the lectern by a white
leader line for70 ticks. The active-hint render shows the lectern beside the
isolator, with the hint box outside the deck rather than over the model.

The resource revision passed a fresh clean build; the final hint adjustment
passed Docker build in26s with JUnit148 and shipping checks. The actual Traditional
Chinese client passed all three scene/replays, compiled item order, adjacency and
clearance assertions, entrance/rotor probes,21 captures and Print-only table UI.

Current candidate SHA-256:
`1a561739938f02cc341d18beabffab93a587404d5a6a9d17d029cfe221a47c02`.
This supersedes the earlier bow-corner candidate below. Render review is scoped
to the captured view; broader player/packaged gates remain as recorded.

### Clear lectern and onward smart chapter — 08:08–08:10Z

The lectern moved from `(5,3,7)` beneath the balloon to the open bow corner
`(1,3,3)`, facing north. Its interaction hint is raised one block above the prior
anchor. The driver checks four blocks of overhead clearance and an empty old
position; the actual lecture capture shows the tabletop and controller clearly.

Ordinary item chapter order is now basics, lectern, then the existing smart-binding
chapter. Smart item order remains basics, binding. Both routes reuse the same
binding storyboard. Exact compiled order is checked independently of the unordered
association sets printed in the report. Binding still depicts the smart block.

Fresh Docker `clean build -Pmod_version=0.3.0-dev` passed in31s with JUnit148 and
shipping checks. The actual Traditional Chinese client passed three full
playback/replays, entrance/rotor checks,21 captures and the real Print-only table UI.

Current candidate SHA-256:
`0330a65757d321563617a942f0b42d65c66341250af3fe04a15e37a268b40964`.
This supersedes the previous entrance hash. No new gameplay server phase or
packaged/player pass is claimed for this teaching-only adjustment.

### Gradual entrance follow-up — 07:54–07:55Z

All three chapters now use native directional fade/slide entrances instead of
immediate section display. Ship A and its rotor enter together; ship B and its
rotor start8 ticks later. The first instruction waits until tick50, after both
entrances settle. Circuit action timings were shifted by20 ticks accordingly.

Fresh Docker `build -Pmod_version=0.3.0-dev` passed in26s with JUnit148 and shipping
checks. The actual Traditional Chinese client passed three full scene/replays and
exact item order, including intermediate/settled fade probes at ticks15/23/45 on
both passes, plus rotor/circuit state and real Print-only table UI checks.

The visual lane now records natural, unseeked opening frames5/15/26/45 for each
chapter in addition to the operation captures:21 screenshots total. Reviewed frames
show an empty stage, models progressively appearing, and settled ships before the
first subtitle. Capture paths end in `<scene>-entrance-<frame>.png`.

Current candidate SHA-256:
`a446c53f54872099567c2c67598ea89f1eaa4e5b95637d737f5425510cabacd0`.
This supersedes the prior chapter-split hash below. The change is client presentation;
no new gameplay server phase or packaged/player acceptance is claimed here.

### Shared-chapter follow-up — 07:41–07:43Z

The owner requested sharing the repeated opening. Current item order is:

- Ordinary isolator: `isolator_basics`, then `isolator_lectern`.
- Smart isolator: the same `isolator_basics`, then `smart_controller_binding`.

The common introduction is defined once. Variant chapters begin with the blue ship
already isolated and contain only their operation steps. Native Ponder ordering
constraints put the shared chapter first. Original ship geometry is generated by
the same source function across chapters.

Fresh Docker `clean build -Pmod_version=0.3.0-dev` passed in34s with JUnit148 and
shipping isolation. The actual Traditional Chinese client passed three full
playback/replays, exact compiled item ordering, visible rotor motion/stop checks,
nine captures and the real Print-only table UI. Lectern and binding screenshots
confirm direct operation starts rather than replaying the opening. English keys
and default titles were checked; no new English render run is claimed here.

Current candidate SHA-256:
`3d7a3a4db603c808a420e13bc2b1fe2bc74fc2532dc93c208f09b3d5eeca5ac3`.
It supersedes the preceding two-story artifact below. This split changes client
teaching only; earlier gameplay and actual Create-only results remain their own
scoped evidence, not new runs of this candidate.

### Preceding two-story revision

This revision supersedes the initial four-story teaching scope below. The owner
selected two already-assembled airships, andesite-casing decks, visible propellers
and short situational captions. Table Ponder, its Java/story registration/NBT/text,
and the table Settings button/pane/actions were removed.

### Current behavior and verification

- A matching-channel lever initially turns both ships' rotors. Placing the isolator
  on blue turns its top red and leaves only blue turning.
- Releasing the lever and using an ordinary handheld drives public orange, not
  private blue. Blue's lectern drives blue; smart binding enables handheld blue
  operation aboard and outside.
- Both receivers are supported by their lamps. All three frequency displays use
  iron ingot/redstone. Visible front rotor sections rotate around their bearing hubs.
  The driver checks rotor-angle change at each control stage and on replay, rather
  than accepting a nominal speed field with no visible motion.
- Table Print is its only action. Calculation is fixed to native mode2 with existing
  BEs protected. Legacy customized jobs cancel without consumption; default-policy
  jobs resume saved progress. Inventory is retained and old top-level settings are
  removed on subsequent saves. Obsolete action IDs1/2 are rejected during active work.

Final `clean build -Pmod_version=0.3.0-dev` passed, including JUnit148 with zero
failures/errors/skips, driver isolation, license inclusion and strict mixins.
Current artifact:
`build/libs/create_fieldwork-neoforge-1.21.1-0.3.0-dev.jar`.

SHA-256: `2b0b5a8a8f876c660a4ff8eb9c59679978c553408c14c07f2317c2105e2d2106`.
The archive contains two Ponder structures and no table story class/resources.

| Executed check | Result |
|---|---|
| Integrated Traditional Chinese client, 07:18–07:19Z | Two stories, full playback/replay,13 captures including real Print-only table UI |
| Integrated English client, 07:19–07:20Z | Two stories, full playback/replay,13 captures including real Print-only table UI |
| Actual Create-only Traditional Chinese client, 07:20Z | Zero Fieldwork story associations, one real table UI capture; no Sable/Aeronautics/Simulated loaded |
| `scribing`, 07:08:26Z | 239 assertions passed |
| Consecutive independent-JVM `scribing-reload`, 07:09:00Z | 26 assertions passed |
| `schematic`, 07:09:31Z | 376 assertions passed |

Main-only verifiers required complete saves and state close. The gameplay checks
preceded final subtitle-only edits; server gameplay classes remained unchanged.
The client opens the real server-backed table menu and checks exactly one Print
button plus39 active slots. Render review covered both rotor assemblies, private
blue/public orange control results and the shortened subtitles. No new network,
binding or physical flight implementation is supplied by the teaching scene.

During development, nominal small-propeller speed/angle checks were insufficient
for its preview rendering. The final scene uses visible independent sail sections
on propeller bearings; test probes verify each section's actual motion/stopped
state. Earlier failed development reports are not counted as passes.

Reproduce client checks with `scripts/verify-ponder-docker.sh`, adding
`-PponderVisualSmoke`, the desired `-PponderLanguage=zh_tw`, and
`-PponderCreateOnly` for the actual base dependency lane. Use
`python3 scripts/generate-ponder-schematics.py --check` for source asset consistency.
Reports/captures remain under the documented build scratch paths.

Player input/pacing, other GUI scales/fonts and packaged installer gates remain
as scoped earlier. No commit, release or publication was performed.

## Ponder teaching development slice — 2026-11-14

Historical initial version; superseded by the two-airship/Print-only revision above.

Implemented on `v0.3.0-dev` above `cf49092`, without changing server gameplay,
dependency pins, strict mixins or the default version. The
[teaching contract](../design/ponder-teaching.md) defines four unique stories:
isolation basics (ordinary + smart items), smart binding, clipboard scribing and
book scribing. Table stories remain available with Create alone.

### Executed client checks

Actual software-rendered client runs used the already-installed
`draconic-insight-tutorial-smoke` Java21/Xvfb image through the non-root wrapper;
no new display packages were installed. Required Create supplied Ponder1.0.82 and
Flywheel1.0.6; Fieldwork embeds no second copy.

| Client run | Unique scenes / full replays | Captures | Result |
|---|---|---|---|
| Integrated, English, 05:57–05:58Z | 4 / 4 | 14 | Passed |
| Actual Create-only, Traditional Chinese, 06:00Z | 2 / 2 | 6 | Passed |
| Integrated, Traditional Chinese, 06:01Z | 4 / 4 | 14 | Passed |

The Create-only mod list actually lacked Sable, Aeronautics and Simulated; it did
not use an activation override. The driver checked exact item/story associations,
nonempty loaded structures, registered palette/properties, English defaults and
Traditional Chinese key completeness, actual storyboard compilation, full
instruction playback and reset/replay. The isolation animation is sampled at
handheld-denied, onboard-lectern-allowed, WORLD-lectern-denied and last-isolator
removal stages, on both play passes. These are assertions about authored teaching
states, not radio simulation.

Inspected English and Traditional Chinese captures show the handheld denial,
onboard lectern success, smart channel hint and table media/partial-list captions
with recognizable models and visible text at the captured GUI scale. This is
sampled visual evidence, not every font, scale, key binding or player's reading
speed. Client runs completed integrated-server saves on exit.

Reproduction (existing Java21/Gradle9.2.1 + Xvfb/Mesa image required; set
`FIELDWORK_PONDER_IMAGE` to another equivalent local image if necessary):

```sh
./scripts/verify-ponder-docker.sh -PponderVisualSmoke
./scripts/verify-ponder-docker.sh -PponderCreateOnly -PponderVisualSmoke -PponderLanguage=zh_tw
./scripts/verify-ponder-docker.sh -PponderVisualSmoke -PponderLanguage=zh_tw
python3 scripts/generate-ponder-schematics.py --check
```

Reports are `build/ponder-smoke-{base,integrated}.json`; captures use
`build/ponder-screens/<mode>-<language>/screenshots/`. The final clean build removes
those scratch outputs; a local capture/report archive was retained separately
during this session. Conclusions and commands here are the durable record.

### Clean build and gameplay regression

`./scripts/gradle-docker.sh clean build -Pmod_version=0.3.0-dev` passed in27s.
JUnit148 passed with zero failures, errors or skips. The shipping check confirmed
driver exclusion, client presentation's optional-API separation, strict mixins,
GPL and retained reference MIT notice. A separate ZIP check found exactly four
Ponder NBTs and no bundled duplicate libraries or smoke-driver classes.

Subsequent main-only verifiers used `-Pmod_version=0.3.0-dev`:

| Phase | Assertions | Report timestamp (UTC) |
|---|---:|---|
| `scribing` | 240 | 06:04:41 |
| `scribing-reload` | 26 | 06:05:15 |
| `network` | 174 | 06:05:48 |
| `binding` | 177 | 06:06:24 |
| `schematic` | 376 | 06:06:57 |

All passed, with verifier-checked complete saves and server-state close. The
scribing pair ran consecutively in independent JVMs with the saved world retained.
The network phase includes real ordinary handheld denial aboard a craft and real
onboard lectern private output; its radio assertions are separate from scene lamp
animation. Existing detached-holder visual retry warnings were observed during
binding; that phase's output assertions and complete save/state-close gates passed.
No unrelated lifecycle implementation was changed to suppress those warnings.

Development artifact:
`build/libs/create_fieldwork-neoforge-1.21.1-0.3.0-dev.jar`

SHA-256: `c348e08026a042108905a91f8bf82d1e56a6c13b619afd114ad4fd42a3162e37`.
The client checks used development output with the unchanged default0.1.0 label;
the final clean-build override labels this local JAR0.3.0-dev. This is not a
packaged-client test of that JAR, an installer pass, or a release/tag/publication.

Remaining: actual item-hover/Ponder-key interaction and reading/pacing acceptance,
other GUI scales/fonts/resource packs, real multiplayer/explosions and table GUI
player checks. No fresh installed-JAR smoke was started: this worktree has no
manually accepted smoke EULA. Prior installed-server evidence below is historical.

### Ponder branch rebase follow-up — 2026-11-14

At the owner's request, `v0.3.0-dev` was rebased onto `main` at `3cf9d01`.
The committed workstation change is now `e056094` (formerly `cf49092`); the
range-diff changes only its README integration with main's dependency guidance.
Uncommitted Ponder work was stashed, restored, and checked: all20 added files
matched the saved blobs, and Ponder Java/resources/build configuration matched
their pre-rebase contents exactly.

A fresh Docker `clean build -Pmod_version=0.3.0-dev` passed in27s, including
JUnit148 (zero failures/errors/skips) and shipping isolation. The new packaged
metadata was parsed and checked: NeoForge21.1.219+, Create6.0.10+, Sable2.0.5+,
Aeronautics/Simulated1.3.2+, and exact Minecraft1.21.1. Build pins remain unchanged.

Current local candidate SHA-256:
`9a7cdbd5a2ee4afe302aee086dd7765d77dc376d06adc11a194d02a41179cdb2`.
This supersedes the pre-rebase artifact hash above. No new client/runtime pass is
claimed for the rebase; unchanged Ponder content retains the earlier scoped
development evidence, with packaged/player gates still pending.

## Earlier implementation and verification history

The current optional-integration revision makes Create the only required gameplay
dependency. A packaged Create-only server containing only NeoForge, Create and
Fieldwork reached `Done`, then console-stopped with complete dimension saves. Its
mod list contained no Sable, Aeronautics or Simulated. With an integration marker,
the same packaged server logged the prominent backup/data-loss warning, reached
`Done`, then console-stopped with complete dimension saves and exit code 0.
Fieldwork's own stable registry identities remain inert, but recovery of content
owned by missing optional mods is not claimed. Packaged Create-only client/offhand
interaction and real asymmetric multiplayer handshakes remain pending.

**Full v0.2 worktree recheck before the latest client hand-swap polish (metadata
still0.1.0, no publication):** JUnit106,
network174, refresh69, movement74, lifecycle23, packed24, schematic347,
binding177, binding-reset57, binding-faults134, controller-index43,
controller-work15 and independent-JVM seed24/reload25 plus
binding-seed30/binding-reload27 all passed. Runtime verifiers
checked complete saves and state close. Current clean-build SHA-256:
`c69b8bb59e098f96027a8d3a8050d7be77cbaa9812d2d88183e382733340886c`.
The same installed JAR reached `Done (0.692s)` at04:59:45Z, reported no withheld
transactions at05:00:09Z, then console-stopped with complete dimension and Sable
sub-level saves at05:00:09Z and exit0. Latest client controller retest remains
pending. See [integrated server evidence](v02-server-binding.md#final-integrated-recheck--2026-11-09)
for times, reproduction and limits. This does not replace historical hashes below
or establish every player check of later client builds. Final integration review
and detailed [client matrix](schematic-client-v02.md#player-matrix--scoped-feedback-and-remaining-checks)
remain partial; the later Iris/ghost follow-up has a scoped owner-reported pass.

The 2026-11-11 right-hand→offhand transform-preservation change then passed a
clean build and schematic348 with complete save/state close. The follow-up forces
the boundary sync even when Create's periodic-sync cooldown is zero. Its JAR
SHA-256 is
`366cf41a1a8b5601bccd83c9553b18df5d16b891215088eb3e51ad9dccd862f8`.
That focused rerun does not claim the full matrix or a player hand-swap retest for
this later hash.

The subsequent projected-block icon/name HUD prototype passed `build`, JUnit
(including six new ray-selection tests), shipping isolation and schematic348 on
2026-11-11; all dimensions were saved at `15:38:26Z` and the verifier confirmed
state close. Candidate JAR SHA-256:
`ddfbb715ad5a919b7e0e923637003195d244b9e266bfe2b63921a67ce99d5f2b`.
The existing schematic phase is regression evidence, not execution of the client
HUD. Icon rendering, crosshair alignment, transformed previews and GUI/Iris
acceptance for this addition remain pending; see the
[prototype boundaries](schematic-client-v02.md#projected-block-identification-prototype-2026-11-11).

This recheck followed a behavior-preserving feature-package reorganization.
`FieldworkServerState` remains the logical-server composition root; controller
validation/index/input/reconciliation/safety work, transition capture and
postconditions, and Create/Simulated refresh passes are package-private
collaborators behind the existing façades. Strict mixin count/cardinality stayed
25 common plus six client entries. Dedicated-server schematic acceptance proves
common mixin loading and client-array server safety, not client transformation.

The final lectern correction keeps Create's completed native lectern assignment
when the subsequent exact wireless refresh throws a `RuntimeException` or
`Error`, and retains that refresh scope for retry. The binding phase injects both failures,
checks that the native lectern identity remains coherent and that retry withdraws
the actual private output to0. Failure inside Create's native assignment still
rolls identity/channel/pending state back and rethrows.

The latest follow-up also removes Simulated indexes on chunk unload, clears player
session state on logout, compacts revoked loaded-BE grants without deleting global
revocation history, memoizes client comparability per tick and removes redundant
controller/binding snapshots. Direct acceptance covers each lifecycle/capacity
change. This is not an FPS/MSPT or measured speedup claim.

Normal container removal keeps the old craft/position/controller footprint under
barrier through withdrawal and publication. Detached serialized markers are not
validated against unstable live plot block states; active UUID absence is the
removal postcondition. Incomplete removal or exceptions retain quarantine. This
does not expand D01 into recovery of already-lost serialized content.

**Player-test candidate preparation/delivery is complete; full client/release acceptance is not.** Main acceptance excludes `fieldwork_probe`. The `src/tools/acceptance/` source set supplies fixtures, faults and counters, not scope/registry answers. Historical train95 and other prototype counts are not evidence for this implementation.

The logic revision's full regression recorded JUnit85, network148, refresh69, lifecycle23, packed24 and independent-JVM seed24/reload25, with complete save/state close checked per phase. Its historical `0.1.0-dev` hash is `765e89e884d8118369825b084ae4e3acfd6048266e39b4ab86e0947dca012266`; see [scoped refresh](scoped-refresh.md).

The subsequent texture-only candidate (`c6536ce872e8acf97b1253f6304c09c7a1fa7b305ef1afb9912077477381ab4a`) retained identical bytes for all 58 Java classes and repeated clean build/JUnit85/network148/installer-save checks. See [texture integration](placeholder-textures.md). Those hashes do **not** identify `0.1.0` after version preparation. That preparation passed clean build, 85 JUnit tests and installed texture/model checks; it did not newly rerun every historical runtime phase.

The later local0.1.0 worktree build adds receiver-limited private-controller position updates. It newly passed JUnit85, network148, refresh69, movement81, lifecycle23, packed24 and independent-JVM seed24/reload25, each with complete save/state close. Its installer also reached ready and console-save/stop exit0. Hash `a7769b40ca8a7f1bee15a6c21929f724e1748eb009e614ab34d312c6408e0000` identifies this build, not the earlier tag's artifact; see [controller movement evidence](controller-movement-refresh.md). No new player rendering/multiplayer/performance claim is made.

## NeoForge 21.1.228 verification — 2026-11-13

Following the range declaration below, the owner requested actual21.1.228
verification, not just compilation or a metadata allowance. Workstation sources
were separated onto the real `v0.3.0-dev` branch; main remains based on the
`v0.2.2` source commit `9ae4ba4` with only metadata/range documentation changes.

In a disposable pure-fix export,25 supplemental toolchain artifacts were resolved
as data and verified against publisher SHA-256/SHA-512 values before compiling
or launching them. The existing trusted checksums were not changed; the main
verification file, smoke lock, normal21.1.257 compile pin and source/index state
were retained. The separate build/server commands ran with strict verification,
not a disabled-verification switch. See [method and limits](neoforge-runtime-range.md).

Newly executed21.1.228 checks:

| Check | Result | Evidence boundary |
|---|---|---|
| `clean build -Pneo_version=21.1.228 -Pmod_version=0.2.2` | JUnit **124**, zero failures/errors/skips; shipping/license/strict-mixin checks passed | Actual228 compilation and tests, not a257-only build |
| `verifyFieldworkAcceptance -PacceptancePhase=schematic -Pneo_version=21.1.228 -Pmod_version=0.2.2` | **376** assertions at `2026-11-13T18:05:14.285804982Z`; complete saves `18:05:19Z`, state close verified | Integrated real server/native schematic and source-scope paths; not GUI rendering |
| `verifyFieldworkAcceptance -PacceptancePhase=network -Pneo_version=21.1.228 -Pmod_version=0.2.2` | **174** assertions at `18:05:49.846697364Z`; final complete saves `18:05:53Z`, state close verified | Integrated isolation/controller/train/native network cases with expected acceptance faults |

Both server mod lists explicitly report **NeoForge21.1.228**, Create6.0.10,
Sable2.0.5, Aeronautics/Simulated1.3.2 and Fieldwork0.2.2. They reached
`Done` before the checks and cleanly saved/stopped. Expected injected receiver,
publication and visual-sync failures in the network phase are checked controls,
not unexamined runtime failures.

All **145 production class bytes** match the257-built pure-fix range reference
`7e32ceb9ded3b28f4ef69bb9c9ebcd80acc1709d2eb5c3b6183223f248a2c50f`.
The228 test artifact has embedded version0.2.2 and the selected NeoForge
`[21.1.219,)` range, and contains no workstation or non-shipping driver content:

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.2.2.jar
70bf83ccd1f0aa82eaa79b0630afc7c2c1540ad109793eceeea03b041525053d
```

This establishes scoped integrated228 compile/server evidence for the same
production bytecode. It does not establish219, every intermediate/future version,
GUI/Iris/shader rendering, real remote multiplayer, full acceptance-phase coverage
or an installed-JAR smoke. It does not republish the existing0.2.2 release.
No compatibility Java change or orchestrator commit/push was performed.

## NeoForge runtime-range follow-up — 2026-11-13

Fieldwork's NeoForge load range follows required Create6.0.10 metadata:
`[21.1.219,)`, rather than exact `[21.1.257]`. The compile pin and normal smoke/
dependency-verification inputs remain21.1.257; other constraints and strict
mixins are unchanged. Pinned Sable/Aeronautics separately require21.1.228.
See [range rationale and limits](neoforge-runtime-range.md).

An isolated export of committed `9ae4ba4` plus the metadata change newly passed
the pinned21.1.257 clean build with **JUnit124**, zero failures/errors/skips,
and shipping/license/strict-mixin checks. Its schematic phase passed **376**
assertions at `2026-11-13T17:26:54.138347831Z`, complete saves at `17:26:59Z`
and verifier-checked state close. This is not a219 or228 runtime pass.

Only `META-INF/neoforge.mods.toml` differs from the preceding pure-fix comment
candidate `a428bc168a9e239386101de4a8613c658a26229df795e7e5dce0844da680695a`;
parsed metadata differs only in the NeoForge range. All class/resource/license/
mixin bytes are unchanged and no workstation content is included.

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.2.1.jar
7e32ceb9ded3b28f4ef69bb9c9ebcd80acc1709d2eb5c3b6183223f248a2c50f
```

The219 attempt stopped before compilation in `createMinecraftArtifacts` because
strict verification lacks the alternative userdev/module inputs. Their cached
bytes match official NeoForge SHA-256 values, but the older toolchain is not
fully audited; no pins were added or checks disabled. No minimum-version server/
client test, full new server matrix, installer run or publication was completed
for this earlier range candidate.

## Fix-only 0.2.1 preparation — 2026-11-13

The owner requested isolating the accepted Position and contact-flicker fixes
before publishing 0.2.1. The selected index is based on `d94afd3` (also the
`v0.2.0` source commit) and includes only the four production fix files, two
focused unit-test files, common schematic acceptance additions and three repair
documents. Unfinished workstation registrations, menus, assets, recipes, tooling
and build changes are excluded and remain uncommitted in the original worktree.
No default-version change is needed: Release takes `release_version=0.2.1` and
builds with `-Pmod_version=0.2.1`.

The selected index was exported to a disposable verification directory, without
stashing or modifying feature files. Through the project's pinned Java21/Gradle9.2.1
Docker image, that isolated source ran these Gradle tasks:

```sh
gradle --no-daemon clean build -Pmod_version=0.2.1
gradle --no-daemon verifyFieldworkAcceptance -PacceptancePhase=schematic -Pmod_version=0.2.1
gradle --no-daemon verifyFieldworkAcceptance -PacceptancePhase=network -Pmod_version=0.2.1
```

- Clean build passed **JUnit124**, zero failures/errors/skips; strict shipping
  isolation, license and mixin-configuration checks passed. The larger counts in
  the earlier worktree records below do not describe this fix-only snapshot.
- `schematic` **376** passed at `2026-11-13T05:11:28.192276983Z`, complete saves
  at `05:11:33Z`; `network` **174** passed at `05:12:04.077095101Z`, final complete
  saves at `05:12:08Z`. Both verifiers checked server-owned state close.
- The shipping archive contains neither new workstation classes/assets nor its
  registration identity, and excludes all non-shipping drivers. Embedded mod
  metadata is exactly `0.2.1`. All 23 schematic input/client/client-mixin class bytes
  are identical to the owner-passed `a4c01956` artifact, including crosshair block
  name/icon presentation and both fixes. This is binary equivalence, not a new
  player or GPU test.
- Original working files were hash-checked for preservation except this deliberate
  evidence update; the original player-test JAR is unchanged. After recording the
  results, only documentation differs from the tested index snapshot's sources.

Isolated fix-only artifact:

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.2.1.jar
c7faa8790f61d39ddbf7195091fe9257403ae5546bd1f84b228b4612e488cfae
```

This is local preparation, not a published release; no tag, push or hosted release
workflow was performed. Earlier player confirmations
below apply to the same fix/HUD class bytes, not a separately observed runtime
hash for this new archive. No new full server matrix or installer smoke is claimed.

## Offhand contact-depth candidate — 2026-11-13

Following the owner's confirmation of the Position coordinate repair below, this
candidate changes only ordinary offhand projection layering to address the
separate contact-face flicker report (Iris installed, no shader pack). Coplanar
depth conflict is a hypothesis, not an independently reproduced diagnosis.

One stable custom RenderType retains native entity-translucent shader, block
atlas, format, batching/sorting, alpha and depth/output/outline state, adding only
VIEW_OFFSET_Z_LAYERING. Its native model-view push/scale/apply and pop/apply run
at actual batch draws, including implicit buffer-switch flushes. It does not
change stored coordinates, native transformation, Position state, material
calculation, caches, mainhand or custom BE render types. A slight render-space
scale can affect silhouettes/nearby occlusion; those detailed cases remain
unrecorded despite the symptom-specific owner pass below. See the
[client boundaries/player checks](schematic-client-v02.md#contact-face-flicker-report--2026-11-13)
and [unchanged hook scope](main-mixin-audit.md#offhand-contact-depth-follow-up-2026-11-13).

Newly executed checks:

- `./scripts/gradle-docker.sh clean build`: **JUnit148**, zero failures/errors/skips;
  shipping isolation, licenses and strict mixin configuration passed. Five new
  ASM tests check exact pinned render states, defaults, setup/clear, singleton/
  passthrough and Catnip phase/type forwarding without client/GL initialization.
  Existing alpha/vertex tests are unchanged and pass.
- `./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic`:
  **376 assertions** at `2026-11-13T04:50:18.838064491Z`, complete dimension saves
  at `04:50:24Z`, state close checked by the verifier. This is common/native
  regression and dedicated-server safety evidence, not a render execution test.
- Uncompressed shipping-entry comparison against captured Position baseline
  `af78820c97b52033f41e829ec9a8453c7a8ea24791e4450eda85023fce38a640`
  found exactly two differences: `OffhandProjectionBuffer.class` and the new
  `OffhandProjectionRenderType.class`. All other class bytes, Position repair,
  scribing behavior, resources, metadata, mixin configs and license entries remain
  identical. Documentation links and whitespace checks passed.

Earlier full-worktree player-test candidate (metadata `0.1.0`, not the fix-only
`0.2.1` artifact above):

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
a4c0195644a63bbd4ee505e0cf8097c60c7bb7dde4b66a6d798c4f9ed7f83e10
```

After this candidate was supplied, the owner confirmed that flicker disappeared
and explicitly reported testing **without and with a shader pack**. Record this
as a **player-reported pass for the reported contact-face flicker in both tested
configurations**. The runtime hash was not independently captured; exact Iris and
shader-pack names/versions and individual wall/occlusion/silhouette, first/third
person, mirror and mainhand/custom BE results were not reported. This is not
compatibility evidence for arbitrary shader packs or a no-Iris baseline.

No agent-executed client launch or GPU-state validation, new installer smoke or
full server matrix was performed. This feedback-only update checks documentation
and existing artifact consistency; it does not rerun builds or runtime tests.

## Native Position state candidate — 2026-11-13

The owner reports intermittent immediate one-block Position shifts with an offhand
schematic and Iris installed without a shader pack. Exact Iris version and
agent-controlled client reproduction remain unestablished. Pinned Create bytecode
exposes a concrete
Fieldwork invalidation defect: native Position writes deployed/anchor on the active
ItemStack, which the next full-stack selection comparison could mistake for a new
schematic and reload settings before its pending native sync.

The candidate acknowledges only that successful synchronous native DeployTool
mutation, with unchanged world/player/slot/stack/prior-snapshot identity, an
accepted pre-action snapshot, no other component changes, and agreement with the
native raw target/deployed state. It leaves coordinate math, transformation,
rotation/mirror, material calculation, controller priority and rendering unchanged.
See the [exact hook audit](main-mixin-audit.md#native-position-acknowledgement-2026-11-13)
and [client report/retest](schematic-client-v02.md#position-mode-one-block-shift-report--2026-11-13).

Newly executed checks:

- `./scripts/gradle-docker.sh clean build`: JUnit **143**, no failures or skips;
  shipping isolation, license and strict-mixin configuration checks passed.
- `./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic`:
  **376 assertions** at `2026-11-13T04:13:40.450521607Z`, complete dimension saves
  at `04:13:46Z`, state close checked by the verifier. The 28 new checks exercise
  common native ItemStack acknowledgement/rejection policy; the existing native
  sync, transforms, comparison and craft exclusions remain regression coverage.
- Three new ASM tests inspect the pinned native tool invocation, component-write/
  transform-call ordering and raw target storage without initializing the client.
  The final clean build adds only the third test relative to the runtime run;
  production and acceptance classes are unchanged between those checks.

Position-only local candidate (before the contact-depth follow-up above;
metadata remains `0.1.0`, no publication):

```text
build/libs/create_fieldwork-neoforge-1.21.1-0.1.0.jar
af78820c97b52033f41e829ec9a8453c7a8ea24791e4450eda85023fce38a640
```

After this candidate was supplied, the owner confirmed that the coordinate issue
is fixed. Record this as a **player-reported pass for the reported Position
symptom**. The tested runtime hash was not independently captured, and individual
rotation/mirror/hand/swap or synchronized-deployment cases were not reported.
No agent-executed client wrapper/mixin validation, new installer smoke or full
server matrix is claimed. This feedback-only update reruns documentation/artifact
consistency checks, not builds or runtime tests. At this Position-only delivery,
contact-face flicker remained unresolved; its subsequent correction and owner
shader-on/shader-off pass are recorded in the contact-depth section above. This
Position-only candidate intentionally includes no render change. The prior scribing
behavior/runtime evidence below is historical baseline, not newly executed checks
on this Position candidate.

## Schematic scribing table development slice — 2026-11-12

This unpublished worktree slice implements the
[schematic scribing table contract](../design/schematic-scribing-table.md) using
the owner-approved original C-01 finish and vertical timed-scribing screen. Default metadata remains `0.1.0`;
planned 0.3.0 feature work is not a new release or version/publication action.

Scribing-only clean-build SHA-256 (before the Position candidate above):
`fa6c06cc1aa0f5d659533ba0052708124be46de2cb1203590875304be04dc6b5`.
This hash has not received a new installer smoke or agent-controlled client test.
The owner-reported material acceptance is recorded below.

The last scribing-only follow-up shortens the empty-media tooltip to `Book / Clipboard`
and its Traditional Chinese equivalent. A fresh clean build passed JUnit140;
archive-entry comparison against the preceding vertical/timed candidate
`f3bad6a21070e8474700241a505334faefeb70c61c6c4bd59befc5ae704f0332`
found only the two locale JSON entries changed. All production class bytes,
settings/job behavior, assets and Print overwrite warnings are identical. Settings
and calculation policy were deliberately left unchanged. Runtime phases were not
newly rerun for this copy-only follow-up; the matrix below records that preceding
behavioral candidate.

| Check | Vertical/timed runtime baseline | Prior A5 screen |
|---|---|---|
| `clean build` / JUnit | 140 tests; no failures, errors or skips | 136 tests |
| `scribing` | 240 assertions | 166 assertions |
| Consecutive independent-JVM `scribing-reload -PacceptanceKeepWorld` | 26 assertions, including in-flight job | 13 assertions |
| `schematic` regression | 348 assertions | 348 assertions |
| `network` regression | 174 assertions | 174 assertions |

The prior A5 client candidate was
`ff3d0bfae0e1c75ca64b497fdd3cada89dd1352eed81d1f718a38e2eedc580c4`.

The previous C-01 material candidate was
`e946bdc5f3aa659d741b21094da91979ced5713a6ca6cecba02091067c6b712e`.

The earlier functional baseline was
`f7f293f41e0cfa9213eead1faac9d0d393da6a8083c7107e16efa2d5cf75859a`.
The C-01 follow-up promotes the selected lighter 16×16 wood core, golden-sheet
fittings and plain square corner nails, retaining the iron legs. Its two golden
sheet recipe inputs are accepted by actual crafting while the former brass
inputs were rejected. At that material-only revision, decoded production PNGs
matched the accepted drafts, and all six unaffected textures, model geometry,
production Java sources and packaged class bytes remained identical to the
functional baseline. Material names
and authoring labels changed from brass to gold. Canonical preview regeneration
is deterministic and reads the newly shipped resources, not private overrides.

The owner reported successful item spawning before these material refinements;
the exact earlier client artifact/context was not independently confirmed. The
selected preview was visually approved. After delivery, the owner explicitly
reported that the **new materials passed acceptance** and requested a UI redesign.
Record this as an owner-reported material-appearance pass for the delivered C-01
candidate, not an independently captured client hash/log or a pass for menu
usability, multiplayer, other lighting/resource-pack cases or Ponder.

Every listed runtime verifier checked the requested phase, assertion result,
complete dimension saves and logical-server state close. The broader historical
isolation/binding lifecycle matrix was not newly rerun for this additive feature.

### Historical A5 UI implementation evidence

The A5 revision implemented a 232×246 iron/wood/paper screen, thin ruled-paper
buttons and local settings pane; the current
[UI contract](../design/schematic-scribing-table-ui.md) supersedes its geometry and timing.
The background is an original text/icon-free 256×256 RGBA texture. Deterministic
regeneration and comparison checked 40,543 static skin pixels against the approved
design, excluding runtime text/items/status/buttons and transparent exterior.
Native container rendering supplies ItemStack/component models, counts, hover and
tooltips; no dependency sprites or custom fonts are shipped.

New tests cover immutable shared geometry, all 39 slot coordinates, disjoint wells,
settings-panel coverage, transparent/blank resources, translations, variable
Latin/Chinese/emoji width-policy metrics and preservation of styled prefixes.
Production uses actual client `Font.width`/component-aware prefix splitting and
native visual ordering, not those injected test metrics. Paper buttons keep
native scrolling text/narration/input. Tooltips are cached until their displayed
settings change.

The scribing phase now additionally checks every relocated production slot,
presentation-only hide/restore of schematic slot 0, actual native QUICK_MOVE
packets for both inputs, printing, output retrieval and return of remaining
inputs without duplication. Native action IDs and server validation are unchanged.
The original C-01 materials/recipe and BE/checklist policy sources remain unchanged;
unrelated production class bytes were compared with the captured functional
baseline, while Screen/Menu changes and the new UI helpers are intentional.

Read-only review found no server/security blocker. Native inactive-slot gates for
rendering, hover and mouse lookup were checked against the pinned client bytecode;
cached hover is cleared on pane changes. This is static seam evidence, not an
executed client interaction test. No actual client was launched: host Xvfb alone
is insufficient because the standard Docker image lacks the required X11/OpenGL
graphics libraries/tooling. No dependencies were installed for a graphics claim.
Actual alpha appearance, en/zh font layout, focus/narration, tooltip delay, drag
behavior, GUI scales and resource packs still need the player pass below.

The owner's subsequent client screenshots reported three concrete follow-ups:
missing empty-slot guide icons, the schematic caption touching its locator/border,
and dark button text with an unwanted shadow/doubled look. Those were not fixed
by the automated A5 pass. The owner then selected a model-aligned larger left
blueprint/right scribing layout and top-to-bottom medium/progress/output.
The current revision implements those changes; the screenshots are scoped defect
reports, not a full acceptance pass for this new JAR.

### Vertical timed-scribing revision — 2026-11-13

The current 232×254 screen has 128-pixel blueprint paper and a 76-pixel right card.
Only empty active input slots have faint runtime-native model guides; output has
no guide or Media/Output label. Explicit no-shadow native-font button text replaces
the old shadowed renderer; ellipsis/full tooltips retain long-label information.
Caption geometry now has nine pixels from locator to label rectangle and eight
before the sheet edge. Actual ghost opacity/depth and player-font appearance still
need client retesting.

The server ticker starts at elapsed0 and completes on active tick21. No medium is
consumed before successful native generation/commit. Repeat start cannot reset
work. Tests cover exact counts/components/options, no-change notifications,
removal/transfer, same-name file replacement/deletion and target-world changes at
completion. Callback/fault tooling exercises pre-generation cancellation plus
restore/nested calls, an exception inside the native-work try path, post-generation
output replacement, RuntimeException failure and Error cancellation/rethrow.
Job identity loss takes precedence over the generated result/status.

The clean-save pair leaves an actual job at elapsed10, starts an independent JVM,
verifies unchanged inputs/output and exact progress10, then completes once after
the remaining11 loaded ticks with no wall-clock catch-up. Malformed/elapsed21/
option/snapshot/output-mismatched NBT fails closed without consumption. The fixture
observes actual BE progress, not an assumption that newly forced chunks immediately
become ticking: its first attempt exposed that global-tick/active-tick distinction
and was corrected without changing the production21-tick boundary.

Read-only review found no remaining transactional blocker conditional on this
successful persistence evidence and updated contracts. The new job/helper and UI
are the intended code changes; C-01 materials, recipe, placement policy and
unrelated isolation/controller classes remain unchanged. This is ordinary
in-memory/clean-save transaction evidence, not cross-chunk crash-atomic durability
or detection of unnotified transient stack changes restored before observation.

The scribing phase uses actual uploaded schematic files, native cannon/checklist
serialization and production main, not a fixture-supplied materials answer. It
covers all mirror/rotation/replacement-mode combinations, already-built blocks,
existing-BE options, native entity requirements without target entity spawning,
adjacent extractable availability without material consumption, partial/unloaded
targets without force-loading, invalid deployment/media/files, same-item filename
replacement and deletion, actual shaped crafting, model flags/open collision
space, inventory drops, native server menu opening and vanilla button handling,
reach/spectator/menu identity rejection, medium consumption, output occupancy,
repeat-start protection and three-slot/settings/job persistence across JVMs.

The first implementation cached a successful file by item components; review
identified same-name file replacement/deletion as a stale-output hazard. Current
production loads a fresh printer only at the21-tick job's completion, and the
replacement/deletion controls pass. A stale success status after extraction was
also corrected. The initial asynchronous fixture needed its table/target chunks
kept loaded; those tickets are fixture-only, not production loading. The headless
menu test uses NeoForge's `configureMockConnection` to support outbound advanced
menu payloads; this does not simulate a real client handshake. The regression tab
assertion now expects the new desk alongside both existing isolators.

Archive inspection confirmed required common/client classes and exact assets,
GPL inclusion, the adapted Create policy's MIT notice, no upstream Create PNGs,
no art/acceptance tools, and no optional API/client-type references in the new
common table classes. Source review approved the corrected authority/transaction
path. These are resource/server checks, not client acceptance.

Beyond that reported appearance check, still pending are the full client
orientation/lighting/resource-pack matrix, localized screen/button/
slot/tooltip usability, shift-click and recipe-unlock UI, collision feel, actual
multiplayer interaction, resource-pack behavior, explosions, size/stress limits
and Ponder scenes. Adjacent shared-wrapper counting and synchronous/per-table
job-cost boundaries are explicit in the gameplay contract; no aggregate abuse or
measured performance claim is made.

## Player observations: single-player, 2026-11-07

These reports preceded the scoped-refresh revision. The installed hash/controller type was not independently confirmed, and no agent-executed log/screenshot evidence accompanied them:

- Two craft built from the same blueprint/frequencies lifted together before isolators; after placement, the owner reported independent operation.
- With an existing WORLD lever-powered source held at15, removing the isolator immediately made the craft take off. This supports restoration of existing WORLD input without toggling the lever again.
- Follow-up tests were reported as looking correct: replacing the isolator blocked existing WORLD15; save/exit/reentry retained isolation and internal control; with two markers, removing one kept isolation and removing the last restored WORLD input.

These support the observed single-player core flow. They do not establish every controller path, dedicated-server restart, multiplayer, explosion handling, performance, or a player retest of later hashes.

## Dedicated creative-tab follow-up: 2026-11-07

The earlier release exposed the isolator through the vanilla Redstone tab but did not register a dedicated Fieldwork tab. The follow-up registers `create_fieldwork:main`, titled **Create: Fieldwork** in both language resources, with the isolator icon and item. The vanilla Redstone entry remains available.

`./scripts/gradle-docker.sh clean build` passed with 85 JUnit tests. The main-only `network` phase passed **152 assertions** with complete saving/state close, including four new checks: actual creative-tab registry identity, icon, visible contents for a non-operator and creative-search contents. Contents are built through the real NeoForge tab-build event, not a test-only item list. Scratch evidence: `run-fieldwork-acceptance/result-network.json` and `state-closed.txt`.

This follow-up is a local build until separately committed/published; the earlier hosted `v0.1.0` artifact does not include it. Actual client tab layout, icon rendering and localized title display still need visual confirmation.

## Pinned environment

Minecraft 1.21.1 / NeoForge 21.1.257 / Create 6.0.10 / Sable 2.0.5 / Aeronautics bundled 1.3.2 / Java 21. Every Gradle verification/extraction checks the three external mod hashes against [`smoke/artifacts.lock`](../../smoke/artifacts.lock).

## Reproduction: main-only automation

```sh
./scripts/gradle-docker.sh clean build
./scripts/gradle-docker.sh verifyFieldworkAcceptance
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=network
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=refresh
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=movement
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=lifecycle
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=packed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic
# Consecutive independent JVMs; no world-resetting phase between these
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=scribing
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=scribing-reload -PacceptanceKeepWorld
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reset
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-faults
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-index
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=controller-work
# Consecutive independent JVMs; do not run a world-resetting phase between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=reload -PacceptanceKeepWorld
# Consecutive independent JVMs; do not run a world-resetting phase between them
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-seed
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=binding-reload -PacceptanceKeepWorld
```

Scratch directory: `run-fieldwork-acceptance/`, separate from smoke. Runs reset `acceptance-world` unless explicitly preserved; reload refuses without `-PacceptanceKeepWorld`. Drivers are absent from shipping JARs/ordinary runs. The verifier requires matching phase/status, complete saving and `state-closed.txt`; exit code alone is insufficient. Results are also saved as `result-<phase>.json`; scratch/logs can be overwritten.

| Layer/phase | Recorded result | Evidence and limits |
|---|---|---|
| JUnit | 106, zero failures/errors/skips | `build/test-results/test/`; pure policy/state/index/client helpers; not game behavior |
| core | 8 assertions | `.audit/registry-main-acceptance-first.log`; real MinecraftServer holder, stop/close, research excluded |
| network | 174 assertions, saved/closed | `run-fieldwork-acceptance/result-network.json`; integrated network, train, recipe and controller boundaries |
| refresh | 69 assertions, saved/closed | `.audit/scoped-refresh-release-counters.log`; unrelated bucket/dimension zero updates, fast-path faults/nesting, cross-dimension provenance, candidate/reentry/fallback |
| lifecycle | 23 assertions, saved/closed | `.audit/scoped-refresh-release-lifecycle.log`; real holding load, duplicate load, observer fault, partial deletion, retry, heatmap split, disassembly |
| packed | 24 assertions, saved/closed | `.audit/scoped-refresh-release-packed.log`; real serializer with synthetic keepPacked flags, unregistered WORLD/private windows; not a claim about natural packed-state frequency |
| seed/reload | 24/25 assertions, each saved/closed | `.audit/scoped-refresh-release-{seed,reload}.log`; two craft, each ≥3 plot chunks, two markers, saved UUIDs, startup reconstruction; after ticks A internal15/B internal0/WORLD0 |
| movement / controller index / controller work | 74 / 43 / 15, saved/closed | zero movement/teleport Fieldwork work, targeted indexes/retries, 200-post safety cadence and native TTL removal |
| binding / reset / faults | 177 / 57 / 134, saved/closed | ordinary/managed routing, real result extraction, named quarantine/repair |
| binding seed/reload | 30/27, each saved/closed | consecutive independent JVMs; force-load, revocation tombstone, unloaded reset and authority reconstruction |
| schematic | 348, saved/closed | native headless schematic regressions including pre-swap transform preservation; not a GUI/player rendering pass |

An earlier integration review found a P1 post-publication barrier-release ordering issue. It was fixed and the follow-up gate passed. The remediation regression is preserved in `.audit/main-phase2-remediation-{build,network,lifecycle,packed,seed,reload}.log`: JUnit82 and phase136/23/24/24/25, each checked independently. Do not combine counts across revisions/phases into a single acceptance claim.

## Network phase coverage

- Actual main markers/assembly: internal15, legal external7 versus forbidden15, WORLD→PRIVATE denial, marker0↔1/2→1 and synchronous Create/Simulated refresh before return.
- Per-receiver range: receiver8 gets in-range7, receiver20 may receive its own in-range15. Cold refresh cannot spread another receiver's aggregate. Hot wrappers call Sable's upstream operation once.
- One-shot candidate permission at the precise updater entry; ordinary resolver callbacks/reentrant network calls remain behind barriers. Callback-overwritten outputs require a bounded additional refresh pass.
- BEGIN receiver failure precedes marker writes and retains a named FAILED/PENDING token. Failed A does not block unrelated B or known unrelated frequencies. Named repair validates marker intent and saves before release.
- Failure after candidate publication but before barrier release retains craft/all affected-position indexes. Visual failure is separate: committed PRIVATE remains, WORLD still gets legal7, cold retry restores ACTIVE.
- Real ServerPlayer/vanilla listener with only outbound transport stubbed: accepted plot, rotation-only, too-fast rejected WORLD movement, airborne gap, teleport-ack/private rejected proposal, actual supported WORLD departure, rotated craft, Nether round trip, disconnect and same-UUID new session.
- Real controller packet/ManualFrequencyEntry: world range coordinates retained; handheld/lectern same-craft control; WORLD operator does not turn private lectern input public; release/30-tick timeout; final-marker removal restores upstream disconnect semantics; fresh session input rebinds and refreshes reused entries.
- Fixed-WORLD train adapter: PRIVATE-only high/low, PENDING-only, WORLD-positive precedence, scope/session cache, root expansion, last-member early return without power-version change, cross-dimension/no-range, other global caller unchanged and Low waiting for an unregistered WORLD source.
- Real recipe matching/missing-ingredient rejection/output1, unlock advancement loading, loot1, tool tags, BE registration, WORLD ACTIVE normalization and no ordinary redstone output.

### Fixture/integration failures retained for context

- Initial Simulated15 expectation failed because the source was behind the directional receiver. Moving both sources to the correct side fixed the fixture; direction checks were retained. A server-owned Simulated position index avoids assuming every receiver is in a Create bucket.
- An early callback fixture used a custom non-LinkBehaviour as its own anchor; upstream does not update its own field. A real WORLD source became the nested anchor, observing another receiver.
- The private lectern's WORLD operator initially exceeded Create's approximately1.8-block reach. The fixture moved to real support beside the projected lectern.
- A same-UUID new session reused an endpoint without refresh because upstream `updatePosition` does not recompute. Main now retains the old published scope until post-position reconciliation; the regression passes.

## Historical installer artifacts

### Initial production candidate: 2026-11-06

- File: `build/libs/create_fieldwork-neoforge-1.21.1-0.1.0-dev.jar`.
- SHA-256: `aa67eebec9c0f5c3ab1819cf9540250650314a2a8e60021608ef254eb8824077`, matching `.smoke/server/mods/create_fieldwork.jar` in that run. Only metadata's development-candidate wording changed before final verification; behavior matched remediation regression.
- Real NeoForge installer, Java21 JRE, lock-verified external mods; `mods/` held only those three + Fieldwork. No research/acceptance/fault/JUnit classes in the JAR.
- `18:25:20Z` ready: `.audit/candidate-final-launch.log`; no observed required-mixin or recipe/advancement loading failure.
- Console `fieldwork status` showed no tokens. A real isolator placed with ACTIVE=true normalized to false in WORLD and printed `FIELDWORK_FINAL_WORLD_INACTIVE_OK`; test block removed afterward.
- `18:26:04Z` console `stop`: all dimensions saved, exited/exit0/OOMfalse, `.audit/candidate-final-console-stop.log`.
- First non-TTY attach did not deliver input; PTY attach succeeded. SIGTERM was not used as save evidence.
- Archive checked main classes, strict mixins, assets/lang/models/data/recipe/loot/tags/advancement and absence of test drivers. [Mixin audit](main-mixin-audit.md) records installed transformations.
- Raw hash/inspect: `.audit/candidate-final-artifacts.log`; archive listing: `.audit/candidate-final-archive.log`. These are separate from launch logs.
- Research profile was changed to exclude main; baseline71 passed with `mainImplementationLoaded=false`. Rebuilding after that run-configuration change retained the main JAR hash.

The delivery review passed for “ready for player testing,” not full release acceptance. Missing raw hash/inspect evidence noted during review was subsequently saved in the files above. Later scoped/texture installer evidence has its own hashes and pages.

## Required boundaries

- **D02:** private train consumers deferred; every train condition reads public WORLD only, without Train ownership resolution.
- **D01:** stronger recovery of upstream-lost marker/data is deferred; normal registry state is not persisted. Runtime uncertainty still withholds authorization.
- A detached serialized craft is not checked through live plot block state. Its old footprint remains barred and removal publishes only after the active UUID is absent; this is lifecycle validation, not D01 data recovery.
- **OUTPUT_UNCONFIRMED:** callback failures are not circuit rollback. Some receivers may already have observed authorized results; do not claim all outputs are zero.
- `/fieldwork status` and `/fieldwork repair <token>` require permission2. Repair verifies original marker intent/unwritten baseline and saves; it refuses without proof, rather than force-clearing barriers.
- Active loads/incomplete publications with unknown frequency are conservatively pending. Confirmed WORLD-positive can still complete High. Queries do not scan craft blocks; enumeration is cold-path work.
- **Liveness limit:** long-lived unregistered WORLD transmitters of unknown frequency can defer other Low queries until registration/removal/unload. Known-frequency failure isolation is tested; precise indexing for unknown frequencies is not claimed.
- Arbitrary third-party endpoints with false `isAlive=true`, wrong coordinates or perpetually unstable callbacks are not verified.

## Outstanding player acceptance

1. Creative entry, English/Traditional Chinese names/tooltips, item/block rendering and gold/red ACTIVE synchronization.
2. Real workbench `OBO/ELE/BPB` crafting, obtaining a Link to unlock, correct-tool mining/drop.
3. World/same-craft/two-craft isolation in both directions with actual frequency/range boundaries.
4. Multiple markers, replacement, actual explosions, final removal/replacement while strong external power remains present.
5. Real handheld/lectern keys/GUI, boarding/departure, rotation/held/release/timeout, logout/rejoin, dimension changes and multiplayer/codec behavior.
6. Actual train-schedule High/Low behavior with public sources; private positives remain invisible.
7. Real craft disassembly/reassembly/split, unload/return and dedicated-server console-save/restart, observing stale power and ACTIVE.

Headless fixtures do not complete these rows. Use the [player guide](player-test-guide.md) to record results.
