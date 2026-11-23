"""按 CG 鲸参考图重写尾部摆动曲线：一整条「抬尾—甩下」的大行程，而不是对称小正弦。

参考图要的是：尾柄整段抬起、尾鳍翘到接近背部高度、再甩下来。
这靠的不是单纯加角度，而是三件事一起做：
  1. 尾链三节同向叠加（相位几乎对齐，末端再收一点）
  2. 幅度做成不对称：上摆大力、下摆只到位一半（真实鲸类推进就是这样）
  3. 躯干同步后仰一点，让「后半身整体上翘」看得出来

符号约定：+X 旋转会让「朝向 +z（后方）」的部分往下走，所以抬尾用负值。

测量：改完跑 tools/verify_nephele_anim.py 看尾端行程（目标 idle Y ≥ 3.5 块）。
"""

import json
import os
import uuid

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")

# (时间, 值)：一条完整的抬尾—甩下循环
IDLE_STROKE = {
    "tail_1": [(0, 0), (0.40, -20), (0.85, -38), (1.35, -18), (1.90, 6), (2.50, 16), (3.00, 0)],
    "tail_2": [(0, 0), (0.45, -22), (0.95, -44), (1.45, -14), (2.00, 8), (2.55, 18), (3.00, 0)],
    "tail_3": [(0, 0), (0.50, -24), (1.05, -48), (1.55, -10), (2.10, 10), (2.60, 20), (3.00, 0)],
    "fan":    [(0, 0), (0.55, -20), (1.15, -42), (1.65, -6), (2.20, 10), (2.65, 16), (3.00, 0)],
    "body":   [(0, 0), (0.40, -3), (0.85, -6), (1.35, -3), (1.90, 1), (2.50, 3), (3.00, 0)],
}
FLY_STROKE = {
    "tail_1": [(0, 0), (0.26, -24), (0.58, -46), (0.95, -20), (1.32, 8), (1.70, 18), (2.00, 0)],
    "tail_2": [(0, 0), (0.30, -26), (0.66, -52), (1.05, -16), (1.42, 10), (1.78, 20), (2.00, 0)],
    "tail_3": [(0, 0), (0.34, -28), (0.74, -58), (1.14, -12), (1.52, 12), (1.85, 22), (2.00, 0)],
    "fan":    [(0, 0), (0.38, -24), (0.82, -50), (1.22, -8), (1.60, 12), (1.90, 18), (2.00, 0)],
    "body":   [(0, 0), (0.26, -4), (0.58, -8), (0.95, -4), (1.32, 2), (1.70, 4), (2.00, 0)],
}
# 尾部横向分量系数（0.35 = 明显的鱼式侧摆，正面视角也能看到）
LATERAL = 0.35
# 整体缩放：1.0 = 上面表里的原值（累积 −172°，会卷成蝎子尾并露出节间缝隙）
# 0.62 → 累积约 −100°，接近 CG 参考图的「尾柄抬起、尾鳍外翻」而不是折回背上
SCALE = 0.62
# 躯干后仰不缩放（它自己只有 −6°，本来就克制）
NO_SCALE = {"body"}

PLAN = {"idle": IDLE_STROKE, "fly": FLY_STROKE}


def keyframe(channel, time, values, interpolation="catmullrom"):
    kf = {
        "channel": channel,
        "data_points": [{"x": "%g" % values[0], "y": "%g" % values[1], "z": "%g" % values[2]}],
        "uuid": str(uuid.uuid4()),
        "time": round(time, 3),
        "color": -1,
        "interpolation": interpolation,
        "datapoints": [round(v, 2) for v in values],
    }
    return kf


with open(BB, encoding="utf-8") as fh:
    project = json.load(fh)

group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
uuid_by_name = {v: k for k, v in group_names.items()}

for anim in project.get("animations", []):
    plan = PLAN.get(anim.get("name"))
    if not plan:
        continue
    for bone, curve in plan.items():
        auuid = uuid_by_name[bone]
        animator = anim["animators"].setdefault(
            auuid, {"name": bone, "type": "bone", "rotation_global": False,
                    "quaternion_interpolation": False, "keyframes": []})
        # 只清掉旋转通道，位置/缩放通道保留
        animator["keyframes"] = [kf for kf in animator["keyframes"] if kf.get("channel") != "rotation"]
        for time, x in curve:
            if bone not in NO_SCALE:
                x = x * SCALE
            y = x * LATERAL if bone.startswith(("tail", "fan")) else 0.0
            animator["keyframes"].append(keyframe("rotation", time, [x, y, 0]))
        animator["keyframes"].sort(key=lambda kf: kf["time"])
    print("%s: 重写 %d 根骨骼的旋转曲线" % (anim["name"], len(plan)))

with open(BB, "w", encoding="utf-8") as fh:
    json.dump(project, fh, separators=(",", ":"))
