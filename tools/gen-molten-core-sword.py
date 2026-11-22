#!/usr/bin/env python3
"""Paint the 128x128 atlas for molten_core_sword — chunky, low-detail pass.

Deliberately coarse: two or three flat tones per material, big readable shapes,
no per-pixel noise (no chips, stitches, scratches, spatter or dithering).

Region layout (uv unit = 8 px at 128), matching tools/remap-molten-core-sword-uv.py:
  A blade    uv (0,0)-(4,12)    px (0,0)-(32,96)     two obsidian planes + lava vein
  B guard    uv (4,0)-(8,2)     px (32,0)-(64,16)    charcoal bar, bevel, molten centre
  C fuller   uv (8,4)-(12,8)    px (64,32)-(96,64)   molten channel, three bands
  D gem      uv (4,8)-(6,10)    px (32,64)-(48,80)   flat three-tone gem
  E tassel   uv (6,8)-(8,12)    px (48,64)-(64,96)   two-tone strands
  F leather  uv (8,0)-(10,4)    px (64,0)-(80,32)    two-tone wrap bands
  G brass    uv (10,0)-(12,4)   px (80,0)-(96,32)    flat brass with a lit top
  H edge     uv (12,0)-(13,4)   px (96,0)-(104,32)   three-column bevel

Usage: python tools/gen-molten-core-sword.py [--preview]
"""
import os
import sys

from PIL import Image, ImageDraw

W = H = 128

C = {
    "ob_dark": (0x15, 0x12, 0x10, 255),
    "ob": (0x22, 0x1C, 0x17, 255),
    "ob_lit": (0x2F, 0x28, 0x22, 255),
    "edge": (0x6B, 0x55, 0x44, 255),
    "pin": (0x0F, 0x0C, 0x0A, 255),
    "ember_deep": (0x5A, 0x24, 0x16, 255),
    "ember": (0xF0, 0x70, 0x3A, 255),
    "ember_hot": (0xFF, 0xC2, 0x66, 255),
    "brass_dark": (0x6B, 0x4E, 0x18, 255),
    "brass": (0xC8, 0x9A, 0x38, 255),
    "brass_hi": (0xF0, 0xD9, 0x8A, 255),
    "leather": (0x3A, 0x2A, 0x18, 255),
    "leather_lit": (0x4E, 0x3A, 0x22, 255),
    "clear": (0, 0, 0, 0),
}

img = Image.new("RGBA", (W, H), C["clear"])
d = ImageDraw.Draw(img)


def fill(x0, y0, x1, y1, color):
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=color)


def px(x, y, color):
    if 0 <= x < W and 0 <= y < H:
        img.putpixel((x, y), color)


def line(x0, y0, x1, y1, color, width=1):
    d.line([x0, y0, x1, y1], fill=color, width=width)


# ---------------------------------------------------------------- A: blade
AX0, AY0, AX1, AY1 = 0, 0, 32, 96
fill(AX0, AY0, AX1, AY1, C["ob"])

# two big planes with one diagonal seam (no per-segment bands)
for y in range(AY0, AY1):
    seam = 15 - int(6 * y / (AY1 - 1))          # 15 -> 9
    for x in range(AX0, seam):
        img.putpixel((x, y), C["ob_lit"])
    px(seam, y, C["ob_dark"])
for y in range(AY0, AY1):
    px(AX0, y, C["edge"] if y > 34 else C["ob_lit"])
    px(AX1 - 1, y, C["pin"])

# lava vein: thick trunk, two branches, no fine detail
TRUNK = [(19, 94), (21, 80), (17, 66), (22, 52), (18, 38), (22, 24), (21, 8)]


def draw_trunk(color, width):
    for (x0, y0), (x1, y1) in zip(TRUNK, TRUNK[1:]):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        for s in range(steps + 1):
            t = s / max(1, steps)
            x = round(x0 + (x1 - x0) * t)
            y = round(y0 + (y1 - y0) * t)
            half = width // 2
            for dx in range(-half, width - half):
                px(x + dx, y, color)


draw_trunk(C["ember_deep"], 7)
draw_trunk(C["ember"], 3)
line(19, 74, 11, 66, C["ember"], 2)
line(20, 50, 28, 42, C["ember"], 2)

# molten base: one solid arc, one hot core
for x in range(5, 28):
    top = 92 - abs(x - 17) // 5
    for y in range(top, AY1):
        img.putpixel((x, y), C["ember"])
for x in range(11, 24):
    top = 93 - abs(x - 17) // 6
    for y in range(top, AY1):
        img.putpixel((x, y), C["ember_hot"])

