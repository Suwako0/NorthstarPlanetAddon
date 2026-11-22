#!/usr/bin/env python3
"""把 minecraft-geckolib-animation-delivery 技能挂到 Minecraft 专家 preset 的 Iron rule 1 里。

只做三件事：
  1. "Four skills" -> "Five skills"
  2. 在技能清单里插入新技能的条目（保持同样的缩进）
  3. 在 rule 2 之前加一段：做实体 / Boss / 动画物品时必须额外加载这个技能

文件是 UTF-8，纯 Python 改写，避免经过 Windows 控制台导致中文变乱码。
"""
import io
import os
import shutil
import sys

PRESET = r"C:\Users\55632\.dsh\profiles\desktop\node_modules\dsh-minecraft-expert-preset"
YML = os.path.join(PRESET, "cordis.patch.yml")
SKILL_SRC = r"C:\Users\55632\.dsh\agent-skills\minecraft\minecraft-geckolib-animation-delivery"
SKILL_DST = os.path.join(PRESET, "skills", "minecraft-geckolib-animation-delivery")

BULLET = (
    "                   - minecraft-geckolib-animation-delivery — **动画落地**：把 bbmodel 的骨骼动画"
    "真正交付到游戏里（GeckoLib 缓存扫不到时自读兜底、单控制器、动画物品的 builtin/entity 与"
    "以原点为中心的 geo、近黑贴图渲染成黑块）、动作幅度与招式区分硬指标、超大体积实体的直驱移动"
    "+ 同步位、以及读日志定位“模型不动”。\n"
)

EXTRA_RULE = (
    "\n                   Entity / Boss / animated-item work additionally loads\n"
    "                   minecraft-geckolib-animation-delivery before touching animation controllers,\n"
    "                   export scripts or keyframe amplitudes: it is the authority on why a model that\n"
    "                   renders fine can still stand frozen in game.\n"
)


def main():
    text = io.open(YML, encoding="utf-8").read()
    original = text

    if "Four skills are mounted" in text:
        text = text.replace("Four skills are mounted", "Five skills are mounted", 1)

    anchor = "                   - minecraft-item-icon-art"
    if "minecraft-geckolib-animation-delivery" not in text:
        if anchor not in text:
            sys.exit("找不到技能清单锚点，未改动")
        text = text.replace(anchor, BULLET + anchor, 1)

    rule_anchor = "                2. Signatures, constants and asset paths come from sources, never memory."
    if "additionally loads" not in text:
        if rule_anchor not in text:
            sys.exit("找不到 rule 2 锚点，未改动")
        text = text.replace(rule_anchor, EXTRA_RULE + rule_anchor, 1)

    if text == original:
        print("已是最新，无需改动")
    else:
        backup = YML + ".bak-skill"
        shutil.copyfile(YML, backup)
        io.open(YML, "w", encoding="utf-8", newline="\n").write(text)
        print(f"已更新 {YML}（备份 {os.path.basename(backup)}）")

    if os.path.isdir(SKILL_SRC):
        os.makedirs(os.path.dirname(SKILL_DST), exist_ok=True)
        if os.path.isdir(SKILL_DST):
            shutil.rmtree(SKILL_DST)
        shutil.copytree(SKILL_SRC, SKILL_DST)
        print(f"已复制技能到 {SKILL_DST}")

    print("校验: Five skills =", "Five skills are mounted" in text,
          "| 条目数 =", text.count("minecraft-geckolib-animation-delivery"))


if __name__ == "__main__":
    main()
