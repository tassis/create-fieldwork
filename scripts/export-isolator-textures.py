#!/usr/bin/env python3
"""Generate original, editable six-face PNG placeholders without copying game textures.

By default this exports to art/ only. --install explicitly copies the seven
editable faces into main resources; existing PNGs are protected unless --force
is supplied. It never builds or replaces a JAR.
"""
import argparse
import json
import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "redstone_link_isolator"
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "create_fieldwork"
INSTALLED = ASSETS / "textures" / "block" / "redstone_link_isolator"
FACES = ("north", "south", "east", "west", "up", "down", "up_active")
SIZES = {name + ".png": (16, 16) for name in FACES}
SIZES.update({"cube_net.png": (64, 48), "preview.png": (512, 384)})
TRANSPARENT = (0, 0, 0, 0)


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def png(pixels):
    height, width = len(pixels), len(pixels[0])
    raw = b"".join(b"\0" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def texture(name, seed):
    rng = random.Random(seed)
    image = []
    for y in range(16):
        row = []
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            noise = rng.choice((-3, -1, 0, 2, 4))
            if name == "up":
                base = (66, 49, 29) if edge else (183, 139, 62)
            elif name == "up_active":
                base = (70, 28, 28) if edge else (183, 47, 40)
            else:
                base = (17, 15, 22) if edge else (44, 35, 55)
            row.append(tuple(max(0, min(255, value + noise)) for value in base) + (255,))
        image.append(row)
    # Independent original placeholder panel, rim and four corner fasteners.
    for x in range(3, 13):
        for y in (3, 12):
            image[y][x] = (93, 71, 42, 255)
    for y in range(3, 13):
        for x in (3, 12):
            image[y][x] = (93, 71, 42, 255)
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        image[y][x] = (220, 179, 89, 255)
    if name in ("up", "up_active"):
        color = (246, 92, 66, 255) if name == "up_active" else (71, 51, 31, 255)
        for y in range(6, 10):
            for x in range(6, 10):
                image[y][x] = color
    if name == "down":
        for i in range(5, 11):
            image[7][i] = image[8][i] = (111, 86, 46, 255)
            image[i][7] = image[i][8] = (111, 86, 46, 255)
    return image


def check_png(path, expected_size):
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path.name}: invalid PNG signature")
    offset, size, compressed, ended = 8, None, bytearray(), False
    ihdr = None
    while offset < len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4:offset + 8]
        payload = data[offset + 8:offset + 8 + length]
        crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        if len(payload) != length or zlib.crc32(kind + payload) & 0xFFFFFFFF != crc:
            raise ValueError(f"{path.name}: invalid chunk/CRC")
        if kind == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", payload)
            size = ihdr[:2]
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            ended = True
            break
        offset += length + 12
    if ihdr is None or size != expected_size or not ended:
        raise ValueError(f"{path.name}: wrong dimensions or missing IEND")
    width, height, depth, color, _, _, interlace = ihdr
    raw = zlib.decompress(compressed)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color)
    if not channels:
        raise ValueError(f"{path.name}: invalid color type")
    if interlace == 0:
        stride = (width * depth * channels + 7) // 8 + 1
        if len(raw) != stride * height or any(raw[y * stride] > 4 for y in range(height)):
            raise ValueError(f"{path.name}: invalid pixel data")
    print(f"OK {path.name}: {width}x{height}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="replace existing PNG outputs in the selected mode")
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--check", action="store_true", help="validate exported PNGs without writing")
    modes.add_argument("--install", action="store_true", help="copy editable face PNGs into main texture resources")
    modes.add_argument("--check-installed", action="store_true", help="validate main textures and model references without writing")
    args = parser.parse_args()
    if not (args.check or args.install or args.check_installed):
        existing = [name for name in SIZES if (OUT / name).exists()]
        if existing and not args.force:
            parser.error("PNG artwork already exists; refusing to overwrite. Use --check, or explicitly --force.")
        OUT.mkdir(parents=True, exist_ok=True)
        textures = {name: texture(name, 20261107 + i) for i, name in enumerate(FACES)}
        for name, pixels in textures.items():
            (OUT / (name + ".png")).write_bytes(png(pixels))
        net = [[TRANSPARENT] * 64 for _ in range(48)]
        layout = {"up": (16, 0), "west": (0, 16), "north": (16, 16), "east": (32, 16), "south": (48, 16), "down": (16, 32)}
        for name, (left, top) in layout.items():
            for y, row in enumerate(textures[name]):
                net[top + y][left:left + 16] = row
        (OUT / "cube_net.png").write_bytes(png(net))
        enlarged = [[net[y // 8][x // 8] for x in range(512)] for y in range(384)]
        (OUT / "preview.png").write_bytes(png(enlarged))
    if not args.check_installed:
        for name, size in SIZES.items():
            check_png(OUT / name, size)
    if args.install:
        existing = [name for name in FACES if (INSTALLED / (name + ".png")).exists()]
        if existing and not args.force:
            parser.error("Main texture PNGs already exist; refusing to overwrite. Use --check-installed, or explicitly --force.")
        INSTALLED.mkdir(parents=True, exist_ok=True)
        for name in FACES:
            (INSTALLED / (name + ".png")).write_bytes((OUT / (name + ".png")).read_bytes())
    if args.install or args.check_installed:
        for name in FACES:
            path = INSTALLED / (name + ".png")
            check_png(path, (16, 16))
            if args.install and path.read_bytes() != (OUT / (name + ".png")).read_bytes():
                raise ValueError(f"{name}: main texture differs from editable art source")
        base = json.loads((ASSETS / "models" / "block" / "redstone_link_isolator.json").read_text())
        active = json.loads((ASSETS / "models" / "block" / "redstone_link_isolator_active.json").read_text())
        prefix = "create_fieldwork:block/redstone_link_isolator"
        if base.get("parent") != "minecraft:block/cube":
            raise ValueError("Inactive model must use the six-face cube parent")
        for name in FACES[:6]:
            if base.get("textures", {}).get(name) != prefix + "/" + name:
                raise ValueError(f"Inactive model is missing the expected {name} face")
        if base.get("textures", {}).get("particle") != prefix + "/north":
            raise ValueError("Particle texture reference is missing")
        if active.get("parent") != prefix or active.get("textures") != {"up": prefix + "/up_active"}:
            raise ValueError("Active model must inherit all faces and override only up")
        print("OK main six-face model references and active top override")


if __name__ == "__main__":
    main()