# ---------------------------------------------------------------- B: guard
GX0, GY0, GX1, GY1 = 32, 0, 64, 16
fill(GX0, GY0, GX1, GY1, C["ob"])
fill(GX0, GY0, GX1, GY0 + 2, C["ob_lit"])
fill(GX0, GY1 - 2, GX1, GY1, C["ob_dark"])
# molten centre, flat bands
fill(38, 11, 58, 16, C["ember"])
fill(43, 13, 53, 16, C["ember_hot"])
# one brass rivet at each end
for cx in (36, 58):
    fill(cx, 5, cx + 3, 8, C["brass"])
    px(cx, 5, C["brass_hi"])

# ---------------------------------------------------------------- C: fuller
FX0, FY0, FX1, FY1 = 64, 32, 96, 64
fill(FX0, FY0, FX1, FY1, C["ob_dark"])
fill(70, FY0, 90, FY1, C["ember"])
fill(72, 44, 88, FY1, C["ember_hot"])
fill(76, 56, 84, FY1, C["ember_hot"])

# ---------------------------------------------------------------- D: gem
DX0, DY0, DX1, DY1 = 32, 64, 48, 80
fill(DX0, DY0, DX1, DY1, C["pin"])
fill(DX0 + 1, DY0 + 1, DX1 - 1, DY1 - 1, C["ember"])
fill(DX0 + 4, DY0 + 4, DX1 - 4, DY1 - 4, C["ember_hot"])

# ---------------------------------------------------------------- E: tassel
EX0, EY0, EX1, EY1 = 48, 64, 64, 96
fill(EX0, EY0, EX1, EY1, C["leather"])
fill(EX0, EY0, EX1, EY0 + 3, C["leather_lit"])
for x in range(EX0 + 1, EX1, 4):
    fill(x, EY0 + 3, x + 2, EY1 - 5, C["leather_lit"])
for x in range(EX0, EX1, 2):
    fill(x, EY1 - 5, x + 1, EY1, C["leather"])

# ---------------------------------------------------------------- F: leather
LX0, LY0, LX1, LY1 = 64, 0, 80, 32
fill(LX0, LY0, LX1, LY1, C["leather"])
for band in range(0, 32, 4):
    fill(LX0, LY0 + band, LX1, LY0 + band + 2, C["leather_lit"])

# ---------------------------------------------------------------- G: brass
BX0, BY0, BX1, BY1 = 80, 0, 96, 32
fill(BX0, BY0, BX1, BY1, C["brass"])
fill(BX0, BY0, BX1, BY0 + 5, C["brass_hi"])
fill(BX0, BY1 - 5, BX1, BY1, C["brass_dark"])

# ---------------------------------------------------------------- H: bevel
HX0, HY0, HX1, HY1 = 96, 0, 104, 32
fill(HX0, HY0, HX1, HY1, C["ob_dark"])
fill(HX0, HY0, HX0 + 1, HY1, C["edge"])
fill(HX0 + 1, HY0, HX0 + 3, HY1, C["ob_lit"])
fill(HX0 + 6, HY0, HX0 + 7, HY1, C["pin"])

# ---------------------------------------------------------------- preview
def preview(box, label, step=1):
    x0, y0, x1, y1 = box
    ramp = ".,:;+=xX#@"
    lums, clear = [], 0
    print(f"--- {label} {x0},{y0}-{x1},{y1} ---")
    for y in range(y0, y1, step):
        row = ""
        for x in range(x0, x1, step):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                row += " "
                clear += 1
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            lums.append(lum)
            row += ramp[min(9, max(1, int((lum / 0.45) * 9)))]
        print("   " + row)
    if lums:
        print(f"    [clear={clear} lum min/avg/max = "
              f"{min(lums):.2f}/{sum(lums) / len(lums):.2f}/{max(lums):.2f}]")
    print()


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    out = os.path.join(here, "src", "main", "resources", "assets",
                       "northstar_planet_addon", "textures", "item", "molten_core_sword.png")
    img.save(out, "PNG", optimize=True)
    print("wrote", out, os.path.getsize(out), "bytes",
          "| colours:", len({img.getpixel((x, y)) for x in range(W) for y in range(H) if img.getpixel((x, y))[3]}))
    if "--preview" in sys.argv:
        preview((0, 0, 32, 96), "A blade", 2)
        preview((32, 0, 64, 16), "B guard", 1)
        preview((64, 32, 96, 64), "C fuller", 2)
        preview((64, 0, 80, 32), "F leather", 1)


if __name__ == "__main__":
    main()
