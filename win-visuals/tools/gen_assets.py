#!/usr/bin/env python3
"""Generates the GUI textures of Win Visuals (icons, logo, rounded panel pieces).

Run from win-visuals/:  python3 tools/gen_assets.py
Everything is drawn at 4x and downsampled, so edges are anti-aliased. The textures are
white on transparent and get tinted in-game; the .mcmeta files switch on linear filtering.
"""
import math
import os
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "winvisuals", "textures", "gui")
os.makedirs(OUT, exist_ok=True)
S = 256          # drawing canvas
W = (255, 255, 255, 255)


def new():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def save(img, name, size=64, blur=True):
    img = img.resize((size, size), Image.LANCZOS)
    path = os.path.join(OUT, name + ".png")
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        f.write('{"texture": {"blur": %s, "clamp": true}}\n' % ("true" if blur else "false"))


def line(d, pts, w):
    d.line(pts, fill=W, width=w, joint="curve")
    for x, y in (pts[0], pts[-1]):
        d.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=W)


def star4(d, cx, cy, r, inner=0.26):
    pts = []
    for i in range(8):
        a = math.pi / 4 * i - math.pi / 2
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=W)


def star5(d, cx, cy, r, inner=0.45):
    pts = []
    for i in range(10):
        a = math.pi / 5 * i - math.pi / 2
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    d.polygon(pts, fill=W)


def dot(d, x, y, r):
    d.ellipse((x - r, y - r, x + r, y + r), fill=W)


# ------------------------------------------------------------------ icons
def moon():
    img, d = new()
    d.ellipse((28, 28, 228, 228), fill=W)
    cut = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(cut).ellipse((84, 8, 264, 188), fill=W)
    a = ImageChops.subtract(img.getchannel("A"), cut.getchannel("A"))
    img.putalpha(a)
    return img


def sparkle():
    img, d = new()
    star4(d, 108, 150, 92)
    star4(d, 196, 66, 46)
    star4(d, 200, 196, 30)
    return img


def particles():
    img, d = new()
    star4(d, 120, 128, 64, 0.3)
    for (x, y, r) in [(54, 60, 16), (198, 48, 12), (206, 140, 18), (66, 204, 13), (170, 214, 10), (36, 130, 9)]:
        dot(d, x, y, r)
    return img


def monitor():
    img, d = new()
    d.rounded_rectangle((20, 34, 236, 178), 22, outline=W, width=18)
    line(d, [(128, 180), (128, 212)], 18)
    line(d, [(78, 216), (178, 216)], 18)
    return img


def gear():
    img, d = new()
    d.ellipse((58, 58, 198, 198), fill=W)
    for i in range(8):
        a = math.pi / 4 * i
        cx, cy = 128 + 96 * math.cos(a), 128 + 96 * math.sin(a)
        c, s = math.cos(a), math.sin(a)
        hw, hh = 22, 30
        pts = [(cx + dx * c - dy * s, cy + dx * s + dy * c) for dx, dy in [(-hh, -hw), (hh, -hw), (hh, hw), (-hh, hw)]]
        d.polygon(pts, fill=W)
    hole = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(hole).ellipse((92, 92, 164, 164), fill=W)
    img.putalpha(ImageChops.subtract(img.getchannel("A"), hole.getchannel("A")))
    return img


def aurora():
    img, d = new()
    d.polygon([(16, 220), (58, 96), (84, 150), (120, 30), (160, 150), (186, 84), (240, 220)], fill=W)
    # carve a few fine rays
    cut = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cut)
    for x in (84, 120, 160, 200):
        cd.line([(x, 232), (x - 6, 130)], fill=W, width=7)
    img.putalpha(ImageChops.subtract(img.getchannel("A"), cut.getchannel("A")))
    return img


def sun():
    img, d = new()
    dot(d, 128, 128, 52)
    for i in range(8):
        a = math.pi / 4 * i
        line(d, [(128 + 80 * math.cos(a), 128 + 80 * math.sin(a)), (128 + 112 * math.cos(a), 128 + 112 * math.sin(a))], 18)
    return img


def star():
    img, d = new()
    star5(d, 128, 136, 112, 0.46)
    return img


def spiral():
    img, d = new()
    pts = []
    t = 0.0
    while t < 5.0 * math.pi:
        r = 8 + t * 6.4
        pts.append((128 + r * math.cos(t), 128 + r * math.sin(t) * 0.85))
        t += 0.05
    d.line(pts, fill=W, width=18, joint="curve")
    dot(d, 128, 128, 16)
    return img


