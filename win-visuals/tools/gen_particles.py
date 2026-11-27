#!/usr/bin/env python3
"""Generates the Win Visuals particle sprites (white, tinted in-game).

Run from win-visuals/:  python3 tools/gen_particles.py
The vanilla particle shader discards pixels with alpha < 0.1, so the halos stay above that
level for most of their width. Files in textures/particle/ are stitched into the particle atlas.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "winvisuals",
                   "textures", "particle")


def white(alpha):
    img = Image.new("RGBA", alpha.size, (255, 255, 255, 0))
    img.putalpha(alpha)
    return img


def radial(size, fn):
    a = Image.new("L", (size, size), 0)
    px = a.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - c, y - c) / (size / 2)
            px[x, y] = int(255 * max(0.0, min(1.0, fn(r))))
    return a


def star(points, inner, outer=0.72, size=64):
    big = 256
    mask = Image.new("L", (big, big), 0)
    pts = []
    for i in range(points * 2):
        a = math.pi / points * i - math.pi / 2
        r = big / 2 * outer * (1 if i % 2 == 0 else inner)
        pts.append((big / 2 + r * math.cos(a), big / 2 + r * math.sin(a)))
    ImageDraw.Draw(mask).polygon(pts, fill=255)
    halo = mask.filter(ImageFilter.GaussianBlur(14)).point(lambda v: min(255, int(v * 0.85)))
    return white(ImageChops.lighter(mask, halo).resize((size, size), Image.LANCZOS))


def glow():
    return white(radial(64, lambda r: (1 - r) ** 1.6 * 1.35 if r < 1 else 0))


def ring():
    # thin bright rim with a soft glow on both sides
    return white(radial(128, lambda r: 0 if r >= 1 else
                        math.exp(-((r - 0.82) / 0.025) ** 2) + 0.45 * math.exp(-((r - 0.82) / 0.10) ** 2)))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, img in dict(star=star(5, 0.46), sparkle=star(4, 0.18, 0.9), glow=glow(), ring=ring()).items():
        img.save(os.path.join(OUT, name + ".png"))
        print(name, img.size)
