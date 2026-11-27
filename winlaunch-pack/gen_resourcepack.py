#!/usr/bin/env python3
"""Builds the WinLaunch resource pack (night-aurora title-screen panorama + WinLaunch logo).

    python3 winlaunch-pack/gen_resourcepack.py

Output: ZalithLauncher/src/main/assets/winlaunch/WinLaunch.zip (bundled into the PvP and Optimization packs,
see WinLaunchResourcesBundle.kt). Everything is procedural, so the pack is reproducible and has no third-party art.

Panorama faces follow Minecraft's cube-map layout: 0 front, 1 right, 2 back, 3 left, 4 up, 5 down
(0..3 stitched left to right form a continuous strip; the up face has the front edge at its bottom).
"""
import importlib.util
import json
import math
import os
import sys
import zipfile

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_ZIP = os.path.join(ROOT, "ZalithLauncher", "src", "main", "assets", "winlaunch", "WinLaunch.zip")
FACE = int(os.environ.get("WL_FACE", "1024"))
TAU = 2 * math.pi


# ----------------------------------------------------------------------------------------------- noise helpers
def hash_u32(a, b, seed):
    h = (a.astype(np.uint32) * np.uint32(73856093)) ^ (b.astype(np.uint32) * np.uint32(19349663)) ^ np.uint32(seed)
    h ^= h >> np.uint32(16)
    h *= np.uint32(0x7FEB352D)
    h ^= h >> np.uint32(15)
    h *= np.uint32(0x846CA68B)
    h ^= h >> np.uint32(16)
    return h


def rnd(a, b, seed):
    return (hash_u32(a, b, seed) & np.uint32(0xFFFFFF)).astype(np.float32) / np.float32(0xFFFFFF)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


# ----------------------------------------------------------------------------------------------- the sky
def stars(az, el):
    """Seamless star field: one star per random cell of an (azimuth, elevation) grid."""
    delta = 0.0105
    row = np.floor((el + math.pi / 2) / delta).astype(np.int64)
    el_c0 = (row + 0.5) * delta - math.pi / 2
    n_az = np.maximum(1, np.floor(TAU * np.cos(el_c0) / delta)).astype(np.int64)
    u = (az % TAU) / TAU
    col = np.minimum(n_az - 1, np.floor(u * n_az)).astype(np.int64)
    has = rnd(row, col, 11) < 0.045
    ox = 0.3 + 0.4 * rnd(row, col, 23)
    oy = 0.3 + 0.4 * rnd(row, col, 37)
    az_c = (col + ox) / n_az * TAU
    el_c = (row + oy) * delta - math.pi / 2
    dx = (az % TAU - az_c)
    dx = (dx + math.pi) % TAU - math.pi
    dx = dx * np.cos(el_c)
    dy = el - el_c
    sigma = 0.0011 + 0.0011 * rnd(row, col, 51)
    mag = rnd(row, col, 67) ** 3.0
    bright = 0.25 + 1.6 * mag
    tint = rnd(row, col, 83)
    r = bright * np.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma)) * has
    col_rgb = np.stack([0.85 + 0.15 * tint, 0.9 + 0.1 * (1 - tint), 1.0 - 0.25 * tint], axis=-1)
    return r[..., None] * col_rgb


def aurora_layer(az, el, seed, strength):
    p = seed
    base = 0.20 + 0.05 * np.sin(2 * az + 1.0 + p) + 0.035 * np.sin(5 * az + 2.0 + 2 * p) + 0.02 * np.sin(9 * az + p)
    height = 0.30 + 0.11 * np.sin(3 * az + 0.5 + p) + 0.06 * np.sin(7 * az + 1.3)
    s = (el - base) / height
    band = smoothstep(-0.02, 0.07, el - base) * np.exp(-np.clip(s, 0, None) * 1.9)
    rays = (0.50 + 0.30 * np.sin(23 * az + 2.2 * np.sin(3 * az + p) + p)
            + 0.20 * np.sin(61 * az + 3.1 * np.sin(5 * az) + 2 * p))
    rays = np.clip(rays, 0, 1.2) ** 1.4
    fine = 0.75 + 0.25 * np.sin(149 * az + 4 * np.sin(11 * az + p))
    env = 0.06 + 0.94 * (0.5 + 0.5 * np.sin(az + 0.6 + 1.7 * p)) ** 2.4
    inten = band * rays * fine * env * strength * smoothstep(1.25, 0.65, el)
    sc = np.clip(s, 0, 1.4)
    green = np.array([0.10, 1.00, 0.45], np.float32)
    teal = np.array([0.08, 0.72, 0.90], np.float32)
    violet = np.array([0.62, 0.28, 0.98], np.float32)
    pink = np.array([0.95, 0.35, 0.75], np.float32)
    t1 = smoothstep(0.0, 0.55, sc)[..., None]
    t2 = smoothstep(0.45, 1.05, sc)[..., None]
    t3 = smoothstep(0.95, 1.4, sc)[..., None]
    c = green * (1 - t1) + teal * t1
    c = c * (1 - t2) + violet * t2
    c = c * (1 - t3) + pink * t3
    return c * inten[..., None]


