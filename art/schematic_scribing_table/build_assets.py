"""Explicit authoring tool; not run by Gradle. Regenerates the desk assets.

Production PNGs/JSON remain directly editable. Running this script intentionally
replaces them with this art recipe. No Create/vanilla texture pixels are copied.
"""

import json
from pathlib import Path
import random

from pixel_image import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets/create_fieldwork'
TEXTURES = ASSETS / 'textures/block/schematic_scribing_table'
MODELS = ASSETS / 'models/block'
PREFIX = 'create_fieldwork:block/schematic_scribing_table/'
SLOPE = {'origin': [8, 13, 8], 'axis': 'x', 'angle': 22.5, 'rescale': False}


def noise(size, base, spread, seed):
    rng = random.Random(seed)
    image = Image(size, size)
    for y in range(size):
        for x in range(size):
            delta = rng.randrange(-spread, spread + 1)
            image.put(x, y, tuple(max(0, min(255, c + delta)) for c in base))
    return image


def author_wood_top():
    """Owner-selected original wood core: coarse clusters, with an 18% softer palette."""
    def softened(color):
        return tuple(round(channel * .82 + target * .18)
                     for channel, target in zip(color, (218, 198, 160)))

    base = softened((135, 84, 32))
    dark = softened((88, 57, 27))
    shade = softened((111, 69, 27))
    mid = softened((145, 91, 35))
    light = softened((158, 103, 42))
    highlight = softened((166, 111, 47))
    image = Image(16, 16, base)
    patches = [
        (0, 0, 16, 1, dark), (0, 1, 3, 3, dark), (3, 1, 10, 2, shade), (10, 1, 16, 2, dark),
        (2, 2, 7, 4, light), (7, 2, 12, 3, highlight), (7, 3, 15, 4, mid), (12, 2, 15, 3, light),
        (0, 4, 5, 5, shade), (5, 4, 11, 5, dark), (11, 4, 16, 5, shade),
        (1, 5, 8, 6, mid), (8, 5, 14, 7, light), (3, 6, 7, 8, light), (0, 7, 3, 8, shade),
        (0, 8, 4, 9, shade), (4, 8, 12, 9, dark), (12, 8, 16, 10, shade), (2, 9, 8, 10, shade),
        (1, 10, 5, 12, mid), (5, 10, 11, 11, light), (11, 11, 15, 13, mid), (3, 12, 8, 13, light),
        (0, 13, 6, 14, dark), (6, 13, 11, 14, shade), (11, 13, 16, 14, base),
        (2, 14, 9, 15, mid), (9, 14, 15, 15, light), (0, 15, 4, 16, shade), (12, 15, 16, 16, shade),
    ]
    for x0, y0, x1, y1, color in patches:
        image.rect(x0, y0, x1, y1, color)
    return image


def author_gold():
    """Original golden sheet with one plain square nail, no bright miniature studs."""
    image = Image(16, 16, (198, 151, 45))
    for x, y, w, h, color in [(3, 5, 3, 2, (210, 164, 54)), (8, 4, 3, 1, (185, 136, 37)),
                             (6, 8, 3, 2, (190, 141, 39)), (10, 10, 3, 2, (215, 169, 57))]:
        image.rect(x, y, x + w, y + h, color)
    image.rect(0, 0, 16, 2, (244, 215, 121))
    image.rect(0, 2, 2, 16, (222, 178, 73))
    image.rect(14, 2, 16, 16, (122, 85, 30))
    image.rect(2, 14, 14, 16, (149, 105, 32))
    image.rect(7, 7, 9, 9, (76, 81, 70))
    return image


