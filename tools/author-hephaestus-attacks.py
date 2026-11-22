#!/usr/bin/env python3
"""重写赫菲斯托斯的七段招式动画：幅度拉到一眼可辨，并且每招用不同的骨骼/轴向做区分。

设计要点（原版模型朝向 -Z，正 X 是它自己的右边，+X 旋转让下垂的肢体向前摆）：
  lava_punch   直拳：右臂后拉 -95° → 前捅 +88°，胸腔绕 Y 反拧，身体前压 8 单位
  backhand     横扫：左臂抬平 + 躯干绕 Y 从 -38° 甩到 +42°（水平弧，和直拳完全不同）
  slag_throw   过顶抛：双臂同时抬到 -135°，身体后仰 → 前倾
  quake_stomp  踏地：右腿抬 72° + 膝反折 -78°，落地时身体下沉 9 单位
  steam_vent   喷汽：驼背 + 双臂下垂外张 + 炉盖全开（持续型）
  overload_burst 过载：双臂张开 85° + 身体后弓 + 炉盖 95°（持续型）
  plate_break  碎甲：胸腔绕 Y 猛拧 + 右臂内收劈向自己胸甲

用法: python tools/author-hephaestus-attacks.py [--dry]
"""
import argparse
import json
import os
import uuid

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BB = os.path.join(HERE, "docs", "models", "hephaestus.bbmodel")

# (bone, channel, time, (x, y, z), interpolation)
PUNCH = [
    ("body", "rotation", 0, (0, 0, 0)),
    ("body", "rotation", 0.25, (-14, 8, 0)),
    ("body", "rotation", 0.45, (16, -10, 0), "linear"),
    ("body", "rotation", 0.7, (6, -4, 0)),
    ("body", "rotation", 1.0, (0, 0, 0)),
    ("body", "position", 0, (0, 0, 0)),
    ("body", "position", 0.25, (0, -4, 2)),
    ("body", "position", 0.45, (0, -8, -4), "linear"),
    ("body", "position", 0.7, (0, -3, -1)),
    ("body", "position", 1.0, (0, 0, 0)),
    ("chest", "rotation", 0.25, (0, 26, 0)),
    ("chest", "rotation", 0.45, (0, -30, 0), "linear"),
    ("chest", "rotation", 1.0, (0, 0, 0)),
    ("head", "rotation", 0.25, (4, 18, 0)),
    ("head", "rotation", 0.45, (8, -20, 0)),
    ("head", "rotation", 1.0, (0, 0, 0)),
    ("arm_right", "rotation", 0, (0, 0, 0)),
    ("arm_right", "rotation", 0.22, (-95, 0, 18)),
    ("arm_right", "rotation", 0.45, (88, 0, -8), "linear"),
    ("arm_right", "rotation", 0.62, (60, 0, 0)),
    ("arm_right", "rotation", 1.0, (0, 0, 0)),
    ("forearm_right", "rotation", 0.22, (55, 0, 0)),
    ("forearm_right", "rotation", 0.45, (-25, 0, 0), "linear"),
    ("forearm_right", "rotation", 1.0, (0, 0, 0)),
    ("fist_right", "rotation", 0, (0, 0, 0)),
    ("fist_right", "rotation", 0.45, (-35, 0, 0), "linear"),
    ("fist_right", "rotation", 0.62, (0, 0, 0)),
    ("arm_left", "rotation", 0.25, (-45, 0, -30)),
    ("arm_left", "rotation", 0.6, (20, 0, -10)),
    ("arm_left", "rotation", 1.0, (0, 0, 0)),
    ("leg_right", "rotation", 0.25, (-12, 0, 0)),
    ("leg_right", "rotation", 0.45, (14, 0, 0)),
    ("leg_right", "rotation", 1.0, (0, 0, 0)),
]