def sky_above(d):
    x, y, z = d[..., 0], d[..., 1], d[..., 2]
    az = np.arctan2(x, -z)
    el = np.arcsin(np.clip(y, -1, 1))
    t = np.clip(el / (math.pi / 2), 0, 1)
    zen = np.array([0.0008, 0.0020, 0.0110], np.float32)
    hor = np.array([0.003, 0.016, 0.024], np.float32)
    k = (1 - t) ** 2.2
    rgb = zen * (1 - k[..., None]) + hor * k[..., None]
    # faint galactic band
    n = np.array([0.35, 0.78, 0.52], np.float32)
    n /= np.linalg.norm(n)
    dn = x * n[0] + y * n[1] + z * n[2]
    clouds = 0.55 + 0.45 * np.sin(5 * az + 3 * np.sin(2 * el * 3 + 1)) * np.sin(7 * el + az * 2)
    band = np.exp(-(dn / 0.20) ** 2) * np.clip(clouds, 0, 1)
    rgb = rgb + band[..., None] * np.array([0.022, 0.016, 0.052], np.float32)
    # stars (denser inside the band), faded out near the horizon haze
    st = stars(az, el) * (0.85 + 1.4 * band)[..., None] * smoothstep(0.0, 0.12, el)[..., None]
    rgb = rgb + st
    # two aurora curtains
    a = aurora_layer(az, el, 0.0, 1.00) + aurora_layer(az, el, 2.4, 0.55)
    rgb = rgb + a * 0.95
    # soft horizon glow tinted by the aurora
    glow = np.exp(-np.clip(el, 0, None) / 0.06)[..., None] * np.array([0.006, 0.030, 0.026], np.float32)
    rgb = rgb + glow * (0.4 + 0.6 * (0.5 + 0.5 * np.sin(az + 0.6)))[..., None]
    return rgb, az, el


def shore(az):
    """Height (rad) of the far shore: layered mountains + pine trees. Returns (mountains, trees)."""
    m = (0.030 + 0.035 * (0.5 + 0.5 * np.sin(2 * az + 0.3))
         + 0.075 * np.clip(np.sin(3 * az + 1.1), 0, 1) ** 2
         + 0.045 * np.clip(np.sin(5 * az + 2.4), 0, 1) ** 2
         + 0.012 * np.sin(17 * az + 1.0))
    n_trees = 120
    ph = (az * n_trees / TAU) % 1.0
    spike = 1.0 - np.abs(2 * ph - 1.0)
    spike = spike ** 1.25
    amp = 0.012 + 0.020 * (0.5 + 0.5 * np.sin(13 * az + 0.7 * np.sin(7 * az)))
    tier = 0.4 + 0.6 * (0.5 + 0.5 * np.sin(n_trees * 0.5 * az + 1))
    trees = 0.012 + 0.020 * (0.5 + 0.5 * np.sin(4 * az)) + amp * spike * (0.6 + 0.4 * tier)
    return m, trees


def scene_above(d):
    rgb, az, el = sky_above(d)
    m, trees = shore(az)
    px = TAU / (FACE * 4.0)  # about one pixel in radians
    mount = smoothstep(m + px, m - px, el) * smoothstep(-0.05, 0.002, el)
    mount_col = np.array([0.012, 0.030, 0.050], np.float32) + 0.04 * np.exp(-np.clip(el, 0, None) / 0.05)[..., None] * \
        np.array([0.10, 0.45, 0.35], np.float32)
    rgb = rgb * (1 - mount[..., None]) + mount_col * mount[..., None]
    tree_m = smoothstep(trees + px, trees - px, el) * smoothstep(-0.05, 0.002, el)
    rgb = rgb * (1 - tree_m[..., None]) + np.array([0.002, 0.005, 0.008], np.float32) * tree_m[..., None]
    return rgb


