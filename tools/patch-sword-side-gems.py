#!/usr/bin/env python3
"""Pull the floating side gems back onto the guard.

`gem_side_left/right` were generated at z 3.75..4.10 while the guard itself sits
at z 6.9..9.1, so the two gems hung 3 units in front of the crossguard. They are
re-seated on the guard face (z 6.72..6.98) where they read as rivet-sized stones
instead of floating lamps.

Idempotent: skips when already seated.

Usage: python tools/patch-sword-side-gems.py
"""
import json
import os

TARGET_Z = (6.72, 6.98)
PARTS = ("gem_side_left", "gem_side_right")


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(here, "src", "main", "resources", "assets",
                        "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)
    by_name = {e["name"]: e for e in model["elements"]}
    for name in PARTS:
        if name not in by_name:
            raise SystemExit(f"{name} missing; model layout changed")
    if abs(by_name["gem_side_left"]["from"][2] - TARGET_Z[0]) < 0.01:
        print("side gems already seated")
        return
    for name in PARTS:
        el = by_name[name]
        el["from"][2], el["to"][2] = TARGET_Z
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"seated {', '.join(PARTS)} at z {TARGET_Z[0]}..{TARGET_Z[1]}")


if __name__ == "__main__":
    main()
