"""重建后的动画曲线：5 节尾链整条上抬—甩下，胸鳍绕 Z 轴上下拍。

相对上一版的三处结构改动：
  * 尾巴 3 节 → 5 节：同样的大弧线由更多小节分摊，节间不会再裂
  * 胸鳍静态姿态外展 65°（烘焙在方块 rotation 上），绕 Z 轴旋转才是「上下拍」
  * 横向（Y 轴）分量压到 0.12：要的是上下摆动，不是左右扇

符号：尾骨 +X 让朝 +z 的部分往下 → 抬尾用负值；
      左鳍（-x 侧）-Z 为上抬，右鳍（+x 侧）+Z 为上抬。
"""

import json
import os
import uuid

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")

LATERAL = 0.12
# 尾链 5 节峰值 10–16°，×1.5 后累积约 105°（3 节版验证过 106° = 明显但不折回背上）
SCALE = 1.5

TAIL_IDLE = {
    "tail_1": [(0, 0), (0.40, -16), (0.85, -30), (1.35, -14), (1.90, 5), (2.50, 12), (3.00, 0)],
    "tail_2": [(0, 0), (0.44, -15), (0.92, -28), (1.42, -13), (1.98, 4), (2.55, 11), (3.00, 0)],
    "tail_3": [(0, 0), (0.48, -14), (0.99, -26), (1.49, -12), (2.06, 4), (2.60, 10), (3.00, 0)],
    "tail_4": [(0, 0), (0.52, -12), (1.06, -22), (1.56, -10), (2.14, 3), (2.65, 8), (3.00, 0)],
    "tail_5": [(0, 0), (0.56, -10), (1.13, -18), (1.63, -8), (2.22, 2), (2.70, 6), (3.00, 0)],
    "fan":    [(0, 0), (0.60, -12), (1.20, -22), (1.70, -8), (2.30, 3), (2.75, 8), (3.00, 0)],
}
TAIL_FLY = {
    "tail_1": [(0, 0), (0.26, -20), (0.58, -38), (0.95, -18), (1.32, 6), (1.70, 14), (2.00, 0)],
    "tail_2": [(0, 0), (0.30, -19), (0.66, -36), (1.05, -17), (1.42, 6), (1.78, 13), (2.00, 0)],
    "tail_3": [(0, 0), (0.34, -18), (0.74, -34), (1.14, -16), (1.52, 5), (1.85, 12), (2.00, 0)],
    "tail_4": [(0, 0), (0.38, -16), (0.82, -30), (1.22, -14), (1.60, 4), (1.90, 10), (2.00, 0)],
    "tail_5": [(0, 0), (0.42, -14), (0.90, -26), (1.30, -12), (1.68, 4), (1.95, 8), (2.00, 0)],
    "fan":    [(0, 0), (0.46, -16), (0.98, -28), (1.38, -10), (1.76, 4), (2.00, 8), (2.00, 0)],
}

FIN_IDLE = {
    "fin_left":  [(0, 0), (0.45, -22), (0.90, -40), (1.40, -16), (1.95, 10), (2.55, 18), (3.00, 0)],
    "fin_right": [(0, 0), (0.45, 22), (0.90, 40), (1.40, 16), (1.95, -10), (2.55, -18), (3.00, 0)],
    "fin_left_tip":  [(0, 0), (0.55, -12), (1.05, -22), (1.55, -8), (2.10, 6), (2.65, 10), (3.00, 0)],
    "fin_right_tip": [(0, 0), (0.55, 12), (1.05, 22), (1.55, 8), (2.10, -6), (2.65, -10), (3.00, 0)],
}
FIN_FLY = {
    "fin_left":  [(0, 0), (0.28, -30), (0.62, -52), (1.00, -20), (1.38, 12), (1.74, 22), (2.00, 0)],
    "fin_right": [(0, 0), (0.28, 30), (0.62, 52), (1.00, 20), (1.38, -12), (1.74, -22), (2.00, 0)],
    "fin_left_tip":  [(0, 0), (0.36, -16), (0.74, -28), (1.14, -10), (1.52, 8), (1.86, 12), (2.00, 0)],
    "fin_right_tip": [(0, 0), (0.36, 16), (0.74, 28), (1.14, 10), (1.52, -8), (1.86, -12), (2.00, 0)],
}

BODY = {
    "idle": [(0, 0), (0.40, -3), (0.85, -6), (1.35, -3), (1.90, 1), (2.50, 3), (3.00, 0)],
    "fly":  [(0, 0), (0.26, -4), (0.58, -8), (0.95, -4), (1.32, 2), (1.70, 4), (2.00, 0)],
}

