# Version 0.3.0 Scope

## Status and authority

The owner identified these two main goals on 2026-11-14:

1. The schematic scribing table, producing clipboard and book outputs.
2. Introducing Ponder teaching for the isolators. The owner later removed the table
   stories and Settings controls, and selected the two-airship comparison.

This document records release scope, not a released version or completed player
acceptance. The [behavior contract](behavior-contract.md),
[architecture policy](project-architecture-policy.md), and existing feature
contracts still govern production behavior. The current checkout is
`v0.3.0-dev`; its starting commit for this scope record is
`cf4909207e9e3c3aa14a68e702b15e1ba7bfb2b1`.

## Schematic scribing table

The working interpretation of the owner's copying/recipe wording is the existing
blueprint material-requirement checklist operation. The table uses Create's native
material accounting and produces a written clipboard or written book from the
selected schematic. A different kind of recipe output requires clarification.

The [gameplay contract](schematic-scribing-table.md) defines accepted media,
deployment/rotation/mirror, fixed default calculation, adjacent inventory availability,
partial unloaded results, the persistent 21-loaded-tick job, and server authority.
The [UI contract](schematic-scribing-table-ui.md) defines the existing screen.

Those functions are implemented in this branch and have prior headless evidence.
Current checks and outstanding client, multiplayer, crafting/unlock and explosion
acceptance remain in the [production acceptance record](../testing/redstone-link-isolator-acceptance.md).
Recording release scope does not count as rerunning them.

## Blocks and teaching coverage

| Player-facing block | Registry ID | Teaching focus |
|---|---|---|
| Redstone Link Isolator | `create_fieldwork:redstone_link_isolator` | Craft-local private standard wireless network and WORLD/other-craft boundaries |
| Smart Redstone Link Isolator | `create_fieldwork:brass_redstone_link_isolator` | Loaded craft binding for Linked Controllers, native channels/range, explicit unbind |
| Schematic Scribing Table | `create_fieldwork:schematic_scribing_table` | Deployed schematic input, clipboard/book medium, Print and output; no Ponder story |

Ponder teaching is implemented with three chapters:

- `isolator_basics`: shared two-ship introduction, channel interference, isolator
  placement and the ordinary handheld source's public behavior.
- `isolator_lectern`: ordinary variant only, starting at the isolated-ship state.
- `smart_controller_binding`: smart operation, also linked from ordinary teaching,
  starting at the isolated-ship state with the smart block.

Both items list shared `isolator_basics` first. Ordinary teaching continues through
`isolator_lectern` to the same `smart_controller_binding`; smart teaching goes
straight to binding. Operation chapters skip the repeated two-ship opening.

The owner authorized the revised layout. Both isolators have
storyboards; English and Traditional Chinese use language resources and runtime
fonts. Actual player acceptance of pacing, entry and controls remains pending.

## Teaching boundaries

- Preserve actual current usage: ordinary handheld controllers are WORLD; smart
  binding is explicit, range-limited and requires a valid loaded witness.
- In the isolation scene, keep the controller ordinary/unbound, its frequencies
  unchanged and its operator within native range. First show handheld denial,
  including while the operator is aboard the craft, then install it in a lectern
  on that same isolated craft and show successful onboard control. The lectern's
  physical membership grants this route; a WORLD lectern remains WORLD. This is
  distinct from the smart variant's bound handheld operation.
- Isolation has no ordinary-world block effect. A Ponder animation must clearly
  identify an assembled-craft illustration and must not suggest a stationary
  isolator divides nearby world links by radius.
- Table documentation follows its actual material checklist and Print-only flow,
  including overwrite guidance, fixed policy and unloaded-result limits.
- Ponder scene playback is presentation, not a live Sable assembly, authoritative
  network transaction or successful server-side print. Runtime acceptance remains
  separate evidence.
- The table works with Create alone. Create-only mode has no Fieldwork Ponder
  scenes; isolator scenes follow complete optional integration activation.
- The two airships begin assembled. Their andesite-casing decks and visible front
  propellers contrast shared-channel interference with isolation after placement.
- Player subtitles describe the current action and observed response.

## Version boundary

Explicit cross-craft bridge groups, a new bridge block/GUI and the purpose-based
network extension discussed for 0.4.0 belong to `v0.4.0-dev`. They are not part of
the 0.3.0 implementation or tutorials. The existing D02 public WORLD observer
contract remains in effect.

## Reference and next step

The owner supplied `/home/tassis/workdir/repos/draconic-insight` as an introduction
reference. Its current local Ponder client setup, plugin, generated structure
resources and non-shipping smoke harness were inspected read-only. Relevant files
include uncommitted work, so its HEAD alone does not identify the reference.

See the [reference investigation](../research/2026-11-14-ponder-reference.md),
[prospective implementation plan](../plans/v0.3.0-ponder/plan.md), and
[continuation checklist](../plans/v0.3.0-ponder/checklist.yaml). The present update
records scope and implementation intent. The subsequently authorized Ponder
implementation and executed checks are recorded in the
[production acceptance record](../testing/redstone-link-isolator-acceptance.md#two-airship-teaching-and-print-only-table--2026-11-14).
The default version is unchanged and no release is published.
