#!/usr/bin/env python3
"""生成阿瑞斯的护盾壳贴图：满幅半透明六边形能量网格。

为什么满幅：新的护盾几何（16 块面板 + 上下盖）UV 铺满整张 256×512，
ShieldLayer 用这张独立贴图重画 `shield` 骨 —— 只要整张都是半透明网格，
任何 UV 都能采到正确的能量膜，不必再逐面标记。

用法: python tools/gen-ares-shield-tex.py
"""
import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon",
                   "textures", "entity", "ares_shield.png")
W, H = 256, 512


def main():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    # 底色：淡青蓝半透明
    d.rectangle([0, 0, W, H], fill=(90, 200, 255, 46))
    # 六边形网格
    r = 16
    dx = r * 1.5
    dy = r * math.sqrt(3)
    for col in range(int(W / dx) + 2):
        for row in range(int(H / dy) + 2):
            cx = col * dx
            cy = row * dy + (dy / 2 if col % 2 else 0)
            pts = [(cx + r * math.cos(math.radians(60 * i)),
                    cy + r * math.sin(math.radians(60 * i))) for i in range(6)]
            d.polygon(pts, outline=(180, 240, 255, 190))
    # 亮边（面板外沿更亮）
    d.rectangle([0, 0, W - 1, H - 1], outline=(220, 250, 255, 150), width=2)
    img.save(OUT)
    print("wrote", os.path.relpath(OUT, HERE), img.size)


if __name__ == "__main__":
    main()
