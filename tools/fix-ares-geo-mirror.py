#!/usr/bin/env python3
"""把导出的 ares geo 的 X 轴还原成「建模空间」。

背景：`new_project(geckolib_model)` 这个工程被 Blockbench 的 bedrock 编解码器导出时把 X 取反了
（`spike_l` 出现在 +10.5、`arm_l_upper` 出现在 +10.5），而动画是用 `tools/bbmodel-to-geckolib.py`
按建模空间生成的 → 两边的左右/旋转方向不一致，翅膀扇动会左右反向。

这里把每个骨头的 pivot 与每个方块的 origin 沿 X 镜像回来（size 不变），动画无需改动。
幂等：脚本只处理带 `"mirrored_back": true` 之外的调用由调用方保证只跑一次。
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEO = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon",
                   "geo", "entity", "ares.geo.json")
MARK = os.path.join(HERE, "build", ".ares_geo_mirrored")


def main():
    data = json.load(open(GEO, encoding="utf-8"))
    geo = data["minecraft:geometry"][0]
    bones = geo["bones"]
    # 用第一条骨头的 pivot 判断是否已经镜像过（pivot X 全为正=未镜像的异常状态）
    if all((b.get("pivot") or [0])[0] >= 0 for b in bones if b["name"].endswith("_l")):
        pass  # 仍然处理，幂等由 mark 文件控制
    for b in bones:
        if b.get("pivot"):
            b["pivot"][0] = round(-b["pivot"][0], 4)
        for c in b.get("cubes", []):
            ox, oz = c["origin"][0], c["origin"][2]
            sx = c["size"][0]
            c["origin"] = [round(-(ox + sx), 4), c["origin"][1], oz]
            if c.get("pivot"):
                c["pivot"][0] = round(-c["pivot"][0], 4)
            # 镜像时旋转也要一起镜像：绕 Y / Z 的角度取反，绕 X 不变
            rot = c.get("rotation")
            if rot:
                c["rotation"] = [rot[0], round(-rot[1], 4), round(-rot[2], 4)]
    with open(GEO, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print("mirrored ares geo X back to authoring space")


if __name__ == "__main__":
    main()
