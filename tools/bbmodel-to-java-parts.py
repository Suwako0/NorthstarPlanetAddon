#!/usr/bin/env python3
"""Port the Blockbench armour design (docs/models/molten_core_armor.bbmodel) into
the Java deco model generator.

Blockbench world space: y up, feet at y=0, model faces -Z, its right = +X.
Java HumanoidModel bone space: head/body pivot at y=24, arms at y=22, legs at y=12,
and vanilla's right_arm sits at -X. Because the set is symmetric we simply map the
+X limb group onto the java LEFT bone and vice versa.

Usage: python tools/bbmodel-to-java-parts.py [--dry-run]
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BB = os.path.join(HERE, "docs", "models", "molten_core_armor.bbmodel")
GEN = os.path.join(HERE, "tools", "gen-molten-core-armor.py")

REGION_BY_PREFIX = [
    ("glow_", "glow"),
    ("trim_", "trim"),
    ("chain_", "chain"),
    ("leather_", "trim"),
    ("plate_", "plate"),
]

# bb group -> (java bone, java pivot x, java pivot y)
# pivots are in HumanoidModel space: head/body 0, arms 2, legs 12 (y DOWN from the neck)
GROUP_MAP = {
    "head": ("head", 0.0, 0.0, False),
    "body": ("body", 0.0, 0.0, False),
    "right_arm": ("left_arm", 5.0, 2.0, False),   # bb right (+x) -> java left
    "left_arm": ("right_arm", -5.0, 2.0, False),  # bb left  (-x) -> java right
    "right_leg": ("left_leg", 1.9, 12.0, False),
    "left_leg": ("right_leg", -1.9, 12.0, False),
}


def load():
    with open(BB, encoding="utf-8") as f:
        return json.load(f)


def group_of(data):
    """element uuid -> group name"""
    names = {g["uuid"]: g["name"] for g in data.get("groups", [])}
    owner = {}
    for g in data.get("outliner", []):
        if isinstance(g, str):
            continue
        gname = names.get(g.get("uuid"), g.get("name"))
        for child in g.get("children", []):
            owner[child] = gname
    return owner


def region_for(name):
    for prefix, region in REGION_BY_PREFIX:
        if name.startswith(prefix):
            return region
    return None


def main():
    data = load()
    owner = group_of(data)
    parts = []
    skipped = []

    for el in data.get("elements", []):
        if el.get("type", "cube") != "cube":
            continue
        name = el["name"]
        if name.startswith("skin"):
            skipped.append(name)
            continue
        if name.startswith(("trim_core_ring", "glow_core")):
            skipped.append(name)     # rebuilt flat below (rotated bands)
            continue
        region = region_for(name)
        group = owner.get(el.get("uuid"))
        if region is None or group not in GROUP_MAP:
            skipped.append(name)
            continue
        bone, px, py, _ = GROUP_MAP[group]
        x0, y0, z0 = el["from"]
        x1, y1, z1 = el["to"]
        # bb world (y up, feet 0) -> HumanoidModel space (y DOWN from the neck at 24),
        # then relative to the bone pivot. The y range flips, so from/to swap.
        ly0 = round((24.0 - y1) - py, 2)
        ly1 = round((24.0 - y0) - py, 2)
        parts.append((
            name, bone,
            round(x0 - px, 2), ly0, round(z0, 2),
            round(x1 - px, 2), ly1, round(z1, 2),
            region,
        ))

    # The Blockbench cylinders are rotated bands; the java deco model is
    # axis-aligned only, so the core is rebuilt as two flat parts.
    # body-local y (y DOWN from the neck): world 16.6..21.4 -> local 2.6..7.4
    parts.append(("core_frame", "body", -3.2, 2.6, -3.05, 3.2, 7.4, -2.0, "trim"))
    parts.append(("core_gem", "body", -2.3, 3.1, -3.2, 2.3, 6.9, -3.0, "gem"))

    lines = ["PARTS = ["]
    for name, bone, x0, y0, z0, x1, y1, z1, region in sorted(parts, key=lambda p: (p[1], p[2])):
        lines.append(
            f'    ("{name}", "{bone}", {x0}, {y0}, {z0}, {x1}, {y1}, {z1}, "{region}"),')
    lines.append("]")

    print(f"ported {len(parts)} cubes, skipped {len(skipped)}: {skipped[:6]}")

    if "--dry-run" in sys.argv:
        print("\n".join(lines[:8]))
        return

    src = open(GEN, encoding="utf-8").read()
    start = src.index("PARTS = [")
    end = src.index("BONES = [")
    src = src[:start] + "\n".join(lines) + "\n\n" + src[end:]
    open(GEN, "w", encoding="utf-8", newline="\n").write(src)
    print("patched", GEN)


if __name__ == "__main__":
    main()
