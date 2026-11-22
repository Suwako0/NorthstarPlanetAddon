#!/usr/bin/env python3
"""Replace the flat-ended blade_seg_4 with a three-step tapered point.

The reference sword has a long triangular tip; a single box ends flat and reads
as a cut-off plank in game. Three stacked boxes of decreasing width give a
staircase point that still looks sharp at item-icon size.

Idempotent: does nothing if the tip has already been replaced.

Usage: python tools/patch-sword-tip.py
"""
import json
import os

TIP_STEPS = [
    # name, y_from, y_to, x_half_width
    ("blade_tip_1", 25.725, 28.40, 0.992),
    ("blade_tip_2", 28.40, 29.60, 0.700),
    ("blade_tip_3", 29.60, 30.50, 0.380),
]


def main():
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    path = os.path.join(here, "src", "main", "resources", "assets",
                        "northstar_planet_addon", "models", "item", "molten_core_sword.json")
    with open(path, encoding="utf-8") as f:
        model = json.load(f)

    names = [e["name"] for e in model["elements"]]
    if "blade_tip_1" in names:
        print("tip already patched, nothing to do")
        return
    if "blade_seg_4" not in names:
        raise SystemExit("blade_seg_4 not found; model layout changed")

    idx = names.index("blade_seg_4")
    base = model["elements"][idx]
    template_faces = {k: dict(v) for k, v in base["faces"].items()}
    template_faces = json.loads(json.dumps(base["faces"]))

    new_elements = []
    for name, y0, y1, half in TIP_STEPS:
        el = {
            "name": name,
            "from": [round(8.0 - half, 4), y0, base["from"][2]],
            "to": [round(8.0 + half, 4), y1, base["to"][2]],
        }
        for key in base:
            if key not in ("name", "from", "to", "faces"):
                el[key] = base[key]
        el["faces"] = json.loads(json.dumps(template_faces))
        new_elements.append(el)

    model["elements"][idx:idx + 1] = new_elements
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"replaced blade_seg_4 with {len(new_elements)} tip steps "
          f"(elements {len(names)} -> {len(model['elements'])})")


if __name__ == "__main__":
    main()
