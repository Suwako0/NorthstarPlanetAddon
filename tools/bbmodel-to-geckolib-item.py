#!/usr/bin/env python3
"""把熔核巨剑的 .bbmodel 导出成 GeckoLib 的 item geo + animation。

与 `bbmodel-to-geckolib.py`（实体用）的区别：
  * 输出到 `geo/item/` + `animations/item/`；
  * 模型是在 8 倍尺度上建的（为了让 box UV 岛有 8 px/单位的密度），这里所有坐标 ÷8；
  * 坐标系保持「建模时的手性」：x 不翻转（Blockbench 的 geckolib 导出同样是 1:1）。

用法: python tools/bbmodel-to-geckolib-item.py --name molten_core_sword --scale 8
"""
import argparse
import base64
import json
import os

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EASING = {"catmullrom": "easeInOutSine", "bezier": "easeInOutSine", "linear": "linear", "step": "step"}


def time_key(t):
    s = f"{float(t):.3f}".rstrip("0")
    if s.endswith("."):
        s += "0"
    return s


def build_geo(data, scale, center):
    """从 outliner 走层次（元素的 parent 字段不一定存在），group 的 origin 已是 16 单位空间。

    ``center=True`` 时把整体平移 -8/-8/-8：GeckoLib 的 {@code GeoItemRenderer#preRender}
    会额外 ``poseStack.translate(0.5, 0.51, 0.5)``（= 8/8.16/8 模型单位），所以物品 geo 必须以原点为中心；
    Java 物品模型那套（x/z 居中在 8）直接搬到 GeckoLib 会偏半个方块。
    """
    off = [8.0, 8.0, 8.0] if center else [0.0, 0.0, 0.0]
    groups = {g["uuid"]: g for g in data.get("groups", [])}
    elements = {e["uuid"]: e for e in data.get("elements", [])}
    bones = []

    def make_cube(el):
        inflate = el.get("inflate") or 0.0
        origin = [round((v - inflate) / scale - off[i], 4) for i, v in enumerate(el["from"])]
        size = [round((el["to"][i] - el["from"][i] + 2 * inflate) / scale, 4) for i in range(3)]
        uv = el.get("uv_offset") or [0, 0]
        cube = {"origin": origin, "size": size, "uv": [round(uv[0]), round(uv[1])]}
        if el.get("rotation"):
            cube["rotation"] = el["rotation"]
            cube["pivot"] = [round(v / scale - off[i], 4) for i, v in enumerate(el.get("origin", [0, 0, 0]))]
        return cube

    def walk(node, parent_name):
        if isinstance(node, str):
            return
        group = groups.get(node.get("uuid"))
        if not group:
            return
        bone = {"name": group["name"], "pivot": [round(v - off[i], 4) for i, v in enumerate(group.get("origin", [0, 0, 0]))]}
        if parent_name:
            bone["parent"] = parent_name
        cubes = []
        child_groups = []
        for child in node.get("children", []):
            uid = child if isinstance(child, str) else child.get("uuid")
            if uid in groups:
                child_groups.append(child)
                continue
            el = elements.get(uid)
            if el and el.get("type", "cube") == "cube":
                cubes.append(make_cube(el))
        if cubes:
            bone["cubes"] = cubes
        bones.append(bone)
        for child in child_groups:
            walk(child, group["name"])

    for root in data.get("outliner", []):
        walk(root, None)

    res = data.get("resolution", {})
    tex_w = res.get("width") or 256
    tex_h = res.get("height") or 256
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.molten_core_sword",
                "texture_width": tex_w,
                "texture_height": tex_h,
                "visible_bounds_width": 3,
                "visible_bounds_height": 4,
                "visible_bounds_offset": [0, 1, 0],
            },
            "bones": bones,
        }],
    }


def convert_animation(anim, uuid_to_bone, scale):
    bones = {}
    for group_uuid, animator in (anim.get("animators") or {}).items():
        bone = uuid_to_bone.get(group_uuid)
        if not bone:
            continue
        channels = {}
        for kf in sorted(animator.get("keyframes") or [], key=lambda k: k["time"]):
            channel = kf.get("channel", "rotation")
            if channel not in ("rotation", "position", "scale"):
                continue
            point = (kf.get("data_points") or [{}])[0]
            vec = [float(point.get("x", 0)), float(point.get("y", 0)), float(point.get("z", 0))]
            divisor = 1.0 if channel == "rotation" else (scale if channel == "position" else 1.0)
            vec = [round(v / divisor, 4) for v in vec]
            entry = {"vector": vec}
            easing = EASING.get(kf.get("interpolation", "catmullrom"))
            if easing:
                entry["easing"] = easing
            channels.setdefault(channel, {})[time_key(kf["time"])] = entry
        if channels:
            bones[bone] = channels
    return {"loop": anim.get("loop", "once") == "loop", "animation_length": anim.get("length", 1.0), "bones": bones}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--name", default="molten_core_sword")
    ap.add_argument("--ns", default="northstar_planet_addon")
    ap.add_argument("--scale", type=float, default=8.0)
    ap.add_argument("--center", action="store_true", help="整体平移 -8/-8/-8，以原点为中心（GeckoLib 物品渲染要求）")
    args = ap.parse_args()

    bb = os.path.join(HERE, "docs", "models", f"{args.name}.bbmodel")
    data = json.load(open(bb, encoding="utf-8"))
    assets = os.path.join(HERE, "src", "main", "resources", "assets", args.ns)

    geo = build_geo(data, args.scale, args.center)
    geo_path = os.path.join(assets, "geo", "item", f"{args.name}.geo.json")
    os.makedirs(os.path.dirname(geo_path), exist_ok=True)
    with open(geo_path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(geo, fh, indent=1)
        fh.write("\n")

    uuid_to_bone = {g["uuid"]: g["name"] for g in data.get("groups", [])}
    animations = {}
    for anim in data.get("animations", []):
        animations[anim["name"].split(".")[-1]] = convert_animation(anim, uuid_to_bone, args.scale)
    anim_path = os.path.join(assets, "animations", "item", f"{args.name}.animation.json")
    os.makedirs(os.path.dirname(anim_path), exist_ok=True)
    with open(anim_path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump({"format_version": "1.8.0", "animations": animations}, fh, indent=1)
        fh.write("\n")

    # 贴图：同一张 PNG（与 3D 物品模型共用）
    tex_dir = os.path.join(assets, "textures", "item")
    os.makedirs(tex_dir, exist_ok=True)
    tex_path = os.path.join(tex_dir, f"{args.name}.png")
    for tex in data.get("textures", []):
        src = tex.get("source") or ""
        if src.startswith("data:image"):
            with open(tex_path, "wb") as fh:
                fh.write(base64.b64decode(src.split(",", 1)[1]))
            break

    print(f"bones {len(geo['minecraft:geometry'][0]['bones'])}  "
          f"cubes {sum(len(b.get('cubes', [])) for b in geo['minecraft:geometry'][0]['bones'])}")
    print(f"animations {list(animations.keys())}")
    print(f"wrote {os.path.relpath(geo_path, HERE)}")
    print(f"wrote {os.path.relpath(anim_path, HERE)}")


if __name__ == "__main__":
    main()
