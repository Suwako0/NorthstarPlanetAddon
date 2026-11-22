#!/usr/bin/env python3
"""Trim the guard's thin plates so the crossguard reads as a bar, not a tsuba.

`ring_thin_low/high` were generated as 8.24 x 8.24 flaps (x AND z), which gives
the sword a wide flat disc around the guard. The reference has a chunky bowtie
crossguard that is wide in x but shallow in z, so the depth is pulled in to a
plate 2.5 units thick.

Idempotent: skips when already trimmed.

Usage: python tools/patch-sword-guard.py
"""
import json
import os

TARGET_Z = (6.75, 9.25)
PARTS = ("ring_thin_low", "ring_thin_high")


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
    if abs(by_name["ring_thin_low"]["from"][2] - TARGET_Z[0]) < 0.01:
        print("guard plates already trimmed")
        return

    for name in PARTS:
        el = by_name[name]
        el["from"][2], el["to"][2] = TARGET_Z

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"trimmed {', '.join(PARTS)} to z {TARGET_Z[0]}..{TARGET_Z[1]}")


if __name__ == "__main__":
    main()
