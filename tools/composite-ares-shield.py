#!/usr/bin/env python3
"""把六边形力场气泡合成到角色图上（等价 PS：延展画布 + Screen + 不透明度）。

关键修正（第一版翻车的地方）：
  * 背景中位色只取**画面边框像素**，不取整幅（否则会混进红色角色 → 延展区发褐）；
  * 延展区用**左右边缘列拉伸 + 横向模糊**做渐变，和原背景的灰色地面接得上，不留矩形接缝；
  * 角色包围盒用**百分位**（2%~98%），忽略脚底熔渣这类稀疏像素；
  * 气泡直径只比身高大 4%，并把罩面亮度压到 0.9，避免白热边缘把角色洗掉。

用法:
  python tools/composite-ares-shield.py --body <角色.png> --shield <护盾.png>
"""
import argparse
import math
import os

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(HERE, "docs", "previews")


def bubble_bounds(shield: Image.Image, threshold: int = 95):
    arr = np.asarray(shield.convert("L")).astype(np.int16)
    ys, xs = np.where(arr > threshold)
    if len(xs) < 1000:
        raise SystemExit("护盾图里没找到足够亮的区域")
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1


def character_bounds(body: Image.Image, sat_threshold: float = 0.14):
    """按饱和度找角色（背景是灰色），再用 2%~98% 百分位去掉脚底稀疏熔渣。"""
    small = body.resize((body.width // 2, body.height // 2), Image.BILINEAR)
    arr = np.asarray(small.convert("RGB")).astype(np.float32) / 255.0
    mx, mn = arr.max(axis=2), arr.min(axis=2)
    sat = np.where(mx > 1e-6, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    mask = (sat > sat_threshold) | (mx < 0.16)
    ys, xs = np.where(mask)
    if len(xs) < 500:
        raise SystemExit("没找到角色区域")
    x0, x1 = np.percentile(xs, [1.5, 98.5])
    y0, y1 = np.percentile(ys, [1.0, 99.0])
    return int(x0 * 2), int(y0 * 2), int(x1 * 2), int(y1 * 2)


def border_color(body: Image.Image):
    """只取四周边框像素的中位色，作为延展背景的底色。"""
    arr = np.asarray(body.convert("RGB"))
    edge = np.concatenate([
        arr[0:4].reshape(-1, 3), arr[-4:].reshape(-1, 3),
        arr[:, 0:4].reshape(-1, 3), arr[:, -4:].reshape(-1, 3),
    ])
    return tuple(int(v) for v in np.median(edge, axis=0))


def stretched_backdrop(body: Image.Image, size) -> Image.Image:
    """左右边缘列拉伸成渐变：保留地面的明暗过渡，横向模糊消除接缝。"""
    w, h = size
    left = body.crop((0, 0, 6, body.height)).resize((w // 2 + 4, h), Image.BILINEAR)
    right = body.crop((body.width - 6, 0, body.width, body.height)).resize((w // 2 + 4, h), Image.BILINEAR)
    canvas = Image.new("RGB", (w, h))
    canvas.paste(left, (0, 0))
    canvas.paste(right, (w - left.width, 0))
    return canvas.filter(ImageFilter.GaussianBlur(radius=26))


def feathered_rect(size, feather: int) -> Image.Image:
    mask = Image.new("L", size, 0)
    inner = Image.new("L", (max(1, size[0] - 2 * feather), max(1, size[1] - 2 * feather)), 255)
    mask.paste(inner, (feather, feather))
    return mask.filter(ImageFilter.GaussianBlur(radius=feather))


def soft_circle(size: int, feather: float = 0.012) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask.filter(ImageFilter.GaussianBlur(radius=max(2, size * feather)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--body", required=True)
    ap.add_argument("--shield", required=True)
    ap.add_argument("--name", default="ares-p3-shield")
    args = ap.parse_args()

    body = Image.open(args.body).convert("RGB")
    shield = Image.open(args.shield).convert("RGB")

    sx0, sy0, sx1, sy1 = bubble_bounds(shield)
    bubble_src = shield.crop((sx0, sy0, sx1, sy1))

    cx0, cy0, cx1, cy1 = character_bounds(body)
    char_w, char_h = cx1 - cx0, cy1 - cy0

    diameter = int(char_h * 1.04)
    canvas_w = int(diameter * 1.18)
    canvas_h = int(diameter * 1.16)

    backdrop = stretched_backdrop(body, (canvas_w, canvas_h))
    base_col = border_color(body)
    backdrop = Image.blend(backdrop, Image.new("RGB", (canvas_w, canvas_h), base_col), 0.35)

    off_x = (canvas_w - body.width) // 2
    off_y = (canvas_h - body.height) // 2
    backdrop.paste(body, (off_x, off_y), feathered_rect(body.size, max(10, body.width // 45)))
    base = backdrop

    bubble = bubble_src.resize((diameter, diameter), Image.LANCZOS)
    bubble = Image.eval(bubble, lambda v: int(v * 0.9))  # 罩面整体压暗，防白热边缘过曝

    new_ccx = off_x + (cx0 + cx1) // 2
    new_ccy = off_y + (cy0 + cy1) // 2
    origin = (new_ccx - diameter // 2, new_ccy - diameter // 2)

    layer = Image.new("RGB", base.size, (0, 0, 0))
    layer.paste(bubble, origin)
    alpha = Image.new("L", base.size, 0)
    alpha.paste(soft_circle(diameter), origin)

    screened = ImageChops.screen(base, layer)

    os.makedirs(OUT_DIR, exist_ok=True)
    written = []
    for opacity in (0.55, 0.68):
        eff = alpha.point(lambda v, o=opacity: int(v * o))
        out = Image.composite(screened, base, eff)
        path = os.path.join(OUT_DIR, f"{args.name}-{int(opacity * 100)}.png")
        out.save(path)
        written.append(path)

    shield_asset = os.path.join(OUT_DIR, f"{args.name}-shield-alpha.png")
    rgba = bubble.convert("RGBA")
    rgba.putalpha(soft_circle(diameter))
    rgba.save(shield_asset)
    written.append(shield_asset)

    plain = os.path.join(OUT_DIR, f"{args.name}-body-padded.png")
    base.save(plain)
    written.append(plain)

    print(f"角色 bbox {char_w}x{char_h}，背景边框色 {base_col}")
    print(f"气泡直径 {diameter}px，画布 {canvas_w}x{canvas_h}")
    for p in written:
        print("wrote", os.path.relpath(p, HERE))


if __name__ == "__main__":
    main()
