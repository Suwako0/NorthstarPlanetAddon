#!/usr/bin/env python3
"""Re-map molten_core_sword UVs onto the hand-painted atlas regions.

Why: the generator gave every blade segment (and many small parts) the same UV
rectangle, so the blade showed the same picture four times and all fittings
sampled one mixed area. This script assigns each part the region it belongs to.

Usage: python tools/remap-molten-core-sword-uv.py [--check]
"""
import json
import os
import sys

# uv rectangles in 0..16 space (texture 128 -> 1 unit = 8 px)
REGIONS = {
    "blade_base": (0, 9, 4, 12),      # px (0,72)-(32,96)
    "blade_low": (0, 6, 4, 9),        # px (0,48)-(32,72)
    "blade_high": (0, 3, 4, 6),       # px (0,24)-(32,48)
    "blade_tip": (0, 0, 4, 3),        # px (0,0)-(32,24)
    "tip_a": (0, 0, 4, 0.8),
    "tip_b": (0, 0.8, 4, 2.1),
    "tip_c": (0, 2.1, 4, 3.0),
    "fuller_low": (8, 6, 12, 8),      # px (64,48)-(96,64)
    "fuller_high": (8, 4, 12, 6),     # px (64,32)-(96,48)
    "bevel": (12, 0, 13, 4),          # px (96,0)-(104,32)
    "guard": (4, 0, 8, 2),            # px (32,0)-(64,16)
    "brass": (10, 0, 12, 4),          # px (80,0)-(96,32)
    "leather": (8, 0, 10, 4),         # px (64,0)-(80,32)
    "gem": (4, 8, 6, 10),             # px (32,64)-(48,80)
    "tassel": (6, 8, 8, 12),          # px (48,64)-(64,96)
}

ASSIGN = {
    # continuous blade: tip at the top of the strip, base at the bottom
    "blade_tip_1": "tip_c",
    "blade_tip_2": "tip_b",
    "blade_tip_3": "tip_a",
    "blade_seg_3": "blade_high",
    "blade_seg_2": "blade_low",
    "blade_seg_1": "blade_base",
    "fuller_upper": "fuller_high",
    "fuller_lower": "fuller_low",
    "edge_left": "bevel",
    "edge_right": "bevel",
    "notch_left": "blade_base",
    "notch_right": "blade_base",
    # charcoal iron parts
    "guard_bar": "guard",
    "guard_plate_left": "guard",
    "guard_plate_right": "guard",
    "gem_setting": "guard",
    "pommel_core": "guard",
    "ring_thin_low": "guard",
    "ring_thin_high": "guard",
    "enrich_head_ring_low": "guard",
    "enrich_head_ring_high": "guard",
    # brass fittings
    "guard_orb_left": "guard",   # dark iron end blocks, brass stays on the rivets
    "guard_orb_right": "guard",
    "rivet_left": "brass",
    "rivet_right": "brass",
    "stud_1": "brass",
    "stud_2": "brass",
    "stud_3": "brass",
    "stud_4": "brass",
    "ferrule_lower": "brass",
    "ferrule_upper": "brass",
    "pommel_cap": "brass",
    "pommel_ring": "brass",
    # leather grip
    "grip_core": "leather",
    "wrap_1": "leather",
    "wrap_2": "leather",
    "wrap_3": "leather",
    "wrap_4": "leather",
    # gems
    "gem": "gem",
    "gem_side_left": "gem",
    "gem_side_right": "gem",
    "pommel_gem": "brass",   # reference has a brass knob, not a gem
    "enrich_head_gem_left": "gem",
    # tassel
    "tassel_cord": "tassel",
    "tassel_knot": "tassel",
    "tassel_tail_left": "tassel",
    "tassel_tail_right": "tassel",
}


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(here, "src", "main", "resources", "assets",
                        "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)

    missing = []
    changed = 0
    for el in model["elements"]:
        name = el["name"]
        region = ASSIGN.get(name)
        if region is None:
            missing.append(name)
            continue
        u0, v0, u1, v1 = REGIONS[region]
        for face in el["faces"].values():
            face["uv"] = [u0, v0, u1, v1]
        changed += 1

    if "--check" in sys.argv:
        print(f"elements with an explicit region: {changed}/{len(model['elements'])}")
        if missing:
            print("NOT mapped (kept as-is):", ", ".join(missing))
        return

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"remapped {changed}/{len(model['elements'])} elements ->", path)
    if missing:
        print("left untouched:", ", ".join(missing))


if __name__ == "__main__":
    main()
