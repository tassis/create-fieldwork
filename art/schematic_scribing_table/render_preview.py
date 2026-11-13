"""Orthographic software preview of the *written* Minecraft JSON and PNG assets.

No Minecraft lighting/AO/model-baking claim: this is a local visual inspection
aid. Uses only Python's standard library; does not import the authoring recipe.
"""

import json
import math
from pathlib import Path

from pixel_image import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'src/main/resources/assets'
OUTPUT = Path(__file__).resolve().parent
INK = (60, 73, 65)
MUTED = (108, 119, 100)
BG = (234, 230, 213)
FONT = {
    'A': ['01110','10001','10001','11111','10001','10001','10001'],
    'B': ['11110','10001','10001','11110','10001','10001','11110'],
    'C': ['01111','10000','10000','10000','10000','10000','01111'],
    'D': ['11110','10001','10001','10001','10001','10001','11110'],
    'E': ['11111','10000','10000','11110','10000','10000','11111'],
    'F': ['11111','10000','10000','11110','10000','10000','10000'],
    'G': ['01111','10000','10000','10111','10001','10001','01111'],
    'H': ['10001','10001','10001','11111','10001','10001','10001'],
    'I': ['11111','00100','00100','00100','00100','00100','11111'],
    'J': ['00111','00010','00010','00010','10010','10010','01100'],
    'K': ['10001','10010','10100','11000','10100','10010','10001'],
    'L': ['10000','10000','10000','10000','10000','10000','11111'],
    'M': ['10001','11011','10101','10101','10001','10001','10001'],
    'N': ['10001','11001','10101','10011','10001','10001','10001'],
    'O': ['01110','10001','10001','10001','10001','10001','01110'],
    'P': ['11110','10001','10001','11110','10000','10000','10000'],
    'Q': ['01110','10001','10001','10001','10101','10010','01101'],
    'R': ['11110','10001','10001','11110','10100','10010','10001'],
    'S': ['01111','10000','10000','01110','00001','00001','11110'],
    'T': ['11111','00100','00100','00100','00100','00100','00100'],
    'U': ['10001','10001','10001','10001','10001','10001','01110'],
    'V': ['10001','10001','10001','10001','10001','01010','00100'],
    'W': ['10001','10001','10001','10101','10101','10101','01010'],
    'X': ['10001','10001','01010','00100','01010','10001','10001'],
    'Y': ['10001','10001','01010','00100','00100','00100','00100'],
    'Z': ['11111','00001','00010','00100','01000','10000','11111'],
    '0': ['01110','10001','10011','10101','11001','10001','01110'],
    '1': ['00100','01100','00100','00100','00100','00100','01110'],
    '2': ['01110','10001','00001','00010','00100','01000','11111'],
    '3': ['11110','00001','00001','01110','00001','00001','11110'],
    '4': ['00010','00110','01010','10010','11111','00010','00010'],
    '5': ['11111','10000','10000','11110','00001','00001','11110'],
    '6': ['01110','10000','10000','11110','10001','10001','01110'],
    '7': ['11111','00001','00010','00100','01000','01000','01000'],
    '8': ['01110','10001','10001','01110','10001','10001','01110'],
    '9': ['01110','10001','10001','01111','00001','00001','01110'],
    '/': ['00001','00001','00010','00100','01000','10000','10000'],
    '-': ['00000','00000','00000','11111','00000','00000','00000'],
    '+': ['00000','00100','00100','11111','00100','00100','00000'],
    '.': ['00000','00000','00000','00000','00000','00110','00110'],
    ':': ['00000','00110','00110','00000','00110','00110','00000'],
}


def text(canvas, x, y, message, scale=2, color=INK):
    for char in message.upper():
        glyph = FONT.get(char, ['00000'] * 7)
        for row, pixels in enumerate(glyph):
            for col, pixel in enumerate(pixels):
                if pixel == '1':
                    canvas.rect(x+col*scale, y+row*scale, x+(col+1)*scale, y+(row+1)*scale, color)
        x += 6 * scale


class TextureSource:
    """Read only this desk's locally authored Fieldwork resources."""

    def __init__(self):
        self.cache = {}

    def image(self, identifier):
        if identifier not in self.cache:
            namespace, name = identifier.split(':')
            if namespace != 'create_fieldwork':
                raise ValueError(f'Unsupported preview texture: {identifier}')
            image = Image.load(ASSETS/namespace/'textures'/(name+'.png'))
            self.cache[identifier] = image
        return self.cache[identifier]


