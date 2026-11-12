# GitHub Actions and CI-Managed Releases

Workflows:

- [`.github/workflows/build.yml`](../../.github/workflows/build.yml): continuous integration only.
- [`.github/workflows/release.yml`](../../.github/workflows/release.yml): guarded manual publication.

## Continuous integration

Branch pushes, pull requests and manual CI runs use Java 21 and Gradle 9.2.1 to
run `gradle --no-daemon clean test build` using the default `mod_version` from
`gradle.properties`. CI does not upload JARs or test reports as artifacts; inspect
the workflow logs for failures. Build artifacts are retained only by Release.

CI has only `contents: read`. It never creates tags or GitHub Releases. Tag pushes
do not trigger this workflow.

## Release workflow

Publication starts only through `workflow_dispatch` on the `main` branch. The
operator must explicitly enter `release_version`, for example `0.3.0` or
`0.3.0-rc.1`. This input is the publication version, not a confirmation of the
default in `gradle.properties`. It is required and has no fallback.

The workflow then:

1. validates the selected branch, version, Minecraft version and mod ID;
2. detects an existing draft or published GitHub Release for `v<release_version>`;
3. rejects an existing lightweight tag, malformed tag or annotated tag that
   resolves to a different commit;
4. runs a fresh `gradle --no-daemon clean test build "-Pmod_version=$RELEASE_VERSION"`
   with the validated release input;
5. copies the exact JAR and writes a SHA-256 checksum file;
6. uploads both files as workflow artifacts;
7. creates an annotated `v<release_version>` tag on the dispatched commit if needed;
8. creates or recovers a draft GitHub Release and normalizes its title/prerelease
   metadata;
9. uploads and downloads both assets to verify their exact bytes;
10. publishes the Release only after that verification succeeds; or
11. for an already-published Release, rebuilds and verifies matching metadata and
    asset bytes without modifying it.

If an earlier attempt created the tag or draft but stopped before publication, a
rerun is allowed only when the annotated tag still resolves to the same commit.
Draft assets are replaced and verified. A published Release is accepted as an
idempotent success only when its tag, title, prerelease state and both asset bytes
match the rebuilt output; otherwise the run fails without overwriting it.
Hyphenated versions are marked prerelease.

The Gradle override supplies both the JAR filename and its embedded mod metadata;
the same validated value supplies the tag, Release title and asset names. The
workflow never rewrites `gradle.properties` or creates a version-bump commit.
The tag identifies the source commit; the dispatch input records the build's
publication version. Reproducing that release locally requires the same
`-Pmod_version=<release_version>` override.

The release job alone receives `contents: write`. Checkout does not persist Git
credentials, action references are full commit SHA pins, release runs are
serialized, and an in-progress release is never cancelled by a newer dispatch.
The job uses the `release` GitHub Environment; repository settings may require an
owner approval there.

## Publishing a version

1. Choose the publication version and update release-facing documentation as
   needed. Updating the default `mod_version` is not required.
2. Run the required local build and acceptance checks. To build the intended
   release version locally, use
   `./scripts/gradle-docker.sh clean test build -Pmod_version=0.3.0`
   (substitute the chosen version).
3. Commit and push the release preparation to `main`.
4. Confirm ordinary CI succeeds for that commit.
5. Open **Actions → Release → Run workflow**, select `main`, and enter the chosen
   `release_version` without the `v` prefix.
6. Inspect the completed run, generated tag, attached checksum and GitHub Release.

Do not create or push the release tag manually. The tag is a publication result,
not a build trigger. A local build or local tag does not establish hosted
publication.

## Scope and limitations

The hosted workflows run the Gradle build and unit checks. They do not run the
full headless-server acceptance matrix, start a GUI client, or publish to
Modrinth/CurseForge. A green workflow is one verification layer and does not imply
complete player acceptance.

Dependency verification remains strict. When changing
`gradle/verification-metadata.xml`, include a refreshed-dependency build because a
warm Gradle cache may not expose missing module metadata.

### Local verification of version-input changes (2026-11-12)

- Both workflow YAML files parsed successfully and all embedded shell blocks
  passed `bash -n`. Eleven release-input/branch cases exercised the actual
  validation block, covering stable/prerelease values, empty/malformed inputs,
  shell-like input and non-main dispatch rejection.
- `./scripts/gradle-docker.sh clean test build -Pmod_version=0.3.0-ci-check`
  passed. The resulting JAR filename and packaged `neoforge.mods.toml` both used
  `0.3.0-ci-check`, while the project default remained `0.1.0`.
- A subsequent default `./scripts/gradle-docker.sh clean build` passed, restoring
  ordinary local output. CI contains no artifact-upload steps; Release retains
  its artifact upload and publication/asset-verification steps.

These are local validation/build checks, not a hosted Actions or publication
test. No tag, draft or GitHub Release was created during verification.
