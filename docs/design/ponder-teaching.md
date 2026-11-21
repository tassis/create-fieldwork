# Ponder Teaching Contract

## Scope

The current owner-selected layout has three chapters for the two isolators. The table's stories
were removed on 2026-11-14. Teaching changes no server network authority.
The [0.3.0 scope](v030-release-scope.md) and existing feature contracts
define the operations taught. Current verification belongs in the production
acceptance record, not in a claim inferred from animations.

| Storyboard | Item associations | Stages |
|---|---|---|
| `isolator_basics` | Ordinary and smart isolators | Shared opening: two assembled airships, matching lever turns both rotors, place isolator on blue, ordinary handheld operates orange |
| `isolator_lectern` | Ordinary isolator | Begin with blue isolated; place/use its lectern controller to operate blue |
| `smart_controller_binding` | Smart isolator; also linked from ordinary teaching | Begin with blue isolated and the smart block; crouch-use binding, then handheld operation aboard and outside |

The shared opening is defined once for both items. Ordinary teaching lists basics,
lectern operation, then the same smart-binding chapter. Smart teaching lists basics
then binding. Neither operation chapter replays the opening. The three chapters
register only when complete optional integration is active.
Create-only mode has no Fieldwork Ponder stories. The table remains available
through its ordinary GUI. Each story uses original structures and localized
English/Traditional Chinese captions.

## Presentation invariants

- All three chapters use the same gradual entrance: ship A and its rotor fade/slide
  in together, followed 8 ticks later by ship B and its rotor. Instruction begins
  after both entrances have settled. Replay resets the fades as well as circuit state.
- Begin with two already-assembled airships, with andesite-casing decks and blue/orange
  balloon stripes. Neither starts with an isolator. The blue ship's lever transmitter
  initially drives both ships' matching receivers and propellers.
- Each balloon's fore/aft fence pillars lie on its width centerline and meet the
  deck and balloon continuously. Receiver links sit on lamp sides to leave the
  centered rear pillar clear.
- Each receiver is mounted directly on its own lamp; the transmitter is mounted on
  a solid lever-powered support. All three native frequency displays show the
  same iron-ingot/redstone pair. Each ship has a front-facing propeller bearing and
  visible rotating sail section, with its hub aligned to the bearing. Animation uses
  Ponder section rotation around the hub, independent of optional preview renderers.
- Place the isolator on the assembled blue ship while the lever remains on. Its top
  turns red; only the blue rotor continues. Release the lever before controller tests.
- Handheld versus lectern uses the same ordinary unbound controller with unchanged
  matching frequencies and valid native range. The lectern must belong to that
  isolated craft. Only its rotor turns. The owner removed the external-lectern
  and last-isolator-removal digressions from this story.
- The ordinary lectern sits at `(1,3,5)` on the ship side, directly beside the
  isolator at `(2,3,5)`, facing north with clear overhead space. The interaction
  hint is offset outside the deck and uses a leader line to the lectern.
- Smart binding is a separate deliberate crouch-use operation, never inferred from
  where the player stands. Show successful handheld operation first aboard, then
  outside the craft, with the other ship stopped in both cases. Do not show a lectern
  in the smart structure. Native channel configuration, range and explicit
  standalone-crafting unbind remain unchanged gameplay, without extra scene detours.
- Lamp states, items and table state in scenes are explanatory animation. They do
  not run binding, print jobs, controller packets or server-owned scope services.
- The table retains its schematic, takes one valid medium after successful work,
  and never builds the target or consumes construction materials. Existing notes
  are overwritten; calculation uses the fixed default policy; unloaded targets can be partial.
- No bridge groups or 0.4.0 behavior appear in these stories.
- Player captions describe the current action and visible result. Keep internal
  verification terminology, presentation disclaimers and unrelated limitations out
  of subtitles. The separate engineering documents define evidence boundaries.
- Ordinary handheld sources remain WORLD: after isolating the blue ship they can
  still drive the orange ship. Do not falsely animate every public receiver as off.

## Technical boundary and acceptance

`integration.ponder` is client presentation only. It uses the Ponder API already
provided by required Create, with no duplicate library packaging or Sable types.
English defaults come from language resources; translated text uses native Ponder
localization/fonts. Scene identities and shared caption keys are stable resources.

The non-shipping client verifier must check actual registration/item associations,
template palettes, captions, full instruction playback and replay. Render captures
support framing/readability review; actual player controls, localized typography,
and real-world operations still need their respective client/player checks.