def scene(d):
    y = d[..., 1]
    up = scene_above(d)
    # water: mirrored scene, dimmed, with gentle ripples
    mirror = d.copy()
    mirror[..., 1] = -mirror[..., 1]
    az = np.arctan2(d[..., 0], -d[..., 2])
    ripple = 0.0035 * np.sin(310 * np.arcsin(np.clip(-y, 0, 1)) + 5 * az) * np.clip(-y * 6, 0, 1)
    mirror[..., 1] = np.clip(mirror[..., 1] + ripple, -1, 1)
    mirror /= np.linalg.norm(mirror, axis=-1, keepdims=True)
    refl = scene_above(mirror)
    depth = np.clip(-y, 0, 1)
    water_base = np.array([0.0008, 0.0030, 0.0060], np.float32)
    refl = refl * (0.55 - 0.30 * depth)[..., None] + water_base
    w = smoothstep(0.0, -0.0006, y)[..., None]
    return up * (1 - w) + refl * w


def to_srgb(rgb):
    rgb = 1.0 - np.exp(-rgb * 1.7)            # soft highlight roll-off
    rgb = np.clip(rgb, 0, 1) ** (1 / 2.0)
    return (rgb * 255 + 0.5).astype(np.uint8)


# ----------------------------------------------------------------------------------------------- cube faces
def face_dirs(face, n):
    f = np.array([0, 0, -1.0], np.float32)  # front
    r = np.array([1.0, 0, 0], np.float32)
    u = np.array([0, 1.0, 0], np.float32)
    s = (np.arange(n, dtype=np.float32) + 0.5) / n * 2 - 1
    uu, vv = np.meshgrid(s, s)  # uu: image x, vv: image y (down)
    uu = uu[..., None]
    vv = vv[..., None]
    if face == 0:
        d = f + uu * r - vv * u
    elif face == 1:
        d = r + uu * (-f) - vv * u
    elif face == 2:
        d = -f + uu * (-r) - vv * u
    elif face == 3:
        d = -r + uu * f - vv * u
    elif face == 4:
        d = u + vv * f + uu * r
    else:
        d = -u - vv * f + uu * r
    return d / np.linalg.norm(d, axis=-1, keepdims=True)


def build_panorama():
    faces = []
    for i in range(6):
        img = to_srgb(scene(face_dirs(i, FACE)))
        faces.append(Image.fromarray(img, "RGB"))
        print("panorama face", i, flush=True)
    return faces


