#!/usr/bin/env python3
"""Move the generator's filler rings/gem off the blade tip onto the ricasso.

The generated model had `enrich_head_ring_*` and `enrich_head_gem_left` sitting
near the point, reading as crossbars on the blade. The reference has a clean
blade with a single collar where it meets the guard, so the three parts are
re-seated just above the guard instead.

Idempotent: skips if the collar is already in place.

Usage: python tools/patch-sword-collar.py
"""
import json
import os

# name -> (x0, y0, z0, x1, y1, z1)
MOVES = {
    "enrich_head_ring_low": (6.30, 11.85, 7.15, 9.70, 12.20, 8.85),
    "enrich_head_ring_high": (6.30, 12.65, 7.15, 9.70, 13.00, 8.85),
    "enrich_head_gem_left": (7.55, 12.15, 6.95, 8.45, 12.75, 7.20),
}

MARK = 11.85


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(here, "src", "main", "resources", "assets",
                        "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)

    by_name = {e["name"]: e for e in model["elements"]}
    if "enrich_head_ring_low" not in by_name:
        raise SystemExit("collar parts missing; model layout changed")
    if abs(by_name["enrich_head_ring_low"]["from"][1] - MARK) < 0.01:
        print("collar already in place, nothing to do")
        return

    for name, (x0, y0, z0, x1, y1, z1) in MOVES.items():
        el = by_name[name]
        el["from"] = [x0, y0, z0]
        el["to"] = [x1, y1, z1]

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"re-seated {len(MOVES)} collar parts at the ricasso")


if __name__ == "__main__":
    main()
