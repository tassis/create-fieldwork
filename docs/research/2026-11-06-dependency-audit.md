# Exact Dependency and Wireless-Path Audit — 2026-11-06

**Historical research.** This audit preceded functional main hooks. Dependencies, production integration and player-test delivery were completed later; see [production acceptance](../testing/redstone-link-isolator-acceptance.md). Early “no filter/no dependency” and EULA-refusal observations below describe this audit stage only.

## Findings and evidence boundary

The audit downloaded exact releases, verified release-API SHA-1 values, pinned SHA-256, inspected metadata/nested artifacts/bytecode and launched the stack through ModDev. It identified narrow composable candidates, not a completed isolation implementation. The first packaged installer pass verified setup and EULA refusal, not readiness or behavior. Later [lifecycle work](2026-11-06-lifecycle-boundaries.md) records ready/console-save after the owner accepted the EULA.

## 1. Exact artifacts

Authoritative URLs/SHA-256: [`smoke/artifacts.lock`](../../smoke/artifacts.lock).

| Project | Modrinth project/version | Artifact metadata |
|---|---|---|
| Create | `LNytGWDc` / `UjX6dr61` | `create=6.0.10` |
| Sable | `T9PomCSv` / `U678xqle` | `sable=2.0.5` |
| Aeronautics bundled | `oWaK0Q19` / `44pLdPGg` | `aeronautics_bundled=1.3.2` plus nested mods |

Release metadata:

- <https://api.modrinth.com/v2/version/UjX6dr61>
- <https://api.modrinth.com/v2/version/U678xqle>
- <https://api.modrinth.com/v2/version/44pLdPGg>

Verified SHA-1 values:

```text
Create       0e97e49837bed766e6f28a4c95b04885d6acc353
Sable        05f666e973d32baaaf405acb9bbed6615b909971
Aeronautics  ddbe9cbc25e8f663218aa1e9fdfc4b99f0e11e4b
```

NeoForge installer checksum source:
<https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.257/neoforge-21.1.257-installer.jar.sha256>

### Nested artifacts and dependency metadata

- Create bundles Registrate `MC1.21-1.3.0+67`, Flywheel `1.0.6`, Ponder `1.0.82+mc1.21.1`.
- Sable bundles Sable Companion `1.6.0`, Sable Rapier `2.0.5`, Veil `4.3.2`.
- Aeronautics bundled includes `aeronautics`, `offroad`, `simulated`, all `1.3.2`. Do not install duplicates.
- Simulated/Aeronautics require Create `[6.0.10,)`, Sable `[2.0.0,3.0.0)`; Aeronautics also requires Simulated `[1.3.2,)`.
- Create requires NeoForge ≥21.1.219; Sable/Simulated/Aeronautics require ≥21.1.228. Selected21.1.257 satisfies metadata, which does not prove behavioral compatibility.

Extracted nested SHA-256:

```text
aeronautics 97fbf1e27f38674145b8521b140613dc8258bea7efca456dc2686e22bd683933
offroad     8513726bc5a8383e11f4691f4dbe62d7d03bfc5f2835ec1ca1fbeffefe743f77
simulated   fdf9d250996a084b52fced3a5b0089e879a5cac05dc7bcf295ac1f4ebe5e2bff
```

Production subsequently added exact dependency ranges, lock-hash verification and Gradle verification metadata. The latter pins build inputs, not independently audited third-party supply chains.

## 2. Create comparison and aggregation

Class: `com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler`.

```text
updateNetworkOf(
  Lnet/minecraft/world/level/LevelAccessor;
  Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;
)V

withinRange(
  Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;
  Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;
)Z
```

The updater obtains the anchor's same-frequency set:

1. First loop removes dead entries and calls `withinRange(anchor,candidate)` at original offset71; eligible sources contribute to maximum transmitted power.
2. If anchor is a listening LinkBehaviour, write the aggregate to it.
3. Second loop calls withinRange at offset187; eligible other listeners receive that aggregate.

Both comparison sites need consistent policy. Filtering only aggregation or only listener writes is insufficient. Preserve frequency buckets/range/power. `IRedstoneLinkable` provides strength, liveness/listening, key and location, **no Level getter**; use the enclosing operation's Level and stored location, without assuming every endpoint is a BE.

