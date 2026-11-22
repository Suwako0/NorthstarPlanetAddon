#!/usr/bin/env python3
"""Off-axis-free front-view preview of molten_core_sword (no Blockbench needed).

Projects the model's north faces orthographically, samples the atlas with
nearest-neighbour filtering and paints back-to-front, so UV mistakes show up
as misplaced texture rather than being hidden by a flat fill.

Usage: python tools/preview-molten-core-sword.py [--scale 10] [--ascii]
"""
import json
import os
import sys

from PIL import Image

S = 10


def load(here):
    model_path = os.path.join(here, "src", "main", "resources", "assets",
                              "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    tex_path = os.path.join(here, "src", "main", "resources", "assets",
                            "northstar_planet_addon", "textures", "item", "molten_core_sword.png")
    with open(model_path, encoding="utf-8") as f:
        model = json.load(f)
    tex = Image.open(tex_path).convert("RGBA")
    return model, tex


def render(model, tex, scale, view="front"):
    """view: 'front' (camera on -Z, north faces) or 'side' (camera on -X, east faces)."""
    els = model["elements"]
    xs = [e["from"][0] for e in els] + [e["to"][0] for e in els]
    ys = [e["from"][1] for e in els] + [e["to"][1] for e in els]
    zs = [e["from"][2] for e in els] + [e["to"][2] for e in els]
    xmin, xmax, ymin, ymax = min(xs), max(xs), min(ys), max(ys)
    zmin, zmax = min(zs), max(zs)
    pad = 1.0
    if view == "front":
        span = xmax - xmin
        base = xmin
    else:
        span = zmax - zmin
        base = zmin
    w = max(4, int((span + 2 * pad) * scale))
    h = int((ymax - ymin + 2 * pad) * scale)
    canvas = Image.new("RGBA", (w, h), (0x8A, 0x86, 0x80, 255))
    out = canvas.load()
    tx = tex.load()
    tw, th = tex.size
    face_key = "north" if view == "front" else "east"
    depth_key = 2 if view == "front" else 0

    def to_px(horiz, y):
        return ((horiz - base + pad) * scale, (ymax + pad - y) * scale)

    # far to near along the view axis (camera sits at -inf on that axis)
    ordered = sorted(els, key=lambda e: -e["from"][depth_key])
    drawn = 0
    for el in ordered:
        f, t = el["from"], el["to"]
        h_from = f[depth_key] if view == "side" else f[0]
        h_to = t[depth_key] if view == "side" else t[0]
        if h_to - h_from <= 0 or t[1] - f[1] <= 0:
            continue
        face = el["faces"].get(face_key)
        if not face:
            continue
        u0, v0, u1, v1 = face["uv"]
        x0, y0 = to_px(h_from, t[1])
        x1, y1 = to_px(h_to, f[1])
        for py in range(int(y0), int(y1)):
            ty = min(1.0, max(0.0, (py - y0) / max(1e-6, (y1 - y0))))
            v = v0 + ty * (v1 - v0)
            sy = min(th - 1, max(0, int(v * th / 16)))
            for px_ in range(int(x0), int(x1)):
                txp = min(1.0, max(0.0, (px_ - x0) / max(1e-6, (x1 - x0))))
                u = u0 + txp * (u1 - u0)
                sx = min(tw - 1, max(0, int(u * tw / 16)))
                c = tx[sx, sy]
                if c[3] < 8:
                    continue
                if 0 <= px_ < w and 0 <= py < h:
                    out[px_, py] = c
        drawn += 1
    return canvas, drawn


def ascii_view(img, cols=56, ramp=".,:;+=xX#@"):
    w, h = img.size
    step_x = max(1, w // cols)
    step_y = step_x * 2
    px = img.load()
    bg = (0x8A, 0x86, 0x80)
    lines = []
    for y in range(0, h, step_y):
        row = ""
        for x in range(0, w, step_x):
            r, g, b, a = px[x, y]
            if (r, g, b) == bg:
                row += " "
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            row += ramp[min(9, max(1, int(lum / 0.45 * 9)))]
        lines.append(row)
    return "\n".join(lines)


def main():
    scale = S
    if "--scale" in sys.argv:
        scale = int(sys.argv[sys.argv.index("--scale") + 1])
    view = "front"
    if "--view" in sys.argv:
        view = sys.argv[sys.argv.index("--view") + 1]
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    model, tex = load(here)
    img, drawn = render(model, tex, scale, view)
    suffix = "" if view == "front" else f"-{view}"
    out_path = os.path.join(here, "build", f"preview-molten_core_sword{suffix}.png")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    img.save(out_path)
    print(f"drew {drawn} {view} faces -> {out_path} ({img.size[0]}x{img.size[1]})")
    if "--ascii" in sys.argv:
        print(ascii_view(img))


if __name__ == "__main__":
    main()