def author_textures():
    textures = {}
    textures['wood_top'] = author_wood_top()
    textures['wood_side'] = noise(16, (95, 63, 39), 6, 12)

    iron = noise(16, (59, 72, 69), 4, 13)
    iron.rect(0, 0, 16, 1, (112, 128, 118))
    iron.rect(0, 1, 1, 16, (89, 106, 99))
    iron.rect(15, 1, 16, 16, (35, 44, 43))
    iron.rect(1, 15, 15, 16, (42, 51, 48))
    for x, y in [(2, 2), (12, 2), (2, 12), (12, 12)]:
        iron.rect(x, y, x + 2, y + 2, (32, 41, 39))
        iron.put(x, y, (163, 160, 131))
    textures['iron'] = iron

    textures['gold'] = author_gold()

    cast = noise(16, (111, 115, 103), 12, 15)
    cast.rect(0, 0, 16, 1, (171, 172, 148))
    cast.rect(0, 1, 1, 16, (146, 152, 133))
    cast.rect(15, 0, 16, 16, (67, 77, 69))
    cast.rect(0, 15, 15, 16, (77, 87, 76))
    textures['cast'] = cast

    roller = noise(16, (215, 204, 163), 3, 16)
    for y in (0, 1, 13, 15):
        roller.rect(0, y, 16, y + 1, (167, 151, 111))
    roller.rect(0, 3, 16, 5, (237, 229, 195))
    textures['roller'] = roller

    blueprint = Image(32, 32, (43, 88, 111))
    for n in range(2, 32, 4):
        blueprint.rect(n, 2, n + 1, 30, (54, 104, 125))
        blueprint.rect(2, n, 30, n + 1, (54, 104, 125))
    blueprint.line(1, 1, 30, 1, (154, 194, 196))
    blueprint.line(1, 1, 1, 30, (154, 194, 196))
    blueprint.line(1, 30, 30, 30, (154, 194, 196))
    blueprint.line(30, 1, 30, 30, (154, 194, 196))
    outline = [(6, 7), (26, 7), (26, 24), (18, 24), (18, 19), (6, 19), (6, 7)]
    for a, b in zip(outline, outline[1:]):
        blueprint.line(*a, *b, (215, 229, 210))
    for coords in [(16, 7, 16, 14), (16, 14, 26, 14), (9, 10, 12, 10),
                   (9, 10, 9, 15), (9, 15, 12, 15), (12, 10, 12, 15),
                   (5, 26, 15, 26), (5, 28, 11, 28)]:
        blueprint.line(*coords, (188, 214, 203))
    textures['blueprint'] = blueprint

    clipboard = Image(16, 16, (68, 84, 63))
    clipboard.rect(1, 2, 15, 15, (222, 213, 173))
    clipboard.rect(2, 3, 14, 14, (235, 225, 188))
    clipboard.rect(5, 0, 11, 3, (174, 131, 58))
    clipboard.rect(6, 0, 10, 1, (233, 200, 115))
    for y in (4, 7, 10):
        clipboard.rect(3, y, 6, y + 3, (105, 117, 84))
        clipboard.put(4, y + 1, (233, 225, 188))
        clipboard.rect(8, y + 1, 13, y + 2, (121, 126, 92))
    textures['clipboard'] = clipboard
    TEXTURES.mkdir(parents=True, exist_ok=True)
    # The owner-selected gold finish supersedes this specific former material.
    (TEXTURES / 'brass.png').unlink(missing_ok=True)
    for name, image in textures.items():
        image.save(TEXTURES / f'{name}.png')
    return textures


def element(name, low, high, texture, sloped=False, top=None, uv=None):
    low = [round(value, 4) for value in low]
    high = [round(value, 4) for value in high]
    x, y, z = low
    X, Y, Z = high
    sizes = {'down': (X-x, Z-z), 'up': (X-x, Z-z),
             'north': (X-x, Y-y), 'south': (X-x, Y-y),
             'west': (Z-z, Y-y), 'east': (Z-z, Y-y)}
    faces = {}
    for face, (w, h) in sizes.items():
        face_uv = [0, 0, round(w, 4), round(h, 4)]
        if face == 'up' and uv:
            face_uv = uv
        faces[face] = {'uv': face_uv, 'texture': '#' + (top if face == 'up' and top else texture)}
    result = {'name': name, 'from': low, 'to': high, 'faces': faces}
    if sloped:
        result['rotation'] = dict(SLOPE)
    return result


