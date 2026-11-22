#!/usr/bin/env python3
"""Export the Blockbench project's own texture + box-UV offsets.

The point: the in-game model must use EXACTLY the pixels and UV layout shown in
Blockbench, otherwise the two look unrelated. So:
  1. texture stored in the .bbmodel  ->  assets/.../molten_core_armor.png
  2. each cube's box-UV offset       ->  PARTS (u, v) for CubeListBuilder.texOffs

Usage: python tools/bbmodel-export.py [--dry-run]
"""
import base64
import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BB = os.path.join(HERE, "docs", "models", "molten_core_armor.bbmodel")
TEX = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon",
                   "textures", "models", "armor", "molten_core_armor.png")
GEN = os.path.join(HERE, "tools", "gen-molten-core-armor.py")

GROUP_MAP = {
    "head": ("head", 0.0, 0.0),
    "body": ("body", 0.0, 0.0),
    "right_arm": ("left_arm", 5.0, 2.0),    # bb right (+x) -> java left
    "left_arm": ("right_arm", -5.0, 2.0),
    "right_leg": ("left_leg", 1.9, 12.0),
    "left_leg": ("right_leg", -1.9, 12.0),
}


def main():
    data = json.load(open(BB, encoding="utf-8"))
    names = {g["uuid"]: g["name"] for g in data.get("groups", [])}
    owner = {}
    for g in data.get("outliner", []):
        if isinstance(g, str):
            continue
        gname = names.get(g.get("uuid"))
        for child in g.get("children", []):
            owner[child] = gname

    # ---- 1. the texture exactly as painted in Blockbench
    src = data["textures"][0]["source"]
    b64 = src.split(",", 1)[1]
    png = base64.b64decode(b64)
    if "--dry-run" not in sys.argv:
        open(TEX, "wb").write(png)
    print(f"texture exported: {len(png)} bytes -> {TEX}")

    # ---- 2. geometry + box-UV offsets
    parts, skipped = [], []
    for el in data.get("elements", []):
        name = el["name"]
        if name.startswith("skin"):
            skipped.append(name)
            continue
        group = owner.get(el.get("uuid"))
        if group not in GROUP_MAP:
            skipped.append(name)
            continue
        bone, px, py = GROUP_MAP[group]
        x0, y0, z0 = el["from"]
        x1, y1, z1 = el["to"]
        # bake Blockbench's per-cube inflate (used to kill coplanar z-fighting)
        # into the box, because the Java exporter has no inflate field
        inf = el.get("inflate") or 0
        if inf:
            x0 -= inf; y0 -= inf; z0 -= inf
            x1 += inf; y1 += inf; z1 += inf
        u, v = el.get("uv_offset", [0, 0])
        # NOTE: no shell clearance here. The clearance is baked into the bbmodel
        # BEFORE pack_uv, otherwise box-UV islands no longer match the boxes.
        parts.append((
            name, bone,
            round(x0 - px, 2), round((24.0 - y1) - py, 2), round(z0, 2),
            round(x1 - px, 2), round((24.0 - y0) - py, 2), round(z1, 2),
            int(round(u)), int(round(v)),
        ))

    # NOTE: no mirroring here. The bbmodel builds both sides with exact mirrored
    # coordinates, and mirroring again produced duplicate "*_left_left" parts.

    lines = ["PARTS = ["]
    for p in sorted(parts, key=lambda p: (p[1], p[2])):
        lines.append("    (" + ", ".join(
            [f'"{p[0]}"', f'"{p[1]}"'] + [str(v) for v in p[2:8]] + [str(p[8]), str(p[9])]
        ) + "),")
    lines.append("]")

    print(f"parts: {len(parts)}  skipped: {len(skipped)}")
    if "--dry-run" in sys.argv:
        print("\n".join(lines[:6]))
        return

    gen = open(GEN, encoding="utf-8").read()
    start = gen.index("PARTS = [")
    end = gen.index("BONES = [")
    gen = gen[:start] + "\n".join(lines) + "\n\n" + gen[end:]
    open(GEN, "w", encoding="utf-8", newline="\n").write(gen)
    print("patched", GEN)


if __name__ == "__main__":
    main()
