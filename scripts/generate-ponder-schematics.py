#!/usr/bin/env python3
"""Author original Fieldwork teaching structures; deterministic vanilla NBT, no dependencies."""
import argparse
import gzip
import struct
from pathlib import Path

OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/create_fieldwork/ponder"


def utf8(text):
    encoded = text.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def encode(kind, value):
    if kind == 3:
        return struct.pack(">i", value)
    if kind == 8:
        return utf8(value)
    if kind == 9:
        element_kind, elements = value
        return bytes([element_kind]) + struct.pack(">i", len(elements)) + b"".join(
            encode(element_kind, element) for element in elements)
    if kind == 10:
        return b"".join(bytes([tag]) + utf8(key) + encode(tag, data)
                        for key, (tag, data) in value.items()) + b"\0"
    raise ValueError(f"Unsupported authored NBT type: {kind}")


def template(width, height, blocks, block_entities=None):
    palette = sorted(set(blocks.values()))
    states = []
    for entry in palette:
        name, _, properties = entry.partition("[")
        state = {"Name": (8, name)}
        if properties:
            state["Properties"] = (10, {k: (8, v) for k, v in
                                       (p.split("=") for p in properties.rstrip("]").split(","))})
        states.append(state)
    entries = []
    for pos, state in sorted(blocks.items()):
        entry = {"pos": (9, (3, list(pos))), "state": (3, palette.index(state))}
        if block_entities and pos in block_entities:
            entry["nbt"] = (10, block_entities[pos])
        entries.append(entry)
    root = {
        "DataVersion": (3, 3955), "size": (9, (3, [width, height, width])),
        "palette": (9, (10, states)),
        "blocks": (9, (10, entries)),
        "entities": (9, (10, [])),
    }
    return gzip.compress(b"\x0a\x00\x00" + encode(10, root), mtime=0)


def floor(width):
    return {(x, 0, z): "minecraft:smooth_stone" for x in range(width) for z in range(width)}


def isolation(smart):
    blocks = floor(15)
    for origin, stripe in ((1, "light_blue"), (9, "orange")):
        for x in range(origin, origin + 5):
            for z in range(3, 10):
                blocks[(x, 2, z)] = "create:andesite_casing"
        for z in range(4, 9):
            blocks[(origin, 1, z)] = "minecraft:dark_oak_slab[type=top]"
            blocks[(origin + 4, 1, z)] = "minecraft:dark_oak_slab[type=top]"
            for x in range(origin + 1, origin + 4):
                blocks[(x, 6, z)] = f"minecraft:{stripe}_wool" if z == 6 else "minecraft:white_wool"
        for z in range(5, 8):
            blocks[(origin + 2, 7, z)] = f"minecraft:{stripe}_wool" if z == 6 else "minecraft:white_wool"
        for z in (4, 8):
            for y in (3, 4, 5):
                blocks[(origin + 2, y, z)] = "minecraft:oak_fence"
        blocks[(origin + 2, 3, 7)] = "minecraft:redstone_lamp[lit=false]"
        blocks[(origin + 1, 3, 7)] = "create:redstone_link[facing=west,receiver=true,powered=false]"
        blocks[(origin + 3, 3, 3)] = "create:creative_motor[facing=north]"
        blocks[(origin + 3, 3, 2)] = "aeronautics:propeller_bearing[facing=north]"
        blocks[(origin + 3, 3, 1)] = "minecraft:andesite"
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            blocks[(origin + 3 + dx, 3 + dy, 1)] = "create:white_sail[facing=north]"
    blocks.update({
        (2, 3, 6): "minecraft:stone",
        (2, 4, 6): "minecraft:lever[face=floor,facing=north,powered=false]",
        (1, 3, 6): "create:redstone_link[facing=west,receiver=false,powered=false]",
    })
    if not smart:
        blocks[(1, 3, 5)] = "create:lectern_controller[facing=north]"
    # Native LinkBehaviour fields: make the matching pair visible on the actual
    # rendered frequency slots, not only in a caption or an empty item hint.
    block_entities = {}
    for pos in ((2, 3, 7), (10, 3, 7), (1, 3, 6)):
        block_entities[pos] = {
            "id": (8, "create:redstone_link"),
            "FrequencyFirst": (10, {"id": (8, "minecraft:iron_ingot"), "count": (3, 1)}),
            "FrequencyLast": (10, {"id": (8, "minecraft:redstone"), "count": (3, 1)}),
        }
    return template(15, 9, blocks, block_entities)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify checked-in bytes without writing")
    args = parser.parse_args()
    ordinary = isolation(False)
    assets = {"isolator_basics": ordinary, "isolator_lectern": ordinary,
              "smart_controller_binding": isolation(True)}
    if args.check and {path.stem for path in OUTPUT.glob("*.nbt")} != set(assets):
        raise SystemExit("Unexpected or missing Ponder structures")
    for name, data in assets.items():
        path = OUTPUT / f"{name}.nbt"
        if args.check:
            if not path.exists() or path.read_bytes() != data:
                raise SystemExit(f"Ponder structure differs: {path}")
        else:
            path.write_bytes(data)
    print(f"{'Verified' if args.check else 'Generated'} {len(assets)} deterministic Ponder structures")


if __name__ == "__main__":
    main()
