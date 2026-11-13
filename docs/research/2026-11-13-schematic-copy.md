# Create Schematic Ownership and Copying — 2026-11-13

## Status and scope

This is dependency research for the proposed table-copy operation, not an
implemented feature or an amendment to the current
[workstation contract](../design/schematic-scribing-table.md). No production,
table, resource, build or existing test source was changed. A disposable native
fixture ran only in an isolated export of committed Fieldwork `fb48bf5`.

During exploration, the owner selected a fresh undeployed result without inherited
placement/rotation/mirror and material-mode-only deployment guidance. After the
findings below, the owner **declined blueprint copying because the additional
complexity was not worthwhile**. The existing material-only workstation and its
deployment guidance remain unchanged. The earlier copy/hint proposal is withdrawn;
this document preserves dependency findings, not an active feature requirement.

## Pinned primary evidence

- Create6.0.10 for Minecraft1.21.1, audited JAR SHA-256
  `ef87fe5709f1ba1f5b8bb20a2925b5afb4669e178fd6d8bf10c167759eefe37a`;
  see the [artifact lock](../../smoke/artifacts.lock).
- Official tag `mc1.21.1-6.0.10` was verified using `git ls-remote` to resolve to
  `ac0c444d9828da3453ae8cc65338e8de063286fb`. No dependency repository was cloned.
- Exact signatures and branches were read from that JAR using Java21
  `javap -p -c`, not inferred from a different Minecraft/Create version.
