"""第三轮振幅调整：把 idle / fly 的尾与鳍推到肉眼明确可见的量级。

量测基线（tools/verify_nephele_anim.py，单位：方块）：
    第二轮后  idle 尾端 Y 1.26、鳍尖 X 1.91
    第三轮目标 idle 尾端 Y ≥ 2.4、鳍尖 X ≥ 3.0；fly 尾端 Y ≥ 3.2、鳍尖 X ≥ 4.0
"""

import json
import os

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")
LIMIT = 70.0
STAGE = 3

TUNE3 = {
    "idle": {
        "tail_1": 2.0, "tail_2": 1.9, "tail_3": 1.8, "fan": 1.9,
        "fin_left": 1.7, "fin_right": 1.7, "fin_left_tip": 1.7, "fin_right_tip": 1.7,
        "streamer_left": 1.6, "streamer_right": 1.6,
        "body": 2.0,
    },
    "fly": {
        "tail_1": 1.7, "tail_2": 1.6, "tail_3": 1.5, "fan": 1.6,
        "fin_left": 1.5, "fin_right": 1.5, "fin_left_tip": 1.5, "fin_right_tip": 1.5,
        "streamer_left": 1.5, "streamer_right": 1.5,
        "body": 1.6,
    },
    "tail_sweep": {"tail_1": 1.15, "tail_2": 1.15, "tail_3": 1.15},
    "dive": {"tail_1": 1.2, "tail_2": 1.2},
}


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
if project.get("_tune_stage", 1) >= STAGE:
    raise SystemExit("第三轮已应用，不重复放大")

group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
for anim in project.get("animations", []):
    tune = TUNE3.get(anim.get("name"))
    if not tune:
        continue
    touched = 0
    for auuid, animator in anim.get("animators", {}).items():
        bone = group_names.get(auuid) or animator.get("name")
        factor = tune.get(bone)
        if not factor:
            continue
        for kf in animator.get("keyframes", []):
            if kf.get("channel") != "rotation":
                continue
            values = [v * factor for v in read_vector(kf)]
            write_vector(kf, [max(-LIMIT, min(LIMIT, v)) for v in values])
            touched += 1
    print("%s: 缩放 %d 帧" % (anim["name"], touched))

project["_tune_stage"] = STAGE
with open(BB, "w", encoding="utf-8") as fh:
    json.dump(project, fh, separators=(",", ":"))
