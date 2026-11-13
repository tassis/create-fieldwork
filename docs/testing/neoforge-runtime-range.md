# NeoForge Runtime Range

## Build pin and load gate

Fieldwork builds against NeoForge `21.1.257`, pinned by `neo_version` in
[`gradle.properties`](../../gradle.properties). The lock-checked smoke installer
and current dependency-verification inputs also retain their audited versions.
These reproducibility choices do not require an exact-version load gate.

The [metadata template](../../src/main/templates/META-INF/neoforge.mods.toml)
instead declares NeoForge `[21.1.219,)`, matching the required Create6.0.10
artifact. Minecraft remains exactly1.21.1. The current mod load gates allow
Create6.0.10+, Sable2.0.5+ and Aeronautics/Simulated1.3.2+; their build and
artifact-lock pins remain unchanged. Strict mixin settings remain unchanged.
These open-ended declarations do not establish runtime compatibility with newer
mod releases. The checks below describe the pinned mod versions used at the time.

## Upstream metadata evidence

The actual pinned JARs in the [artifact lock](../../smoke/artifacts.lock) declare:

| Artifact | Required NeoForge range |
|---|---|
| Create6.0.10 | `[21.1.219,)` |
| Sable2.0.5 | `[21.1.228,)` |
| Aeronautics bundled1.3.2 | `[21.1.228,)` |

Fieldwork's base gate follows its mandatory dependency. Installing the optional
integration bundle still requires satisfying that bundle's own higher floor;
Fieldwork's range does not override another mod's dependency checks. The
Minecraft constraint continues to apply alongside these NeoForge ranges.

## Executed checks and boundaries

### NeoForge21.1.228 integrated verification

After separating unfinished workstation work onto the real `v0.3.0-dev` branch,
an isolated export of the `v0.2.2` source plus the runtime-range template change
completed a clean build and actual schematic/network server phases on
NeoForge21.1.228. The observed mod list included required Create6.0.10 and the
pinned Sable/Aeronautics/Simulated integration bundle; servers reached ready and
completed clean saves/state close. This is runtime evidence, not merely a build
or metadata declaration. Exact counts/times and artifact hash are in the
[228 acceptance record](redstone-link-isolator-acceptance.md#neoforge-211228-verification--2026-11-13).

All production class bytes from the228 build match the257-built pure-fix
reference. No compatibility Java changes, runtime-gate rewrite or workstation
content were needed for this check. The normal compile pin remains257.

Supplemental228 toolchain inputs were prepared only in that isolated project:
a resolution-only Gradle task downloaded/recorded inputs without compiling or
launching them; every added artifact was then checked against its publisher's
SHA-256 or SHA-512 before a separate strict-verification build and server runs.
Existing trusted checksum values were required to remain unchanged. This extends
scratch verification inputs rather than disabling verification or changing the
main project's lock/verification files. It is published-checksum validation, not
an independent audit of every dependency's source/supply chain.

The lane used FML4.0.42, ASM9.8, Sponge Mixin0.15.2+mixin0.8.7 and
MixinExtras0.5.3 as selected by that toolchain. Shader/client rendering, actual
remote multiplayer, full server-phase coverage and installed-JAR startup remain
separate checks. Existing player feedback at257 is not relabeled as a228 GUI test.

### Earlier pinned257 check

A pure-fix source export, excluding unfinished workstation content, passed a
clean build and the existing schematic server phase on the pinned257 toolchain.
The final archive differs from the preceding pure-fix artifact only in the
packaged metadata text, and its parsed metadata differs only in the NeoForge
range. Java classes, resources, other dependencies, licenses and mixin configs
are unchanged. Exact counts, times and hash are in the
[production acceptance record](redstone-link-isolator-acceptance.md#neoforge-runtime-range-follow-up--2026-11-13).

An alternate `-Pneo_version=21.1.219` clean-build attempt stopped in
`createMinecraftArtifacts`, before application compilation, tests or server
launch. Strict dependency verification lacks the alternative
`neoforge-21.1.219-userdev.jar` and `.module` entries. This is a toolchain-input
verification limit, not an observed Fieldwork API incompatibility and not a219
compatibility pass. The published NeoForge checksums for those two cached files
were compared and match, but no219 toolchain pins were added and no
dependency-verification checks were disabled.

The219 userdev configuration additionally identifies older FML/Mixin/ASM/tool
inputs, so validating two file checksums is not a complete219 toolchain audit.
Completing that lane requires explicitly verified alternative build inputs and
a Create-only server probe; the pinned optional bundle cannot be used at219
because its own metadata requires228 or newer.

No219, arbitrary future-version or every-intermediate-version pass is claimed.
The declaration is dependency-aligned; verified environments and test scope are
recorded separately.

## Reproduction

For228, use a disposable source export of the `v0.2.2` base plus the intended
metadata template. With the audited supplemental verification inputs described
above, these were the actual Gradle tasks:

```sh
gradle --no-daemon clean build -Pneo_version=21.1.228 -Pmod_version=0.2.2
gradle --no-daemon verifyFieldworkAcceptance -PacceptancePhase=schematic -Pneo_version=21.1.228 -Pmod_version=0.2.2
gradle --no-daemon verifyFieldworkAcceptance -PacceptancePhase=network -Pneo_version=21.1.228 -Pmod_version=0.2.2
```

The version override labels the local test artifact; it does not republish or
replace the existing hosted0.2.2 release. An unmodified main-project verification
file still pins the normal257 toolchain and does not automatically admit all
alternate-version inputs.

On the committed fix-only source plus this metadata change, the normal pinned
build and regression commands are:

```sh
./scripts/gradle-docker.sh clean build -Pmod_version=0.2.1
./scripts/gradle-docker.sh verifyFieldworkAcceptance -PacceptancePhase=schematic -Pmod_version=0.2.1
```

The alternative-toolchain attempt uses the additional
`-Pneo_version=21.1.219` override. It is not a request to change the normal build
pin or to ignore dependency-verification failures. A GUI and optional-free
runtime verification require their own appropriate environment/evidence.