Same PRIVATE scope still requires upstream range success. Later targeted cold refresh separately addresses spreading one anchor's aggregate to receivers with different eligible sources.

## 3. Sable redirect and transformed flow

Mixin:

```text
dev.ryanhcode.sable.neoforge.mixin.compatibility.create.redstone_links.
RedstoneLinkNetworkHandlerMixin
```

- Config `sable-neoforge.mixins.json`: required/defaultRequire1.
- Default mixin priority1000; no explicit priority.
- `@Redirect` on updateNetworkOf's withinRange INVOKE, remap=false; no slice/ordinal, so both sites match. Redirect injector order10000 is separate from priority.
- Original handler `sable$projectComparisons` captures enclosing LevelAccessor through `@Local(argsOnly=true)`.
- Convert stored endpoint positions to centers, project each containing SubLevel through `logicalPose().transformPosition`, compare squared distance with Create linkRange. Preserve same-endpoint direct success.

Observed runtime: Mixin0.8.7, MixinExtras0.5.5. Both exported calls bridge through the redirect:

```text
redirect$znh000$sable$projectComparisons$mixinextras$bridge$12(..., LocalRef)Z
→ redirect$znh000$sable$projectComparisons(..., LevelAccessor)Z
→ ownership / logicalPose projection / distanceSquared
```

Decorated suffixes are export identifiers, not stable APIs. That transformed class hash was `10249fff1ef7b34b46604a6e8639f9c4ed94ab0e7933008957ae2275a9a70841`.

Rejected approaches: injecting only original withinRange (redirect bypasses it), competing ordinary Redirect, targeting decorated private handlers, replacing the whole network updater, or intercepting Level#getSignal.

### Candidate selection history

The first candidate was ModifyExpressionValue on both original INVOKE results: upstream AND scope, with access to Level/anchor/current candidate. Original local slot6 was a bytecode observation, not a stable capture contract.

WrapOperation at the same sites provided both operands directly and preserved the replaced Sable operation through Operation. At audit time this was not yet demonstrated on the exact0.5.5 composition. The later [probe record](../testing/scope-hook-probe.md) established it, calling upstream once and ANDing policy. Production uses two ordinal-specific strict wrappers; no decorated-handler target is required.

References:

- <https://github.com/LlamaLad7/MixinExtras/wiki/ModifyExpressionValue>
- <https://github.com/LlamaLad7/MixinExtras/wiki/WrapOperation>
- <https://github.com/LlamaLad7/MixinExtras/blob/0.5.0/src/main/java/com/llamalad7/mixinextras/injector/LateInjectionApplicatorExtension.java>
- <https://github.com/SpongePowered/Mixin/wiki/Advanced-Mixin-Usage---Callback-Injectors>

## 4. Simulated's independent consumer

Class: `dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity`.

It creates Create LinkBehaviour but server tick calls its own `updateSignal()V`:

```text
networksIn(this.level) → same-frequency endpoints → transmitted strength
→ Sable source/receiver projection → getSignalFromLink(Vec3,int)
→ local aggregation → setSignal(int,double)
```

It does not call Create withinRange; the pair hook alone leaks this path. The narrow candidate wraps the first `IRedstoneLinkable.getTransmittedStrength()I` call (ordinal0), returning0 for denied sources so upstream skips them. Keep projection, direction and modulation. It was a candidate at this stage, later validated in prototypes/main.

The design therefore needs one shared policy over two audited standard-network consumers, plus the separate train adapter. This does not justify broad Flight Control/vanilla interception.

## 5. Ownership, identity and lifecycle

Bytecode established:

- `Sable.HELPER.getContaining(Level,Vec3i)` uses Level's container holder, chunk/plot conversion and direct plot-array lookup: O(1), no block scan.
- SubLevel UUID identifies an instance. allocateNewSubLevel creates a UUID; allocateSubLevel accepts one. Saved UUID restoration was audited in the later lifecycle pass.
- `getSubLevel(UUID)` is a map lookup; `queryIntersecting` traverses loaded SubLevels and must not enter wireless queries.
- Observers expose added/removed/tick. Allocate writes plot/occupancy/list/UUID indexes before onAdded; this is not content-ready.
- Removal notifies observers before onRemove/index cleanup; reasons are UNLOADED and REMOVED.
- ContainerReady is container availability, not craft content completion.
- Block beforeMove/afterMove callbacks are per-block, not batch commit.
- `setSplitFrom` records source UUID/pose; it does not transfer addon registry automatically.

