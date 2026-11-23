"""第二轮振幅调整（在第一轮基础上再放大，并给尾部加横向分量）。

第一轮的量测结果（tools/verify_nephele_anim.py）：
    idle 尾端 Y 1.87 块、鳍尖 X 2.07 块   —— 体长 9.6 格，看不出摆动
    fly  尾端 Y 2.36 块、鳍尖 X 3.05 块   —— 仍然偏软

本轮目标：
    idle 尾端 Y ≥ 3.2 块、鳍尖 X ≥ 3.4 块，并把 idle 周期 4s 压到 3s
    fly  尾端 Y ≥ 4.0 块、鳍尖 X ≥ 4.2 块
    尾部额外混入横向（Y 轴）分量 = 0.5 × 纵向，让正面视角也能看出摆动
"""

import json
import os

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")
LIMIT = 68.0
STAGE = 2

TUNE2 = {
    "idle": {
        "tail_1": 2.0, "tail_2": 2.0, "tail_3": 1.9, "fan": 1.9,
        "fin_left": 1.7, "fin_right": 1.7, "fin_left_tip": 1.8, "fin_right_tip": 1.8,
        "streamer_left": 1.8, "streamer_right": 1.8,
        "body": 1.8,
    },
    "fly": {
        "tail_1": 1.7, "tail_2": 1.7, "tail_3": 1.7, "fan": 1.7,
        "fin_left": 1.4, "fin_right": 1.4, "fin_left_tip": 1.5, "fin_right_tip": 1.5,
        "streamer_left": 1.6, "streamer_right": 1.6,
        "body": 1.4,
    },
    "tail_sweep": {"tail_1": 1.2, "tail_2": 1.2, "tail_3": 1.2, "fan": 1.2, "body": 1.2},
    "dive": {"tail_1": 1.2, "tail_2": 1.2, "tail_3": 1.2},
    "cloud_burst": {"fin_left": 1.3, "fin_right": 1.3},
    "plate_break": {"body": 1.6},
    "death": {"fin_left": 1.2, "fin_right": 1.2},
}

# 尾部纵向摆动混入多少横向分量（0 = 纯上下拍水，0.5 = 明显的鱼式侧摆）
YAW_MIX = {"idle": 0.5, "fly": 0.45, "dive": 0.2}
TAIL_BONES = {"tail_1", "tail_2", "tail_3", "fan", "vent"}

# idle 周期 4s → 3s（每秒摆一次多，肉眼才跟得上）
SPEED_UP = {"idle": 0.75}


def read_vector(kf):
    points = kf.get("data_points") or []
    if points and isinstance(points[0], dict):
        p = points[0]
        return [float(p.get("x", 0)), float(p.get("y", 0)), float(p.get("z", 0))]
    flat = kf.get("datapoints") or [0, 0, 0]
    return [float(v) for v in flat[:3]]


def write_vector(kf, values):
    kf["data_points"] = [{"x": "%g" % values[0], "y": "%g" % values[1], "z": "%g" % values[2]}]
    kf["datapoints"] = [round(v, 2) for v in values]


with open(BB, encoding="utf-8") as fh:
    project = json.load(fh)

stage = project.get("_tune_stage", 1)
if stage >= STAGE:
    raise SystemExit("已经做过第二轮调整（_tune_stage=%s），不重复放大" % stage)

group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
report = []
for anim in project.get("animations", []):
    name = anim.get("name")
    tune = TUNE2.get(name)
    yaw_mix = YAW_MIX.get(name, 0.0)
    if tune:
        touched = 0
        for auuid, animator in anim.get("animators", {}).items():
            bone = group_names.get(auuid) or animator.get("name")
            factor = tune.get(bone)
            if not factor:
                continue
            for kf in animator.get("keyframes", []):
                if kf.get("channel") != "rotation":
                    continue
                x, y, z = read_vector(kf)
                x, y, z = x * factor, y * factor, z * factor
                if yaw_mix and bone in TAIL_BONES:
                    y = max(-LIMIT, min(LIMIT, x * yaw_mix))
                write_vector(kf, [max(-LIMIT, min(LIMIT, v)) for v in (x, y, z)])
                touched += 1
        report.append("%s: 缩放 %d 帧（横向混合 %.2f）" % (name, touched, yaw_mix))
    speed = SPEED_UP.get(name)
    if speed:
        for animator in anim.get("animators", {}).values():
            for kf in animator.get("keyframes", []):
                kf["time"] = round(float(kf["time"]) * speed, 3)
        anim["length"] = round(float(anim.get("length", 1)) * speed, 3)
        report.append("%s: 周期压到 %.2fs" % (name, anim["length"]))

project["_tune_stage"] = STAGE
with open(BB, "w", encoding="utf-8") as fh:
    json.dump(project, fh, separators=(",", ":"))

for line in report:
    print(line)
