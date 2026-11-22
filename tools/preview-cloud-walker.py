#!/usr/bin/env python3
"""Orthographic preview of docs/models/cloud_walker_armor.bbmodel.

The bbmodel is exported from the same data as the Java model, so this preview
shows the geometry and atlas the game will use.  A blocked-out player body is
drawn underneath so the armour reads in context.  Box faces use affine texture
mapping, alpha cut-out and vanilla-like face shading.

Usage: python tools/preview-cloud-walker.py
"""
import base64
import io
import json
import math
import os

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BB = os.path.join(HERE, "docs", "models", "cloud_walker_armor.bbmodel")
OUT = os.path.join(HERE, "docs", "previews", "cloud_walker_armor-sheet.png")

bb = json.load(open(BB, encoding="utf-8"))
tex = Image.open(io.BytesIO(base64.b64decode(bb["textures"][0]["source"].split(",", 1)[1]))).convert("RGBA")
TEX = np.asarray(tex).astype(np.float32)
TH, TW = TEX.shape[:2]

NORMALS = {
    "north": (0, 0, -1), "south": (0, 0, 1),
    "east": (1, 0, 0), "west": (-1, 0, 0),
    "up": (0, 1, 0), "down": (0, -1, 0),
}
# corner order matches box-UV order: [u0v0, u1v0, u1v1, u0v1]
CORNERS = {
    "north": [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)],
    "south": [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)],
    "east":  [(1, 1, 0), (1, 1, 1), (1, 0, 1), (1, 0, 0)],
    "west":  [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)],
    "up":    [(0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)],
    "down":  [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)],
}
SHADE = {"up": 1.04, "down": 0.7, "east": 0.84, "west": 0.84,
         "north": 0.95, "south": 0.88}

SKIN = (0xDA, 0xB9, 0xA6)
HAIR = (0xD9, 0xE7, 0xEE)
CLOTH = (0xC7, 0xE9, 0xF7)
PLATE = (0xBD, 0xE1, 0xF0)

# blocked-out vanilla body (bb space: y up, front -Z)
BASE_BOXES = [
    ((-4, 24, -4), (4, 32, 4), SKIN),
    ((-4.3, 30.8, -3.2), (4.3, 33, 4.6), HAIR),
    ((-4.3, 24, 3.0), (4.3, 32, 4.4), HAIR),
    ((-2.2, 26.2, -4.15), (-1.2, 27.0, -4.0), (0x3C, 0x5D, 0x70)),
    ((1.2, 26.2, -4.15), (2.2, 27.0, -4.0), (0x3C, 0x5D, 0x70)),
    ((-4, 12, -2), (4, 24, 2), PLATE),
    ((-8, 12, -2), (-4, 24, 2), CLOTH),
    ((4, 12, -2), (8, 24, 2), CLOTH),
    ((-8, 12, -2), (-4.01, 15.5, 2), SKIN),
    ((4.01, 12, -2), (8, 15.5, 2), SKIN),
    ((-4, 0, -2), (0, 12, 2), CLOTH),
    ((0, 0, -2), (4, 12, 2), CLOTH),
]


def build_faces():
    faces = []
    for lo, hi, col in BASE_BOXES:
        for name, n in NORMALS.items():
            idx = CORNERS[name]
            pts = np.array([[hi[0] if ix else lo[0],
                             hi[1] if iy else lo[1],
                             hi[2] if iz else lo[2]] for ix, iy, iz in idx], float)
            faces.append({"pts": pts, "normal": np.array(n, float),
                          "color": tuple(c / 255.0 for c in col), "name": name})
    for el in bb["elements"]:
        lo, hi = el["from"], el["to"]
        for name, rect in el["faces"].items():
            idx = CORNERS[name]
            pts = np.array([[hi[0] if ix else lo[0],
                             hi[1] if iy else lo[1],
                             hi[2] if iz else lo[2]] for ix, iy, iz in idx], float)
            faces.append({"pts": pts, "normal": np.array(NORMALS[name], float),
                          "uv": rect["uv"], "shade": SHADE[name], "name": name})
    return faces