def desk_geometry():
    parts = []
    def add(name, low, high, texture, **kwargs):
        parts.append(element(name, low, high, texture, **kwargs))

    for x, side in [(1.1, 'left'), (13.1, 'right')]:
        for z, position, height in [(2.0, 'rear', 14.25), (11.8, 'front', 10.55)]:
            add(f'{side}_{position}_leg', [x, .5, z], [x+1.45, height, z+1.45], 'iron')
            add(f'{side}_{position}_cast_foot', [x-.45, 0, z-.4], [x+1.9, .7, z+1.85], 'cast', uv=[0, 0, 16, 16])
            add(f'{side}_{position}_cast_saddle', [x-.12, height-.8, z-.12],
                [x+1.57, height, z+1.57], 'cast', uv=[0, 0, 16, 16])
            add(f'{side}_{position}_gold_collar', [x-.1, 2.5, z-.1], [x+1.55, 3.15, z+1.55], 'gold')
        add(f'{side}_low_stretcher', [x+.2, 3.2, 3], [x+1.25, 4.1, 12], 'iron')
        add(f'{side}_underdesk_support', [x+.1, 10.2, 3], [x+1.35, 11, 11.8], 'iron')
    add('front_crossbar', [2, 3.25, 12], [14, 4.25, 13], 'iron')
    add('rear_crossbar', [2, 5, 2.3], [14, 6, 3.3], 'iron')
    add('front_crossbar_gold_plate', [6.5, 3.4, 13], [9.5, 4.1, 13.18], 'gold')

    add('desktop_metal_frame', [.5, 12.15, 1.25], [15.5, 12.9, 14.75], 'iron', sloped=True)
    add('desktop_inset_wood', [1.1, 12.85, 1.9], [14.9, 13, 14.1], 'wood_side',
        sloped=True, top='wood_top', uv=[0, 0, 16, 16])
    for x in [.5, 14.2]:
        for z in [1.25, 13.5]:
            add(f'corner_cap_{x}_{z}', [x, 12.9, z], [x+1.3, 13.14, z+1.25],
                'gold', sloped=True, uv=[0, 0, 16, 16])
    add('front_paper_stop', [2, 13, 13.8], [13.8, 13.22, 14.1], 'gold', sloped=True)

    for x, side in [(1.1, 'left'), (10.2, 'right')]:
        add(f'{side}_roller_bearing', [x, 13, 2.2], [x+1.1, 13.75, 3.6], 'gold', sloped=True)
        add(f'{side}_bearing_cap', [x+.2, 13.75, 2.45], [x+.9, 13.9, 3.35], 'iron', sloped=True)
    add('paper_feed_roller', [2.2, 13.15, 2.45], [10.2, 13.75, 3.35], 'roller', sloped=True)
    add('roller_drive_axle', [11.3, 13.26, 2.6], [13.5, 13.58, 3.15], 'iron', sloped=True)
    add('feed_drive_housing', [12.2, 13, 2.2], [14, 13.8, 3.6], 'cast', sloped=True)
    add('feed_drive_gold_cover', [12.4, 13.8, 2.35], [13.8, 13.9, 3.45], 'gold', sloped=True)
    add('crank_spindle', [14, 13.3, 2.65], [15.25, 13.62, 3.15], 'gold', sloped=True)
    add('crank_arm', [14.9, 12.2, 2.65], [15.25, 13.62, 3.15], 'iron', sloped=True)
    add('crank_wood_grip', [15.05, 12.1, 2.5], [15.8, 12.6, 3.3], 'wood_side', sloped=True)
    return parts


def write_models(textures):
    base = desk_geometry()
    blueprint = element('inserted_schematic', [1.65, 13.025, 3.8], [10.65, 13.075, 12.9],
                        'blueprint', sloped=True, uv=[0, 0, 16, 16])
    clipboard = element('inserted_clipboard', [11.05, 13.025, 5.4], [14.4, 13.15, 11.7],
                        'iron', top='clipboard', sloped=True, uv=[0, 0, 16, 16])
    variants = {'': [], '_schematic': [blueprint], '_clipboard': [clipboard],
                '_schematic_clipboard': [blueprint, clipboard]}
    for suffix, inputs in variants.items():
        model = {'parent': 'minecraft:block/block', 'ambientocclusion': True,
                 'gui_light': 'front',
                 'textures': {name: PREFIX + name for name in textures},
                 'elements': base + inputs}
        model['textures']['particle'] = PREFIX + 'iron'
        path = MODELS / f'schematic_scribing_table{suffix}.json'
        path.write_text(json.dumps(model, indent=2) + '\n')
    item = ASSETS / 'models/item/schematic_scribing_table.json'
    item.write_text(json.dumps({'parent': 'create_fieldwork:block/schematic_scribing_table'}, indent=2) + '\n')
    print(f'Wrote {len(variants)} block models, item model, {len(textures)} original textures')


if __name__ == '__main__':
    write_models(author_textures())
