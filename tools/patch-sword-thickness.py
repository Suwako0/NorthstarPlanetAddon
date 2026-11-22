#!/usr/bin/env python3
"""Thin the blade down to 0.8 units and keep the raised parts flush.

The first pass left the blade 1.5 units thick on a 3.2-wide blade (and the
fuller actually wider than the blade at 1.8), which reads as a slab rather than
a sword. Everything on the blade is pulled in to a 0.8 core; the fuller and the
edge strips stay a hair proud so they do not z-fight.

Idempotent: skips when the blade is already thin.

Usage: python tools/patch-sword-thickness.py
"""
import json
import os

BLADE_Z = (7.65, 8.35)
FULLER_Z = (7.61, 8.39)
EDGE_Z = (7.67, 8.33)

GROUPS = {
    BLADE_Z: ("blade_seg_1", "blade_seg_2", "blade_seg_3",
              "blade_tip_1", "blade_tip_2", "blade_tip_3"),
    FULLER_Z: ("fuller_upper", "fuller_lower"),
    EDGE_Z: ("edge_left", "edge_right"),
}


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(here, "src", "main", "resources", "assets",
                        "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)

    by_name = {e["name"]: e for e in model["elements"]}
    if "blade_seg_1" not in by_name:
        raise SystemExit("blade_seg_1 missing; model layout changed")
    if abs(by_name["blade_seg_1"]["from"][2] - BLADE_Z[0]) < 0.005:
        print("blade already thin")
        return

    touched = 0
    for (z0, z1), names in GROUPS.items():
        for name in names:
            el = by_name.get(name)
            if el is None:
                continue
            el["from"][2], el["to"][2] = z0, z1
            touched += 1

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"thinned {touched} blade parts to z {BLADE_Z[0]}..{BLADE_Z[1]} "
          f"(was 1.5 units thick, now {BLADE_Z[1] - BLADE_Z[0]:.2f})")


if __name__ == "__main__":
    main()
