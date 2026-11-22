#!/usr/bin/env python3
"""Convert a Blockbench .bbmodel into GeckoLib game resources.

Writes:
  assets/<ns>/animations/entity/<name>.animation.json   (Bedrock 1.8.0 schema, GeckoLib-ready)
  assets/<ns>/textures/entity/<name>.png                (the bbmodel's painted texture)
  assets/<ns>/textures/entity/<name>_glow.png           (its emissive mask, if present)

The geometry itself is exported from Blockbench (bedrock codec ->
assets/<ns>/geo/entity/<name>.geo.json); this script only handles the two
things Blockbench's MCP export does not: the animation file and the textures.

Usage: python tools/bbmodel-to-geckolib.py [--name hephaestus] [--ns northstar_planet_addon] [--dry-run]
"""
import argparse
import base64
import json
import os

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

EASING = {"catmullrom": "easeInOutSine", "bezier": "easeInOutSine", "linear": "linear", "step": "step"}


def time_key(t):
    """Bedrock/GeckoLib timestamps: JSON object keys, always with a decimal point ("0.0", "0.375")."""
    s = f"{t:.3f}".rstrip("0")
    if s.endswith("."):
        s += "0"
    return s


def convert_animation(anim, uuid_to_bone):
    bones = {}
    for group_uuid, animator in (anim.get("animators") or {}).items():
        bone = uuid_to_bone.get(group_uuid)
        if not bone:
            continue
        # Blockbench stores every channel in one flat "keyframes" list per bone
        channels = {}
        for kf in sorted(animator.get("keyframes") or [], key=lambda k: k["time"]):
            channel = kf.get("channel", "rotation")
            if channel not in ("rotation", "position", "scale"):
                continue
            point = (kf.get("data_points") or [{}])[0]
            vec = [round(float(point.get("x", 0)), 4), round(float(point.get("y", 0)), 4),
                   round(float(point.get("z", 0)), 4)]
            entry = {"vector": vec}
            # GeckoLib defaults a keyframe with no "easing" to LINEAR; emit it always so the
            # smooth cycles stay smooth. "easeInOutSine" is what the shipped Ares animation uses.
            easing = EASING.get(kf.get("interpolation", "catmullrom"))
            if easing:
                entry["easing"] = easing
            channels.setdefault(channel, {})[time_key(kf["time"])] = entry
        if channels:
            bones[bone] = channels
    out = {"loop": anim.get("loop", "once") == "loop", "animation_length": anim.get("length", 1.0)}
    out["bones"] = bones
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", default="hephaestus")
    ap.add_argument("--ns", default="northstar_planet_addon")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    bb_path = os.path.join(HERE, "docs", "models", f"{args.name}.bbmodel")
    data = json.load(open(bb_path, encoding="utf-8"))

    uuid_to_bone = {g["uuid"]: g["name"] for g in data.get("groups", [])}

    animations = {}
    for anim in data.get("animations", []):
        short = anim["name"].split(".")[-1]
        animations[short] = convert_animation(anim, uuid_to_bone)

    anim_dir = os.path.join(HERE, "src", "main", "resources", "assets", args.ns, "animations", "entity")
    tex_dir = os.path.join(HERE, "src", "main", "resources", "assets", args.ns, "textures", "entity")
    anim_out = os.path.join(anim_dir, f"{args.name}.animation.json")

    print(f"animations: {', '.join(f'{k} ({v['animation_length']}s, loop={v['loop']})' for k, v in animations.items())}")
    for name, anim in animations.items():
        nbones = len(anim["bones"])
        nkeys = sum(len(ch) for b in anim["bones"].values() for ch in b.values())
        print(f"  {name:16s} bones={nbones:2d} keyframes={nkeys}")

    tex_outs = []
    for tex in data.get("textures", []):
        src = tex.get("source") or ""
        if not src.startswith("data:image"):
            continue
        png = base64.b64decode(src.split(",", 1)[1])
        tname = tex.get("name", "texture")
        # GeckoLib's AutoGlowingGeoLayer looks for "<texture>_glowmask.png"
        if tname == args.name or not tex_outs:
            suffix = ""
        elif "glow" in tname:
            suffix = "_glowmask"
        else:
            suffix = "_" + tname.replace(args.name + "_", "")
        target = os.path.join(tex_dir, f"{args.name}{suffix}.png")
        tex_outs.append((target, len(png)))

    if args.dry_run:
        print("dry run — nothing written")
        return

    os.makedirs(anim_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)
    with open(anim_out, "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"format_version": "1.8.0", "animations": animations}, fh, indent=1)
    print(f"wrote {anim_out}")

    for tex, (target, size) in zip([t for t in data["textures"] if (t.get("source") or "").startswith("data:image")], tex_outs):
        png = base64.b64decode(tex["source"].split(",", 1)[1])
        with open(target, "wb") as fh:
            fh.write(png)
        print(f"wrote {target} ({len(png)} bytes)")


if __name__ == "__main__":
    main()
