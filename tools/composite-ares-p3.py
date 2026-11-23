#!/usr/bin/env python3
"""P3 全身合成：背景延展 + 双翼（身后）+ 角色 + 护盾。

图层顺序（从后到前）：延展背景 → 左右翼 → 角色（带抠像蒙版）→ 六边形护盾。

用法:
  python tools/composite-ares-p3.py --body <角色.png> --shield <护盾.png> \
      --wing <翅膀.png> --wing-crop 0,0,506,506 --wing-scale 0.62 --wing-dy 0.16
"""
import argparse
import os

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(HERE, "docs", "previews")


# ---------------------------------------------------------------- 工具

def border_color(body: Image.Image):
    arr = np.asarray(body.convert("RGB"))
    edge = np.concatenate([arr[0:4].reshape(-1, 3), arr[-4:].reshape(-1, 3),
                           arr[:, 0:4].reshape(-1, 3), arr[:, -4:].reshape(-1, 3)])
    return tuple(int(v) for v in np.median(edge, axis=0))


def stretched_backdrop(body: Image.Image, size) -> Image.Image:
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


def bubble_bounds(shield: Image.Image, threshold: int = 95):
    arr = np.asarray(shield.convert("L")).astype(np.int16)
    ys, xs = np.where(arr > threshold)
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1


def character_mask(body: Image.Image, sat_threshold: float = 0.10) -> Image.Image:
    """从四角泛洪求「外部背景」，取反得到角色蒙版。

    背景是灰色（饱和很低、明度居中），角色是红橙高饱和或近黑：先把「像背景」的像素标出来，
    再从四个角泛洪 —— 能连通到画面边缘的才算外部，其余（含角色内部深色孔洞）都算角色。
    """
    arr = np.asarray(body.convert("RGB")).astype(np.float32) / 255.0
    mx, mn = arr.max(axis=2), arr.min(axis=2)
    sat = np.where(mx > 1e-6, (mx - mn) / np.maximum(mx, 1e-6), 0.0)
    bg_like = (sat < sat_threshold).astype(np.uint8) * 255        # 可能是背景
    bg_img = Image.fromarray(bg_like, "L")
    bg_img = bg_img.filter(ImageFilter.MedianFilter(3))

    fill = bg_img.copy()
    for corner in [(0, 0), (bg_img.width - 1, 0), (0, bg_img.height - 1), (bg_img.width - 1, bg_img.height - 1)]:
        if fill.getpixel(corner) > 200:
            ImageDraw.floodfill(fill, corner, 128, thresh=60)
    exterior = fill.point(lambda v: 255 if v == 128 else 0)
    mask = Image.eval(exterior, lambda v: 255 - v)
    mask = mask.filter(ImageFilter.MaxFilter(5)).filter(ImageFilter.MinFilter(5))
    return mask.filter(ImageFilter.GaussianBlur(radius=1.2))


def wing_alpha(wing: Image.Image, low: int = 30, high: int = 62) -> Image.Image:
    """深灰底 → 透明：亮度低于 low 全透明，高于 high 全不透明，中间线性过渡。"""
    lum = np.asarray(wing.convert("L")).astype(np.float32)
    a = np.clip((lum - low) / max(1.0, high - low), 0.0, 1.0) * 255.0
    return Image.fromarray(a.astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius=1.0))


