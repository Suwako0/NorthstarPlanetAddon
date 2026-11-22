#!/usr/bin/env python3
"""Export a Blockbench .bbmodel into a Java 3D item model + its texture.

Geometry and texture come from the SAME .bbmodel (the equipment pipeline's rule):
  docs/models/<name>.bbmodel
    -> assets/<ns>/models/item/<name>.json   (elements + texture_size + display)
    -> assets/<ns>/textures/item/<name>.png  (the bbmodel's painted texture)

The bbmodel is built at ``--scale`` times the 16-unit item space (so box UV islands
get that many pixels per game unit); coordinates are divided by the scale on export.

Usage:
  python tools/bbmodel-to-item-model.py --name molten_core_sword --scale 8
"""
import argparse
import base64
import json
import os

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 沿用旧模型的持握姿态（第三方称/第一人称 各两组 + gui/ground/fixed/head）
DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 50], "translation": [0, 1.5, 0.5], "scale": [0.8, 0.8, 0.8]},
    "thirdperson_lefthand": {"rotation": [0, 90, -50], "translation": [0, 1.5, 0.5], "scale": [0.8, 0.8, 0.8]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.66, 0.66, 0.66]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.66, 0.66, 0.66]},
    "ground": {"translation": [0, 4, 0], "scale": [1, 1, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, -1], "scale": [2, 2, 1]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 6, 0], "scale": [1.5, 1.5, 1.5]},
    "gui": {"rotation": [0, 0, 0], "translation": [4, 0.25, 0], "scale": [0.5, 0.5, 0.5]},
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", default="molten_core_sword")
    ap.add_argument("--ns", default="northstar_planet_addon")
    ap.add_argument("--scale", type=float, default=8.0, help="bbmodel 相对 16 单位空间的放大倍数")
    args = ap.parse_args()

    bb_path = os.path.join(HERE, "docs", "models", f"{args.name}.bbmodel")
    data = json.load(open(bb_path, encoding="utf-8"))
    tex_w = data.get("resolution", {}).get("width") or 256
    tex_h = data.get("resolution", {}).get("height") or 256
    # 贴图位图的真实尺寸更权威（pack_uv 可能把项目尺寸撑大）
    for tex in data.get("textures", []):
        src = tex.get("source") or ""
        if src.startswith("data:image"):
            try:
                from PIL import Image
                import io
                img = Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1])))
                tex_w, tex_h = img.size
            except Exception:
                pass
            break

    elems = []
    for el in data.get("elements", []):
        if el.get("type", "cube") != "cube":
            continue
        # fix_z_fighting 的 inflate 必须烘进坐标：Java 物品模型没有 inflate 字段
        inflate = el.get("inflate") or 0.0
        from_ = [round((v - inflate) / args.scale, 3) for v in el["from"]]
        to_ = [round((v + inflate) / args.scale, 3) for v in el["to"]]
        faces = {}
        for face_name, face in (el.get("faces") or {}).items():
            uv = face.get("uv")
            if not uv:
                continue
            faces[face_name] = {
                "uv": [round(uv[0] * 16.0 / tex_w, 3), round(uv[1] * 16.0 / tex_h, 3),
                       round(uv[2] * 16.0 / tex_w, 3), round(uv[3] * 16.0 / tex_h, 3)],
                "texture": "#all",
            }
            rotation = face.get("rotation", 0)
            if rotation:
                faces[face_name]["rotation"] = rotation
        if not faces:
            continue
        elem = {"name": el.get("name", "part"), "from": from_, "to": to_, "faces": faces}
        if el.get("rotation"):
            elem["rotation"] = {"origin": [round(v / args.scale, 3) for v in el.get("origin", [8, 8, 8])],
                                "axis": "y", "angle": el["rotation"][1], "rescale": False}
        elems.append(elem)

    model = {
        # 关键：NeoForge 只有在模型 isCustomRenderer() 为真时才会去问 IClientItemExtensions 要
        # GeckoLib 的 BEWLR；而 isCustomRenderer 来自父模型 builtin/entity。少了这一行，
        # 剑会一直用静态 elements 渲染（看起来"动画没实装"）。
        # elements 仍保留：万一 BEWLR 不可用，还能退化成静态模型而不是看不见。
        "gui_light": "front",
        "texture_size": [tex_w, tex_h],
        "textures": {"all": f"{args.ns}:item/{args.name}", "particle": f"{args.ns}:item/{args.name}"},
        "display": DISPLAY,
        "elements": elems,
    }

    model_dir = os.path.join(HERE, "src", "main", "resources", "assets", args.ns, "models", "item")
    tex_dir = os.path.join(HERE, "src", "main", "resources", "assets", args.ns, "textures", "item")
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)

    model_path = os.path.join(model_dir, f"{args.name}.json")
    with open(model_path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(model, fh, indent=1)
        fh.write("\n")

    tex_path = os.path.join(tex_dir, f"{args.name}.png")
    written = 0
    for tex in data.get("textures", []):
        src = tex.get("source") or ""
        if src.startswith("data:image"):
            with open(tex_path, "wb") as fh:
                fh.write(base64.b64decode(src.split(",", 1)[1]))
            written += 1
            break

    print(f"elements {len(elems)}  texture {tex_w}x{tex_h}")
    print(f"wrote {os.path.relpath(model_path, HERE)}")
    print(f"wrote {os.path.relpath(tex_path, HERE)} ({written} texture)")


if __name__ == "__main__":
    main()