def rotate(point, rotation):
    if not rotation:
        return point
    if rotation.get('rescale', False):
        raise ValueError('Preview does not implement element rescale')
    axis = {'x': 0, 'y': 1, 'z': 2}[rotation['axis']]
    a, b = (axis + 1) % 3, (axis + 2) % 3
    origin = rotation['origin']
    result = [point[i] - origin[i] for i in range(3)]
    cosine = math.cos(math.radians(rotation['angle']))
    sine = math.sin(math.radians(rotation['angle']))
    result[a], result[b] = result[a]*cosine-result[b]*sine, result[a]*sine+result[b]*cosine
    return tuple(result[i] + origin[i] for i in range(3))


def faces(element):
    x, y, z = element['from']
    X, Y, Z = element['to']
    return {
        'up': [(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],
        'down': [(x,y,Z),(X,y,Z),(X,y,z),(x,y,z)],
        'south': [(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],
        'north': [(X,Y,z),(x,Y,z),(x,y,z),(X,y,z)],
        'east': [(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],
        'west': [(x,Y,z),(x,Y,Z),(x,y,Z),(x,y,z)],
    }


def triangle(canvas, depths, points, uv, texture, shade):
    (ax,ay,az),(bx,by,bz),(cx,cy,cz) = points
    det = (by-cy)*(ax-cx)+(cx-bx)*(ay-cy)
    if abs(det) < .00001:
        return
    x0,x1 = max(0,math.floor(min(ax,bx,cx))),min(canvas.width-1,math.ceil(max(ax,bx,cx)))
    y0,y1 = max(0,math.floor(min(ay,by,cy))),min(canvas.height-1,math.ceil(max(ay,by,cy)))
    colors = [tuple(min(255,round(c*shade)) for c in texture.get(x,y))
              for y in range(texture.height) for x in range(texture.width)]
    for y in range(y0,y1+1):
        for x in range(x0,x1+1):
            a = ((by-cy)*(x+.5-cx)+(cx-bx)*(y+.5-cy))/det
            b = ((cy-ay)*(x+.5-cx)+(ax-cx)*(y+.5-cy))/det
            c = 1-a-b
            if min(a,b,c) < -.000001:
                continue
            depth = a*az+b*bz+c*cz
            index = y*canvas.width+x
            if depth <= depths[index]:
                continue
            u = a*uv[0][0]+b*uv[1][0]+c*uv[2][0]
            v = a*uv[0][1]+b*uv[1][1]+c*uv[2][1]
            tx = min(texture.width-1,max(0,int(u*texture.width/16)))
            ty = min(texture.height-1,max(0,int(v*texture.height/16)))
            canvas.put(x,y,colors[ty*texture.width+tx])
            depths[index] = depth


def render(canvas, source, model_path, center, scale, yaw=45, elevation=28):
    model = json.loads(model_path.read_text())
    textures = {key: source.image(value) for key,value in model['textures'].items()}
    yaw,elevation = math.radians(yaw),math.radians(elevation)
    sy,cy,se,ce = math.sin(yaw),math.cos(yaw),math.sin(elevation),math.cos(elevation)
    def project(point):
        x,y,z = (point[i]-8 for i in range(3))
        return (center[0]+scale*(sy*x-cy*z),
                center[1]+scale*(cy*se*x-ce*y+sy*se*z),
                cy*ce*x+se*y+sy*ce*z)

    depths = [float('-inf')] * (canvas.width * canvas.height)
    normals = {'up':(0,1,0),'down':(0,-1,0),'north':(0,0,-1),
               'south':(0,0,1),'east':(1,0,0),'west':(-1,0,0)}
    for element in model['elements']:
        rotation = element.get('rotation')
        for direction, vertices in faces(element).items():
            face = element['faces'].get(direction)
            if not face:
                continue
            normal = normals[direction]
            if rotation:
                normal = rotate(normal, dict(rotation, origin=[0,0,0]))
            if normal[0]*cy*ce+normal[1]*se+normal[2]*sy*ce <= .0001:
                continue
            shade = .64 + .36 * max(0, -.25*normal[0]+.90*normal[1]+.35*normal[2])
            u,v,U,V = face['uv']
            uv = [(u,v),(U,v),(U,V),(u,V)]
            if face.get('rotation',0):
                raise ValueError('Preview does not implement face UV rotation')
            points = [project(rotate(vertex,rotation)) for vertex in vertices]
            texture = textures[face['texture'][1:]]
            for indices in [(0,1,2),(0,2,3)]:
                triangle(canvas,depths,[points[i] for i in indices],[uv[i] for i in indices],texture,shade)


def shadow(canvas,cx,cy,rx,ry):
    for y in range(int(cy-ry),int(cy+ry)):
        for x in range(int(cx-rx),int(cx+rx)):
            r = ((x-cx)/rx)**2+((y-cy)/ry)**2
            if r < 1:
                alpha = .16 * (1-r)
                base = canvas.get(x,y)
                canvas.put(x,y,tuple(round(c*(1-alpha)+75*alpha) for c in base))


def main():
    source = TextureSource()
    canvas = Image(1500,1080,BG)
    for n in range(26,1500,32):
        canvas.line(n,26,n,1054,(225,221,204))
    for n in range(26,1080,32):
        canvas.line(26,n,1474,n,(225,221,204))
    text(canvas,58,49,'CREATE: FIELDWORK / 0.3.0 VISUAL STUDY',2,MUTED)
    text(canvas,56,87,'SCHEMATIC SCRIBING TABLE',5)
    text(canvas,59,141,'C-01 / SELECTED WOOD + GOLDEN SHEET + FLAT NAILS',2,MUTED)
    canvas.line(58,177,1440,177,(177,175,151),2)
    shadow(canvas,510,843,330,63)
    base = ASSETS / 'create_fieldwork/models/block'
    render(canvas,source,base/'schematic_scribing_table.json',(508,552),31)
    text(canvas,72,215,'GOLDEN SHEET + HAND FEED',2)
    canvas.line(322,238,512,275,(139,148,116),2)
    text(canvas,73,871,'OPEN FRAME / CAST FEET',2)
    text(canvas,73,904,'ORIGINAL WOOD / FIELDWORK TEXTURE',2,MUTED)
    canvas.line(988,207,988,936,(177,175,151),2)
    text(canvas,1030,219,'WITH INSERTS',2)
    shadow(canvas,1230,493,155,25)
    render(canvas,source,base/'schematic_scribing_table_schematic_clipboard.json',(1220,380),15,yaw=52)
    text(canvas,1030,543,'ALTERNATE VIEW',2)
    shadow(canvas,1230,819,155,25)
    render(canvas,source,base/'schematic_scribing_table_schematic_clipboard.json',(1220,706),14,yaw=110,elevation=35)
    text(canvas,1030,888,'ONE BLOCK / 22.5 DEG TOP',2,MUTED)
    text(canvas,1030,920,'NO POWER INPUT IMPLIED',2,MUTED)
    canvas.line(58,957,1440,957,(177,175,151),2)
    text(canvas,59,983,'RENDERED FROM MODEL JSON + PIXEL PNGS',2)
    text(canvas,59,1015,'SOFTWARE PREVIEW - NOT AN IN-GAME SCREENSHOT',2,MUTED)
    text(canvas,1100,1015,'FIELDWORK ART / C-01',2,MUTED)
    canvas.save(OUTPUT/'preview.png')

    sheet = Image(1120,560,BG)
    text(sheet,32,28,'ORIGINAL FIELDWORK PIXEL TEXTURES',3)
    paths = sorted((ASSETS/'create_fieldwork/textures/block/schematic_scribing_table').glob('*.png'))
    entries = [(path.stem.replace('_',' '),Image.load(path),[0,0,16,16]) for path in paths]
    for index,(label,texture,uv) in enumerate(entries):
        col,row = index%4,index//4
        ox,oy = 32+col*277,92+row*235
        for y in range(160):
            for x in range(160):
                tx = int((uv[0]+x*(uv[2]-uv[0])/160)*texture.width/16)
                ty = int((uv[1]+y*(uv[3]-uv[1])/160)*texture.height/16)
                sheet.put(ox+x,oy+y,texture.get(tx,ty))
        text(sheet,ox,oy+176,label,2)
        caption = f'{texture.width} X {texture.height}'
        text(sheet,ox,oy+199,caption,2,MUTED)
    sheet.save(OUTPUT/'texture-sheet.png')
    print(OUTPUT/'preview.png')
    print(OUTPUT/'texture-sheet.png')


if __name__ == '__main__':
    main()
