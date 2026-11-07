# GitHub Actions and Tag Releases

Workflow: [`.github/workflows/build.yml`](../../.github/workflows/build.yml). It follows the build-artifact → tag-Release pattern used by the neighboring `emotecraft-compat` project.

## Workflow behavior

- Branch pushes, pull requests and `workflow_dispatch`: Java21/Gradle9.2.1, installed texture/model validation, `gradle --no-daemon clean test build`, then JAR artifact upload. Failed builds attempt to upload JUnit reports.
- `v*` tag pushes: tag must exactly equal `v` + `gradle.properties`'s `mod_version`. After build success, a separate release job downloads the same artifact and runs `gh release create --verify-tag --generate-notes` with the JAR attached.
- Hyphenated versions add `--prerelease` (for example `v0.1.0-dev`). `mod_version=0.1.0` requires `v0.1.0` and produces a regular GitHub Release. Release classification does not imply complete player acceptance.
- Artifact name: `${mod_id}-neoforge-${minecraft_version}-${mod_version}`, matching the NeoForge-only `archivesName` in build.gradle. Version/game/mod ID are validated; the workflow does not rewrite them.
- Global build permission is `contents: read`; only release has `contents: write`. Checkout does not persist credentials, and all action references use 40-character SHA pins. PRs do not publish.

This workflow does not run the full headless-server/client matrix or publish to Modrinth/CurseForge. A green build is one verification layer.

## Version/tag workflow

For a future version, update `mod_version`, run clean build/resource checks, commit the release preparation, then create an annotated matching tag. Push the branch and tag when publishing is intended:

```sh
git push origin main
git push origin v0.1.0
```

`v0.1.0` was created during version preparation. Check actual local/remote tag and Actions state before repeating publication steps. The earlier implementation-stage notes saying no commits/tags existed are historical. No hosted result is inferred from local validation or a tag alone; inspect the GitHub run/Release directly.

The repository must allow Actions and the release job's contents-write permission. If a Release already exists for a tag, `gh release create` does not overwrite it. Use a new version/tag or let the owner manage the existing release; this workflow does not force-push tags or replace attachments automatically.

## Historical local validation: 2026-11-07

- actionlint1.7.7 passed.
- Read-only `gh api` verified all five action commits. The reference project's Gradle pin `0b6dd653ba04f4f93bf581ec31e66cbd7dcb644d` returned422 in the official repository; this project uses verified v4 `ed408507eac070d1f99cc633dbcf757c94c7933a` with `setup-gradle/action.yml` present. The reference project was not modified.
- Actual YAML version Bash was extracted and executed with temporary fixtures: branch, dev tag and stable tag passed; mismatch/invalid version/game version/mod ID failed without outputs.
- Actual release Bash was extracted and its final publishing call replaced **in memory** with argv printing. Stable/dev tag, path, repo, title, verify-tag/generate-notes/prerelease flags passed; no real Release command was executed by that test.
- Texture validation and Docker `clean test build` passed (some Gradle tasks restored from cache). At that slice the JAR hash remained `c6536ce872e8acf97b1253f6304c09c7a1fa7b305ef1afb9912077477381ab4a`; this is a historical dev-candidate hash.
- Read-only workflow review found no blockers. Hosted execution was not established by these local tests.

## Dependency-verification repair: 2026-11-07

The GPL-licensed `v0.1.0` tag build failed in `createMinecraftArtifacts`: the verification metadata pinned JUnit BOM POMs but lacked their Gradle `.module` metadata. Refreshing local dependency resolution reproduced the same three compile-classpath failures (5.10.0, 5.10.1 and 5.9.3). After those were supplied, the runtime classpath exposed the same omission for 5.7.2.

All four `.module` files were fetched separately from Maven Central and NeoForged Releases, compared byte-for-byte, and checked against Central's published `.sha256` values before adding exact artifact entries to `gradle/verification-metadata.xml`. Existing checksums and strict metadata verification remain enabled; no wildcard trust or verification bypass was added.

The repaired dependency resolution and full build passed with:

```sh
./scripts/gradle-docker.sh clean test build --refresh-dependencies
```

Compilation/test outputs can still come from Gradle's build cache; `--refresh-dependencies` specifically rechecks dependency metadata resolution. The successful local run restored the 85-test JUnit result from cache. When auditing verification metadata, include a refreshed-dependency run: an ordinary warm-cache build alone did not expose these missing artifacts.
