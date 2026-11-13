"""Explicit original UI background authoring; no text, icons, widgets or dependency art.

The approved layout is shared by production Java; resource tests check this
background's item wells against those coordinates. Does not run from Gradle.
"""
from pathlib import Path
import struct
import zlib

from pixel_image import Image

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT/'src/main/resources/assets/create_fieldwork'
SIZE = 256
TRANSPARENT = (1,2,3)
PAPER = (241,239,221)
EDGE = (153,127,96)
SLOTS = [(66,64),(176,44),(176,94)]
SLOTS += [(35+col*18,172+row*18) for row in range(3) for col in range(9)]
SLOTS += [(35+col*18,230) for col in range(9)]


def blend(color,target,amount):
    return tuple(round(a*(1-amount)+b*amount) for a,b in zip(color,target))


def bevel(image,x,y,w,h,fill,light,dark):
    image.rect(x,y,x+w,y+h,dark)
    image.rect(x+1,y+1,x+w-1,y+h-1,fill)
    image.line(x,y,x+w-2,y,light)
    image.line(x,y,x,y+h-2,light)


def paper(image,x,y,w,h,fill=PAPER):
    bevel(image,x,y,w,h,fill,(255,251,235),EDGE)
    image.line(x+2,y+h-2,x+w-2,y+h-2,(206,190,162))


def author():
    image = Image(SIZE,SIZE,TRANSPARENT)
    source = ASSETS/'textures/block/schematic_scribing_table'
    wood = Image.load(source/'wood_top.png')
    blueprint = Image.load(source/'blueprint.png')
    blue = blend(blueprint.get(0,0),PAPER,.22)
    grid = blend(blueprint.get(2,3),PAPER,.22)
    major = blend(blueprint.get(1,1),blue,.35)
    bevel(image,4,16,224,238,(115,126,123),(186,195,188),(38,44,42))
    wood_base = wood.get(15,5)
    for y in range(21,248):
        for x in range(10,222):
            grain = wood.get(((x-10)//2)%wood.width,((y-21)//2)%wood.height)
            image.put(x,y,blend(wood_base,grain,.45))
    image.line(9,22,9,247,(63,74,70))
    image.line(222,22,222,247,(172,183,176))
    image.line(11,247,221,247,(181,189,179))
    for x,y,w,color in [(5,46,3,(139,149,143)),(5,118,3,(98,111,105)),
                         (224,74,2,(142,153,145)),(224,184,2,(96,110,104)),
                         (27,250,14,(142,151,143)),(161,251,19,(99,112,106))]:
        image.rect(x,y,x+w,y+1,color)
    paper(image,5,1,222,19,(239,229,207))
    image.rect(0,5,6,16,EDGE)
    image.rect(1,6,5,15,(233,217,188))
    image.rect(226,5,232,16,EDGE)
    image.rect(227,6,231,15,(233,217,188))
    image.rect(0,9,2,12,TRANSPARENT)
    image.rect(230,9,232,12,TRANSPARENT)
    image.rect(12,29,140,120,(94,70,44))
    image.rect(10,27,138,118,major)
    image.rect(11,28,137,117,blue)
    for x in range(15,137,8):
        image.line(x,29,x,116,grid)
    for y in range(31,117,8):
        image.line(12,y,136,y,grid)
    for x in range(18,136,16):
        image.line(x,28,x,31,(215,229,210))
    for y in range(34,116,8):
        image.line(12,y,15,y,(215,229,210))
    for x,y,sx,sy in [(55,54,1,1),(93,54,-1,1),(55,88,1,-1),(93,88,-1,-1)]:
        image.line(x,y,x+sx*5,y,(220,231,211))
        image.line(x,y,x,y+sy*5,(220,231,211))
    image.rect(134,27,138,29,(208,226,212))
    image.rect(136,29,138,31,(208,226,212))
    image.rect(98,71,143,73,(193,211,193))
    image.rect(141,68,143,76,(193,211,193))
    image.rect(143,69,145,75,(193,211,193))
    paper(image,146,27,76,91)
    # Native server progress fills this vertical channel from top to bottom.
    bevel(image,180,66,8,24,(216,207,180),(253,247,224),(141,125,98))
    image.rect(183,70,185,85,(179,167,134))
    image.rect(181,82,187,84,(179,167,134))
    image.rect(182,84,186,86,(179,167,134))
    image.rect(183,86,185,88,(179,167,134))
    paper(image,10,120,212,16)
    # Status, button faces, labels and all item/count graphics are runtime layers.
    paper(image,28,160,176,89,(237,235,216))
    for x,y in SLOTS:
        image.rect(x-1,y-1,x+17,y+17,(253,249,232))
        image.rect(x-1,y-1,x+16,y+16,(108,104,91))
        image.rect(x,y,x+16,y+16,(166,167,154))
    return image


def save_rgba(image,path):
    def chunk(kind,data):
        return struct.pack('!I',len(data))+kind+data+struct.pack('!I',zlib.crc32(kind+data)&0xffffffff)
    rows = bytearray()
    for y in range(image.height):
        rows.append(0)
        for x in range(image.width):
            pixel = image.get(x,y)
            rows.extend((*pixel,0 if pixel == TRANSPARENT else 255))
    data = b'\x89PNG\r\n\x1a\n'
    data += chunk(b'IHDR',struct.pack('!2I5B',image.width,image.height,8,6,0,0,0))
    data += chunk(b'IDAT',zlib.compress(rows,9))+chunk(b'IEND',b'')
    path.write_bytes(data)


if __name__ == '__main__':
    destination = ASSETS/'textures/gui/schematic_scribing_table.png'
    destination.parent.mkdir(parents=True,exist_ok=True)
    save_rgba(author(),destination)
    print(destination)