FACES = build_faces()


def render(yaw_deg, scale=20, pad=24):
    a = math.radians(yaw_deg)
    d = np.array([math.sin(a), 0.0, math.cos(a)])
    right = np.array([math.cos(a), 0.0, -math.sin(a)])
    up = np.array([0.0, 1.0, 0.0])

    vis = []
    for f in FACES:
        if float(f["normal"] @ d) >= -1e-6:
            continue
        scr = np.stack([f["pts"] @ right, f["pts"] @ up], axis=1)
        depth = float((f["pts"] @ d).mean())
        vis.append((depth, scr, f))
    vis.sort(key=lambda v: -v[0])

    pts = np.concatenate([v[1] for v in vis])
    minx, miny = pts.min(axis=0)
    maxx, maxy = pts.max(axis=0)
    W = int((maxx - minx) * scale) + pad * 2
    H = int((maxy - miny) * scale) + pad * 2
    canvas = np.zeros((H, W, 3), dtype=np.float32)
    canvas[:] = (0.72, 0.70, 0.66)

    for depth, scr, f in vis:
        sx = (scr[:, 0] - minx) * scale + pad
        sy = (maxy - scr[:, 1]) * scale + pad
        c0 = np.array([sx[0], sy[0]])
        e1 = np.array([sx[1] - sx[0], sy[1] - sy[0]])
        e2 = np.array([sx[3] - sx[0], sy[3] - sy[0]])
        det = e1[0] * e2[1] - e1[1] * e2[0]
        if abs(det) < 1e-8:
            continue
        bx0 = max(0, int(math.floor(min(sx))) - 1)
        bx1 = min(W, int(math.ceil(max(sx))) + 2)
        by0 = max(0, int(math.floor(min(sy))) - 1)
        by1 = min(H, int(math.ceil(max(sy))) + 2)
        if bx1 <= bx0 or by1 <= by0:
            continue
        yy, xx = np.mgrid[by0:by1, bx0:bx1]
        px = xx + 0.5 - c0[0]
        py = yy + 0.5 - c0[1]
        aa = (px * e2[1] - py * e2[0]) / det
        bq = (e1[0] * py - e1[1] * px) / det
        mask = (aa >= -0.02) & (aa <= 1.02) & (bq >= -0.02) & (bq <= 1.02)
        if not mask.any():
            continue
        if "uv" not in f:
            canvas[yy[mask], xx[mask]] = np.array(f["color"]) * 0.95
            continue
        u0, v0, u1, v1 = f["uv"]
        tu = u0 + aa * (u1 - u0)
        tv = v0 + bq * (v1 - v0)
        ti = np.clip(np.floor(tu).astype(int), 0, TW - 1)
        tj = np.clip(np.floor(tv).astype(int), 0, TH - 1)
        sample = TEX[tj, ti]
        opaque = (sample[..., 3] >= 128) & mask
        rgb = sample[..., :3] / 255.0 * f["shade"]
        canvas[yy[opaque], xx[opaque]] = rgb[opaque]

    return Image.fromarray(np.clip(canvas * 255, 0, 255).astype(np.uint8), "RGB")


def label(img, text):
    im = Image.new("RGB", (img.width, img.height + 26), (0x2A, 0x2F, 0x33))
    im.paste(img, (0, 26))
    d = ImageDraw.Draw(im)
    d.text((8, 7), text, fill=(0xD8, 0xF2, 0xFC))
    return im


views = [(-28, "three-quarter (front-right)"), (0, "front"), (180, "back"),
         (90, "side (model left)")]
imgs = [label(render(yaw), name) for yaw, name in views]
w = sum(i.width for i in imgs)
h = max(i.height for i in imgs)
sheet = Image.new("RGB", (w, h), (0x2A, 0x2F, 0x33))
x = 0
for im in imgs:
    sheet.paste(im, (x, 0))
    x += im.width
os.makedirs(os.path.dirname(OUT), exist_ok=True)
sheet.save(OUT)
print("wrote", OUT, sheet.size)