- Primary source entry points at the verified commit:
  [SchematicItem](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/src/main/java/com/simibubi/create/content/schematics/SchematicItem.java),
  [ServerSchematicLoader](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/src/main/java/com/simibubi/create/content/schematics/ServerSchematicLoader.java),
  [ClientSchematicLoader](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/src/main/java/com/simibubi/create/content/schematics/client/ClientSchematicLoader.java),
  [SchematicInstances](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/src/main/java/com/simibubi/create/content/schematics/SchematicInstances.java),
  [native table menu](https://github.com/Creators-of-Create/Create/blob/ac0c444d9828da3453ae8cc65338e8de063286fb/src/main/java/com/simibubi/create/content/schematics/table/SchematicTableMenu.java).

## Findings

### 1. The owner is a storage namespace, not an exclusive-user check in loading

`ServerSchematicLoader.handleNewUpload` and `handleFinishedUpload` use
`ServerPlayer.getGameProfile().getName()`. The stored owner is a name string,
not a UUID-based access token.

`SchematicItem.loadSchematic(Level,ItemStack)` reads `SCHEMATIC_OWNER` and
`SCHEMATIC_FILE`. On the logical server it resolves:

```text
<game directory>/schematics/uploaded/<stored owner>/<filename>
```

The method receives no player argument and does not compare the stored owner to
the current holder. An original author need not be online for this method to load
the file. In the probe, the fixture manually placed the reference in a differently
named, non-connected `ServerPlayer` inventory and directly called the raw loader;
it returned the same contents. This does not exercise ordinary transfer, inventory
synchronization, deployment, printing, persistence or other gameplay authorization,
and is not a real remote-player/client test.

Changing the owner component to the recipient's name does not transfer the file.
It changes which server path is read. If that path is absent, loading is empty;
if a different file has the same name there, the different content is loaded.

### 2. Client projection has a separate filename-only dependency

The same native loader branches on `Level.isClientSide()`. On the logical client
it loads:

```text
<client game directory>/schematics/<filename>
```

The owner is still required to be present, but is not used in that client path.
The schematic item is not a self-contained compressed template. Its factory
stores file/owner, deployment components and bounds, not the entire building.

The inspected `ClientSchematicLoader` sends local files through
`SchematicUploadPacket.begin/write/finish` to the server. The JAR's schematic packet
directory lists upload, instant export, placement and deployment-sync classes;
upload and sync implementations were audited here. No automatic server-to-recipient
template download was found in the examined loading/upload/sync paths, but this is
not an exhaustive audit of every packet registration or possible integration.
`SchematicSyncPacket` updates deployment components, not building data.

Consequently, server-side readability does not establish another client's preview
availability. Missing local files and a same-filename/different-content local file
are source-derived failure risks. In particular, two server references with
different owners but the same filename resolve to the same client-local path;
that path cannot distinguish their different contents. These client symptoms were not reproduced in a
graphical or remote-multiplayer session, and other mods' possible transfer support
was not audited. Single-player file sharing must not be generalized to multiplayer.

### 3. Native construction already provides the selected fresh placement state

The exact factory is:

```java
SchematicItem.create(level, filename, owner)
```

It returns one native written schematic and sets:

| Component | Native fresh value |
|---|---|
| `SCHEMATIC_FILE` / `SCHEMATIC_OWNER` | Passed file reference |
| `SCHEMATIC_DEPLOYED` | `false` |
| `SCHEMATIC_ANCHOR` | `BlockPos.ZERO` |
| `SCHEMATIC_ROTATION` | `Rotation.NONE` |
| `SCHEMATIC_MIRROR` | `Mirror.NONE` |
| `SCHEMATIC_BOUNDS` | Loaded from the file by `writeSize` |
| `SCHEMATIC_HASH` | Cleared by `writeSize` |

Thus a new item can have fresh placement state while retaining the original
file/owner reference. It does not mutate the original or create a new backing file.
Plain `ItemStack.copy()` alone is unsuitable: it retains deployment, orientation
and the cached hash component. Arbitrary custom-name/components are not automatically
inherited by the fresh factory; filename preservation and custom item naming are
separate policy choices.

The factory is not a success validator: missing files can still produce a
nonempty item with zero bounds. Copy completion must validate actual readable
content before consuming a blank medium, not merely test for a nonempty result.

### 4. A reference copy is not an independent backup

Two items with the same owner/filename read the same server file. Replacing that
file changes both raw loads; deleting it makes both raw loads empty. Merely
creating another item does not preserve old contents.

Native upload start deletes an existing same-name destination before replacement
bytes arrive and, under the configured `maxSchematics` limit, can remove an oldest
file in that upload namespace. Cancellation deletes the partial upload rather
than restoring the old contents. Reference-copy limitations therefore exist even
without Fieldwork file mutation, and native upload is not a transactional copy
primitive. These upload/eviction/cancellation branches were audited statically;
interrupted uploads and quota eviction were not run as part of the bounded probe.

`SchematicInstances` additionally has a level-attached, five-minute
expire-after-access virtual-world cache keyed by a component-derived hash. The
probe's file replacement/deletion checks deliberately use direct
`SchematicItem.loadSchematic`, not the virtual-world cache. They do not establish
how quickly every already-cached preview/printer notices a file change. A copy
operation should validate fresh file content rather than treat cached bounds or
an existing virtual schematic as proof of current readability.

### 5. Native blank medium exists, but native upload is not a general copy API

`AllItems.EMPTY_SCHEMATIC` exists. Native table input `SchematicTableMenu$1.mayPlace`
also accepts schematic-and-quill and written schematic items for its upload flow;
that does not make the upload flow a copy-existing-server-file operation.

`SchematicItem.create` constructs the item, while `ServerSchematicLoader` owns
uploads/instant exports and their table/player context. No ready-made general
copy-an-existing-schematic-to-another-owner method was established by this audit.
Using a new owner requires a separately designed file operation; it cannot be
implemented by relabeling the item or blindly calling a native upload endpoint.

Native raw loading checks the supported extension and applies lexical path
`normalize()` plus `startsWith(base)`. On the server this base is the uploaded root,
not the selected owner's directory. This is not proof of owner-namespace isolation,
real-path/symlink confinement or authorization for arbitrary client-chosen copy
destinations. Any future file-writing or transfer feature needs its own
server-owned destination, collision, quota and rollback decisions.

## Bounded native probe

Twenty-eight checks passed at `2026-11-13T05:36:07.719670703Z`. The fixture used
real `ServerLevel`, `StructureTemplate`, compressed native files, native
`SchematicItem.create/loadSchematic` and a separately named `ServerPlayer` inventory.
It generated small asymmetric templates in private scratch namespaces and removed
only its own files. It supplied no replacement ownership or registry answers.

The checks cover fresh component defaults, content equality, untouched original,
plain-copy inheritance, offline owner, different-holder inventory loading,
owner-only relabel failure, owner-separated same-name contents, no filename-only
local file creation, raw file replacement/deletion effects and missing-file
factory behavior. Expected missing-file controls log native read warnings.

The isolated export also completed the existing `schematic` phase with376
assertions at `2026-11-13T05:36:09.719729848Z`, all dimensions saved at `05:36:15Z`
and verifier-confirmed state close. Those376 checks are a separate regression/
shutdown control in this research run, not28+376 product-copy acceptance tests.
No new JUnit suite, clean release build, shipped copy operation, cross-JVM reload,
actual file transfer, client projection or remote multiplayer test is claimed.

Reproduce the dependency inspection with Java21, setting `CREATE_JAR` to the
lock-verified Create JAR path:

```sh
javap -p -c -classpath "$CREATE_JAR" \
  com.simibubi.create.content.schematics.SchematicItem \
  com.simibubi.create.content.schematics.SchematicInstances \
  com.simibubi.create.content.schematics.ServerSchematicLoader \
  com.simibubi.create.content.schematics.client.ClientSchematicLoader
```

To reproduce the bounded probe, write two tiny native templates under distinct
uploaded-owner directories, construct using the same filename/owner, place the
item in a differently named player's inventory, then exercise factory defaults,
owner-only retargeting and raw file replacement/deletion before cleaning those
fixture files. Run this only in a disposable server directory, never an important
world or a player's existing upload namespace.

## Implications and owner decision

The minimal native-compatible operation is **another native item reference with
fresh placement**, preserving owner/filename and validating its current file.
It can satisfy the selected fresh-position requirement without file writes, but
must not be advertised as independent storage or automatic client sharing.

Independent content duplication and automatic recipient projection are separate
extensions. A server-side file copy alone does not solve the filename-only client
dependency. That unresolved choice—inherit local-file requirements or add bounded
client-data distribution and real multiplayer verification—motivated the owner's
decision not to add copying.

There is no active copying implementation target or pending copy-specific UI/job
revision. Keep existing material-list inputs, deployed-source guidance and
server-owned21-tick job behavior. Reopening copying would require a new explicit
product decision; these findings must not be treated as authorization to implement it.
