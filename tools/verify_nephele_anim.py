"""测量涅斐勒各段动画的实际位移（方块单位）。

读的是真正会进游戏的两份文件：geo/entity/nephele.geo.json 与 animations/entity/nephele.animation.json，
按 Bedrock/GeckoLib 的骨骼链做正向运动学（骨旋转绕自身 pivot，父子链相乘），
输出尾端（vent 骨 pivot = 弱点位置）与左鳍尖的行程。

用法：python tools/verify_nephele_anim.py
"""

import json
import math
import os

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
ASSETS = os.path.join(ROOT, r"src\main\resources\assets\northstar_planet_addon")
GEO = os.path.join(ASSETS, r"geo\entity\nephele.geo.json")
ANIM = os.path.join(ASSETS, r"animations\entity\nephele.animation.json")

UNITS_PER_BLOCK = 16.0


# ------------------------------------------------------------------ 4x4 矩阵
def identity():
    return [[1.0 if i == j else 0.0 for j in range(4)] for i in range(4)]


def matmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    m = identity()
    m[0][3], m[1][3], m[2][3] = x, y, z
    return m


def rot_xyz(rx, ry, rz):
    """Bedrock/GeckoLib 的欧拉旋转（度）；先 X 再 Y 再 Z。"""
    rx, ry, rz = (math.radians(v) for v in (rx, ry, rz))
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = [[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]]
    my = [[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]]
    mz = [[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]
    return matmul(mz, matmul(my, mx))


def apply(m, p):
    x = m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2] + m[0][3]
    y = m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2] + m[1][3]
    z = m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2] + m[2][3]
    return [x, y, z]


# ------------------------------------------------------------------ 数据
with open(GEO, encoding="utf-8") as fh:
    bones = json.load(fh)["minecraft:geometry"][0]["bones"]
with open(ANIM, encoding="utf-8") as fh:
    animations = json.load(fh)["animations"]

BONE = {b["name"]: b for b in bones}


def local_matrix(bone_name, rotations, offsets):
    bone = BONE[bone_name]
    p = bone.get("pivot") or [0, 0, 0]
    rot = rotations.get(bone_name, [0, 0, 0])
    pos = offsets.get(bone_name, [0, 0, 0])
    m = rot_xyz(*rot)
    m[0][3], m[1][3], m[2][3] = pos
    return matmul(translate(*p), matmul(m, translate(-p[0], -p[1], -p[2])))


def chain_matrix(bone_name, rotations, offsets):
    """root → 该骨的累积矩阵。"""
    chain = []
    cur = bone_name
    while cur:
        chain.append(cur)
        cur = BONE[cur].get("parent")
    m = identity()
    for name in reversed(chain):
        m = matmul(m, local_matrix(name, rotations, offsets))
    return m


def _vec(value):
    """动画文件里的关键帧值可能是 {post:[x,y,z], lerp_mode:...} 或裸数组。"""
    if isinstance(value, dict):
        return list(value.get("post") or value.get("pre") or [0, 0, 0])
    return list(value)


def sample(channel_map, t, default=0.0):
    """线性插值采样（只用于量级测量，catmullrom 与线性的峰值差异 <10%）。"""
    if not channel_map:
        return [default, default, default]
    keys = sorted((float(k), _vec(v)) for k, v in channel_map.items())
    if t <= keys[0][0]:
        return list(keys[0][1])
    if t >= keys[-1][0]:
        return list(keys[-1][1])
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            f = 0.0 if t1 == t0 else (t - t0) / (t1 - t0)
            return [v0[i] + (v1[i] - v0[i]) * f for i in range(3)]
    return list(keys[-1][1])


# 鳍的静态姿态是外展 40°（烘焙在方块自身的 rotation 上），所以标记点也要先按 40° 转过去，
# 否则测出来的永远是「绕 Z 轴转一个朝下的点」= 纯横向位移。geo 是 X 镜像的，左侧 pivot 在 +x。
_FIN_OFFSET = [20.0 * math.sin(math.radians(65)), -20.0 * math.cos(math.radians(65)), 0.0]

MARKERS = {
    "尾端(weakness)": ("vent", [0.0, 0.0, 0.0]),
    "左鳍尖": ("fin_left_tip", list(_FIN_OFFSET)),
    "右鳍尖": ("fin_right_tip", [-_FIN_OFFSET[0], _FIN_OFFSET[1], 0.0]),
    "尾扇尖": ("fan", [0.0, -14.0, 0.0]),
}


def rest_point(bone_name, offset):
    """标记点必须取「pivot + 偏移」：骨骼的局部矩阵是绕 pivot 转的。"""
    pivot = BONE[bone_name].get("pivot") or [0, 0, 0]
    return [pivot[i] + offset[i] for i in range(3)]

print("%-18s %-10s %-9s %-9s %-9s" % ("动画", "标记点", "X行程", "Y行程", "Z行程"))
print("-" * 62)
for name, anim in animations.items():
    length = anim.get("animation_length", 1.0) or 1.0
    steps = 48
    for label, (bone_name, offset) in MARKERS.items():
        pts = []
        for i in range(steps + 1):
            t = length * i / steps
            rotations, offsets = {}, {}
            for bname, chans in anim.get("bones", {}).items():
                if "rotation" in chans:
                    rotations[bname] = sample(chans["rotation"], t)
                if "position" in chans:
                    offsets[bname] = sample(chans["position"], t)
            m = chain_matrix(bone_name, rotations, offsets)
            pts.append(apply(m, rest_point(bone_name, offset)))
        span = [max(p[i] for p in pts) - min(p[i] for p in pts) for i in range(3)]
        print("%-18s %-10s %6.2f块 %6.2f块 %6.2f块" % (
            name, label, span[0] / UNITS_PER_BLOCK, span[1] / UNITS_PER_BLOCK, span[2] / UNITS_PER_BLOCK))
    print()