# ----------------------------------------------------------------------------------------------- logo / icon
def load_gen_assets():
    path = os.path.join(ROOT, "win-visuals", "tools", "gen_assets.py")
    spec = importlib.util.spec_from_file_location("gen_assets", path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def chrome_text(text, font, tracking, width, height, fill_top=250, fill_bot=150):
    """Metallic text on a transparent RGBA canvas (tracking in px)."""
    mask = Image.new("L", (width, height), 0)
    d = ImageDraw.Draw(mask)
    widths = [d.textlength(ch, font=font) for ch in text]
    total = sum(widths) + tracking * (len(text) - 1)
    x = (width - total) / 2
    bbox = d.textbbox((0, 0), "WL", font=font)
    y = (height - (bbox[3] - bbox[1])) / 2 - bbox[1]
    for ch, w in zip(text, widths):
        d.text((x, y), ch, font=font, fill=255)
        x += w + tracking
    grad = Image.new("RGBA", (width, height))
    px = grad.load()
    for yy in range(height):
        t = yy / max(1, height - 1)
        v = fill_top + (fill_bot - fill_top) * (0.5 - 0.5 * math.cos(min(1.0, t * 1.15) * math.pi * 0.9))
        v = int(max(90, min(255, v)))
        for xx in range(width):
            px[xx, yy] = (v, v, min(255, v + 8), 255)
    out = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    out.paste(grad, (0, 0), mask)
    # soft glow below
    glow = Image.new("RGBA", (width, height), (120, 255, 200, 0))
    glow.putalpha(mask.filter(ImageFilter.GaussianBlur(10)).point(lambda v: int(v * 0.35)))
    return Image.alpha_composite(glow, out)


def font(name, size):
    for p in (f"/usr/share/fonts/opentype/inter/{name}.otf",
              "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"):
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def build_logo(emblem):
    """minecraft.png: drawn at 256x44 from a 256x64 texture -> 1024x256 with the art in the top 172 rows."""
    W, H, ART = 1024, 256, 172
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    emb = emblem.resize((ART - 10, ART - 10), Image.LANCZOS)
    f = font("Inter-ExtraBold", 100)
    text = chrome_text("WINLAUNCH", f, 6, W, ART)
    bbox = text.getbbox()
    text = text.crop(bbox)
    gap = 6
    total = emb.width + gap + text.width
    x0 = (W - total) // 2
    img.alpha_composite(emb, (x0, (ART - emb.height) // 2))
    img.alpha_composite(text, (x0 + emb.width + gap, (ART - text.height) // 2 + 4))
    # soft drop shadow so it reads on bright panorama areas
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    shadow.putalpha(img.getchannel("A").filter(ImageFilter.GaussianBlur(6)).point(lambda v: int(v * 0.65)))
    return Image.alpha_composite(shadow, img)


def build_edition():
    """edition.png: drawn at 128x14 from a 128x16 texture -> 512x64 with the art in the top 56 rows."""
    W, H, ART = 512, 64, 56
    f = font("Inter-SemiBold", 30)
    text = chrome_text("WINLAUNCH EDITION", f, 6, W, ART, fill_top=235, fill_bot=165)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    img.alpha_composite(text, (0, 0))
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    shadow.putalpha(img.getchannel("A").filter(ImageFilter.GaussianBlur(3)).point(lambda v: int(v * 0.7)))
    return Image.alpha_composite(shadow, img)


def build_icon(emblem):
    size = 256
    bg = Image.new("RGBA", (size, size), (6, 10, 18, 255))
    px = bg.load()
    for y in range(size):
        for x in range(size):
            t = y / size
            px[x, y] = (int(6 + 8 * (1 - t)), int(14 + 30 * (1 - t) ** 2), int(26 + 40 * (1 - t) ** 2), 255)
    emb = emblem.resize((196, 196), Image.LANCZOS)
    bg.alpha_composite(emb, ((size - 196) // 2, (size - 196) // 2))
    return bg.convert("RGB")


def png_bytes(img):
    import io
    b = io.BytesIO()
    img.save(b, "PNG", optimize=True)
    return b.getvalue()


def main():
    ga = load_gen_assets()
    emblem = ga.logo(final=512, write=False)
    faces = build_panorama()
    mcmeta = {
        "pack": {
            "pack_format": 34,
            "min_format": 34,
            "max_format": 75,
            "supported_formats": {"min_inclusive": 34, "max_inclusive": 75},
            "description": "§fWinLaunch§r §7night aurora panorama & logo",
        }
    }
    os.makedirs(os.path.dirname(OUT_ZIP), exist_ok=True)
    with zipfile.ZipFile(OUT_ZIP, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        def add(name, data):
            zi = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(zi, data)
        add("pack.mcmeta", json.dumps(mcmeta, indent=2))
        add("pack.png", png_bytes(build_icon(emblem)))
        for i, f in enumerate(faces):
            add(f"assets/minecraft/textures/gui/title/background/panorama_{i}.png", png_bytes(f))
        add("assets/minecraft/textures/gui/title/minecraft.png", png_bytes(build_logo(emblem)))
        add("assets/minecraft/textures/gui/title/edition.png", png_bytes(build_edition()))
    print("wrote", OUT_ZIP, os.path.getsize(OUT_ZIP) // 1024, "KB")
    # contact sheet for a quick visual check (not shipped)
    if "--preview" in sys.argv:
        sheet = Image.new("RGB", (FACE * 4 // 2, FACE * 3 // 2), (0, 0, 0))
        s = FACE // 2
        pos = {4: (1, 0), 0: (1, 1), 1: (2, 1), 2: (3, 1), 3: (0, 1), 5: (1, 2)}
        for i, (cx, cy) in pos.items():
            sheet.paste(faces[i].resize((s, s), Image.LANCZOS), (cx * s, cy * s))
        sheet.save("/tmp/wl_cross.png")
        strip = Image.new("RGB", (FACE * 4, FACE))
        for k, i in enumerate((0, 1, 2, 3)):
            strip.paste(faces[i], (k * FACE, 0))
        strip.resize((FACE * 2, FACE // 2), Image.LANCZOS).save("/tmp/wl_strip.png")
        prev = Image.new("RGB", (1024, 300), (10, 20, 30))
        lg = build_logo(emblem).crop((0, 0, 1024, 172))
        prev.paste(lg.convert("RGB"), (0, 10), lg)
        ed = build_edition().crop((0, 0, 512, 56))
        prev.paste(ed.convert("RGB"), (256, 200), ed)
        prev.save("/tmp/wl_logo.png")


if __name__ == "__main__":
    main()
