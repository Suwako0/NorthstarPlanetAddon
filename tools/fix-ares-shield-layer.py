#!/usr/bin/env python3
"""阿瑞斯护盾的两件收尾工作：

1. **橙黄色半透明六边形**护盾贴图（替换原来的青蓝）；
2. 把主体贴图（`ares.png` 与三阶段变体）里**护盾骨占用的 UV 区域擦成全透明** ——
   这样主渲染通道画不到护盾，只有 `ShieldLayer`（用 `ares_shield.png`）在护盾展开时
   才把它画出来，实现「没护盾时只能看到本体」。

UV 区域来自 geo 里每个 `shield_*` 方块的 box UV：
   宽 = 2*(size_x + size_z)，高 = size_y + size_z，起点 = uv
"""
import json
import math
import os

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon")
GEO = os.path.join(ASSETS, "geo", "entity", "ares.geo.json")
TEX_DIR = os.path.join(ASSETS, "textures", "entity")


def shield_uv_rects():
    geo = json.load(open(GEO, encoding="utf-8"))["minecraft:geometry"][0]
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    rects = []
    for bone in geo["bones"]:
        if bone["name"] != "shield":
            continue
        for cube in bone.get("cubes", []):
            u, v = cube["uv"][0], cube["uv"][1]
            w = 2 * (cube["size"][0] + cube["size"][2])
            h = cube["size"][1] + cube["size"][2]
            rects.append((u, v, u + w, v + h))
    return rects, (tw, th)


def clear_shield_uvs(path, rects):
    img = Image.open(path).convert("RGBA")
    px = img.load()
    w, h = img.size
    for (x0, y0, x1, y1) in rects:
        for x in range(max(0, int(x0) - 1), min(w, int(math.ceil(x1)) + 1)):
            for y in range(max(0, int(y0) - 1), min(h, int(math.ceil(y1)) + 1)):
                r, g, b, _ = px[x, y]
                px[x, y] = (r, g, b, 0)
    img.save(path)
    return img.size


def make_shield_tex(size):
    w, h = size
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    d.rectangle([0, 0, w, h], fill=(255, 170, 60, 52))          # 橙黄底
    r = 15
    dx = r * 1.5
    dy = r * math.sqrt(3)
    for col in range(int(w / dx) + 2):
        for row in range(int(h / dy) + 2):
            cx = col * dx
            cy = row * dy + (dy / 2 if col % 2 else 0)
            pts = [(cx + r * math.cos(math.radians(60 * i)),
                    cy + r * math.sin(math.radians(60 * i))) for i in range(6)]
            d.polygon(pts, outline=(255, 226, 150, 200))
    d.rectangle([0, 0, w - 1, h - 1], outline=(255, 240, 190, 160), width=2)
    img.save(os.path.join(TEX_DIR, "ares_shield.png"))
    return img.size


def main():
    rects, size = shield_uv_rects()
    print(f"护盾 UV 区域 {len(rects)} 块，贴图 {size}")
    for name in ("ares.png", "ares_phase2.png", "ares_phase1.png", "ares_phase3.png"):
        path = os.path.join(TEX_DIR, name)
        if os.path.exists(path):
            clear_shield_uvs(path, rects)
            print("已擦除护盾 UV:", name)
    print("护盾贴图:", make_shield_tex(size))


if __name__ == "__main__":
    main()