def cloud():
    img, d = new()
    d.rounded_rectangle((30, 120, 226, 196), 38, fill=W)
    dot(d, 92, 118, 48)
    dot(d, 150, 100, 60)
    dot(d, 198, 136, 36)
    return img


def check():
    img, d = new()
    line(d, [(46, 136), (100, 190), (212, 66)], 30)
    return img


def refresh():
    img, d = new()
    box = (44, 44, 212, 212)
    d.arc(box, 30, 320, fill=W, width=24)
    a = math.radians(320)
    ex, ey = 128 + 84 * math.cos(a), 128 + 84 * math.sin(a)
    tx, ty = -math.sin(a), math.cos(a)       # direction of travel
    nx, ny = -ty, tx
    tip = (ex + tx * 62, ey + ty * 62)
    b1 = (ex - tx * 14 + nx * 52, ey - ty * 14 + ny * 52)
    b2 = (ex - tx * 14 - nx * 52, ey - ty * 14 - ny * 52)
    d.polygon([tip, b1, b2], fill=W)
    return img


def minus():
    img, d = new()
    line(d, [(54, 128), (202, 128)], 26)
    return img


def close():
    img, d = new()
    line(d, [(58, 58), (198, 198)], 26)
    line(d, [(198, 58), (58, 198)], 26)
    return img


def cube():
    img, d = new()
    cx, cy, r = 128, 128, 108
    top = [(cx, cy - r), (cx + r * 0.866, cy - r / 2), (cx, cy), (cx - r * 0.866, cy - r / 2)]
    left = [(cx - r * 0.866, cy - r / 2 + 12), (cx - 6, cy + 12), (cx - 6, cy + r - 6), (cx - r * 0.866, cy + r / 2 - 6)]
    right = [(cx + r * 0.866, cy - r / 2 + 12), (cx + 6, cy + 12), (cx + 6, cy + r - 6), (cx + r * 0.866, cy + r / 2 - 6)]
    d.polygon(top, fill=W)
    d.polygon(left, fill=W)
    d.polygon(right, fill=W)
    return img


def bolt():
    img, d = new()
    d.polygon([(150, 14), (58, 142), (116, 142), (96, 242), (200, 100), (140, 100)], fill=W)
    return img


def burst():
    img, d = new()
    pts = []
    for i in range(16):
        a = math.pi / 8 * i
        rr = 112 if i % 2 == 0 else 54
        pts.append((128 + rr * math.cos(a), 128 + rr * math.sin(a)))
    d.polygon(pts, fill=W)
    return img


def arrow_up():
    img, d = new()
    line(d, [(128, 220), (128, 56)], 26)
    line(d, [(60, 120), (128, 48), (196, 120)], 26)
    return img


def grid():
    img, d = new()
    for x in (28, 144):
        for y in (28, 144):
            d.rounded_rectangle((x, y, x + 84, y + 84), 18, fill=W)
    return img


def expand():
    img, d = new()
    for (sx, sy) in [(1, 1), (-1, -1)]:
        ox, oy = 128 + sx * 100, 128 + sy * 100
        line(d, [(ox, oy), (128 + sx * 30, 128 + sy * 30)], 20)
        line(d, [(ox, oy - sy * 64), (ox, oy), (ox - sx * 64, oy)], 20)
    return img


def users():
    img, d = new()
    dot(d, 98, 86, 42)
    d.pieslice((20, 138, 176, 294), 180, 360, fill=W)
    dot(d, 186, 100, 32)
    d.pieslice((140, 146, 244, 250), 180, 360, fill=W)
    return img


def ring():
    img, d = new()
    d.ellipse((30, 30, 226, 226), outline=W, width=22)
    dot(d, 128, 128, 36)
    return img


def palette():
    img, d = new()
    d.ellipse((14, 26, 242, 230), fill=W)
    cut = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    cd = ImageDraw.Draw(cut)
    for (x, y, r) in [(76, 118, 20), (112, 70, 20), (170, 70, 20), (206, 118, 20)]:
        dot(cd, x, y, r)
    cd.ellipse((140, 150, 214, 224), fill=W)
    img.putalpha(ImageChops.subtract(img.getchannel("A"), cut.getchannel("A")))
    return img


