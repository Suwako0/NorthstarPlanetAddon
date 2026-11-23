#!/usr/bin/env python3
"""用「背面视角出图」当身后的翼层，合成到 P3 正面身上。

思路：背面那张图里的机甲和正面是同一个人（比例接近），所以整张背面图按身高对齐后放到
**角色身后**，正面角色会把背面的躯干完全挡住，只留两侧的翼露出来 —— 这样不需要单独裁翼，
也不会有翼根错位的问题。

用法:
  python tools/composite-ares-backwing.py --body <P3.png> --back <背面图.png> \
      --back-crop 0,379,382,758 --shield <护盾.png>
"""
import argparse
import os

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(HERE, "docs", "previews")


def alpha_sat_lum(img: Image.Image, sat_threshold: float = 0.20, lum_floor: int = 95) -> Image.Image:
    """红橙主体 + 亮部保留，深灰背景透明。"""
    arr = np.asarray(img.convert("RGB")).astype(np.float32) / 255.0
    mx, mn = arr.max(axis=2), arr.min(axis=2)
    sat = np.where(mx > 1e-6, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    lum = np.asarray(img.convert("L")).astype(np.float32)
    keep = (sat > sat_threshold) | (lum > lum_floor)
    a = (keep.astype(np.float32) * 255.0)
    return Image.fromarray(a.astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius=1.2))


def char_mask_flood(body: Image.Image, sat_threshold: float = 0.10) -> Image.Image:
    arr = np.asarray(body.convert("RGB")).astype(np.float32) / 255.0
    mx, mn = arr.max(axis=2), arr.min(axis=2)
    sat = np.where(mx > 1e-6, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    bg_like = (sat < sat_threshold).astype(np.uint8) * 255
    img = Image.fromarray(bg_like, "L").filter(ImageFilter.MedianFilter(3))
    fill = img.copy()
    for corner in [(0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)]:
        if fill.getpixel(corner) > 200:
            ImageDraw.floodfill(fill, corner, 128, thresh=60)
    exterior = fill.point(lambda v: 255 if v == 128 else 0)
    mask = Image.eval(exterior, lambda v: 255 - v)
    mask = mask.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.MinFilter(5))
    return mask.filter(ImageFilter.GaussianBlur(radius=1.2))


def soft_circle(size: int, feather: float = 0.012) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    return mask.filter(ImageFilter.GaussianBlur(radius=max(2, size * feather)))


def bubble_bounds(shield: Image.Image, threshold: int = 95):
    arr = np.asarray(shield.convert("L")).astype(np.int16)
    ys, xs = np.where(arr > threshold)
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--body", required=True)
    ap.add_argument("--back", required=True)
    ap.add_argument("--back-crop")
    ap.add_argument("--shield")
    ap.add_argument("--shield-opacity", type=float, default=0.5)
    ap.add_argument("--back-scale", type=float, default=1.0, help="额外缩放")
    ap.add_argument("--back-width-frac", type=float, default=0.92, help="缩放后占画面宽度比例")
    ap.add_argument("--back-dy", type=float, default=0.0, help="上下微调，占角色身高比例")
    ap.add_argument("--name", default="ares-p3-backwing")
    args = ap.parse_args()

    body = Image.open(args.body).convert("RGB")
    back = Image.open(args.back).convert("RGB")
    if args.back_crop:
        back = back.crop(tuple(int(v) for v in args.back_crop.split(",")))

    bmask = char_mask_flood(body)
    cx0, cy0, cx1, cy1 = bmask.getbbox()
    char_w, char_h = cx1 - cx0, cy1 - cy0
    ccx, ccy = (cx0 + cx1) // 2, (cy0 + cy1) // 2

    b_alpha = alpha_sat_lum(back)
    bb = b_alpha.getbbox()
    if not bb:
        raise SystemExit("背面图抠不出内容，检查 --back-crop 或阈值")
    back_c = back.crop(bb)
    ba_c = b_alpha.crop(bb)

    # 默认按「画面宽度」缩放（height-match 会把裁出来的翼区放大到离谱）
    scale = (body.width * args.back_width_frac / back_c.width) * args.back_scale
    w2, h2 = int(back_c.width * scale), int(back_c.height * scale)
    back_r = back_c.resize((w2, h2), Image.LANCZOS)
    ba_r = ba_c.resize((w2, h2), Image.LANCZOS)

    # 脚底对齐：背面图底边 -> 角色底边（cy1）
    px = ccx - w2 // 2
    py = cy0 + int(char_h * args.back_dy)

    base = body.copy()
    base.paste(back_r, (px, py), ba_r)

    # 正面角色盖回（只露两侧的翼）
    body_layer = body.copy()
    base = Image.composite(body_layer, base, bmask)

    # 护盾
    if args.shield and args.shield_opacity > 0.001:
        shield = Image.open(args.shield).convert("RGB")
        diameter = int(char_h * 1.10)
        sx0, sy0, sx1, sy1 = bubble_bounds(shield)
        bubble = shield.crop((sx0, sy0, sx1, sy1)).resize((diameter, diameter), Image.LANCZOS)
        bubble = Image.eval(bubble, lambda v: int(v * 0.9))
        origin = (ccx - diameter // 2, ccy - diameter // 2)
        layer = Image.new("RGB", base.size, (0, 0, 0))
        layer.paste(bubble, origin)
        a = Image.new("L", base.size, 0)
        a.paste(soft_circle(diameter), origin)
        screened = ImageChops.screen(base, layer)
        base = Image.composite(screened, base, a.point(lambda v: int(v * args.shield_opacity)))

    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, f"{args.name}.png")
    base.save(path)
    print(f"正面 {char_w}x{char_h}；背面缩放 {scale:.3f} -> {w2}x{h2}；位置 ({px},{py})")
    print("wrote", os.path.relpath(path, HERE))


if __name__ == "__main__":
    main()
