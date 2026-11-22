#!/usr/bin/env python3
"""Measure the Hephaestus reference sheet (front / side / back) and cut per-view crops.

The sheet is a contact sheet on a light grey background.  `--dump` writes
`docs/previews/hephaestus-<view>.png` crops (transparent background) so each view
can be scored with compare_reference, and a mask preview.

Prints, per view: bbox, the height-normalised width profile, shoulder / waist /
fist landmarks, and colour shares (rust armour vs dark steel vs hot glow).

Usage: python tools/measure-hephaestus-ref.py [--dump]
"""
import os
import sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REF = os.path.join(HERE, "docs", "previews", "hephaestus-reference.jpg")
OUT = os.path.join(HERE, "docs", "previews")

# panel x ranges, found from the empty-column runs of the sheet (gap 2219..2363)
PANELS = [("front", 0, 1170), ("side", 1170, 2219), ("back", 2364, 3546)]
Y0 = 100          # drop the FRONT / SIDE / BACK label row


def main():
    img = Image.open(REF).convert("RGB")
    a = np.asarray(img).astype(np.int16)
    bg = np.median(np.concatenate([a[0:6].reshape(-1, 3), a[-6:].reshape(-1, 3)]), axis=0)
    mask = np.abs(a - bg).sum(axis=2) > 110
    mask[:Y0] = False

    if "--dump" in sys.argv:
        Image.fromarray((mask * 255).astype(np.uint8)).save(os.path.join(OUT, "hephaestus-ref-mask.png"))

    for name, x0, x1 in PANELS:
        sub = mask[:, x0:x1]
        rows = np.where(sub.any(axis=1))[0]
        cols = np.where(sub.any(axis=0))[0]
        top, bot, left, right = rows[0], rows[-1], cols[0], cols[-1]
        h = bot - top + 1
        w = right - left + 1
        widths = np.array([(np.where(sub[y])[0][-1] - np.where(sub[y])[0][0] + 1) if sub[y].any() else 0
                           for y in range(top, bot + 1)])
        print(f"\n=== {name.upper()}  {w}x{h}px  w/h={w / h:.3f}  bbox x[{x0 + left},{x0 + right}] y[{top},{bot}]")
        print("   y%    width   width/H")
        for i in range(25):
            k = int(round((h - 1) * i / 24.0))
            print(f"  {100 * i // 24:3d}%  {widths[k]:6d}   {widths[k] / h:6.3f}")
        print(f"  widest: y={100 * int(np.argmax(widths)) / h:.1f}%  {widths.max()}px = {widths.max() / h:.3f} x H")

        crop = a[:, x0:x1]
        px = crop[sub]
        r, g, b = (px[:, i].astype(int) for i in range(3))
        total = max(1, len(px))
        glow = ((r > 140) & (g > 60) & (b < 120) & (r - b > 60)).sum()
        rust = ((r > 45) & (r - g > 18) & (r - b > 18) & (r < 170)).sum()
        steel = ((abs(r - g) < 22) & (abs(g - b) < 22)).sum()
        print(f"  glow={100 * glow / total:.1f}%  rust={100 * rust / total:.1f}%  steel/dark={100 * steel / total:.1f}%")

        if "--dump" in sys.argv:
            rgba = np.dstack([np.asarray(img)[:, x0:x1], (sub * 255).astype(np.uint8)])
            Image.fromarray(rgba[top:bot + 1, left:right + 1]).save(os.path.join(OUT, f"hephaestus-{name}.png"))
            print(f"  crop -> docs/previews/hephaestus-{name}.png  ({w}x{h})")


if __name__ == "__main__":
    main()