BACKHAND = [
    ("body", "rotation", 0, (0, 0, 0)),
    ("body", "rotation", 0.2, (0, -38, 0)),
    ("body", "rotation", 0.45, (0, 42, 0), "linear"),
    ("body", "rotation", 0.7, (0, 15, 0)),
    ("body", "rotation", 1.0, (0, 0, 0)),
    ("body", "position", 0.2, (0, -2, 0)),
    ("body", "position", 0.45, (0, -5, 0), "linear"),
    ("body", "position", 1.0, (0, 0, 0)),
    ("chest", "rotation", 0.2, (0, -30, 0)),
    ("chest", "rotation", 0.45, (0, 34, 0), "linear"),
    ("chest", "rotation", 1.0, (0, 0, 0)),
    ("head", "rotation", 0.2, (0, -24, 0)),
    ("head", "rotation", 0.45, (0, 28, 0)),
    ("head", "rotation", 1.0, (0, 0, 0)),
    ("arm_left", "rotation", 0, (0, 0, 0)),
    ("arm_left", "rotation", 0.2, (-30, 0, -72)),
    ("arm_left", "rotation", 0.45, (18, 0, -98), "linear"),
    ("arm_left", "rotation", 0.62, (8, 0, -55)),
    ("arm_left", "rotation", 1.0, (0, 0, 0)),
    ("forearm_left", "rotation", 0.2, (32, 0, 0)),
    ("forearm_left", "rotation", 0.45, (-12, 0, 0)),
    ("forearm_left", "rotation", 1.0, (0, 0, 0)),
    ("fist_left", "rotation", 0.45, (-28, 0, 0), "linear"),
    ("fist_left", "rotation", 0.7, (0, 0, 0)),
    ("arm_right", "rotation", 0.2, (-28, 0, 25)),
    ("arm_right", "rotation", 0.45, (16, 0, 10)),
    ("arm_right", "rotation", 1.0, (0, 0, 0)),
    ("leg_left", "rotation", 0.2, (-8, 0, 0)),
    ("leg_left", "rotation", 0.45, (10, 0, 0)),
    ("leg_left", "rotation", 1.0, (0, 0, 0)),
]

SLAG = [
    ("body", "rotation", 0, (0, 0, 0)),
    ("body", "rotation", 0.35, (-24, 6, 0)),
    ("body", "rotation", 0.6, (18, -6, 0), "linear"),
    ("body", "rotation", 0.85, (8, 0, 0)),
    ("body", "rotation", 1.3, (0, 0, 0)),
    ("body", "position", 0.35, (0, 2, 3)),
    ("body", "position", 0.6, (0, -3, -3), "linear"),
    ("body", "position", 1.3, (0, 0, 0)),
    ("chest", "rotation", 0.35, (-16, 0, 0)),
    ("chest", "rotation", 0.6, (14, 0, 0), "linear"),
    ("chest", "rotation", 1.3, (0, 0, 0)),
    ("head", "rotation", 0.35, (-20, 0, 0)),
    ("head", "rotation", 0.6, (10, 0, 0)),
    ("head", "rotation", 1.3, (0, 0, 0)),
    ("arm_right", "rotation", 0, (0, 0, 0)),
    ("arm_right", "rotation", 0.35, (-135, 0, 12)),
    ("arm_right", "rotation", 0.6, (-20, 0, 0), "linear"),
    ("arm_right", "rotation", 0.85, (-45, 0, 0)),
    ("arm_right", "rotation", 1.3, (0, 0, 0)),
    ("arm_left", "rotation", 0, (0, 0, 0)),
    ("arm_left", "rotation", 0.35, (-130, 0, -12)),
    ("arm_left", "rotation", 0.6, (-15, 0, 0), "linear"),
    ("arm_left", "rotation", 0.85, (-40, 0, 0)),
    ("arm_left", "rotation", 1.3, (0, 0, 0)),
    ("forearm_right", "rotation", 0.35, (42, 0, 0)),
    ("forearm_right", "rotation", 0.6, (-32, 0, 0)),
    ("forearm_right", "rotation", 1.3, (0, 0, 0)),
    ("forearm_left", "rotation", 0.35, (42, 0, 0)),
    ("forearm_left", "rotation", 0.6, (-32, 0, 0)),
    ("forearm_left", "rotation", 1.3, (0, 0, 0)),
    ("furnace_cover", "rotation", 0, (0, 0, 0)),
    ("furnace_cover", "rotation", 0.35, (-28, 0, 0)),
    ("furnace_cover", "rotation", 0.6, (22, 0, 0)),
    ("furnace_cover", "rotation", 1.3, (0, 0, 0)),
]