# ---------------------------------------------------------------- 主流程

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--body", required=True)
    ap.add_argument("--shield", required=True)
    ap.add_argument("--wing")
    ap.add_argument("--wing-crop", default="0,0,506,506")
    ap.add_argument("--wing-scale", type=float, default=0.62, help="单翼宽度 = 角色身高 × 该值")
    ap.add_argument("--wing-dy", type=float, default=0.10, help="翼根相对角色顶部的下移（占身高比例）")
    ap.add_argument("--wing-gap", type=float, default=-0.06, help="翼内缘相对角色中线的水平偏移（占角色宽比例）")
    ap.add_argument("--shield-opacity", type=float, default=0.55)
    ap.add_argument("--name", default="ares-p3")
    ap.add_argument("--wing-alpha-low", type=int, default=30)
    ap.add_argument("--wing-alpha-high", type=int, default=62)
    args = ap.parse_args()

    body = Image.open(args.body).convert("RGB")
    shield = Image.open(args.shield).convert("RGB")

    bmask = character_mask(body)
    coverage = sum(bmask.getdata()) / 255.0 / (body.width * body.height)
    print(f"蒙版覆盖率 {coverage:.1%}（正常 15%~45%；接近 100% 说明抠图失败）")
    bmask.save(os.path.join(OUT_DIR, "debug-mask.png"))
    bbox = bmask.getbbox()
    cx0, cy0, cx1, cy1 = bbox
    char_w, char_h = cx1 - cx0, cy1 - cy0
    ccx, ccy = (cx0 + cx1) // 2, (cy0 + cy1) // 2

    # 不延展画布：直接用原图当底，只叠「翼 + 护盾」。
    # 之前试过扩画布装整个气泡，但背景延展的接缝怎么调都看得见，得不偿失 —— 护盾两侧被画框裁掉反而更像游戏截图。
    diameter = int(char_h * 1.10)
    canvas_w, canvas_h = body.size
    base = body.copy()
    off_x = off_y = 0



    # ---- 双翼（放在角色后面）
    if args.wing:
        wing = Image.open(args.wing).convert("RGB")
        x0, y0, x1, y1 = (int(v) for v in args.wing_crop.split(","))
        wing = wing.crop((x0, y0, x1, y1))
        wa = wing_alpha(wing, args.wing_alpha_low, args.wing_alpha_high)
        wbox = wa.getbbox()
        if wbox:
            wing, wa = wing.crop(wbox), wa.crop(wbox)
        scale_w = int(char_h * args.wing_scale)
        scale_h = int(wing.height * scale_w / wing.width)
        wing = wing.resize((scale_w, scale_h), Image.LANCZOS)
        wa = wa.resize((scale_w, scale_h), Image.LANCZOS)

        root_y = off_y + cy0 + int(char_h * args.wing_dy)
        gap = int(char_w * args.wing_gap)          # 负数=往中线里塞
        right_x = off_x + ccx + gap
        left_wing = wing.transpose(Image.FLIP_LEFT_RIGHT)
        left_alpha = wa.transpose(Image.FLIP_LEFT_RIGHT)
        base.paste(wing, (right_x, root_y), wa)
        base.paste(left_wing, (right_x - scale_w, root_y), left_alpha)

    # ---- 角色（盖住翼根）
    body_layer = Image.new("RGB", base.size, (0, 0, 0))
    body_layer.paste(body, (off_x, off_y))
    full_mask = Image.new("L", base.size, 0)
    full_mask.paste(bmask, (off_x, off_y))
    base = Image.composite(body_layer, base, full_mask)

    # ---- 护盾（--shield-opacity 0 表示不加护盾，只留翼）
    if args.shield_opacity <= 0.001:
        os.makedirs(OUT_DIR, exist_ok=True)
        path = os.path.join(OUT_DIR, f"{args.name}-full.png")
        base.save(path)
        print("wrote (no shield)", os.path.relpath(path, HERE))
        return
    sx0, sy0, sx1, sy1 = bubble_bounds(shield)
    bubble = shield.crop((sx0, sy0, sx1, sy1)).resize((diameter, diameter), Image.LANCZOS)
    bubble = Image.eval(bubble, lambda v: int(v * 0.9))
    origin = (off_x + ccx - diameter // 2, off_y + ccy - diameter // 2)
    layer = Image.new("RGB", base.size, (0, 0, 0))
    layer.paste(bubble, origin)
    alpha = Image.new("L", base.size, 0)
    alpha.paste(soft_circle(diameter), origin)
    screened = ImageChops.screen(base, layer)
    out = Image.composite(screened, base, alpha.point(lambda v: int(v * args.shield_opacity)))

    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, f"{args.name}-full.png")
    out.save(path)
    print(f"角色 {char_w}x{char_h}，画布 {canvas_w}x{canvas_h}，护盾直径 {diameter}")
    print("wrote", os.path.relpath(path, HERE))


if __name__ == "__main__":
    main()
