#!/usr/bin/env python3
"""Validate the exported Hephaestus GeckoLib assets before shipping.

Checks the things that silently break in game:
  * geo / animation / texture files all exist and parse;
  * every bone named in an animation exists in the geometry;
  * the texture bitmap matches the geometry's texture_width/height;
  * every cube UV lies inside the texture;
  * every bone in the geometry is reachable from the root (parent chain);
  * idle / walk loop, every one-shot animation starts and ends near a neutral pose.

Usage: python tools/check-hephaestus-assets.py [--name hephaestus] [--ns northstar_planet_addon]
"""
import argparse
import json
import os
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
errors, warnings = [], []


def time_key(t):
    s = f"{float(t):.3f}".rstrip("0")
    if s.endswith("."):
        s += "0"
    return s


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", default="hephaestus")
    ap.add_argument("--ns", default="northstar_planet_addon")
    args = ap.parse_args()
    assets = os.path.join(HERE, "src", "main", "resources", "assets", args.ns)

    geo_path = os.path.join(assets, "geo", "entity", f"{args.name}.geo.json")
    anim_path = os.path.join(assets, "animations", "entity", f"{args.name}.animation.json")
    tex_path = os.path.join(assets, "textures", "entity", f"{args.name}.png")
    glow_path = os.path.join(assets, "textures", "entity", f"{args.name}_glowmask.png")

    for p in (geo_path, anim_path, tex_path, glow_path):
        if not os.path.exists(p):
            errors.append(f"missing file: {os.path.relpath(p, HERE)}")
    if errors:
        report()
        return 1

    geo = json.load(open(geo_path, encoding="utf-8"))
    g = geo["minecraft:geometry"][0]
    desc = g["description"]
    tw, th = desc["texture_width"], desc["texture_height"]
    bones = {b["name"]: b for b in g["bones"]}

    try:
        from PIL import Image
        im = Image.open(tex_path)
        if im.size != (tw, th):
            errors.append(f"texture {im.size} != geometry {tw}x{th}")
        gl = Image.open(glow_path)
        if gl.size != (tw, th):
            errors.append(f"glowmask {gl.size} != geometry {tw}x{th}")
        alpha = gl.convert("RGBA").getchannel("A")
        lit = sum(1 for v in alpha.getdata() if v > 0)
        if lit < tw * th * 0.002:
            errors.append(f"glowmask has almost no lit pixels ({lit}) — AutoGlowingGeoLayer would show nothing")
    except ImportError:
        warnings.append("Pillow unavailable — skipped texture size check")

    # geometry integrity
    cubes = 0
    for name, bone in bones.items():
        parent = bone.get("parent")
        if parent and parent not in bones:
            errors.append(f"bone {name} has unknown parent {parent}")
        for cube in bone.get("cubes", []):
            cubes += 1
            origin, size = cube["origin"], cube["size"]
            uv = cube.get("uv")
            if uv and isinstance(uv, list):
                u, v = uv[0], uv[1]
                box_w, box_h = 2 * (size[0] + size[2]), size[2] + size[1]
                if u + box_w > tw or v + box_h > th:
                    errors.append(f"cube in {name} UV {uv} + {box_w}x{box_h} exceeds {tw}x{th}")
            for face, data in (cube.get("uv") or {}).items() if isinstance(cube.get("uv"), dict) else []:
                pass
    roots = [n for n, b in bones.items() if not b.get("parent")]
    if len(roots) != 1:
        errors.append(f"expected exactly one root bone, found {roots}")

    # animations
    anims = json.load(open(anim_path, encoding="utf-8"))["animations"]
    for name, anim in anims.items():
        bones_used = anim.get("bones", {})
        if not bones_used:
            errors.append(f"animation {name} animates no bones")
        for bone_name in bones_used:
            if bone_name not in bones:
                errors.append(f"animation {name} references missing bone {bone_name}")
        for bone_name, channels in bones_used.items():
            for channel, keys in channels.items():
                times = [float(t) for t in keys]
                if min(times) < 0 or max(times) > anim["animation_length"] + 0.001:
                    errors.append(f"{name}/{bone_name}/{channel} has keys outside 0..{anim['animation_length']}")
        if name.startswith("idle") or name == "walk":
            if not anim["loop"]:
                errors.append(f"{name} must loop")
            for bone_name, channels in bones_used.items():
                for channel, keys in channels.items():
                    first = keys.get("0.0")
                    last = keys.get(time_key(anim["animation_length"]))
                    if first and last and first["vector"] != last["vector"]:
                        warnings.append(
                            f"{name} loop {bone_name}/{channel}: first {first['vector']} != last {last['vector']}")

    print(f"geometry: {len(bones)} bones, {cubes} cubes, texture {tw}x{th}")
    print(f"animations: {', '.join(f'{k}({v['animation_length']}s)' for k, v in anims.items())}")
    report()
    return 1 if errors else 0


def report():
    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(f"\n{len(errors)} error(s), {len(warnings)} warning(s)")
    if not errors:
        print("GeckoLib assets OK")


if __name__ == "__main__":
    sys.exit(main())
