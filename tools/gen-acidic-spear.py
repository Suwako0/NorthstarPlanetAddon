#!/usr/bin/env python3
"""Build the acidic spear: 60-unit model + 128 atlas + front preview.

Reference: docs/previews/acidic_spear-reference.png — polished leaf blade with two
acid slots, brass sleeve, wrapped grip, block butt, green tassel.

Usage: python tools/gen-acidic-spear.py [--ascii]
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon")
MODEL = os.path.join(ASSETS, "models", "item", "acidic_spear.json")
TEX = os.path.join(ASSETS, "textures", "item", "acidic_spear.png")

C = {
    "steel_d": (0x4A, 0x54, 0x58, 255),
    "steel": (0x76, 0x82, 0x86, 255),
    "steel_l": (0xA8, 0xB4, 0xB4, 255),
    "steel_h": (0xD8, 0xE2, 0xDF, 255),
    "edge": (0xEF, 0xF6, 0xF3, 255),
    "dark": (0x14, 0x18, 0x1A, 255),
    "brass_d": (0x6B, 0x4E, 0x18, 255),
    "brass": (0xB0, 0x8A, 0x3A, 255),
    "brass_l": (0xE0, 0xBE, 0x6A, 255),
    "slot": (0x10, 0x14, 0x16, 255),
    "acid_d": (0x2E, 0x5B, 0x1E, 255),
    "acid": (0x7D, 0xFF, 0x4A, 255),
    "acid_h": (0xE2, 0xFF, 0xC8, 255),
    "cord": (0x23, 0x26, 0x2A, 255),
    "cord_l": (0x3E, 0x44, 0x4A, 255),
    "cloth": (0x6F, 0xBF, 0x3A, 255),
    "cloth_d": (0x3E, 0x6E, 0x22, 255),
    "clear": (0, 0, 0, 0),
}

# uv regions (0..16 units, 1 unit = 8 px)
REGION = {
    "blade": (0, 0, 4, 8),      # px (0,0)-(32,64)   vertical strip, tip at v=0
    "steel": (4, 0, 8, 4),      # px (32,0)-(64,32)
    "shaft": (4, 4, 8, 8),      # px (32,32)-(64,64)
    "brass": (8, 0, 12, 4),     # px (64,0)-(96,32)
    "cord": (8, 4, 12, 8),      # px (64,32)-(96,64)
    "acid": (12, 0, 14, 4),     # px (96,0)-(112,32)
    "slot": (14, 0, 16, 4),     # px (112,0)-(128,32)
    "cloth": (12, 4, 14, 8),    # px (96,32)-(112,64)
    "stain": (14, 4, 16, 8),    # px (112,32)-(128,64)
}

TH = 0.72                 # blade thickness
# 持握点固定在模型原点 (8,8,8)：把整把矛下移，让手正好落在矛的中段
Y_OFFSET = -22.1
Z0, Z1 = 8 - TH / 2, 8 + TH / 2

# blade slabs: (name, y0, y1, half_width)
BLADE = [
    ("blade_point", 59.5, 60.0, 0.35),
    ("blade_tip2", 58.9, 59.5, 0.80),
    ("blade_tip", 58.2, 58.9, 1.40),
    ("blade_narrow", 56.6, 58.2, 2.10),
    ("blade_wide", 55.0, 56.6, 2.80),
    ("blade_mid", 52.0, 55.0, 2.45),
    ("blade_base", 49.5, 52.0, 1.60),
]
BLADE_TOP, BLADE_BOT = 60.0, 49.5
BLADE_V = (0.0, 8.0)

# (name, x0,y0,z0, x1,y1,z1, region)
PARTS = [
    # tassel
    ("tassel_ring", 7.4, 2.6, 7.4, 8.6, 3.2, 8.6, "brass"),
    ("tassel_a", 7.2, 0.4, 7.5, 7.7, 2.7, 8.1, "cloth"),
    ("tassel_b", 7.9, 0.2, 7.5, 8.4, 2.7, 8.1, "cloth"),
    ("tassel_c", 8.4, 0.6, 7.5, 8.9, 2.7, 8.1, "cloth"),
    # butt
    ("butt", 6.4, 3.2, 6.55, 9.6, 6.2, 9.45, "shaft"),
    ("butt_cap", 6.8, 6.2, 6.9, 9.2, 6.6, 9.1, "steel"),
    ("butt_bolt_l", 6.3, 4.2, 7.6, 6.45, 4.9, 8.4, "brass"),
    ("butt_bolt_r", 9.55, 4.2, 7.6, 9.7, 4.9, 8.4, "brass"),
    # shaft
    ("shaft", 7.05, 6.6, 7.05, 8.95, 44.5, 8.95, "shaft"),
    ("stain", 7.0, 30.0, 7.01, 9.0, 35.0, 7.07, "stain"),
    # wrapped grip
    ("wrap_1", 6.9, 18.1, 6.9, 9.1, 19.1, 9.1, "cord"),
    ("wrap_2", 6.9, 19.52, 6.9, 9.1, 20.52, 9.1, "cord"),
    ("wrap_3", 6.9, 20.94, 6.9, 9.1, 21.94, 9.1, "cord"),
    ("wrap_4", 6.9, 22.36, 6.9, 9.1, 23.36, 9.1, "cord"),
    ("wrap_5", 6.9, 23.78, 6.9, 9.1, 24.78, 9.1, "cord"),
    ("wrap_6", 6.9, 25.2, 6.9, 9.1, 26.2, 9.1, "cord"),
    # brass sleeve
    ("sleeve", 6.9, 44.5, 6.9, 9.1, 49.5, 9.1, "brass"),
    ("sleeve_low", 6.75, 44.5, 6.75, 9.25, 45.3, 9.25, "steel"),
    ("sleeve_high", 6.75, 48.7, 6.75, 9.25, 49.5, 9.25, "steel"),
    ("sleeve_bolt", 6.8, 46.6, 6.78, 7.3, 47.4, 6.86, "brass"),
]
for name, y0, y1, half in BLADE:
    PARTS.append((name, 8 - half, y0, Z0, 8 + half, y1, Z1, "blade"))


def blade_slice(y0, y1):
    """vertical uv slice of the blade strip, proportional to the slab height"""
    span = BLADE_V[1] - BLADE_V[0]
    total = BLADE_TOP - BLADE_BOT
    v0 = BLADE_V[0] + (BLADE_TOP - y1) / total * span
    v1 = BLADE_V[0] + (BLADE_TOP - y0) / total * span
    return v0, v1


# ---------------------------------------------------------------- atlas
def emit_atlas():
    img = Image.new("RGBA", (128, 128), C["clear"])
    d = ImageDraw.Draw(img)

    # --- blade strip (32 x 64): polished steel + two acid slots + pits
    bx0, by0, bx1, by1 = 0, 0, 31, 63
    d.rectangle([bx0, by0, bx1, by1], fill=C["steel"])
    d.polygon([(0, 0), (12, 0), (7, 63), (0, 63)], fill=C["steel_l"])
    d.polygon([(20, 0), (31, 0), (31, 63), (24, 63)], fill=C["steel_d"])
    d.rectangle([0, 0, 0, 63], fill=C["steel_h"])
    d.rectangle([1, 0, 1, 63], fill=C["edge"])
    d.rectangle([31, 0, 31, 63], fill=C["dark"])
    # two slots, offset like the reference
    for sx in (8, 17):
        d.rectangle([sx, 14, sx + 4, 40], fill=C["slot"])
        d.rectangle([sx + 1, 16, sx + 3, 38], fill=C["acid_d"])
        d.rectangle([sx + 1, 22, sx + 3, 36], fill=C["acid"])
        d.rectangle([sx + 2, 26, sx + 2, 33], fill=C["acid_h"])
        d.rectangle([sx, 14, sx + 4, 15], fill=C["steel_h"])
    # corrosion pits near the base
    for px_, py_ in ((5, 46), (6, 47), (12, 52), (24, 49), (25, 50), (18, 55)):
        d.rectangle([px_, py_, px_ + 1, py_ + 1], fill=C["acid_d"])
    d.rectangle([23, 44, 28, 46], fill=C["acid"])

    # --- steel / shaft / brass / cord blocks
    for (rx0, ry0, rx1, ry1), base, lit, dark in (
        ((32, 0, 63, 31), C["steel"], C["steel_h"], C["steel_d"]),
        ((32, 32, 63, 63), C["steel_d"], C["steel"], C["dark"]),
        ((64, 0, 95, 31), C["brass"], C["brass_l"], C["brass_d"]),
        ((96, 0, 111, 31), C["acid"], C["acid_h"], C["acid_d"]),
        ((112, 0, 127, 31), C["dark"], C["slot"], C["dark"]),
    ):
        d.rectangle([rx0, ry0, rx1, ry1], fill=base)
        d.rectangle([rx0, ry0, rx1, ry0 + 2], fill=lit)
        d.rectangle([rx0, ry1 - 2, rx1, ry1], fill=dark)
        d.rectangle([rx0, ry0, rx0 + 1, ry1], fill=lit)

    # cord: dark with horizontal ridges
    d.rectangle([64, 32, 95, 63], fill=C["cord"])
    for y in range(32, 64, 5):
        d.rectangle([64, y, 95, y + 1], fill=C["cord_l"])

    # cloth tassel
    d.rectangle([96, 32, 111, 63], fill=C["cloth_d"])
    for x in range(96, 112, 2):
        d.rectangle([x, 34, x, 62], fill=C["cloth"])

    # stain: steel with a green splatter
    d.rectangle([112, 32, 127, 63], fill=C["steel_d"])
    d.ellipse([115, 40, 124, 51], fill=C["acid_d"])
    d.ellipse([117, 43, 122, 48], fill=C["acid"])

    img.save(TEX, "PNG", optimize=True)
    print("wrote", TEX, os.path.getsize(TEX), "bytes",
          "| colours:", len({img.getpixel((x, y)) for x in range(128) for y in range(128)
                             if img.getpixel((x, y))[3]}))


# ---------------------------------------------------------------- model
def uv_for(part_name, region):
    u0, v0, u1, v1 = REGION[region]
    if region == "blade":
        part = next(p for p in PARTS if p[0] == part_name)
        y0, y1 = part[2], part[5]
        sv0, sv1 = blade_slice(y0, y1)
    else:
        sv0, sv1 = v0, v1
    return [u0, round(sv0, 3), u1, round(sv1, 3)]


def emit_model():
    elements = []
    for name, x0, y0, z0, x1, y1, z1, region in PARTS:
        uv = uv_for(name, region)
        faces = {k: {"uv": uv, "texture": "#all"} for k in
                 ("north", "east", "south", "west", "up", "down")}
        elements.append({
            "name": name,
            "from": [round(v, 3) for v in (x0, y0 + Y_OFFSET, z0)],
            "to": [round(v, 3) for v in (x1, y1 + Y_OFFSET, z1)],
            "faces": faces,
        })

    model = {
        "gui_light": "front",
        "texture_size": [128, 128],
        "textures": {
            "all": "northstar_planet_addon:item/acidic_spear",
            "particle": "northstar_planet_addon:item/acidic_spear",
        },
        "display": {
            "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 1, 0.5],
                                      "scale": [0.58, 0.58, 0.58]},
            "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 1, 0.5],
                                     "scale": [0.58, 0.58, 0.58]},
            "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13],
                                      "scale": [0.46, 0.46, 0.46]},
            "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13],
                                     "scale": [0.46, 0.46, 0.46]},
            "ground": {"translation": [0, 3, 0], "scale": [0.72, 0.72, 0.36]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -1], "scale": [1.3, 1.3, 1]},
            "head": {"rotation": [0, 0, 0], "translation": [0, 6, 0], "scale": [1.1, 1.1, 1.1]},
            "gui": {"rotation": [0, 0, 0], "translation": [5.5, 1.2, 0], "scale": [0.25, 0.25, 0.25]},
        },
        "elements": elements,
    }
    with open(MODEL, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("wrote", MODEL, len(elements), "elements")


# ---------------------------------------------------------------- preview
def emit_preview():
    tex = Image.open(TEX).convert("RGBA")
    tx = tex.load()
    S = 6
    xs = [p[1] for p in PARTS] + [p[4] for p in PARTS]
    ys = [p[2] + Y_OFFSET for p in PARTS] + [p[5] + Y_OFFSET for p in PARTS]
    xmin, xmax, ymin, ymax = min(xs), max(xs), min(ys), max(ys)
    pad = 1.0
    w = int((xmax - xmin + 2 * pad) * S)
    h = int((ymax - ymin + 2 * pad) * S)
    img = Image.new("RGBA", (w, h), (0x8A, 0x86, 0x80, 255))
    out = img.load()

    for name, x0, y0, z0, x1, y1, z1, region in sorted(PARTS, key=lambda p: -p[3]):
        u0, v0, u1, v1 = uv_for(name, region)
        sx0 = (x0 - xmin + pad) * S
        sy0 = (ymax + pad - (y1 + Y_OFFSET)) * S
        sx1 = (x1 - xmin + pad) * S
        sy1 = (ymax + pad - (y0 + Y_OFFSET)) * S
        for py in range(int(sy0), int(sy1)):
            fy = (py - sy0) / max(1e-6, sy1 - sy0)
            v = v0 + fy * (v1 - v0)
            for px_ in range(int(sx0), int(sx1)):
                fx = (px_ - sx0) / max(1e-6, sx1 - sx0)
                u = u0 + fx * (u1 - u0)
                c = tx[min(127, max(0, int(u * 8))), min(127, max(0, int(v * 8)))]
                if c[3] > 8 and 0 <= px_ < w and 0 <= py < h:
                    out[px_, py] = c

    path = os.path.join(HERE, "build", "preview-acidic_spear.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", path, img.size)
    if "--ascii" in sys.argv:
        ramp = ".,:;+=xX#@"
        step = 3
        for y in range(0, h, step * 2):
            row = ""
            for x in range(0, w, step):
                r, g, b, a = out[x, y]
                if (r, g, b) == (0x8A, 0x86, 0x80):
                    row += " "
                    continue
                lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
                row += ramp[min(9, max(1, int(lum / 0.45 * 9)))]
            print("   " + row)


def main():
    emit_atlas()
    emit_model()
    emit_preview()


if __name__ == "__main__":
    main()
