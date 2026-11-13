"""Dependency-free RGB output and RGB/RGBA/indexed-8 PNG input for this art study."""

import struct
import zlib


class Image:
    def __init__(self, width, height, color=(0, 0, 0)):
        self.width = width
        self.height = height
        self.pixels = bytearray(color * (width * height))

    def get(self, x, y):
        offset = (y * self.width + x) * 3
        return tuple(self.pixels[offset:offset + 3])

    def put(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            offset = (y * self.width + x) * 3
            self.pixels[offset:offset + 3] = bytes(color)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(max(0, int(y0)), min(self.height, int(y1))):
            for x in range(max(0, int(x0)), min(self.width, int(x1))):
                self.put(x, y, color)

    def line(self, x0, y0, x1, y1, color, width=1):
        steps = max(abs(int(x1 - x0)), abs(int(y1 - y0)), 1)
        for i in range(steps + 1):
            x = round(x0 + (x1 - x0) * i / steps)
            y = round(y0 + (y1 - y0) * i / steps)
            self.rect(x, y, x + width, y + width, color)

    def save(self, path):
        def chunk(kind, data):
            return struct.pack('!I', len(data)) + kind + data + struct.pack(
                '!I', zlib.crc32(kind + data) & 0xffffffff)

        stride = self.width * 3
        scanlines = b''.join(b'\0' + self.pixels[y * stride:(y + 1) * stride]
                             for y in range(self.height))
        data = b'\x89PNG\r\n\x1a\n'
        data += chunk(b'IHDR', struct.pack('!2I5B', self.width, self.height, 8, 2, 0, 0, 0))
        data += chunk(b'IDAT', zlib.compress(scanlines, 9))
        data += chunk(b'IEND', b'')
        path.write_bytes(data)

    @classmethod
    def load(cls, path):
        return cls.from_bytes(path.read_bytes())

    @classmethod
    def from_bytes(cls, data):
        if data[:8] != b'\x89PNG\r\n\x1a\n':
            raise ValueError('Not a PNG image')
        offset, compressed = 8, bytearray()
        width, height, channels = 0, 0, 0
        palette = None
        while offset < len(data):
            size = struct.unpack_from('!I', data, offset)[0]
            kind = data[offset + 4:offset + 8]
            payload = data[offset + 8:offset + 8 + size]
            if kind == b'IHDR':
                width, height, bits, mode, _, _, interlace = struct.unpack('!2I5B', payload)
                if bits != 8 or mode not in (2, 3, 6) or interlace:
                    raise ValueError('Preview accepts non-interlaced RGB/RGBA/indexed-8 PNGs')
                channels = {2: 3, 3: 1, 6: 4}[mode]
            elif kind == b'PLTE':
                palette = [payload[i:i + 3] for i in range(0, len(payload), 3)]
            elif kind == b'tRNS' and any(alpha != 255 for alpha in payload):
                raise ValueError('Preview does not implement transparent indexed textures')
            elif kind == b'IDAT':
                compressed.extend(payload)
            offset += size + 12

        if not width or not height or not channels:
            raise ValueError('PNG has no supported image header')
        raw = zlib.decompress(compressed)
        image = cls(width, height)
        stride = width * channels
        previous = bytearray(stride)
        for y in range(height):
            pos = y * (stride + 1)
            filtering = raw[pos]
            row = bytearray(raw[pos + 1:pos + 1 + stride])
            for i in range(stride):
                a = row[i - channels] if i >= channels else 0
                b = previous[i]
                c = previous[i - channels] if i >= channels else 0
                if filtering == 1:
                    predict = a
                elif filtering == 2:
                    predict = b
                elif filtering == 3:
                    predict = (a + b) // 2
                elif filtering == 4:
                    guess = a + b - c
                    distances = [abs(guess - v) for v in (a, b, c)]
                    predict = (a, b, c)[distances.index(min(distances))]
                elif filtering == 0:
                    predict = 0
                else:
                    raise ValueError(f'Unsupported PNG filter {filtering}')
                row[i] = (row[i] + predict) & 255
            for x in range(width):
                if channels == 1:
                    if palette is None:
                        raise ValueError('Indexed PNG has no palette')
                    image.put(x, y, palette[row[x]])
                else:
                    image.put(x, y, row[x * channels:x * channels + 3])
            previous = row
        return image
