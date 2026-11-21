# Schematic Scribing Table — UI Contract

## Status

The owner-selected vertical scribing revision is implemented in the current
development worktree. It retains the A5 iron/wood/paper style, C-01 materials,
lighter blue drafting sheet and ruled-paper buttons, but aligns blueprint-left
and scribing-right with the physical model. Owner screenshots identified missing
guides, caption/frame contact and shadowed dark button text in the earlier A5
build; those reports are not a pass for this new revision.

The [gameplay contract](schematic-scribing-table.md) defines the persisted,
server-owned 21-tick job and transaction semantics. Current counts/hash and
earlier evidence live in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md#schematic-scribing-table-development-slice--2026-11-12).
No publication or completed real-client acceptance is implied.

## Layout and semantic indices

The screen is **232×254 logical GUI pixels**, inside a 256×256 texture. The
blueprint sheet is 128 pixels wide versus the right card's 76, separated by an
8-pixel gap. Immutable `ScribingTableLayout` geometry is shared by both menu
constructors and the screen; the original 39 semantic indices are preserved.

| Menu index | Role | Item origin X/Y |
|---|---|---|
| 0 | Blueprint in larger left sheet | 66 / 64 |
| 1 | Book/written-book/clipboard, upper right | 176 / 44 |
| 2 | Completed checklist, lower right | 176 / 94 |
| 3–29 | Player rows | X=35+18×column, Y=172+18×row |
| 30–38 | Hotbar | X=35+18×column, Y=230 |

Native items remain 16 pixels in 18-pixel wells. The right progress channel
`[180,66,8,24]` lies between aligned medium/output wells and fills **top to bottom**
using synchronized server progress. It is fixed-duration scribing work, not a
measurement of file parsing or an amortized scanner. Output appears only at
successful completion. No Media/Output labels or output guide icon are used.

The schematic locator ends at Y=89; its caption rectangle starts at Y=98 and ends
at Y=110, leaving 9 pixels above and 8 below before the sheet edge at Y=118.
Runtime text uses native font metrics within those bounds, not a smaller forced
font intended to conceal the prior collision. GUI scale controls physical size.

## Original background and runtime content

[The original background](../../src/main/resources/assets/create_fieldwork/textures/gui/schematic_scribing_table.png)
is RGBA, with transparent exterior/nameplate cutouts. It contains static panels,
wood, paper, grid/rulers, empty wells, a small data-direction cue and the empty
vertical progress housing. It contains no text, item sprites/counts, status dot,
progress fill or button faces.

Explicit regeneration:

```sh
python3 art/schematic_scribing_table/build_ui.py
```

The [tool](../../art/schematic_scribing_table/build_ui.py) reads only original
Fieldwork wood/blueprint PNGs, not reference screenshots or dependency art. The
GUI is its own overridable resource; changing only a block texture in a resource
pack does not regenerate it. No Create GUI sprites/icons or custom font files
are extracted into the shipping JAR.

Actual occupied slots retain native `AbstractContainerScreen` ItemStack rendering,
component-driven model overrides, counts and item tooltips. Empty **input** slots
add presentation-only hints using native client item models: schematic for slot0,
alternating book/clipboard for slot1. The icon is washed toward the well color,
has no fake count and never becomes a menu/inventory entry. Hints disappear when
occupied or inactive. Empty output has a purpose tooltip but no icon guide.

Native ghost rendering uses `renderFakeItem`, a flush and an explicit front-depth
translucent well-color wash, confined to 16 pixels. Its actual opacity/depth for
glinting, 3-D or resource-pack models remains a real-client check.

## Fonts and paper buttons

The empty-media tooltip is deliberately just `Book / Clipboard`, localized for
the player's language. Full consumption/overwrite guidance stays on the Print
button tooltip; shortening this hint does not change accepted item types or
material calculation. The table uses a fixed calculation policy.

Text uses translated Components and the player's current Minecraft font, glyph
providers and language; nothing is baked from the mockup's bitmap lettering.
Measured component-aware prefix fitting/native visual ordering handles long labels.
Short status messages have full detailed hover explanations; they describe input
presence or server job results, not unverified file readiness.

Paper buttons keep the thin dark outline and inset top/bottom rules. Their labels
now explicitly use `drawString(..., false)` to remove the unwanted native button
text-shadow layer seen in the owner's screenshot. Long button text is fitted with
an ellipsis rather than the former shadowed scrolling renderer; the full Print
tooltip and native input/focus/narration are retained. This does not remove native
styling from tooltips or stack counts, nor alter a font pack's own glyph artwork.
The Print tooltip is fixed guidance for its operation.

Injected-width tests cover variable Latin/Chinese/emoji metrics and styled-prefix
retention. They are policy tests, not execution of actual client glyphs, font-pack
line heights or every bidirectional/grapheme case.

## Print-only controls

The owner removed Settings, its replacement controls and the covering pane.
Print is the sole button. All three work slots and all inventory slots stay
presentation-active. Native quick-move keeps the original semantic indices and
input mutations can cancel running work. Server reach/menu identity/spectator
validation is retained; old settings button IDs are rejected.

The fixed policy and legacy job handling are defined by the gameplay contract.
No hidden saved option continues to influence a new job.

## Verification boundary

Automated tests cover geometry, caption clearance, vertical channel spacing,
input-only hint policy, original RGBA/blank wells, translations, text fitting,
native transfer packets, exact job timing, cancellation/fault/reentrancy, removal
and independent-JVM mid-job persistence. Production files preserve the selected
block finish, recipe and unrelated isolation/controller behavior.

The current Xvfb/software-rendered verification opens the actual server-backed
table screen, checks the sole Print button and active slots, and captures it.
The standard build image still lacks display tooling; the dedicated client wrapper
uses an existing display-capable image. New player
acceptance is still needed for guide visibility/depth, no-shadow lettering,
actual caption gaps, progress direction, GUI scales, localization/font/resource
packs, dragging, narration and multiplayer use.
