#!/usr/bin/env python3
"""从阿瑞斯的 P2 主体贴图生成三阶段变体（同一套 UV，只换色板）。

阶段映射（对应 BOSS-ART-BRIEF §5.4）：
  P1 脆化  <500℃ ：降饱和 + 偏冷青灰，熔光压暗成暗红余烬
  P2 常态  500~1500℃ ：原图（ares.png）
  P3 狂暴  >1500℃ ：整体偏赤红、熔光提亮到金白、阴影加深

做法是逐像素色相/明度重映射（不改 UV、不改 alpha），保证与 geo 完全对齐。

用法: python tools/gen-ares-phases.py
"""
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon", "textures", "entity")


def rgb_to_hsv(arr):
    """arr: HxWx3 float 0..1 → h(0..360), s, v"""
    r, g, b = arr[..., 0], arr[..., 1], arr[..., 2]
    mx = arr.max(axis=-1)
    mn = arr.min(axis=-1)
    diff = mx - mn
    h = np.zeros_like(mx)
    mask = diff > 1e-6
    rmask = mask & (mx == r)
    gmask = mask & (mx == g) & ~rmask
    bmask = mask & (mx == b) & ~rmask & ~gmask
    h[rmask] = ((g[rmask] - b[rmask]) / diff[rmask]) % 6
    h[gmask] = ((b[gmask] - r[gmask]) / diff[gmask]) + 2
    h[bmask] = ((r[bmask] - g[bmask]) / diff[bmask]) + 4
    h = (h * 60.0) % 360.0
    s = np.where(mx > 1e-6, diff / np.maximum(mx, 1e-6), 0.0)
    return h, s, mx


def hsv_to_rgb(h, s, v):
    h = (h % 360.0) / 60.0
    i = np.floor(h).astype(int) % 6
    f = h - np.floor(h)
    p = v * (1 - s)
    q = v * (1 - s * f)
    t = v * (1 - s * (1 - f))
    out = np.zeros(h.shape + (3,))
    for idx, (r_, g_, b_) in enumerate([(v, t, p), (q, v, p), (p, v, t), (p, q, v), (t, p, v), (v, p, q)]):
        m = i == idx
        out[m, 0], out[m, 1], out[m, 2] = r_[m], g_[m], b_[m]
    return np.clip(out, 0, 1)


def remap(img: Image.Image, mode: str) -> Image.Image:
    arr = np.asarray(img.convert("RGBA")).astype(np.float32) / 255.0
    rgb, a = arr[..., :3], arr[..., 3]
    h, s, v = rgb_to_hsv(rgb)
    if mode == "p1":
        # 冷灰：色相拉到 200~215，饱和压到 0.12~0.18，明度略降；熔光（高亮暖色）保留一点暗红
        hot = (v > 0.55) & (s > 0.35)
        h_new = np.full_like(h, 208.0)
        s_new = np.clip(s * 0.22, 0.02, 0.16)
        v_new = v * 0.86
        # 熔核/缝隙：压成暗红余烬
        h_new = np.where(hot, 8.0, h_new)
        s_new = np.where(hot, 0.62, s_new)
        v_new = np.where(hot, np.clip(v * 0.42, 0.1, 0.35), v_new)
        rgb2 = hsv_to_rgb(h_new, s_new, v_new)
    elif mode == "p3":
        # 赤红 + 白热熔光
        hot = (v > 0.5) & (s > 0.3)
        h_new = np.where(s > 0.05, np.clip(h * 0.55, 0.0, 14.0), h)
        s_new = np.clip(s * 1.18, 0, 1)
        v_new = np.clip(v * 1.12, 0, 1)
        # 熔光推到金白
        h_new = np.where(hot, 34.0, h_new)
        s_new = np.where(hot, 0.55, s_new)
        v_new = np.where(hot, np.clip(v * 1.45, 0, 1), v_new)
        rgb2 = hsv_to_rgb(h_new, s_new, v_new)
    else:
        rgb2 = rgb
    out = np.concatenate([rgb2, a[..., None]], axis=-1)
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


def main():
    base_path = os.path.join(TEX_DIR, "ares.png")
    base = Image.open(base_path).convert("RGBA")
    mapping = {"ares_phase1.png": "p1", "ares_phase2.png": "p2", "ares_phase3.png": "p3"}
    for name, mode in mapping.items():
        out = remap(base, mode) if mode != "p2" else base
        path = os.path.join(TEX_DIR, name)
        out.save(path)
        print("wrote", os.path.relpath(path, HERE), out.size)


if __name__ == "__main__":
    main()
