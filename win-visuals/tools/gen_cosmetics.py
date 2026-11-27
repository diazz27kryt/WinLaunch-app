#!/usr/bin/env python3
"""Generates the textures of the Win Visuals cosmetics (wing and a plain white texel).

Run from win-visuals/:  python3 tools/gen_cosmetics.py
The wing is the right-hand one: its root is at the left edge (u = ROOT_U, v = ROOT_V).
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "winvisuals",
                   "textures", "cosmetic")
S = 1024
ROOT = (40.0, 560.0)


def bezier(p0, p1, p2, t):
    return tuple((1 - t) ** 2 * a + 2 * (1 - t) * t * b + t ** 2 * c for a, b, c in zip(p0, p1, p2))


def feather(canvas, start, angle, length, width, base, tip):
    """A long pointed blade with a bright quill, a darker lower edge and a colour gradient to the tip."""
    a = math.radians(angle)
    ux, uy = math.cos(a), -math.sin(a)
    nx, ny = -uy, ux
    end = (start[0] + ux * length, start[1] + uy * length)
    mid = (start[0] + ux * length * 0.5 - nx * length * 0.05, start[1] + uy * length * 0.5 - ny * length * 0.05)
    spine, upper, lower = [], [], []
    for i in range(41):
        t = i / 40
        x, y = bezier(start, mid, end, t)
        w = width * math.sin(math.pi * min(1.0, 0.12 + t * 0.88)) ** 0.7 * (1 - t ** 5)
        spine.append((x, y))
        upper.append((x - nx * w * 0.42, y - ny * w * 0.42))
        lower.append((x + nx * w * 0.58, y + ny * w * 0.58))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).polygon(upper + lower[::-1], fill=255)

    # colour gradient along the feather
    small = Image.new("RGB", (64, 64))
    px = small.load()
    for y in range(64):
        for x in range(64):
            t = ((x / 63 * S - start[0]) * ux + (y / 63 * S - start[1]) * uy) / length
            t = max(0.0, min(1.0, t))
            px[x, y] = tuple(int(b + (c - b) * t) for b, c in zip(base, tip))
    layer = small.resize((S, S), Image.BILINEAR).convert("RGBA")
    layer.putalpha(mask)

    # soft shadow, so that overlapping feathers stay readable
    shadow = Image.new("RGBA", (S, S), (70, 96, 160, 0))
    shadow.putalpha(mask.filter(ImageFilter.GaussianBlur(7)).point(lambda v: int(v * 0.55)))
    canvas.alpha_composite(shadow)
    canvas.alpha_composite(layer)
    detail = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(detail)
    d.line(lower[3:-1], fill=(112, 140, 205, 190), width=4, joint="curve")
    d.line(upper[2:-1], fill=(255, 255, 255, 235), width=4, joint="curve")
    d.line(spine[1:-5], fill=(255, 255, 255, 255), width=3, joint="curve")
    canvas.alpha_composite(detail)


def wing():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    shoulder = (ROOT[0], ROOT[1]), (330.0, 120.0), (640.0, 250.0)   # leading edge of the wing
    # long flight feathers: from the hanging inner ones to the raised outer ones
    n = 13
    for i in range(n):
        k = i / (n - 1)
        start = bezier(*shoulder, 0.08 + 0.92 * k)
        feather(img, start, -68 + 90 * k, 285 + 125 * math.sin(math.pi * (0.15 + 0.7 * k)) + 55 * k,
                66, (206, 220, 252), (150, 180, 244))
    # two rows of shorter covering feathers
    for row, (count, length, width, drop) in enumerate([(11, 235, 62, 16), (10, 140, 54, 0)]):
        for i in range(count):
            k = i / (count - 1)
            x, y = bezier(*shoulder, 0.02 + 0.93 * k)
            feather(img, (x, y + drop), -62 + 66 * k, length * (0.75 + 0.35 * k), width,
                    (255, 255, 255), (204, 218, 255) if row == 0 else (230, 238, 255))
    # bright leading edge
    edge = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    for i in range(60):  # tapers towards the outer end
        ImageDraw.Draw(edge).line([bezier(*shoulder, i / 60), bezier(*shoulder, (i + 1) / 60)],
                                  fill=(255, 255, 255, 235), width=max(2, int(13 * (1 - i / 60))))
    img.alpha_composite(edge.filter(ImageFilter.GaussianBlur(3)))
    # outer glow
    halo = Image.new("RGBA", (S, S), (178, 202, 255, 0))
    halo.putalpha(ImageChops.subtract(img.getchannel("A").filter(ImageFilter.GaussianBlur(16)),
                                      img.getchannel("A")).point(lambda v: min(255, int(v * 1.1))))
    img.alpha_composite(halo)
    return img.resize((512, 512), Image.LANCZOS)


def save(img, name):
    path = os.path.join(OUT, name + ".png")
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        f.write('{"texture": {"blur": true, "clamp": true}}\n')


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    w = wing()
    print("bbox", w.getchannel("A").point(lambda v: 255 if v > 25 else 0).getbbox())
    save(w, "wing")
    save(Image.new("RGBA", (4, 4), (255, 255, 255, 255)), "white")