def zoom():
    img, d = new()
    d.ellipse((24, 24, 184, 184), outline=W, width=22)
    line(d, [(160, 160), (226, 226)], 28)
    line(d, [(72, 104), (136, 104)], 16)
    line(d, [(104, 72), (104, 136)], 16)
    return img


# ------------------------------------------------------------------ chrome
def round_rect(name, stroke=0, size=64, radius=24):
    big = 4 * size
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = radius * 4
    if stroke:
        d.rounded_rectangle((0, 0, big - 1, big - 1), r, outline=W, width=stroke * 4)
    else:
        d.rounded_rectangle((0, 0, big - 1, big - 1), r, fill=W)
    img = img.resize((size, size), Image.LANCZOS)
    path = os.path.join(OUT, name + ".png")
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        f.write('{"texture": {"blur": true, "clamp": true}}\n')


def glow():
    """Soft radial blob used for the aurora highlights in the preview."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    for y in range(64):
        for x in range(64):
            r = math.hypot(x - 31.5, y - 31.5) / 32.0
            a = max(0.0, 1.0 - r)
            px[x, y] = (255, 255, 255, int(255 * a * a))
    path = os.path.join(OUT, "glow.png")
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        f.write('{"texture": {"blur": true, "clamp": true}}\n')


def logo():
    size = 4 * 256
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # metallic "W"
    font = None
    for p in ["/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]:
        if os.path.exists(p):
            font = ImageFont.truetype(p, 760)
            break
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    bbox = md.textbbox((0, 0), "W", font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    md.text(((size - tw) / 2 - bbox[0], (size - th) / 2 - bbox[1] + 20), "W", font=font, fill=255)
    grad = Image.new("RGBA", (size, size))
    gp = grad.load()
    for y in range(size):
        t = y / size
        # bright top, darker waist, bright lower edge: brushed-chrome look
        v = 0.95 - 0.55 * math.sin(min(1.0, max(0.0, (t - 0.25) / 0.55)) * math.pi) + 0.1 * t
        v = max(0.25, min(1.0, v))
        c = int(255 * v)
        for x in range(size):
            gp[x, y] = (c, c, min(255, c + 6), 255)
    letter = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    letter.paste(grad, (0, 0), mask)
    # inner edge highlight
    edge = mask.filter(ImageFilter.GaussianBlur(5))
    edge = ImageChops.subtract(mask, edge).point(lambda v: min(255, v * 4))
    hl = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    hl.putalpha(edge)
    letter = Image.alpha_composite(letter, hl)
    img = Image.alpha_composite(img, letter)
    # orbit ring
    ring_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rd = ImageDraw.Draw(ring_layer)
    rd.ellipse((size * 0.04, size * 0.30, size * 0.96, size * 0.74), outline=(255, 255, 255, 255), width=int(size * 0.028))
    ring_layer = ring_layer.rotate(20, resample=Image.BICUBIC, center=(size / 2, size / 2))
    # fade the ring: bright on the left/top, fading to the right so it reads as an orbit
    fade = Image.new("L", (size, size), 0)
    fp = fade.load()
    for x in range(size):
        a = int(255 * (0.25 + 0.75 * (1 - x / size)))
        for y in range(size):
            fp[x, y] = a
    ra = ImageChops.multiply(ring_layer.getchannel("A"), fade)
    ring_layer.putalpha(ra)
    img = Image.alpha_composite(img, ring_layer)
    img = img.resize((256, 256), Image.LANCZOS)
    path = os.path.join(OUT, "logo.png")
    img.save(path)
    with open(path + ".mcmeta", "w") as f:
        f.write('{"texture": {"blur": true, "clamp": true}}\n')


ICONS = dict(moon=moon, sparkle=sparkle, particles=particles, monitor=monitor, gear=gear, aurora=aurora, sun=sun,
             star=star, spiral=spiral, cloud=cloud, check=check, refresh=refresh, minus=minus, close=close, cube=cube,
             bolt=bolt, burst=burst, arrow_up=arrow_up, grid=grid, expand=expand, users=users, ring=ring,
             palette=palette, zoom=zoom)

if __name__ == "__main__":
    for n, fn in ICONS.items():
        save(fn(), "icon_" + n)
    round_rect("round_fill")
    round_rect("round_stroke", stroke=4)
    glow()
    logo()
    print("generated", len(ICONS), "icons")