STOMP = [
    ("leg_right", "rotation", 0, (0, 0, 0)),
    ("leg_right", "rotation", 0.45, (72, 0, 8)),
    ("leg_right", "rotation", 0.7, (-6, 0, 0), "linear"),
    ("leg_right", "rotation", 1.0, (4, 0, 0)),
    ("leg_right", "rotation", 1.6, (0, 0, 0)),
    ("shin_right", "rotation", 0, (0, 0, 0)),
    ("shin_right", "rotation", 0.45, (-78, 0, 0)),
    ("shin_right", "rotation", 0.7, (6, 0, 0), "linear"),
    ("shin_right", "rotation", 1.6, (0, 0, 0)),
    ("foot_right", "rotation", 0.45, (-22, 0, 0)),
    ("foot_right", "rotation", 0.7, (12, 0, 0)),
    ("foot_right", "rotation", 1.6, (0, 0, 0)),
    ("body", "position", 0, (0, 0, 0)),
    ("body", "position", 0.45, (0, 1, 0)),
    ("body", "position", 0.7, (0, -9, 0), "linear"),
    ("body", "position", 0.95, (0, -4, 0)),
    ("body", "position", 1.6, (0, 0, 0)),
    ("body", "rotation", 0.45, (-10, 0, 0)),
    ("body", "rotation", 0.7, (12, 0, 0), "linear"),
    ("body", "rotation", 1.6, (0, 0, 0)),
    ("chest", "rotation", 0.45, (-12, 0, 0)),
    ("chest", "rotation", 0.7, (16, 0, 0), "linear"),
    ("chest", "rotation", 1.6, (0, 0, 0)),
    ("head", "rotation", 0.45, (-14, 0, 0)),
    ("head", "rotation", 0.7, (18, 0, 0), "linear"),
    ("head", "rotation", 1.6, (0, 0, 0)),
    ("arm_right", "rotation", 0.45, (-35, 0, 38)),
    ("arm_right", "rotation", 0.7, (28, 0, 20), "linear"),
    ("arm_right", "rotation", 1.6, (0, 0, 0)),
    ("arm_left", "rotation", 0.45, (-35, 0, -38)),
    ("arm_left", "rotation", 0.7, (28, 0, -20), "linear"),
    ("arm_left", "rotation", 1.6, (0, 0, 0)),
    ("leg_left", "rotation", 0.45, (6, 0, -4)),
    ("leg_left", "rotation", 0.7, (-10, 0, 0)),
    ("leg_left", "rotation", 1.6, (0, 0, 0)),
]

STEAM = [
    ("body", "rotation", 0, (0, 0, 0)),
    ("body", "rotation", 0.3, (16, 0, 0)),
    ("body", "rotation", 1.8, (14, 3, 0)),
    ("body", "rotation", 2.2, (0, 0, 0)),
    ("body", "position", 0.3, (0, -3, 0)),
    ("body", "position", 1.8, (0, -3, 0)),
    ("body", "position", 2.2, (0, 0, 0)),
    ("chest", "rotation", 0.3, (18, 0, 0)),
    ("chest", "rotation", 2.2, (0, 0, 0)),
    ("head", "rotation", 0.3, (24, 0, 0)),
    ("head", "rotation", 1.8, (22, -4, 0)),
    ("head", "rotation", 2.2, (0, 0, 0)),
    ("arm_right", "rotation", 0.3, (-22, 0, 48)),
    ("arm_right", "rotation", 1.8, (-20, 0, 52)),
    ("arm_right", "rotation", 2.2, (0, 0, 0)),
    ("arm_left", "rotation", 0.3, (-22, 0, -48)),
    ("arm_left", "rotation", 1.8, (-20, 0, -52)),
    ("arm_left", "rotation", 2.2, (0, 0, 0)),
    ("forearm_right", "rotation", 0.3, (26, 0, 0)),
    ("forearm_right", "rotation", 2.2, (0, 0, 0)),
    ("forearm_left", "rotation", 0.3, (26, 0, 0)),
    ("forearm_left", "rotation", 2.2, (0, 0, 0)),
    ("shoulder_right", "rotation", 0.3, (0, 0, 14)),
    ("shoulder_right", "rotation", 2.2, (0, 0, 0)),
    ("shoulder_left", "rotation", 0.3, (0, 0, -14)),
    ("shoulder_left", "rotation", 2.2, (0, 0, 0)),
    ("furnace_cover", "rotation", 0, (0, 0, 0)),
    ("furnace_cover", "rotation", 0.3, (-72, 0, 0)),
    ("furnace_cover", "rotation", 1.8, (-72, 0, 0)),
    ("furnace_cover", "rotation", 2.2, (0, 0, 0)),
]

BURST = [
    ("body", "rotation", 0, (0, 0, 0)),
    ("body", "rotation", 0.5, (-26, 0, 0)),
    ("body", "rotation", 1.6, (-18, 0, 0)),
    ("body", "rotation", 2.6, (0, 0, 0)),
    ("chest", "rotation", 0.5, (-22, 0, 0)),
    ("chest", "rotation", 2.6, (0, 0, 0)),
    ("head", "rotation", 0.5, (-32, 0, 0)),
    ("head", "rotation", 1.6, (-20, 0, 0)),
    ("head", "rotation", 2.6, (0, 0, 0)),
    ("arm_right", "rotation", 0.5, (-30, 0, 85)),
    ("arm_right", "rotation", 1.6, (-24, 0, 78)),
    ("arm_right", "rotation", 2.6, (0, 0, 0)),
    ("arm_left", "rotation", 0.5, (-30, 0, -85)),
    ("arm_left", "rotation", 1.6, (-24, 0, -78)),
    ("arm_left", "rotation", 2.6, (0, 0, 0)),
    ("forearm_right", "rotation", 0.5, (-35, 0, 0)),
    ("forearm_right", "rotation", 2.6, (0, 0, 0)),
    ("forearm_left", "rotation", 0.5, (-35, 0, 0)),
    ("forearm_left", "rotation", 2.6, (0, 0, 0)),
    ("furnace_cover", "rotation", 0, (0, 0, 0)),
    ("furnace_cover", "rotation", 0.5, (95, 0, 0)),
    ("furnace_cover", "rotation", 1.6, (88, 0, 0)),
    ("furnace_cover", "rotation", 2.6, (0, 0, 0)),
    ("leg_right", "rotation", 0.5, (0, 0, 12)),
    ("leg_right", "rotation", 2.6, (0, 0, 0)),
    ("leg_left", "rotation", 0.5, (0, 0, -12)),
    ("leg_left", "rotation", 2.6, (0, 0, 0)),
]