Use logical-server-owned `(dimension,UUID) → lifecycle + marker-position set`, without long-lived craft references. Known uninitialized craft stay private until atomic reconstruction. Holding/load/assembly/split/BE ordering needed the [next audit](2026-11-06-lifecycle-boundaries.md).

Scope0↔1 or initialization/split may not trigger upstream updates. Refresh both sides: updating onboard sources alone can skip rejected outside listeners and leave stale output. Zeroing all denied receivers destroys legal same-scope weak sources. Snapshot endpoint tables during transitions, never craft-scan in pair queries; include non-BE endpoints.

## 6. Global train query

`hasAnyLoadedPower(Couple)Z` scans loaded maps' same-frequency positive sources with no receiver/range/scope. Constant search/javap found RedstoneLinkCondition as an external caller. Pair/Simulated hooks cannot cover it. The audit left product semantics undecided rather than silently excluding it. D02 later approved fixed-WORLD visibility; see [train research](2026-11-06-train-global-condition.md).

## 7. Launch and smoke evidence at this stage

### ModDev baseline

Temporary `.audit/stack.init.gradle` runtimeOnly artifacts launched the exact mod list, including nested dependencies. Sable redirects were exported and inspected; Rapier initialized in three dimensions; ready at `09:34:34`, `Done (9.148s)!`.

No EULA file was required by that development launch. Initial missing server.properties logged NoSuchFileException before eventual ready; upstream refmap/annotation warnings also appeared. The command hit a ten-minute timeout rather than a normal console stop; no project container remained afterward. It is not shutdown/save evidence.

### First packaged smoke setup/EULA checks

- Real installer setup succeeded; installation/staged files owned by1000:1000.
- Scaffold Fieldwork hash: `fd10beb23aa5071e2d2147d66b2afae4b82ef90756cc42582219cd6138e5b869`.
- Wrapper start with eula=false refused exit1, leaving no running service.
- A separate disposable, unpublished-port smoke JVM loaded the correct mod list, printed the EULA agreement requirement and exited before ready.
- Digest/resource/security-setting update and repeat setup preserved EULA, server.properties and JVM-args hashes.
- A temporary unknown .jar proved setup refusal; it was removed. Root wrapper invocation also refused exit1.
- Three shell-script syntax checks, Compose config and repeated clean build passed.

These are installation/EULA-boundary checks, not isolation or ready acceptance. Later owner-approved EULA/console-stop results are separately recorded.

### Reproduce bytecode inspection

After smoke setup downloads verified inputs, from project root:

```sh
LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)" \
  docker compose run --rm --entrypoint javap build \
  -classpath .smoke/downloads/create-1.21.1-6.0.10.jar -p -c -s \
  com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler

LOCAL_UID="$(id -u)" LOCAL_GID="$(id -g)" \
  docker compose run --rm --entrypoint javap build \
  -classpath .smoke/downloads/sable-neoforge-1.21.1-2.0.5.jar -p -c -s -v \
  dev.ryanhcode.sable.neoforge.mixin.compatibility.create.redstone_links.RedstoneLinkNetworkHandlerMixin
```

Local evidence: `.audit/bytecode/`, `.audit/baseline-launch.log`, `.audit/smoke-setup.log`, `.audit/production-eula-launch.log`. Public source branches are references only; artifacts/transformed bytecode define this audit. Sable metadata names PolyForm Shield1.0.0; Create/Simulated package licenses must be read. Do not infer asset redistribution rights. Dependency JARs are not redistributed by this repository.

## 8. Original follow-up gates

Resolve Simulated/train scope, prove narrow MixinExtras composition, establish lifecycle completion/refresh and then implement the block. If no composable hook exists, stop/redesign instead of replacing the handler. These historical gates were followed by the prototype and production records; they are not a current instruction to restart completed research.