# 躯干动态：[pitch, yaw, roll]，让身体随尾摆动「跟着晃」，而不是一根硬棍子
BODY3 = {
    "idle": [(0, 0,0,0), (0.40, -5,2,4), (0.85, -9,4,7), (1.35, -4,2,3), (1.90, 2,-2,-3), (2.50, 4,-3,-5), (3.00, 0,0,0)],
    "fly":  [(0, 0,0,0), (0.26, -7,3,6), (0.58, -13,5,9), (0.95, -6,2,4), (1.32, 3,-3,-4), (1.70, 6,-4,-7), (2.00, 0,0,0)],
}
# 后半身：滞后 0.15–0.2s、与尾同向，形成「身体 → 尾柄 → 尾鳍」的波浪
REAR3 = {
    "idle": [(0, 0,0,0), (0.55, -6,0,3), (1.05, -11,0,6), (1.55, -4,0,2), (2.10, 3,0,-3), (2.65, 6,0,-5), (3.00, 0,0,0)],
    "fly":  [(0, 0,0,0), (0.38, -9,0,5), (0.82, -16,0,8), (1.22, -6,0,3), (1.60, 5,0,-4), (1.92, 9,0,-7), (2.00, 0,0,0)],
}

PLAN = {
    "idle": {**TAIL_IDLE, **FIN_IDLE},
    "fly": {**TAIL_FLY, **FIN_FLY},
}
NO_SCALE = {"fin_left", "fin_right", "fin_left_tip", "fin_right_tip"}
LATERAL_BONES = ("tail_", "fan")
TRIPLES = {"idle": {"body": BODY3["idle"], "body_rear": REAR3["idle"]},
           "fly": {"body": BODY3["fly"], "body_rear": REAR3["fly"]}}


def keyframe(channel, time, values):
    return {
        "channel": channel,
        "data_points": [{"x": "%g" % values[0], "y": "%g" % values[1], "z": "%g" % values[2]}],
        "uuid": str(uuid.uuid4()),
        "time": round(time, 3),
        "color": -1,
        "interpolation": "catmullrom",
        "datapoints": [round(v, 2) for v in values],
    }


def animator_for(anim, bone, uuid_by_name):
    auuid = uuid_by_name[bone]
    return anim["animators"].setdefault(
        auuid, {"name": bone, "type": "bone", "rotation_global": False,
                "quaternion_interpolation": False, "keyframes": []})


with open(BB, encoding="utf-8") as fh:
    project = json.load(fh)

group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
uuid_by_name = {v: k for k, v in group_names.items()}

for anim in project.get("animations", []):
    name = anim.get("name")
    plan = PLAN.get(name)
    if plan:
        for bone, curve in plan.items():
            target = animator_for(anim, bone, uuid_by_name)
            target["keyframes"] = [kf for kf in target["keyframes"] if kf.get("channel") != "rotation"]
            for time, angle in curve:
                if bone in NO_SCALE:
                    values = [0.0, 0.0, angle]
                else:
                    x = angle * SCALE
                    values = [x, x * LATERAL if bone.startswith(LATERAL_BONES) else 0.0, 0.0]
                target["keyframes"].append(keyframe("rotation", time, values))
            target["keyframes"].sort(key=lambda kf: kf["time"])
        # 躯干与后半身：显式三维曲线
        for bone, curve in TRIPLES.get(name, {}).items():
            target = animator_for(anim, bone, uuid_by_name)
            target["keyframes"] = [kf for kf in target["keyframes"] if kf.get("channel") != "rotation"]
            for row in curve:
                target["keyframes"].append(keyframe("rotation", row[0], list(row[1:])))
            target["keyframes"].sort(key=lambda kf: kf["time"])
        print("%s: rewrote %d bones" % (name, len(plan) + len(TRIPLES.get(name, {}))))
        continue

    # 攻击/一次性动画：后半身继承躯干动作（延迟 0.05s、幅度 ×1.25），保证不再是硬棍子
    body = anim["animators"].get(uuid_by_name.get("body") or "", {})
    body_keys = [kf for kf in body.get("keyframes", []) if kf.get("channel") == "rotation"]
    if not body_keys:
        continue
    target = animator_for(anim, "body_rear", uuid_by_name)
    target["keyframes"] = [kf for kf in target["keyframes"] if kf.get("channel") != "rotation"]
    for kf in body_keys:
        src = kf.get("data_points") or [{"x": "0", "y": "0", "z": "0"}]
        p = src[0]
        vals = [float(p.get("x", 0)) * 1.25, float(p.get("y", 0)) * 1.25, float(p.get("z", 0)) * 1.25]
        target["keyframes"].append(keyframe("rotation", round(float(kf["time"]) + 0.05, 3), vals))
    target["keyframes"].sort(key=lambda kf: kf["time"])
    print("%s: body_rear inherited %d keys" % (name, len(body_keys)))

with open(BB, "w", encoding="utf-8") as fh:
    json.dump(project, fh, separators=(",", ":"))
