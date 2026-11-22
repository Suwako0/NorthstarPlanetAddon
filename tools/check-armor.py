#!/usr/bin/env python3
""" armour pre-flight checks — 见 docs/ARMOR-PITFALLS.md

拦下这几类已经踩过的坑：
  1. 穿戴层尺寸不对
  2. 手/脚末端断面没挖空（"手上黑方块"）
  3. 装饰件完全埋在护甲壳里（壳 = 皮肤 +1.0）
  4. 手臂/腿骨上有套筒式大件（"跟着手臂摆的黑块"）
  5. 左右不互为镜像
  6. box-UV 面采样到透明像素

用法:
  python tools/check-armor.py --set molten_core
可选:
  --gen tools/gen-molten-core-armor.py   指定哪个生成脚本提供 PARTS
"""
import argparse
import importlib.util
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARMOR = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon",
                     "textures", "models", "armor")

# vanilla armour shell in HumanoidModel bone space, inflated by 1.0
SHELL = {
    "head": (-5, -9, -5, 5, 1, 5),
    "body": (-5, -1, -3, 5, 13, 3),
    "left_arm": (-2, -3, -3, 4, 11, 3),
    "right_arm": (-4, -3, -3, 2, 11, 3),
    "left_leg": (-3, -1, -3, 3, 13, 3),
    "right_leg": (-3, -1, -3, 3, 13, 3),
}

LIMB_BONES = {"left_arm", "right_arm", "left_leg", "right_leg"}
SLEEVE_PREFIX = ("plate_upperarm", "plate_bracer", "plate_thigh", "plate_shin", "trim_cuff")

# windows that must be fully transparent in layer_1 (64x32)
# 手必须露出来（露脸看设计），脚不用：靴子本来就包住脚，本套装还画了发光的靴底。
REQUIRED_HOLES_L1 = {
    "hand sides (bottom 3)": (40, 29, 56, 32),
    "hand end face": (48, 16, 52, 20),
}


def load_parts(gen_path, set_name):
    if not gen_path:
        candidate = os.path.join(HERE, "tools", f"gen-{set_name.replace('_', '-')}-armor.py")
        gen_path = candidate if os.path.exists(candidate) else None
    if not gen_path or not os.path.exists(gen_path):
        return None
    spec = importlib.util.spec_from_file_location("armorgen", gen_path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def box_faces(u, v, w, h, d):
    return {
        "up": (u + d, v, d, w),
        "down": (u + d + w, v, d, w),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h),
        "south": (u + d + w + d, v + d, w, h),
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--set", required=True)
    ap.add_argument("--gen")
    args = ap.parse_args()

    errors, warns = [], []

    # a fully transparent layer_1 means this set is 3D-only: the vanilla armour
    # shell is not drawn at all, so the shell-clearance checks do not apply
    three_d_only = False

    # ---- 1. layer sizes + 2. required holes
    for layer, holes in (("layer_1", REQUIRED_HOLES_L1), ("layer_2", {})):
        path = os.path.join(ARMOR, f"{args.set}_{layer}.png")
        if not os.path.exists(path):
            errors.append(f"{layer}: 文件不存在 {path}")
            continue
        im = Image.open(path).convert("RGBA")
        if im.size != (64, 32):
            errors.append(f"{layer}: 尺寸 {im.size}，应为 64x32")
            continue
        px = im.load()
        opaque = sum(1 for x in range(64) for y in range(32) if px[x, y][3] > 8)
        if layer == "layer_1" and opaque == 0:
            three_d_only = True
            warns.append("layer_1 全透明 → 3D-only 套装，跳过护甲壳与手部窗口检查")
            continue
        for label, (x0, y0, x1, y1) in holes.items():
            solid = sum(1 for x in range(x0, min(x1, 64)) for y in range(y0, min(y1, 32))
                        if px[x, y][3] > 8)
            if solid:
                errors.append(f"{layer}: {label} 未被挖空（{solid} 个不透明像素）→ 游戏里会看到黑色方块")

    # ---- model-side checks
    mod = load_parts(args.gen, args.set)
    if mod is None:
        warns.append("没找到生成脚本，跳过几何检查")
    else:
        parts = mod.PARTS
        # 3. buried in shell
        for name, bone, x0, y0, z0, x1, y1, z1, u, v in parts:
            if three_d_only:
                break
            s = SHELL.get(bone)
            if not s:
                continue
            if (x0 >= s[0] and x1 <= s[3] and y0 >= s[1] and y1 <= s[4]
                    and z0 >= s[2] and z1 <= s[5]):
                errors.append(f"{name}: 完全埋在 {bone} 的护甲壳里 → 看不见或闪")
        # 4. arm plates must stop above the hand (world y 12..14 → local y >= 8)
        for name, bone, x0, y0, z0, x1, y1, z1, u, v in parts:
            if bone in ("left_arm", "right_arm") and y1 > 8.2:
                errors.append(f"{name}: 手臂件下探到 y{y1:.1f}（局部），会盖住手（手 = 局部 y 8..10）")
        # 5. mirror symmetry
        by_key = {}
        for name, bone, x0, y0, z0, x1, y1, z1, u, v in parts:
            if bone in ("left_arm", "right_arm", "left_leg", "right_leg"):
                key = (bone, round(y0, 1), round(y1, 1), round(z0, 1), round(z1, 1))
                by_key.setdefault(key, []).append((round(x0, 1), round(x1, 1)))
        for key, xs in by_key.items():
            if len(xs) == 2:
                a, b = xs
                if not (abs(a[0] + b[1]) < 0.35 and abs(a[1] + b[0]) < 0.35):
                    warns.append(f"肢体件可能不对称: {key} {xs}")
        # 6. uv sampling
        tex_path = os.path.join(ARMOR, f"{args.set}_armor.png")
        if os.path.exists(tex_path):
            tex = Image.open(tex_path).convert("RGBA")
            tp = tex.load()
            for name, bone, x0, y0, z0, x1, y1, z1, u, v in parts:
                w, h, d = round(x1 - x0), round(y1 - y0), round(z1 - z0)
                for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
                    empty = total = 0
                    for yy in range(int(fy), int(fy + fh)):
                        for xx in range(int(fx), int(fx + fw)):
                            total += 1
                            if not (0 <= xx < tex.width and 0 <= yy < tex.height) or tp[xx, yy][3] < 8:
                                empty += 1
                    if total and empty / total > 0.2:
                        errors.append(f"{name}.{face}: 采样到 {empty}/{total} 个透明像素 → 游戏里是黑面")

    for w in warns:
        print("WARN ", w)
    for e in errors:
        print("ERROR", e)
    print(f"\n{args.set}: {len(errors)} error(s), {len(warns)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
