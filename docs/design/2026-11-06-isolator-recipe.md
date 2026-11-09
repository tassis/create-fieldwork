# D03 — Isolator Recipe

Initially approved under owner delegation on 2026-11-06, based on actual Create 6.0.10 / Minecraft 1.21.1 resources rather than an older wiki. The owner later revised the three sheet slots to Andesite Alloy. The current recipe is implemented and headlessly tested. Client crafting/unlock interaction and balance feedback remain player acceptance work.

## Upstream comparison

Create source: `create-1.21.1-6.0.10.jar`; checksum in [`smoke/artifacts.lock`](../../smoke/artifacts.lock). Paths below are relative to `data/create/recipe/`.

| Component | Actual cost/output | Recipe path |
|---|---|---|
| Transmitter | 1 Lightning Rod + 3 Copper Sheets + 1 Redstone → 1 | `crafting/materials/transmitter.json` |
| Redstone Link | 1 Transmitter + 1 Andesite Casing → 2 | `crafting/logistics/redstone_link.json` |
| Linked Controller | 1 Redstone Link + 6 Wooden Buttons → 1 | `crafting/appliances/linked_controller.json` |
| Electron Tube | 1 Polished Rose Quartz + 1 Iron Sheet → 1 | `crafting/materials/electron_tube.json` |
| Redstone Contact | 1 Iron Sheet + 1 Redstone + 5 Cobblestone → 2 | `crafting/logistics/redstone_contact.json` |
| Sequenced Gearshift | 1 Brass Casing + 1 Cogwheel + 1 Electron Tube → 1, shapeless | `crafting/kinetics/sequenced_gearshift.json` |
| Precision Mechanism | Gold Sheet, three deploying steps repeated five times | `sequenced_assembly/precision_mechanism.json` |

Minecraft resources came from ModDev's `neoforge-21.1.257-client-extra-aka-minecraft-resources.jar`:

- Comparator: 3 Redstone Torches + 1 Quartz + 3 Stone → 1, ordinary 3×3; `data/minecraft/recipe/comparator.json`.
- Observer: 6 Cobblestone + 2 Redstone + 1 Quartz → 1, ordinary 3×3; `data/minecraft/recipe/observer.json`.

Most control components use the workbench; complex manufacturing is already represented in their ingredients.

## Approved recipe

**Ordinary shaped 3×3 crafting; output one Redstone Link Isolator.**

```text
O B O
E L E
B P B
```

| Symbol | Ingredient | Quantity/ID |
|---|---|---|
| O | Obsidian | 2, `minecraft:obsidian` |
| B | Andesite Alloy | 3, `create:andesite_alloy` |
| E | Electron Tube | 2, `create:electron_tube` |
| L | Redstone Link | 1, `create:redstone_link` |
| P | Precision Mechanism | 1, `create:precision_mechanism` |

The current production recipe uses exact `create:andesite_alloy` items in all
three B slots. The older Brass Sheet choice is historical.

Production resource: `src/main/resources/data/create_fieldwork/recipe/redstone_link_isolator.json`; type `minecraft:crafting_shaped`, category `redstone`, result count 1. Obtaining a Redstone Link unlocks it through the accompanying advancement.

## Balance rationale

- Retain the original ingredient quantities rather than adding arbitrary Diamond/Netherite requirements. Link represents wireless control, Obsidian isolation, and Alloy/Tube control components.
- Usually one isolator per craft is enough. Precision Mechanism supplies the mid-game manufacturing gate; requiring nine Mechanical Crafters merely to combine the component adds unnecessary machinery.
- The earlier Mechanical Crafting proposal became ordinary shaped crafting. No duplicate recipe or extra sequenced assembly is needed.
- Output is one, unlike the Link's output of two, because an isolator is not needed in pairs per channel. Multiple markers are supported, not encouraged as a throughput mechanic.

See [production acceptance](../testing/redstone-link-isolator-acceptance.md) for actual matching, missing-ingredient, output and advancement-resource checks.