PLATE = [
    ("chest", "rotation", 0, (0, -30, 0)),
    ("chest", "rotation", 0.25, (0, 35, 0), "linear"),
    ("chest", "rotation", 0.55, (0, 10, 0)),
    ("chest", "rotation", 0.8, (0, 0, 0)),
    ("body", "rotation", 0, (0, -20, 0)),
    ("body", "rotation", 0.25, (0, 24, 0), "linear"),
    ("body", "rotation", 0.8, (0, 0, 0)),
    ("head", "rotation", 0, (0, -18, 0)),
    ("head", "rotation", 0.25, (0, 20, 0)),
    ("head", "rotation", 0.8, (0, 0, 0)),
    ("arm_right", "rotation", 0, (-42, 0, -30)),
    ("arm_right", "rotation", 0.25, (55, 0, 15), "linear"),
    ("arm_right", "rotation", 0.55, (20, 0, 0)),
    ("arm_right", "rotation", 0.8, (0, 0, 0)),
    ("forearm_right", "rotation", 0, (32, 0, 0)),
    ("forearm_right", "rotation", 0.25, (-42, 0, 0), "linear"),
    ("forearm_right", "rotation", 0.8, (0, 0, 0)),
    ("fist_right", "rotation", 0.25, (-28, 0, 0)),
    ("fist_right", "rotation", 0.55, (0, 0, 0)),
    ("arm_left", "rotation", 0.25, (18, 0, -20)),
    ("arm_left", "rotation", 0.8, (0, 0, 0)),
]

SPEC = {
    "animation.hephaestus.lava_punch": (PUNCH, 1.0, "once"),
    "animation.hephaestus.backhand": (BACKHAND, 1.0, "once"),
    "animation.hephaestus.slag_throw": (SLAG, 1.3, "once"),
    "animation.hephaestus.quake_stomp": (STOMP, 1.6, "once"),
    "animation.hephaestus.steam_vent": (STEAM, 2.2, "once"),
    "animation.hephaestus.overload_burst": (BURST, 2.6, "once"),
    "animation.hephaestus.plate_break": (PLATE, 0.8, "once"),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry", action="store_true")
    args = ap.parse_args()

    bb = json.load(open(BB, encoding="utf-8"))
    uuid_by_bone = {g["name"]: g["uuid"] for g in bb["groups"]}

    problems = []
    for anim in bb["animations"]:
        spec = SPEC.get(anim["name"])
        if not spec:
            continue
        keys, length, loop = spec
        animators = {}
        for bone, channel, time, vec, *interp in keys:
            if bone not in uuid_by_bone:
                problems.append(bone)
                continue
            bone_uuid = uuid_by_bone[bone]
            slot = animators.setdefault(bone_uuid, {"name": bone, "type": "bone",
                                                    "rotation_global": False, "keyframes": []})
            slot["keyframes"].append({
                "time": round(float(time), 3),
                "channel": channel,
                "interpolation": (interp[0] if interp else "catmullrom"),
                "data_points": [{"x": float(vec[0]), "y": float(vec[1]), "z": float(vec[2])}],
            })
        for slot in animators.values():
            slot["keyframes"].sort(key=lambda k: k["time"])
        anim["animators"] = animators
        anim["length"] = length
        anim["loop"] = loop
        anim.pop("animation_length", None)
        print(f"{anim['name']:38s} bones={len(animators):>2} keys={sum(len(a['keyframes']) for a in animators.values())}")

    if problems:
        raise SystemExit(f"未知骨骼: {sorted(set(problems))}")
    if args.dry:
        print("(dry run, 未写回)")
        return
    with open(BB, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(bb, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print(f"wrote {os.path.relpath(BB, HERE)}")


if __name__ == "__main__":
    main()
