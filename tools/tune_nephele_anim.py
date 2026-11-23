"""把涅斐勒的动画幅度调到「一条游动的鱼」的量级。

做法：直接改 build/scratch/models/nephele.bbmodel 里的关键帧旋转值，
然后重新跑 bbmodel_to_geckolib.py 产出资产的 animation.json。

调幅原则：
  * 尾链越靠末端振幅越大（真实鱼类的推进波），前段小、尾端大
  * 胸鳍是主推进面，幅度必须比身体大一个档次
  * 躯干只做轻微的滚动/俯仰，避免整体像果冻
  * 所有分量夹在 ±65° 内，防止方块互相穿插
"""

import json
import os

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")
LIMIT = 65.0

TUNE = {
    "idle": {
        "tail_1": 2.4, "tail_2": 2.9, "tail_3": 3.5, "fan": 2.6,
        "fin_left": 2.2, "fin_right": 2.2, "fin_left_tip": 2.4, "fin_right_tip": 2.4,
        "streamer_left": 2.0, "streamer_right": 2.0,
        "body": 1.5, "head": 1.4,
    },
    "fly": {
        "tail_1": 2.6, "tail_2": 3.1, "tail_3": 3.6, "fan": 2.8,
        "fin_left": 2.3, "fin_right": 2.3, "fin_left_tip": 2.6, "fin_right_tip": 2.6,
        "streamer_left": 2.2, "streamer_right": 2.2,
        "body": 1.8,
    },
    "tail_sweep": {
        "tail_1": 1.6, "tail_2": 1.5, "tail_3": 1.4, "fan": 1.5,
        "body": 1.3, "fin_left": 1.4, "fin_right": 1.4,
    },
    "dive": {
        "tail_1": 1.6, "tail_2": 1.7, "tail_3": 1.8,
        "fin_left": 1.3, "fin_right": 1.3,
    },
    "cloud_burst": {"fin_left": 1.5, "fin_right": 1.5, "head": 1.2},
    "summon": {"crest": 1.3},
    "death": {"fin_left": 1.3, "fin_right": 1.3},
}

with open(BB, encoding="utf-8") as fh:
    project = json.load(fh)

def read_vector(kf):
    points = kf.get("data_points") or []
    if points and isinstance(points[0], dict):
        point = points[0]
        return [float(point.get("x", 0)), float(point.get("y", 0)), float(point.get("z", 0))]
    flat = kf.get("datapoints") or [0, 0, 0]
    return [float(v) for v in flat[:3]]


def write_vector(kf, values):
    kf["data_points"] = [{"x": "%g" % values[0], "y": "%g" % values[1], "z": "%g" % values[2]}]
    kf["datapoints"] = [round(v, 2) for v in values]


group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
report = []
for anim in project.get("animations", []):
    tune = TUNE.get(anim.get("name"))
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
            values = read_vector(kf)
            write_vector(kf, [max(-LIMIT, min(LIMIT, v * factor)) for v in values])
            touched += 1
    report.append("{}: {} 帧 ×{} 根骨骼".format(anim["name"], touched, len(tune)))

with open(BB, "w", encoding="utf-8") as fh:
    json.dump(project, fh, separators=(",", ":"))

for line in report:
    print(line)
