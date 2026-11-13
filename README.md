# Create: Fieldwork

## Introduction

A Create addon offering tools for building and operating your creations:

- **Redstone Link Isolator** — gives an assembled Sable/Aeronautics craft its own
  private Redstone Link network, preventing interference with the world and other
  craft while preserving communication aboard the same craft.
- **Smart Redstone Link Isolator** — binds Linked Controllers to a craft so you can
  operate its private network from outside, within normal dimension and range limits.
- **Offhand schematic assistance** — compares a deployed blueprint with your build,
  highlights mismatches, hides confirmed matching ghosts within supported limits,
  and shows the item icon and localized name of the projected block you look at.

## License

Copyright (C) 2026 Tassis. Licensed under the **GNU General Public License v3.0
only** (`GPL-3.0-only`). See [LICENSE](LICENSE).

## Requirements

| Component | Version | Required |
|---|---|---|
| Java | 21 | Yes |
| Minecraft | 1.21.1 | Yes |
| NeoForge | 21.1.219+ | Yes |
| Create | 6.0.10+ | Yes |
| Sable | 2.0.5+ | For isolators |
| Create Aeronautics bundled | 1.3.2+, including Simulated 1.3.2+ | For isolators |

Offhand schematic assistance works with Create alone. Isolators require NeoForge
21.1.228+ and the complete Sable, Aeronautics and Simulated bundle on both client
and server. Use the bundled Aeronautics artifact without installing duplicate
nested mods; client and server must use matching integration modes.

The mod version ranges allow newer releases. Build and runtime verification use
the pinned Create 6.0.10, Sable 2.0.5 and Aeronautics/Simulated 1.3.2 stack;
compatibility with newer releases requires separate runtime verification.
